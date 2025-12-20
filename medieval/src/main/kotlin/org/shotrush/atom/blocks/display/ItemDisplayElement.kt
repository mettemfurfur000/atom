package org.shotrush.atom.blocks.display

import com.github.shynixn.mccoroutine.folia.asyncDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticks
import it.unimi.dsi.fastutil.ints.IntList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.momirealms.craftengine.bukkit.entity.data.ItemDisplayEntityData
import net.momirealms.craftengine.bukkit.nms.FastNMS
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes
import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.entity.player.Player
import net.momirealms.craftengine.core.plugin.CraftEngine
import org.bukkit.inventory.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import org.shotrush.atom.Atom
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ItemDisplayElement(
    override val id: String,
    private val origin: Prop<Vector3f>,
    private val position: Prop<Vector3f>,
    private val displayedItem: Prop<ItemStack>,
    private val scale: Prop<Vector3f>,
    private val rotation: Prop<Quaternionf>,
    private val translation: Prop<Vector3f>,
    private val billboard: Prop<Billboard>,
    private val displayContext: Prop<ItemDisplayContext>,
    private val shadowRadius: Prop<Float>,
    private val shadowStrength: Prop<Float>,
    private val viewRange: Prop<Float>,
    private val updatesPerSecond: Prop<Float>,
    private val visible: Prop<Boolean>,
) : DisplayElement {

    private val nms = FastNMS.INSTANCE
    private val entityId = CoreReflections.`instance$Entity$ENTITY_COUNTER`.incrementAndGet()
    private val uuid = UUID.randomUUID()
    private val spawnedFor = Collections.newSetFromMap(ConcurrentHashMap<UUID, Boolean>())

    private fun worldPos(player: Player): Vector3f {
        val o = origin.resolve(player)
        val p = position.resolve(player)
        return Vector3f(o.x + p.x, o.y + p.y, o.z + p.z)
    }

    private fun addStaticIfNonDefault(player: Player, metas: MutableList<Any>) {
        if (displayedItem.isPresent() && !displayedItem.isDynamic()) {
            val v = displayedItem.resolve(player)
            ItemDisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(
                CraftEngine.instance().itemManager<ItemStack>().wrap(v).literalObject,
                metas
            )
        }
        if (scale.isPresent() && !scale.isDynamic()) {
            ItemDisplayEntityData.Scale.addEntityDataIfNotDefaultValue(
                scale.resolve(player),
                metas
            )
        }
        if (rotation.isPresent() && !rotation.isDynamic()) {
            ItemDisplayEntityData.RotationLeft.addEntityDataIfNotDefaultValue(
                rotation.resolve(player),
                metas
            )
        }
        if (translation.isPresent() && !translation.isDynamic()) {
            ItemDisplayEntityData.Translation.addEntityDataIfNotDefaultValue(
                translation.resolve(player),
                metas
            )
        }
        if (billboard.isPresent() && !billboard.isDynamic()) {
            ItemDisplayEntityData.BillboardConstraints.addEntityDataIfNotDefaultValue(
                billboard.resolve(player).id(),
                metas
            )
        }
        if (displayContext.isPresent() && !displayContext.isDynamic()) {
            ItemDisplayEntityData.DisplayType.addEntityDataIfNotDefaultValue(
                displayContext.resolve(player).id(),
                metas
            )
        }
        if (shadowRadius.isPresent() && !shadowRadius.isDynamic()) {
            ItemDisplayEntityData.ShadowRadius.addEntityDataIfNotDefaultValue(
                shadowRadius.resolve(player),
                metas
            )
        }
        if (shadowStrength.isPresent() && !shadowStrength.isDynamic()) {
            ItemDisplayEntityData.ShadowStrength.addEntityDataIfNotDefaultValue(
                shadowStrength.resolve(player),
                metas
            )
        }
        if (viewRange.isPresent() && !viewRange.isDynamic()) {
            ItemDisplayEntityData.ViewRange.addEntityDataIfNotDefaultValue(
                viewRange.resolve(player),
                metas
            )
        }
    }

    private fun addDynamicValues(player: Player, metas: MutableList<Any>) {
        if (displayedItem.isPresent() && displayedItem.isDynamic()) {
            val v = displayedItem.resolve(player)
            ItemDisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(
                CraftEngine.instance().itemManager<ItemStack>().wrap(v).literalObject,
                metas
            )
        }
        if (scale.isPresent() && scale.isDynamic()) {
            ItemDisplayEntityData.Scale.addEntityDataIfNotDefaultValue(
                scale.resolve(player),
                metas
            )
        }
        if (rotation.isPresent() && rotation.isDynamic()) {
            ItemDisplayEntityData.RotationLeft.addEntityDataIfNotDefaultValue(
                rotation.resolve(player),
                metas
            )
        }
        if (translation.isPresent() && translation.isDynamic()) {
            ItemDisplayEntityData.Translation.addEntityDataIfNotDefaultValue(
                translation.resolve(player),
                metas
            )
        }
        if (billboard.isPresent() && billboard.isDynamic()) {
            ItemDisplayEntityData.BillboardConstraints.addEntityDataIfNotDefaultValue(
                billboard.resolve(player).id(),
                metas
            )
        }
        if (displayContext.isPresent() && displayContext.isDynamic()) {
            ItemDisplayEntityData.DisplayType.addEntityDataIfNotDefaultValue(
                displayContext.resolve(player).id(),
                metas
            )
        }
        if (shadowRadius.isPresent() && shadowRadius.isDynamic()) {
            ItemDisplayEntityData.ShadowRadius.addEntityDataIfNotDefaultValue(
                shadowRadius.resolve(player),
                metas
            )
        }
        if (shadowStrength.isPresent() && shadowStrength.isDynamic()) {
            ItemDisplayEntityData.ShadowStrength.addEntityDataIfNotDefaultValue(
                shadowStrength.resolve(player),
                metas
            )
        }
        if (viewRange.isPresent() && viewRange.isDynamic()) {
            ItemDisplayEntityData.ViewRange.addEntityDataIfNotDefaultValue(
                viewRange.resolve(player),
                metas
            )
        }
    }

    override fun show(player: Player) {
        val isVisible = !visible.isPresent() || visible.resolve(player)
        if (isVisible) {
            spawn(player)
        } else {
            if (spawnedFor.contains(player.uuid())) {
                despawn(player)
            }
        }
    }

    override fun update(player: Player) {
        val isVisible = !visible.isPresent() || visible.resolve(player)
        val isSpawned = spawnedFor.contains(player.uuid())

        when {
            isVisible && !isSpawned -> {
                // became visible -> spawn with current state
                spawn(player)
            }

            !isVisible && isSpawned -> {
                // became invisible -> despawn
                despawn(player)
            }

            isVisible && isSpawned -> {
                // normal dynamic update
                val metas = mutableListOf<Any>()
                addDynamicValues(player, metas)
                if (metas.isNotEmpty()) {
                    val metaPkt = nms.`constructor$ClientboundSetEntityDataPacket`(entityId, metas)
                    player.sendPacket(metaPkt, true)
                }
            }
        }
    }

    override fun hide(player: Player) {
        if (spawnedFor.contains(player.uuid())) {
            despawn(player)
        }
    }

    private fun spawn(player: Player) {
        val wp = worldPos(player)
        val spawn = nms.`constructor$ClientboundAddEntityPacket`(
            entityId,
            uuid,
            (wp.x().toDouble() + 0.5),
            (wp.y().toDouble() + 1.0),
            (wp.z().toDouble() + 0.5),
            0f,
            0f,
            MEntityTypes.ITEM_DISPLAY,
            0,
            CoreReflections.`instance$Vec3$Zero`,
            0.0
        )
        val metas = mutableListOf<Any>()
        addStaticIfNonDefault(player, metas)
        addDynamicValues(player, metas)
        if (metas.isNotEmpty()) {
            val metaPkt = nms.`constructor$ClientboundSetEntityDataPacket`(entityId, metas)
            player.sendPackets(listOf(spawn, metaPkt), true)
        } else {
            player.sendPacket(spawn, true)
        }
        spawnedFor.add(player.uuid())
    }

    private fun despawn(player: Player) {
        val despawn = nms.`constructor$ClientboundRemoveEntitiesPacket`(IntList.of(entityId))
        player.sendPacket(despawn, false)
        spawnedFor.remove(player.uuid())
    }

    override fun startTicking(player: Player, tm: TickManager) {
        if (!updatesPerSecond.isPresent()) {
            stopTicking(player, tm); return
        }

        val job = Atom.instance.launch(Atom.instance.asyncDispatcher) {
            while (isActive) {
                if (!updatesPerSecond.isPresent() || !player.isOnline) {
                    stopTicking(player, tm)
                    return@launch
                }
                val ups = updatesPerSecond.resolve(player)
                if (ups <= 0.0) {
                    delay(1.ticks)
                    continue
                }
                val periodMs = (1000.0 / ups).toLong().coerceAtLeast(1L)
                update(player)
                delay(periodMs)
            }
        }
        tm.startOrReplace(player, id, job)
    }

    override fun stopTicking(player: Player, tm: TickManager) {
        tm.stop(player, id)
    }
}