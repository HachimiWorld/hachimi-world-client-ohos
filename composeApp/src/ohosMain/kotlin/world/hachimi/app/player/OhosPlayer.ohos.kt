@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package world.hachimi.app.player

import cnames.structs.OH_AVPlayer
import io.github.vinceglb.filekit.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.AVCodecKit.Core.*
import platform.AVCodecKit.Core.OH_AVErrCode.*
import platform.MediaKit.AVPlayer.*
import platform.MediaKit.AVPlayer.AVPlayerState.*
import platform.MediaKit.AVPlayer.AVPlayerSeekMode.*
import platform.MediaKit.AVPlayer.AVPlayerOnInfoType.*
import platform.posix.*
import world.hachimi.app.logging.Logger
import world.hachimi.app.storage.writePrivateFile
import kotlin.time.Duration.Companion.milliseconds

/** Native AVPlayer owns decoding/audio focus; commands and polling are serialized off the UI thread. */
class OhosPlayer : PlayerEngine {
    // PlayerService downloads remote tracks before prepare when this is false.
    override val supportRemotePlay: Boolean = false
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val listeners = MutableStateFlow<List<PlayerEngine.Listener>>(emptyList())
    private var polling: Job? = null
    private var initialized = false
    private var engine: CPointer<OH_AVPlayer>? = null
    private var callbacks: StableRef<PlayerCallbacks>? = null
    private var sourceFd = -1
    private var sourcePath: String? = null
    private var volume = 1f
    private var replayGain = 0f
    private var replayGainEnabled = true
    private var lastState: AVPlayerState? = null
    private var reportedError: String? = null

