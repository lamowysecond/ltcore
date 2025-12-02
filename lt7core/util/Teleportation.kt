package pl.lamas.lt7core.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerMoveEvent
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager

class Teleportation(
    val player: Player,
    val location: Location,
    val timeWaiting: Int = 3,
) {
    private var haveStarted = false
    private var haveMoved = false

    @EventHandler
    private fun onMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return
        if (!haveStarted) return
        if (event.player.name != player.name) return

        event.player.sendMessage(LTCore.PREFIX + "tp.player_moved")
    }

    fun start(callback: (Boolean) -> Unit) {
        if (haveStarted) {
            throw IllegalStateException("Teleportation already started!")
        }

        Bukkit.getScheduler().runTaskLater(LTCore.instance, Runnable {
            if (!haveMoved) {
                player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tp.player_notmoved"))
                player.teleport(location)
            }

            // po wykonaniu schedulera zwracamy wynik
            callback(haveMoved)
        }, 20L * timeWaiting)
    }
}