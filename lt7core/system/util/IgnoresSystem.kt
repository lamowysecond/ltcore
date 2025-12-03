package pl.lamas.lt7core.system.util

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.lamas.lt7core.util.filesystem.PersistType
import org.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.economy.MoneySystem

object IgnoresSystem : Listener {
    @FilePersisted(filePath = "ignores.json", persistType = PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var ignores: MutableMap<String, MutableList<String>> = mutableMapOf()

    fun isIgnoring(ignorer: String, ignoree: String): Boolean {
        val ignoredList = ignores[ignorer] ?: return false
        return ignoredList.contains(ignoree)
    }

    fun ignore(sender: Player, ignoree: String) {
        if (sender.name == ignoree) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "ignores.cannot_ignore_yourself"))
            return
        }

        if (ignoree !in MoneySystem.balances.keys) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "ignores.player_not_found", mapOf("%p" to ignoree)))
            return
        }

        val ignoredList = ignores.getOrPut(sender.name) { mutableListOf() }
        if (!ignoredList.contains(ignoree)) {
            ignoredList.add(ignoree)
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "ignores.now_ignoring", mapOf("%p" to ignoree)))
        } else {
            ignoredList.remove(ignoree)
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "ignores.stopped_ignoring", mapOf("%p" to ignoree)))
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private fun onChat(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
        event.recipients.removeIf { isIgnoring(it.name, event.player.name) }
    }

    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
    }
}