    override suspend fun initialize() = command {
        if (!initialized) {
            initialized = true
            polling = scope.launch {
                while (isActive) {
                    delay(100)
                    mutex.withLock {
                        if (engine != null) {
                            try {
                                reportState()
                            } catch (error: Exception) {
                                reportError(error.message ?: "Cannot read player state")
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun isReady(): Boolean = command { initialized }
    override suspend fun isPlaying(): Boolean = command { engine?.let { OH_AVPlayer_IsPlaying(it) } ?: false }
    override suspend fun isEnd(): Boolean = command { engine != null && state() == AV_COMPLETED }
    override suspend fun currentPosition(): Long = command {
        if (engine == null) 0L else position()
    }
    override suspend fun getVolume(): Float = command { volume }

    override suspend fun bufferedProgress(): Float = command {
        if (engine != null && state() in playableStates) 1f else 0f
    }

    override suspend fun stop() = command {
        releaseEngine()
        emit(PlayEvent.Pause)
    }

    override suspend fun setReplayGainEnabled(enabled: Boolean) = command {
        replayGainEnabled = enabled
        if (engine != null && state() in playableStates) applyVolume()
    }

    override suspend fun setVolume(value: Float) = command {
        require(value.isFinite()) { "Volume must be finite" }
        volume = value.coerceIn(0f, 1f)
        if (engine != null && state() in playableStates) applyVolume()
    }

    override suspend fun prepare(item: SongItem, autoPlay: Boolean) {
        require(item is SongItem.Local) { "OhosPlayer requires downloaded audio" }
        initialize()
        command {
            require(item.audioBytes.isNotEmpty()) { "Audio is empty" }
            require(item.replayGainDB.isFinite()) { "Replay gain must be finite" }
            releaseEngine()
            try {
                // mkstemp prevents collisions between player instances and concurrent track changes.
                sourcePath = memScoped {
                    val template = "${FileKit.cacheDir.path}/hachimi-player-XXXXXX".cstr.getPointer(this)
                    val fd = mkstemp(template)
                    check(fd >= 0) { "Cannot create audio source: errno=$errno" }
                    close(fd)
                    template.toKString()
                }
                writePrivateFile(checkNotNull(sourcePath), item.audioBytes)
                sourceFd = open(checkNotNull(sourcePath), O_RDONLY)
                check(sourceFd >= 0) { "Cannot open audio source: errno=$errno" }
                engine = checkNotNull(OH_AVPlayer_Create()) { "Cannot create AVPlayer" }
                callbacks = StableRef.create(PlayerCallbacks())
                checkResult(OH_AVPlayer_SetOnErrorCallback(engine, staticCFunction { _, code, message, data ->
                    data?.asStableRef<PlayerCallbacks>()?.get()?.error?.value =
                        "AVPlayer error $code: ${message?.toKString().orEmpty()}"
                }, callbacks!!.asCPointer()), "register error callback")
                checkResult(OH_AVPlayer_SetOnInfoCallback(engine, staticCFunction { _, type, _, data ->
                    if (type == AV_INFO_TYPE_SEEKDONE) {
                        data?.asStableRef<PlayerCallbacks>()?.get()?.seeks?.update { it + 1 }
                    }
                }, callbacks!!.asCPointer()), "register info callback")
                checkResult(OH_AVPlayer_SetFDSource(engine, sourceFd, 0, item.audioBytes.size.toLong()), "set source")
                awaitState(setOf(AV_INITIALIZED))
                checkResult(OH_AVPlayer_Prepare(engine), "prepare")
                awaitState(setOf(AV_PREPARED))
                replayGain = item.replayGainDB
                applyVolume()
                if (autoPlay) {
                    checkResult(OH_AVPlayer_Play(engine), "play")
                    awaitState(setOf(AV_PLAYING))
                }
                reportState()
            } catch (error: Throwable) {
                // Keep native teardown deterministic even when a track request is cancelled.
                releaseEngine()
                throw error
            }
        }
    }

    override suspend fun play() = command {
        if (engine != null && state() in playableStates && state() != AV_PLAYING) {
            checkResult(OH_AVPlayer_Play(engine), "play")
            awaitState(setOf(AV_PLAYING))
            reportState()
        }
    }

    override suspend fun pause() = command {
        if (engine != null && state() == AV_PLAYING) {
            checkResult(OH_AVPlayer_Pause(engine), "pause")
            awaitState(setOf(AV_PAUSED))
            reportState()
        }
    }

    override suspend fun seek(position: Long, autoStart: Boolean) = command {
        if (engine != null && state() in playableStates) {
            val duration = memScoped {
                val result = alloc<IntVar>()
                checkResult(OH_AVPlayer_GetDuration(engine, result.ptr), "get duration")
                result.value.coerceAtLeast(0)
            }
            val target = position.coerceIn(0L, duration.toLong()).toInt()
            val callback = checkNotNull(callbacks).get()
            val previous = callback.seeks.value
            checkResult(OH_AVPlayer_Seek(engine, target, AV_SEEK_CLOSEST), "seek")
            withTimeout(15_000.milliseconds) {
                while (callback.seeks.value == previous) {
                    checkNativeError()
                    delay(20.milliseconds)
                }
            }
            emit(PlayEvent.Seek(position()))
            if (autoStart && state() != AV_PLAYING) {
                checkResult(OH_AVPlayer_Play(engine), "play after seek")
                awaitState(setOf(AV_PLAYING))
            }
            reportState()
        }
    }

    override suspend fun release() = withContext(NonCancellable + Dispatchers.Default) {
        mutex.withLock {
            polling?.cancel()
            polling = null
            releaseEngine()
            initialized = false
        }
    }

    override fun addListener(listener: PlayerEngine.Listener) {
        listeners.update { if (listener in it) it else it + listener }
    }
    override fun removeListener(listener: PlayerEngine.Listener) {
        listeners.update { it - listener }
    }

    private suspend fun <T> command(block: suspend () -> T): T = withContext(Dispatchers.Default) {
        mutex.withLock { block() }
    }

    private fun state(): AVPlayerState = memScoped {
        val result = alloc<AVPlayerStateVar>()
        checkResult(OH_AVPlayer_GetState(engine, result.ptr), "get state")
        result.value
    }

    private fun position(): Long = memScoped {
        val result = alloc<IntVar>()
        checkResult(OH_AVPlayer_GetCurrentTime(engine, result.ptr), "get position")
        result.value.toLong().coerceAtLeast(0)
    }

    private suspend fun awaitState(expected: Set<AVPlayerState>) = withTimeout(30_000.milliseconds) {
        while (true) {
            checkNativeError()
            val current = state()
            check(current != AV_ERROR) { "AVPlayer entered error state" }
            if (current in expected) break
            delay(20.milliseconds)
        }
    }

    private fun checkNativeError() {
        callbacks?.get()?.error?.value?.let { error(it) }
    }

    private fun applyVolume() {
        val mixed = PlayerEngine.mixVolume(if (replayGainEnabled) replayGain else 0f, volume)
        checkResult(OH_AVPlayer_SetVolume(engine, mixed, mixed), "set volume")
    }

    private fun reportState() {
        callbacks?.get()?.error?.value?.let { reportError(it) }
        val current = state()
        if (current == lastState) return
        val previous = lastState
        lastState = current
        when (current) {
            AV_PLAYING -> emit(PlayEvent.Play)
            AV_PAUSED -> emit(PlayEvent.Pause)
            AV_COMPLETED -> emit(PlayEvent.End)
            AV_ERROR -> reportError(callbacks?.get()?.error?.value ?: "AVPlayer entered error state")
            else -> if (previous == AV_PLAYING) emit(PlayEvent.Pause)
        }
    }

    private fun reportError(message: String) {
        if (reportedError != message) {
            reportedError = message
            emit(PlayEvent.Error(IllegalStateException(message)))
        }
    }

    private fun emit(event: PlayEvent) {
        scope.launch(Dispatchers.Main) {
            listeners.value.forEach { listener ->
                try { listener.onEvent(event) }
                catch (error: Exception) { Logger.e("OhosPlayer", "Player listener failed", error) }
            }
        }
    }

    private fun releaseEngine() {
        engine?.let {
            // Synchronous release stops native callbacks before their StableRef is disposed.
            checkResult(OH_AVPlayer_ReleaseSync(it), "release")
        }
        engine = null
        callbacks?.dispose()
        callbacks = null
        if (sourceFd >= 0) close(sourceFd)
        sourceFd = -1
        sourcePath?.let { unlink(it) }
        sourcePath = null
        lastState = null
        reportedError = null
    }

    private companion object {
        val playableStates = setOf(AV_PREPARED, AV_PLAYING, AV_PAUSED, AV_COMPLETED)
        fun checkResult(result: OH_AVErrCode, operation: String) {
            check(result == AV_ERR_OK) { "AVPlayer cannot $operation: $result" }
        }
    }
}

private class PlayerCallbacks {
    val error = MutableStateFlow<String?>(null)
    val seeks = MutableStateFlow(0L)
}
