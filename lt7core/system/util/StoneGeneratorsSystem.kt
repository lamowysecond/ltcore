package pl.lamas.lt7core.system.util

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.lamas.lt7core.util.filesystem.PersistType
import org.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.util.Utils.hasPersistentKey
import pl.lamas.lt7core.util.Utils.itemStack
import pl.lamas.lt7core.util.Utils.persistentDataGetString

object StoneGeneratorsSystem : Listener {
    data class StoneGenerator(
        val location: Location,
        val dropIntervalTicks: Int,
    )

    @FilePersisted("stone_generators.json", persistType = PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    val generators: MutableList<StoneGenerator> = mutableListOf()

    val generatorItem: ItemStack = itemStack(
        Material.STONE,
        1,
        "§6§l§kk §r§8§lStone Generator §6§l§kk",
    ).apply {
        val meta = this.itemMeta!!
        val key = NamespacedKey(LTCore.instance, "generator.item")
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, "speed:20")

        itemMeta = meta
    }

    fun getGeneratorAtLocation(location: Location): StoneGenerator? {
        return generators.firstOrNull { it.location == location }
    }

    @EventHandler
    private fun onPlace(event: BlockPlaceEvent) {
        if (event.itemInHand.hasPersistentKey("generator.item")) {
            val generator = StoneGenerator(
                event.block.location,
                event.itemInHand.persistentDataGetString("generator.item")!!.split(":")[1].toInt()
            )
            generators.add(generator)
        }
    }

    @EventHandler
    private fun onBreak(event: BlockBreakEvent) {
        if (event.player.inventory.itemInMainHand.type != Material.GOLDEN_PICKAXE) return
        val generator = getGeneratorAtLocation(event.block.location) ?: return
        generators.removeIf { it.location == generator.location }
        event.block.world.dropItemNaturally(event.block.location, generatorItem)
    }

    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
    }
}