package pl.lamas.lt7core.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerMoveEvent
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager

class Teleportation(
    val player: Player,
    val location: Location,
    val timeWaiting: Int = 3,
) : org.bukkit.event.Listener {
    private var haveStarted = false
    private var haveMoved = false

    @EventHandler
    private fun onMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return
        if (!haveStarted) return
        if (haveMoved) return
        if (event.player.name != player.name) return

        event.player.sendMessage(LTCore.PREFIX + "tp.player_moved")
        haveMoved = true
    }

    fun start(callback: (Boolean) -> Unit) {
        if (haveStarted) {
            throw IllegalStateException("Teleportation already started!")
        }

        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
        haveStarted = true

        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tp.wait", mapOf("%i" to timeWaiting.toString())))

        Bukkit.getScheduler().runTaskLater(LTCore.instance, Runnable {
            if (!haveMoved) {
                player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tp.player_notmoved"))
                player.teleport(location)
            }
            val h = haveStarted
            haveStarted = false

            // po wykonaniu schedulera zwracamy wynik
            HandlerList.unregisterAll(this)
            callback(haveMoved)
        }, 20L * timeWaiting)
    }
}