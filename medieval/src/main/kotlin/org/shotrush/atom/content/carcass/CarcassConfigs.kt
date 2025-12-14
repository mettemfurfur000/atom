package org.shotrush.atom.content.carcass

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.shotrush.atom.Atom
import org.shotrush.atom.content.AnimalType
import java.io.File
import java.util.logging.Level

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
        val configFile = File(plugin.dataFolder, CONFIG_FILE_NAME)

        if (!configFile.exists()) {
            plugin.saveResource(CONFIG_FILE_NAME, false)
        }

        val yaml = YamlConfiguration.loadConfiguration(configFile)
        val defaultDecomposition = yaml.getLong("settings.default_decomposition_ticks", DEFAULT_DECOMPOSITION)
        
        val animalsSection: ConfigurationSection = yaml.getConfigurationSection("animals") ?: run {
            plugin.logger.warning("No animals section found in $CONFIG_FILE_NAME")
            return
        }

        val loadedConfigs = mutableMapOf<AnimalType, CarcassAnimalConfig>()

        for (animalKey in animalsSection.getKeys(false)) {
            val animalType = AnimalType.byId(animalKey)
            if (animalType == null) {
                plugin.logger.warning("Unknown animal type in carcass config: $animalKey")
                continue
            }

            val animalSection: ConfigurationSection = animalsSection.getConfigurationSection(animalKey) ?: continue
            
            val displayName = animalSection.getString("display_name", animalKey.replaceFirstChar { it.uppercase() }) ?: animalKey
            val decompositionTicks = animalSection.getLong("decomposition_ticks", defaultDecomposition)
            val guiRows = animalSection.getInt("gui_rows", 3)
            val requiresButchering = animalSection.getBoolean("requires_butchering", true)
            
            val partsSection: ConfigurationSection = animalSection.getConfigurationSection("parts") ?: run {
                plugin.logger.warning("No parts section for animal: $animalKey")
                continue
            }

            val parts = mutableListOf<CarcassPartDef>()
            
            for (partKey in partsSection.getKeys(false)) {
                val partSection: ConfigurationSection = partsSection.getConfigurationSection(partKey) ?: continue
                
                try {
                    val itemId = partSection.getString("item") ?: continue
                    val part = CarcassPartDef(
                        id = partKey,
                        displayName = partSection.getString("display_name", partKey) ?: partKey,
                        requiredTool = parseToolRequirement(partSection.getString("tool", "none") ?: "none"),
                        itemId = itemId,
                        minAmount = partSection.getInt("min_amount", 1),
                        maxAmount = partSection.getInt("max_amount", 1),
                        guiSlot = partSection.getInt("gui_slot", 0),
                        external = partSection.getBoolean("external", false)
                    )
                    parts.add(part)
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Failed to parse part $partKey for animal $animalKey", e)
                }
            }

            if (parts.isNotEmpty()) {
                loadedConfigs[animalType] = CarcassAnimalConfig(
                    animalType = animalType,
                    displayName = displayName,
                    decompositionTicks = decompositionTicks,
                    guiRows = guiRows,
                    parts = parts,
                    requiresButchering = requiresButchering
                )
            }
        }

        configs = loadedConfigs
        plugin.logger.info("Loaded carcass configs for ${configs.size} animals")
    }

    private fun parseToolRequirement(tool: String): ToolRequirement {
        return when (tool.lowercase()) {
            "none" -> ToolRequirement.NONE
            "knife" -> ToolRequirement.KNIFE
            "axe" -> ToolRequirement.AXE
            "water_bucket" -> ToolRequirement.WATER_BUCKET
            else -> ToolRequirement.NONE
        }
    }

    fun configFor(animalType: AnimalType): CarcassAnimalConfig? = configs[animalType]

    fun hasCarcassConfig(animalType: AnimalType): Boolean = configs.containsKey(animalType)

    val supportedAnimals: Set<AnimalType> get() = configs.keys
}
