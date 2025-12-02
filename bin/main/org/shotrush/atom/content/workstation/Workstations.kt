package org.shotrush.atom.content.workstation

import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.block.entity.BlockEntityTypes
import net.momirealms.craftengine.core.util.Key
import org.shotrush.atom.content.workstation.clay_cauldron.ClayCauldronBlockBehavior
import org.shotrush.atom.content.workstation.craftingbasket.CraftingBasketBlockBehavior
import org.shotrush.atom.content.workstation.knapping.KnappingBlockBehavior
import org.shotrush.atom.content.workstation.leatherbed.LeatherBedBlockBehavior

data class WorkstationDef(val key: Key, val type: BlockEntityType<BlockEntity>) {}

object Workstations {
    val KNAPPING_STATION_KEY = Key.of("atom:knapping_station")
    val KNAPPING_STATION_BEHAVIOR = BlockBehaviors.register(KNAPPING_STATION_KEY, KnappingBlockBehavior.Factory)
    val KNAPPING_STATION_ENTITY_TYPE = BlockEntityTypes.register<BlockEntity>(KNAPPING_STATION_KEY)

    val LEATHER_BED = register("atom:leather_bed", LeatherBedBlockBehavior.Factory)
    val CLAY_CAULDRON = register("atom:clay_cauldron", ClayCauldronBlockBehavior.Factory)

    val CRAFTING_BASKET_KEY = Key.of("atom:crafting_basket")
    val CRAFTING_BASKET_BEHAVIOR = BlockBehaviors.register(CRAFTING_BASKET_KEY, CraftingBasketBlockBehavior.Companion.Factory)
    val CRAFTING_BASKET_ENTITY_TYPE = BlockEntityTypes.register<BlockEntity>(CRAFTING_BASKET_KEY)

    fun init() = Unit

    fun register(key: String, factory: BlockBehaviorFactory): WorkstationDef {
        val key = Key.of(key)
        BlockBehaviors.register(key, factory)
        return WorkstationDef(key, BlockEntityTypes.register(key))
    }
}