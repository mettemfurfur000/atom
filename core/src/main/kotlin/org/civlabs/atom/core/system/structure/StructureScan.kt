package org.civlabs.atom.core.system.structure

interface StructureScan {
    suspend fun scan(): Boolean

    fun toStructure(): Structure?
}
