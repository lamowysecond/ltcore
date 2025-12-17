package pl.lamas.lt7core.system.admin

import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.punishments.Ban
import pl.lamas.lt7core.util.punishments.PunishmentReasonType

object CheckingSystem : Listener {
    val playersInChecking: MutableMap<Player, Location> = mutableMapOf()

    @EventHandler
    private fun onMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return
        if (playersInChecking.any { it.key.name == event.player.name }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    private fun onChat(event: AsyncPlayerChatEvent) {
        if (event.player.hasPermission("lt7core.admin.checking")) return
        event.recipients.removeAll {
            it.name in playersInChecking.map { it.key.name }
        }
    }

    @EventHandler
    private fun onQuit(event: PlayerQuitEvent) {
        if (event.player !in playersInChecking.keys) return
        playersInChecking.remove(event.player)

        Bukkit.getScheduler().runTaskLater(LTCore.instance, Runnable {
            if (PunishmentSystem.bans.any { it.target == event.player.name || it.target == event.player.address?.address?.hostAddress }) return@Runnable
            PunishmentSystem.bans.add(
                Ban(
                    event.player.name,
                    null,
                    "AutoMod",
                    PunishmentReasonType.QUITTING_WHILE_BEING_CHECKED,
                    null
                )
            )
        }, 5L)
    }

    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
    }

    fun checkPlayer(sender: Player, player: Player) {
        if (!sender.hasPermission("lt7core.admin.checking")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        if (playersInChecking.contains(player)) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "checking.player_inlist"))
            return
        }

        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "checking.success_adding", mapOf("%s" to player.name)))
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "checking.youre_checked"))
        player.sendTitle(langManager.getString(player, "checking.youre_checked_title"), langManager.getString(player, "checking.youre_checked_subtitle"), 20, 50, 20)

        playersInChecking[player] = player.location

        player.teleport(Location(
            Bukkit.getWorlds().find { it.name == "world" },
            config.checkingPlaceCoords[0],
            config.checkingPlaceCoords[1],
            config.checkingPlaceCoords[2],
            config.checkingPlaceCoords[3].toFloat(),
            config.checkingPlaceCoords[4].toFloat()
        ))
    }

    fun freePlayer(sender: Player, player: Player) {
        if (!sender.hasPermission("lt7core.admin.checking")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        if (!playersInChecking.contains(player)) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "checking.player_notinlist"))
            return
        }

        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "checking.success_removing", mapOf("%s" to player.name)))
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "checking.youre_free"))

        player.teleport(playersInChecking[player]!!)
        playersInChecking.remove(player)
    }
}