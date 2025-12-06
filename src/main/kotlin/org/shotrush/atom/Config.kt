package org.shotrush.atom

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.benwoodworth.knbt.Nbt
import java.nio.file.Path
import kotlin.io.path.createParentDirectories

sealed interface FileFormat {
    fun <T : Any> decodeFromFile(file: Path, serializer: KSerializer<T>): T
    fun <T : Any> encodeToFile(file: Path, serializer: KSerializer<T>, value: T)
    data class StringBased(val format: StringFormat) : FileFormat {
        override fun <T : Any> decodeFromFile(file: Path, serializer: KSerializer<T>): T {
            return format.decodeFromString(serializer, file.toFile().readText())
        }

        override fun <T : Any> encodeToFile(
            file: Path,
            serializer: KSerializer<T>,
            value: T,
        ) {
            file.toFile().writeText(format.encodeToString(serializer, value))
        }
    }
    data class BinaryBased(val format: BinaryFormat) : FileFormat {
        override fun <T : Any> decodeFromFile(
            file: Path,
            serializer: KSerializer<T>,
        ): T {
            return format.decodeFromByteArray(serializer, file.toFile().readBytes())
        }

        override fun <T : Any> encodeToFile(
            file: Path,
            serializer: KSerializer<T>,
            value: T,
        ) {
            file.toFile().writeBytes(format.encodeToByteArray(serializer, value))
        }
    }
}

enum class FileType(val format: FileFormat) {
    JSON(FileFormat.StringBased(Json)),
    YAML(FileFormat.StringBased(Yaml.default)),
    NBT(FileFormat.BinaryBased(Nbt { }))
}

inline fun <reified T> readSerializedFile(path: String, type: FileType): T =
    readSerializedFile(Atom.instance.dataPath.resolve(path), type)

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> readSerializedFile(path: Path, type: FileType): T =
    type.format.decodeFromFile(path, T::class.serializer())

inline fun <reified T : Any> writeSerializedFile(value: T, path: String, type: FileType) =
    writeSerializedFile(value, Atom.instance.dataPath.resolve(path), type)

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> writeSerializedFile(value: T, path: Path, type: FileType) {
    path.createParentDirectories()
    type.format.encodeToFile(path, T::class.serializer(), value)
}