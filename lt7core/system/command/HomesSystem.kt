package pl.lamas.lt7core.system.command

import org.bukkit.Location
import org.bukkit.entity.Player
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.util.Teleportation
import pl.lamas.lt7core.util.Utils.getRank

object HomesSystem {
    data class Home(
        val name: String,
        val location: Location
    )

    @FilePersisted("homes.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 300L)
    var homes: MutableMap<String, MutableList<Home>> = mutableMapOf()

    fun home(sender: Player, homeName: String) {
        val playerHomes = homes[sender.name] ?: mutableListOf()
        val home = playerHomes.firstOrNull { it.name.equals(homeName, ignoreCase = true) }
        if (home == null) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "homes.home_not_found", mapOf("%h" to homeName)))
            return
        }

        Teleportation(
            sender,
            home.location
        ).start {  }
    }

    fun setHome(sender: Player, homeName: String) {
        val playerHomes = homes.getOrPut(sender.name) { mutableListOf() }
        if (playerHomes.any { it.name.equals(homeName, ignoreCase = true) }) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "homes.home_already_exists", mapOf("%h" to homeName)))
            return
        }

        if (!homeName.contains(Regex("^[a-zA-Z0-9_]{1,16}$"))) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "homes.invalid_home_name", mapOf("%h" to homeName)))
            return
        }

        if (playerHomes.size >= LTCore.config.maxHomesPerPlayer[sender.getRank()]!!) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "homes.home_limit_reached", mapOf("%m" to LTCore.config.maxHomesPerPlayer.toString())))
            return
        }

        playerHomes.add(
            Home(
                name = homeName,
                location = sender.location
            )
        )

        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "homes.home_set_success", mapOf("%h" to homeName)))
    }

    fun deleteHome(sender: Player, homeName: String) {
        val playerHomes = homes[sender.name] ?: mutableListOf()
        val home = playerHomes.firstOrNull { it.name.equals(homeName, ignoreCase = true) }
        if (home == null) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "homes.home_not_found", mapOf("%h" to homeName)))
            return
        }

        playerHomes.remove(home)
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "homes.home_deleted_success", mapOf("%h" to homeName)))
    }
}