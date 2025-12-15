package org.shotrush.atom.content.carcass

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.shotrush.atom.Atom
import org.shotrush.atom.FileType
import org.shotrush.atom.content.AnimalType
import org.shotrush.atom.readSerializedFileOrNull
import java.util.logging.Level

// Custom serializer for AnimalType enum
object AnimalTypeSerializer : KSerializer<AnimalType> {
    override val descriptor = PrimitiveSerialDescriptor("AnimalType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: AnimalType) = encoder.encodeString(value.id)
    override fun deserialize(decoder: Decoder): AnimalType {
        val id = decoder.decodeString()
        return AnimalType.byId(id) ?: throw IllegalArgumentException("Unknown animal type: $id")
    }
}

// Custom serializer for ToolRequirement (parses from string)
object ToolRequirementSerializer : KSerializer<ToolRequirement> {
    override val descriptor = PrimitiveSerialDescriptor("ToolRequirement", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ToolRequirement) {
        val stringValue = when (value) {
            is ToolRequirement.None -> "none"
            is ToolRequirement.ItemTag -> value.tag
            is ToolRequirement.SpecificMaterial -> value.material.name
        }
        encoder.encodeString(stringValue)
    }
    override fun deserialize(decoder: Decoder): ToolRequirement {
        return ToolRequirement.parse(decoder.decodeString())
    }
}

// Serializable configuration structure matching the YAML file
@Serializable
data class CarcassConfigSettings(
    val default_decomposition_ticks: Long = 48000L
)

@Serializable
data class CarcassConfigAnimal(
    val display_name: String,
    val decomposition_ticks: Long? = null,
    val gui_rows: Int = 3,
    val requires_butchering: Boolean = true,
    val parts: Map<String, CarcassConfigPart>
)

@Serializable
data class CarcassConfigPart(
    val display_name: String,
    @Serializable(with = ToolRequirementSerializer::class)
    val tool: ToolRequirement = ToolRequirement.None,
    val item: String,
    val min_amount: Int = 1,
    val max_amount: Int = 1,
    val gui_slot: Int = 0,
    val external: Boolean = false
)

@Serializable
data class CarcassConfigFile(
    val settings: CarcassConfigSettings = CarcassConfigSettings(),
    val animals: Map<String, CarcassConfigAnimal>
)

// Runtime configuration used by the system
data class CarcassAnimalConfig(
    val animalType: AnimalType,
    val displayName: String,
    val decompositionTicks: Long,
    val guiRows: Int,
    val parts: List<CarcassPartDef>,
    val requiresButchering: Boolean = true
)

object CarcassConfigs {
    private const val CONFIG_FILE_NAME = "carcass_config.yml"
    private const val DAY_TICKS = 24000L
    private const val DEFAULT_DECOMPOSITION = DAY_TICKS * 2

    @Volatile
    private var configs: Map<AnimalType, CarcassAnimalConfig> = emptyMap()

    fun load() {
        val plugin = Atom.instance
        val configPath = plugin.dataPath.resolve(CONFIG_FILE_NAME)

        // Ensure config file exists by extracting from resources if needed
        if (!configPath.toFile().exists()) {
            plugin.saveResource(CONFIG_FILE_NAME, false)
        }

        // Load configuration using new Config system
        val configFile = try {
            readSerializedFileOrNull<CarcassConfigFile>(configPath, FileType.YAML)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to load $CONFIG_FILE_NAME", e)
            null
        }

        if (configFile == null) {
            plugin.logger.warning("No valid configuration found in $CONFIG_FILE_NAME")
            return
        }

        val defaultDecomposition = configFile.settings.default_decomposition_ticks
        val loadedConfigs = mutableMapOf<AnimalType, CarcassAnimalConfig>()

        // Process each animal configuration
        for ((animalKey, animalConfig) in configFile.animals) {
            val animalType = AnimalType.byId(animalKey)
            if (animalType == null) {
                plugin.logger.warning("Unknown animal type in carcass config: $animalKey")
                continue
            }

            // Convert config parts to runtime part definitions
            val parts = animalConfig.parts.map { (partKey, partConfig) ->
                CarcassPartDef(
                    id = partKey,
                    displayName = partConfig.display_name,
                    requiredTool = partConfig.tool,
                    itemId = partConfig.item,
                    minAmount = partConfig.min_amount,
                    maxAmount = partConfig.max_amount,
                    guiSlot = partConfig.gui_slot,
                    external = partConfig.external
                )
            }

            if (parts.isNotEmpty()) {
                loadedConfigs[animalType] = CarcassAnimalConfig(
                    animalType = animalType,
                    displayName = animalConfig.display_name,
                    decompositionTicks = animalConfig.decomposition_ticks ?: defaultDecomposition,
                    guiRows = animalConfig.gui_rows,
                    parts = parts,
                    requiresButchering = animalConfig.requires_butchering
                )
            } else {
                plugin.logger.warning("No parts defined for animal: $animalKey")
            }
        }

        configs = loadedConfigs
        plugin.logger.info("Loaded carcass configs for ${configs.size} animals")
    }

    fun configFor(animalType: AnimalType): CarcassAnimalConfig? = configs[animalType]

    fun hasCarcassConfig(animalType: AnimalType): Boolean = configs.containsKey(animalType)

    val supportedAnimals: Set<AnimalType> get() = configs.keys
}
