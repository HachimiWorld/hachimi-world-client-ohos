@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.time.ExperimentalTime::class)

package world.hachimi.app.player

import cnames.structs.OH_AVSession
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVSessionKit.OHAVSession.AV_SESSION_ERR_SUCCESS
import platform.AVSessionKit.OHAVSession.AVMETADATA_SUCCESS
import platform.AVSessionKit.OHAVSession.AVSESSION_CALLBACK_RESULT_SUCCESS
import platform.AVSessionKit.OHAVSession.AVSessionCallback_Result
import platform.AVSessionKit.OHAVSession.AVSession_ControlCommand
import platform.AVSessionKit.OHAVSession.AVSession_ErrCode
import platform.AVSessionKit.OHAVSession.AVSession_PlaybackPosition
import platform.AVSessionKit.OHAVSession.AVSession_PlaybackState
import platform.AVSessionKit.OHAVSession.AVSession_Type
import platform.AVSessionKit.OHAVSession.AVMetadata_Result
import platform.AVSessionKit.OHAVSession.CONTROL_CMD_PAUSE
import platform.AVSessionKit.OHAVSession.CONTROL_CMD_PLAY
import platform.AVSessionKit.OHAVSession.CONTROL_CMD_PLAY_NEXT
import platform.AVSessionKit.OHAVSession.CONTROL_CMD_PLAY_PREVIOUS
import platform.AVSessionKit.OHAVSession.OH_AVMetadata
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder_Create
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder_Destroy
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder_GenerateAVMetadata
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder_SetAssetId
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder_SetArtist
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder_SetDuration
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder_SetMediaImageUri
import platform.AVSessionKit.OHAVSession.OH_AVMetadataBuilder_SetTitle
import platform.AVSessionKit.OHAVSession.OH_AVMetadata_Destroy
import platform.AVSessionKit.OHAVSession.OH_AVSessionCallback_OnCommand
import platform.AVSessionKit.OHAVSession.OH_AVSessionCallback_OnSeek
import platform.AVSessionKit.OHAVSession.OH_AVSession_Activate
import platform.AVSessionKit.OHAVSession.OH_AVSession_Create
import platform.AVSessionKit.OHAVSession.OH_AVSession_Deactivate
import platform.AVSessionKit.OHAVSession.OH_AVSession_Destroy
import platform.AVSessionKit.OHAVSession.OH_AVSession_RegisterCommandCallback
import platform.AVSessionKit.OHAVSession.OH_AVSession_RegisterSeekCallback
import platform.AVSessionKit.OHAVSession.OH_AVSession_SetAVMetadata
import platform.AVSessionKit.OHAVSession.OH_AVSession_SetPlaybackPosition
import platform.AVSessionKit.OHAVSession.OH_AVSession_SetPlaybackState
import platform.AVSessionKit.OHAVSession.OH_AVSession_UnregisterCommandCallback
import platform.AVSessionKit.OHAVSession.OH_AVSession_UnregisterSeekCallback
import platform.AVSessionKit.OHAVSession.PLAYBACK_STATE_BUFFERING
import platform.AVSessionKit.OHAVSession.PLAYBACK_STATE_IDLE
import platform.AVSessionKit.OHAVSession.PLAYBACK_STATE_PAUSED
import platform.AVSessionKit.OHAVSession.PLAYBACK_STATE_PLAYING
import platform.AVSessionKit.OHAVSession.SESSION_TYPE_AUDIO
import platform.AbilityKit.Native_Bundle.OH_NativeBundle_GetCurrentApplicationInfo
import world.hachimi.app.sendOhosPlatformAction
import world.hachimi.app.logging.Logger
import world.hachimi.app.model.PlayerService
import kotlin.time.Clock

private const val TAG = "OhosMediaSession"

/** Session tag, must be unique inside the app. */
private const val SESSION_TAG = "hachimi-playback"

/** The ability declared in harmonyApp/entry/src/main/module.json5. */
private const val ABILITY_NAME = "EntryAbility"

/** How often the player state is polled, and how often the position is pushed while playing. */
private const val POLL_INTERVAL_MS = 500L
private const val POSITION_INTERVAL_MS = 2_000L

/** Whether to hand the cover URL to the media control centre; it resolves the URI itself. */
private const val PUBLISH_MEDIA_IMAGE = true

/** Shared by every registered command; the session is passed back through `userData`. */
private val commandCallback: OH_AVSessionCallback_OnCommand = staticCFunction { _, command, userData ->
    userData?.asStableRef<OhosMediaSession>()?.get()?.handleCommand(command)
    AVSESSION_CALLBACK_RESULT_SUCCESS
}

