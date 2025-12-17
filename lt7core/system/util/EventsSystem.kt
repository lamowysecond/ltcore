package pl.lamas.lt7core.system.util

import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Teleportation

object EventsSystem {
    data class Event(
        val name: String,
        val localization: Location
    )
    var eventActive: Event? = null

    fun startEvent(sender: Player, name: String) {
        if (!sender.hasPermission("lt7core.event.edit")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        eventActive = Event(
            name,
            sender.location
        )

        Bukkit.broadcast(TextComponent(""))
        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(LTCore.PREFIX + langManager.getString(sender, "event.created_global", mapOf("%s" to eventActive!!.name)))
        }
        Bukkit.broadcast(TextComponent(""))
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "event.created"))

        Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            if (eventActive?.name != name) return@Runnable
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(LTCore.PREFIX + langManager.getString(sender, "event.created_global", mapOf("%s" to eventActive!!.name)))
            }
        }, 900L, 900L)
    }

    fun endEvent(sender: Player) {
        if (!sender.hasPermission("lt7core.event.edit")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        if (eventActive == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "event.event_notfound"))
            return
        }

        eventActive = null

        Bukkit.broadcast(TextComponent(""))
        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(LTCore.PREFIX + langManager.getString(sender, "event.ended_global"))
        }
        Bukkit.broadcast(TextComponent(""))
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "event.ended"))
    }

    fun joinEvent(sender: Player) {
        if (eventActive == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "event.event_notfound"))
            return
        }
        val tp = Teleportation(sender, eventActive!!.localization).start {}
    }
}