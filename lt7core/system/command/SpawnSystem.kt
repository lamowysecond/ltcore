package pl.lamas.lt7core.system.command

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.lamas.lt7core.util.Teleportation

object SpawnSystem {
    fun spawn(sender: Player) {
        Teleportation(
            sender,
            Bukkit.getWorlds().find { it.name == "world" }!!.spawnLocation
        ).start {

        }
    }
}