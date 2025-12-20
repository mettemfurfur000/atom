package org.shotrush.atom.systems.structure

interface StructureScan {
    suspend fun scan(): Boolean

    fun toStructure(): Structure?
}
