package pl.lamas.lt7core.system.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.util.filesystem.FilePersisted
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
    var generators: MutableList<StoneGenerator> = mutableListOf()

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

    val generatorSpawnerItem: ItemStack = itemStack(
        Material.GOLDEN_PICKAXE,
        1,
        "§a§l§kk §r§8§lStone Generator Spawner §a§l§kk",
    ).apply {
        val meta = this.itemMeta!!
        val key = NamespacedKey(LTCore.instance, "generatorspawner.item")
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, "spawner:20")

        itemMeta = meta
    }

    fun getGeneratorAtLocation(location: Location): StoneGenerator? {
        return generators.firstOrNull { it.location == location }
    }

    fun giveGenerator(player: Player) {
        if (!player.hasPermission("op")) {
            return
        }
        player.inventory.addItem(generatorSpawnerItem.clone())
        player.sendMessage(LTCore.PREFIX + "§aYou have got a stone generator spawner.")
    }

    @EventHandler
    private fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        if (!item.hasPersistentKey("generatorspawner.item")) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val clicked = event.clickedBlock ?: return
        val placeLoc = clicked.location.add(0.0, 1.0, 0.0)

        if (!placeLoc.block.type.isAir) {
            return
        }

        placeLoc.block.type = Material.STONE

        val generator = StoneGenerator(
            placeLoc,
            item.persistentDataGetString("generatorspawner.item")!!.split(":")[1].toInt()
        )
        generators.add(generator)
        event.isCancelled = true
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
        val generator = getGeneratorAtLocation(event.block.location) ?: return
        if (event.player.inventory.itemInMainHand.type != Material.GOLDEN_PICKAXE) {
            Bukkit.getScheduler().runTaskLater(LTCore.instance, Runnable {
                event.block.type = Material.STONE
            }, generator.dropIntervalTicks .toLong())
        }
        generators.removeIf { it.location == generator.location }
        event.block.world.dropItemNaturally(event.block.location, generatorItem)
    }

    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
    }
}