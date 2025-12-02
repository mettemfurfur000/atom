package org.shotrush.atom.content.workstation

import org.bukkit.plugin.Plugin
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import org.shotrush.atom.core.api.annotation.RegisterSystem


@RegisterSystem(
    id = "workstation_system",
    priority = 1,
    toggleable = false,
    description = "Manages workstation data persistence and initialization"
)
class WorkstationSystem(private val plugin: Plugin) {

    init {

        Workstations.init()


        plugin.logger.info("Initializing Workstation System...")
        WorkstationDataManager.initialize()

        plugin.logger.info("Workstation System initialized successfully")
    }
}
