package org.shotrush.atom.api

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import org.bukkit.Material
import org.bukkit.block.Block
import org.shotrush.atom.matches
import org.shotrush.atom.util.Key

sealed class BlockRef {
    abstract fun matches(block: Block): Boolean

    companion object {
        fun vanilla(material: Material) = MaterialRef(material)
        fun custom(key: Key) = CEBlockRef(key)
        fun custom(key: String) = custom(Key("atom", key))
    }

    data class MaterialRef(val material: Material) : BlockRef() {
        override fun matches(block: Block): Boolean {
            if (CraftEngineBlocks.getCustomBlockState(block) != null) return false
            return block.type == material
        }
    }

    data class CEBlockRef(val key: Key) : BlockRef() {
        override fun matches(block: Block): Boolean {
            if (CraftEngineBlocks.getCustomBlockState(block) == null) return false
            return block.matches(key)
        }
    }
}