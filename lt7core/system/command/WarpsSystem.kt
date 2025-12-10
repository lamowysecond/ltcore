package pl.lamas.lt7core.system.command

import org.bukkit.Location
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Teleportation
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType

object WarpsSystem {
    data class Warp(
        val name: String,
        val aliases: List<String>,
        val location: Location
    )

    @FilePersisted("warps.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var warps: MutableList<Warp> = mutableListOf()

    fun warp(sender: Player, warpName: String?) {
        if (warpName == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "warps.available_warps_header"))
            warps.forEach {
                sender.sendMessage(LTCore.PREFIX + it.name)
            }
            return
        }

        val warp = warps.firstOrNull { it.name.equals(warpName, true) || it.aliases.any { alias -> alias.equals(warpName, true) } }
        if (warp == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "warps.warp_not_found", mapOf("%w" to (warpName ?: ""))))
            return
        }

        Teleportation(
            sender,
            warp.location
        ).start {  }
    }

    fun addWarp(sender: Player, name: String?) {
        if (!sender.hasPermission("lt7core.warps.edit")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        if (name.isNullOrBlank()) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "warps.invalid_name"))
            return
        }

        if (warps.any { it.name.equals(name, true) || it.aliases.any { alias -> alias.equals(name, true) } }) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "warps.warp_exists", mapOf("%w" to name)))
            return
        }

        val warpName = name
        warps.add(
            Warp(
                name = warpName,
                aliases = listOf(),
                location = sender.location
            )
        )

        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "warps.warp_added", mapOf("%w" to warpName)))
    }

    fun removeWarp(sender: Player, name: String?) {
        if (!sender.hasPermission("lt7core.warps.edit")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        if (name.isNullOrBlank()) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "warps.invalid_name"))
            return
        }

        val warp = warps.firstOrNull { it.name.equals(name, true) || it.aliases.any { alias -> alias.equals(name, true) } }
        if (warp == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "warps.warp_not_found", mapOf("%w" to name)))
            return
        }

        warps.remove(warp)
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "warps.warp_removed", mapOf("%w" to warp.name)))
    }
}