private val seekCallback: OH_AVSessionCallback_OnSeek = staticCFunction { _, position, userData ->
    userData?.asStableRef<OhosMediaSession>()?.get()?.handleSeek(position.toLong())
    AVSESSION_CALLBACK_RESULT_SUCCESS
}

/**
 * Publishes playback to the HarmonyOS media control centre (and the lock screen) through AVSession.
 *
 * Metadata comes from [PlayerService.playerState], and the transport commands the system sends are
 * handed back to [PlayerService], so the session stays correct regardless of which engine plays the
 * audio. Everything degrades to a no-op when the session cannot be created.
 */
class OhosMediaSession(private val playerService: PlayerService) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var session: CPointer<OH_AVSession>? = null
    private var metadata: CPointer<OH_AVMetadata>? = null
    private var metadataBuilder: CPointer<OH_AVMetadataBuilder>? = null
    private var callbackRef: StableRef<OhosMediaSession>? = null

    fun initialize() {
        if (session != null) return
        val created = createSession() ?: return
        session = created

        val activated = OH_AVSession_Activate(created)
        if (activated != AV_SESSION_ERR_SUCCESS) {
            Logger.e(TAG, "Cannot activate the session: $activated")
            release()
            return
        }

        registerCallbacks(created)
        scope.launch { publishPlayerState() }
        Logger.i(TAG, "Media session ready")
    }

    fun release() {
        session?.let { active ->
            controlCommands.forEach { OH_AVSession_UnregisterCommandCallback(active, it, commandCallback) }
            OH_AVSession_UnregisterSeekCallback(active, seekCallback)
            OH_AVSession_Deactivate(active)
            OH_AVSession_Destroy(active)
        }
        session = null
        metadata?.let { OH_AVMetadata_Destroy(it) }
        metadata = null
        metadataBuilder?.let { OH_AVMetadataBuilder_Destroy(it) }
        metadataBuilder = null
        callbackRef?.dispose()
        callbackRef = null
    }

    private fun createSession(): CPointer<OH_AVSession>? {
        val bundleName = runCatching {
            OH_NativeBundle_GetCurrentApplicationInfo().useContents { bundleName?.toKString() }
        }.getOrNull()
        if (bundleName == null) {
            Logger.e(TAG, "Cannot read the bundle name, the media session is disabled")
            return null
        }
        return memScoped {
            val out = alloc<CPointerVar<OH_AVSession>>()
            val code = OH_AVSession_Create(SESSION_TYPE_AUDIO, SESSION_TAG, bundleName, ABILITY_NAME, out.ptr)
            if (code != AV_SESSION_ERR_SUCCESS) {
                Logger.e(TAG, "Cannot create the session: $code (bundle=$bundleName)")
                return@memScoped null
            }
            out.value
        }
    }

    private fun registerCallbacks(active: CPointer<OH_AVSession>) {
        val ref = StableRef.create(this)
        callbackRef = ref
        val userData: CValuesRef<*> = ref.asCPointer()

        controlCommands.forEach { command ->
            OH_AVSession_RegisterCommandCallback(active, command, commandCallback, userData)
        }
        OH_AVSession_RegisterSeekCallback(active, seekCallback, userData)
    }

    private suspend fun publishPlayerState() {
        val state = playerService.playerState
        var publishedTitle: String? = null
        var publishedPlaying: Boolean? = null
        var publishedPositionAt = 0L

        while (currentCoroutineContext().isActive) {
            val title = if (state.hasSong) state.displayedTitle else null
            if (title != publishedTitle) {
                publishedTitle = title
                if (title == null) {
                    applyIdleState()
                } else {
                    applyMetadata(
                        assetId = state.songInfo?.displayId
                            ?: state.previewMetadata?.id?.toString()
                            ?: title,
                        title = title,
                        artist = state.displayedAuthor,
                        cover = state.displayedCover,
                        durationMillis = state.displayedDurationMillis,
                    )
                    applyPlaybackState()
                    applyPosition()
                    publishedPositionAt = nowMillis()
                }
            }

            val playing = state.isPlaying
            if (playing != publishedPlaying) {
                publishedPlaying = playing
                applyPlaybackState()
                applyPosition()
                publishedPositionAt = nowMillis()
                // Let the host start/stop the audio-playback continuous task, so playback survives
                // the app moving to the background.
                sendOhosPlatformAction(PLAYBACK_ACTION, playing.toString())
            } else if (playing && nowMillis() - publishedPositionAt >= POSITION_INTERVAL_MS) {
                applyPosition()
                publishedPositionAt = nowMillis()
            }

            delay(POLL_INTERVAL_MS)
        }
    }

    private fun applyMetadata(
        assetId: String,
        title: String,
        artist: String,
        cover: String?,
        durationMillis: Long,
    ) {
        val active = session ?: return
        metadata?.let { OH_AVMetadata_Destroy(it) }
        metadata = null
        metadataBuilder?.let { OH_AVMetadataBuilder_Destroy(it) }
        metadataBuilder = null

        val built = memScoped {
            val builderOut = alloc<CPointerVarOf<CPointer<OH_AVMetadataBuilder>>>()
            var code = OH_AVMetadataBuilder_Create(builderOut.ptr)
            val builder = builderOut.value
            if (code != AVMETADATA_SUCCESS || builder == null) {
                Logger.e(TAG, "Cannot create the metadata builder: $code")
                return@memScoped null
            }
            // assetId is required: the session service rejects metadata without it.
            OH_AVMetadataBuilder_SetAssetId(builder, assetId)
            OH_AVMetadataBuilder_SetTitle(builder, title)
            if (artist.isNotEmpty()) OH_AVMetadataBuilder_SetArtist(builder, artist)
            if (PUBLISH_MEDIA_IMAGE && !cover.isNullOrEmpty()) {
                OH_AVMetadataBuilder_SetMediaImageUri(builder, cover)
            }
            if (durationMillis > 0) OH_AVMetadataBuilder_SetDuration(builder, durationMillis)

            val metadataOut = alloc<CPointerVar<OH_AVMetadata>>()
            code = OH_AVMetadataBuilder_GenerateAVMetadata(builder, metadataOut.ptr)
            if (code != AVMETADATA_SUCCESS) {
                Logger.e(TAG, "Cannot build the metadata: $code")
                OH_AVMetadataBuilder_Destroy(builder)
                null
            } else {
                // Keep the builder alive together with the metadata: the generated object may refer
                // to memory owned by the builder.
                metadataBuilder = builder
                metadataOut.value
            }
        } ?: return

        val code = OH_AVSession_SetAVMetadata(active, built)
        if (code != AV_SESSION_ERR_SUCCESS) {
            Logger.e(TAG, "Cannot publish the metadata: $code")
            OH_AVMetadata_Destroy(built)
        } else {
            metadata = built
        }
    }

    private fun applyPlaybackState() {
        val active = session ?: return
        val state = playerService.playerState
        val playbackState: AVSession_PlaybackState = when {
            state.buffering || state.fetchingMetadata -> PLAYBACK_STATE_BUFFERING
            state.isPlaying -> PLAYBACK_STATE_PLAYING
            else -> PLAYBACK_STATE_PAUSED
        }
        val code = OH_AVSession_SetPlaybackState(active, playbackState)
        if (code != AV_SESSION_ERR_SUCCESS) {
            Logger.e(TAG, "Cannot publish the playback state: $code")
        }
    }

    private fun applyIdleState() {
        session?.let { OH_AVSession_SetPlaybackState(it, PLAYBACK_STATE_IDLE) }
    }

    private fun applyPosition() {
        val active = session ?: return
        val position = playerService.playerState.displayedCurrentMillis
        memScoped {
            val value = alloc<AVSession_PlaybackPosition>()
            value.elapsedTime = position
            value.updateTime = nowMillis()
            OH_AVSession_SetPlaybackPosition(active, value.ptr)
        }
    }

    /** Called from [commandCallback], which is a top-level C callback. */
    internal fun handleCommand(command: AVSession_ControlCommand) {
        when (command) {
            CONTROL_CMD_PLAY ->
                if (!playerService.playerState.isPlaying) playerService.playOrPause()

            CONTROL_CMD_PAUSE ->
                if (playerService.playerState.isPlaying) playerService.playOrPause()

            CONTROL_CMD_PLAY_NEXT -> playerService.next()
            CONTROL_CMD_PLAY_PREVIOUS -> playerService.previous()
            else -> Unit
        }
    }

    /** Called from [seekCallback], which is a top-level C callback. */
    internal fun handleSeek(positionMillis: Long) {
        val duration = playerService.playerState.displayedDurationMillis
        if (duration > 0) {
            playerService.setSongProgress((positionMillis.toFloat() / duration).coerceIn(0f, 1f))
        }
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val PLAYBACK_ACTION = "playbackActive"

        val controlCommands: List<AVSession_ControlCommand> = listOf(
            CONTROL_CMD_PLAY,
            CONTROL_CMD_PAUSE,
            CONTROL_CMD_PLAY_NEXT,
            CONTROL_CMD_PLAY_PREVIOUS,
        )
    }
}
