package pl.lamas.lt7core.system.command

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.util.Utils.addItemOrDrop
import pl.lamas.lt7core.util.gui.Gui
import pl.lamas.lt7core.util.gui.GuiButton

object KitsSystem {

    data class Kit(
        val name: String,
        val icon: ItemStack,
        val slot: Int,
        val items: MutableList<ItemStack>,
        val cooldown: Long,
        val permission: String
    )

    @FilePersisted("kits.json", PersistType.READ)
    var kits: MutableList<Kit> = mutableListOf()

    @FilePersisted("kits_cooldowns.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var kitsCooldowns: MutableMap<String, MutableMap<String, Long>> = mutableMapOf()

    fun collectKit(sender: Player, kitName: String) {
        val kit = kits.find { it.name.equals(kitName, ignoreCase = true) }
        if (kit == null) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "kits.kit-not-found"))
            return
        }

        if (!sender.hasPermission(kit.permission)) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "kits.no_permission"))
            return
        }

        val playerCooldowns = kitsCooldowns.getOrPut(sender.name) { mutableMapOf() }
        val lastCollected = playerCooldowns[kit.name] ?: 0L
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCollected < kit.cooldown) {
            val timeLeft = kit.cooldown - (currentTime - lastCollected)
            val secondsLeft = timeLeft / 1000
            sender.sendMessage(
                LTCore.PREFIX + LTCore.langManager.getString(
                    sender,
                    "kits.cooldown-active",
                    mapOf("%t" to secondsLeft.toString())
                )
            )
            return
        }

        for (item in kit.items) {
            sender.addItemOrDrop(item)
        }

        playerCooldowns[kit.name] = currentTime
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "kits.kit-collected", mapOf("%k" to kit.name)))
    }

    fun kitList(sender: Player) {
        Gui(
            "kits",
            LTCore.langManager.getString(sender, "kits.gui-title"),
            3,
            sender,
            kits.map { kit ->
                GuiButton(
                    kit.slot,
                    kit.icon,
                    onClick = {
                        collectKit(sender, kit.name)
                    },
                )
            }
        ).open()
    }
}