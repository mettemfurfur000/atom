package org.shotrush.atom.content.workstation.leatherbed

import it.unimi.dsi.fastutil.ints.IntList
import net.momirealms.craftengine.bukkit.entity.data.ItemDisplayEntityData
import net.momirealms.craftengine.bukkit.nms.FastNMS
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes
import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer
import net.momirealms.craftengine.core.block.properties.Property
import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.entity.player.Player
import net.momirealms.craftengine.core.plugin.CraftEngine
import net.momirealms.craftengine.core.util.HorizontalDirection
import net.momirealms.craftengine.core.util.QuaternionUtils
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import org.shotrush.atom.content.base.AtomBlockEntityRenderer
import java.util.*

class LeatherBedBlockDynamicRenderer(val entity: LeatherBedBlockEntity) : AtomBlockEntityRenderer() {
    private var cachedSpawnPacket: Any? = null
    private var cachedDespawnPacket: Any? = null
    private var entityId = 0

    fun displayNewItem() {
        val pos = entity.pos()
        val entityId = CoreReflections.`instance$Entity$ENTITY_COUNTER`.incrementAndGet()
        this.cachedSpawnPacket = FastNMS.INSTANCE.`constructor$ClientboundAddEntityPacket`(
            entityId,
            UUID.randomUUID(),
            (pos.x().toDouble() + 0.5),
            (pos.y().toDouble() + 1),
            (pos.z().toDouble() + 0.5),
            0f,
            0f,
            MEntityTypes.ITEM_DISPLAY,
            0,
            CoreReflections.`instance$Vec3$Zero`,
            0.0
        )
        this.cachedDespawnPacket = FastNMS.INSTANCE.`constructor$ClientboundRemoveEntitiesPacket`(IntList.of(entityId))
        this.entityId = entityId
    }

    fun getDataValues(): List<Any> {
        val rotation = entity.rotation
        val idx = when (rotation) {
            HorizontalDirection.NORTH -> 0
            HorizontalDirection.EAST -> 1
            HorizontalDirection.SOUTH -> 2
            HorizontalDirection.WEST -> 3
        }

        val dataValues = mutableListOf<Any>()
        ItemDisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(
            CraftEngine.instance().itemManager<ItemStack>().wrap(entity.storedItem).literalObject,
            dataValues
        )
        ItemDisplayEntityData.Scale.addEntityDataIfNotDefaultValue(Vector3f(1f, 1f, 1f), dataValues)
        ItemDisplayEntityData.RotationLeft.addEntityDataIfNotDefaultValue(
            QuaternionUtils.toQuaternionf(0.0, Math.toRadians(idx * 90.0), 0.0), dataValues
        )
        ItemDisplayEntityData.BillboardConstraints.addEntityDataIfNotDefaultValue(Billboard.FIXED.id(), dataValues)
        ItemDisplayEntityData.Translation.addEntityDataIfNotDefaultValue(
            Vector3f(
                rotation.stepX() * 0.5f,
                0f,
                rotation.stepZ() * 0.5f
            ), dataValues
        )
        ItemDisplayEntityData.DisplayType.addEntityDataIfNotDefaultValue(ItemDisplayContext.NONE.id(), dataValues)
        ItemDisplayEntityData.ShadowRadius.addEntityDataIfNotDefaultValue(0.0f, dataValues)
        ItemDisplayEntityData.ShadowStrength.addEntityDataIfNotDefaultValue(1.0f, dataValues)
        ItemDisplayEntityData.ViewRange.addEntityDataIfNotDefaultValue(64f, dataValues)
        return dataValues
    }

    override fun show(player: Player) {
        if (cachedSpawnPacket == null) displayNewItem()
        val packet = cachedSpawnPacket ?: return
        player.sendPackets(
            listOf(
                packet,
                FastNMS.INSTANCE.`constructor$ClientboundSetEntityDataPacket`(
                    this.entityId,
                    getDataValues()
                )
            ), true
        )
    }

    override fun hide(player: Player) {
        if (this.cachedDespawnPacket == null) return
        player.sendPacket(this.cachedDespawnPacket, false)
    }

    override fun update(player: Player) {
        hide(player)
        displayNewItem()
        show(player)
    }
}