@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package world.hachimi.app.storage

import io.github.vinceglb.filekit.*
import kotlinx.cinterop.*
import kotlinx.coroutines.IO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import platform.posix.*
import kotlin.reflect.KClass

actual class PreferenceKey<T : Any> actual constructor(
    actual val name: String,
    val clazz: KClass<T>,
)

/** App-private, atomically replaced preferences. All instances share the write lock. */
class MyDataStoreImpl : MyDataStore {
    private val file get() = PlatformFile(FileKit.filesDir, "preferences.json")

    private suspend fun read(): Map<String, JsonElement> =
        if (file.exists()) Json.parseToJsonElement(file.readBytes().decodeToString()).jsonObject
        else emptyMap()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> get(key: PreferenceKey<T>): T? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entry = read()[key.name]?.jsonObject ?: return@withLock null
            check(entry.getValue("type").jsonPrimitive.content == typeName(key.clazz)) {
                "Preference type mismatch for ${key.name}"
            }
            val text = entry.getValue("value").jsonPrimitive.content
            (when (key.clazz) {
                String::class -> text
                Boolean::class -> text.toBooleanStrict()
                Byte::class -> text.toByte()
                Short::class -> text.toShort()
                Int::class -> text.toInt()
                Long::class -> text.toLong()
                Float::class -> text.toFloat()
                Double::class -> text.toDouble()
                else -> error("Unsupported preference type ${key.clazz}")
            }) as T
        }
    }

    override suspend fun <T : Any> set(key: PreferenceKey<T>, value: T) = withContext(Dispatchers.IO) {
        require(key.clazz.isInstance(value)) { "Preference type mismatch for ${key.name}" }
        val entry = buildJsonObject {
            put("type", typeName(key.clazz))
            // Strings preserve all Long bits, special floating values and embedded NULs.
            put("value", value.toString())
        }
        mutex.withLock { save(read() + (key.name to entry)) }
    }

    override suspend fun <T : Any> delete(key: PreferenceKey<T>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entries = read()
            if (key.name in entries) save(entries - key.name)
        }
    }

    private fun save(entries: Map<String, JsonElement>) {
        val target = file.path
        val temporary = "$target.tmp"
        try {
            writePrivateFile(temporary, JsonObject(entries).toString().encodeToByteArray())
            check(rename(temporary, target) == 0) { "Cannot replace preferences: errno=$errno" }
        } finally {
            unlink(temporary)
        }
    }

    private companion object {
        val mutex = Mutex()
        fun typeName(clazz: KClass<*>): String = when (clazz) {
            String::class -> "string"
            Boolean::class -> "boolean"
            Byte::class -> "byte"
            Short::class -> "short"
            Int::class -> "int"
            Long::class -> "long"
            Float::class -> "float"
            Double::class -> "double"
            else -> error("Unsupported preference type $clazz")
        }
    }
}

/** Writes complete bytes with owner-only permissions; used by preferences and audio staging. */
internal fun writePrivateFile(path: String, bytes: ByteArray) {
    val fd = open(path, O_WRONLY or O_CREAT or O_TRUNC, 384 /* 0600 */)
    check(fd >= 0) { "Cannot open private file: errno=$errno" }
    try {
        if (bytes.isNotEmpty()) bytes.usePinned { pinned ->
            var offset = 0
            while (offset < bytes.size) {
                val count = write(fd, pinned.addressOf(offset), (bytes.size - offset).convert())
                if (count < 0 && errno == EINTR) continue
                check(count > 0) { "Cannot write private file: errno=$errno" }
                offset += count.toInt()
            }
        }
        check(fsync(fd) == 0) { "Cannot flush private file: errno=$errno" }
    } finally {
        close(fd)
    }
}
