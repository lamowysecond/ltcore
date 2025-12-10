package pl.lamas.lt7core.system.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.util.FriendsSystem
import pl.lamas.lt7core.system.util.IgnoresSystem
import pl.lamas.lt7core.system.util.SettingsSystem
import pl.lamas.lt7core.util.Teleportation

object TpaSystem {
    data class TpaRequest(
        val sender: String,
        val target: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    val tpaRequests = mutableListOf<TpaRequest>()

    fun sendTpa(player: Player, target: String) {
        if (tpaRequests.any { it.sender == player.name && it.target == target }) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.request_already_made"))
            return
        }

        val targetPlayer = Bukkit.getPlayer(target)
        if (targetPlayer == null) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.player_offline"))
            return
        }

        if (SettingsSystem.getSetting(target, "autoAcceptTpaRequestFromFriends") &&
            FriendsSystem.friends.getOrDefault(target, mutableListOf()).contains(player.name)
            ) {
            tpaRequests.add(TpaRequest(sender = player.name, target = target))
            acceptTpa(targetPlayer, player.name)
            return
        }

        if (!IgnoresSystem.isIgnoring(target, player.name)) {
            targetPlayer.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.request_received", mapOf("%p" to player.name)))
            targetPlayer.sendMessage(
                Component.text(langManager.getString(player, "tpa.request_accept_btn"))
                    .clickEvent(ClickEvent.runCommand("tpaccept ${player.name}"))
                    .hoverEvent(Component.text(langManager.getString(targetPlayer, "tpa.request_accept_hover")))
                    .append(Component.text(" §f| "))
                    .append(
                        Component.text(langManager.getString(targetPlayer, "tpa.request_deny_btn"))
                            .clickEvent(ClickEvent.runCommand("tpdeny ${player.name}"))
                            .hoverEvent(Component.text(langManager.getString(targetPlayer, "tpa.request_deny_hover")))
                    )
            )
        }

        tpaRequests.add(TpaRequest(sender = player.name, target = target))
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.request_sent", mapOf("%p" to target)))
        player.sendMessage(
            Component.text(langManager.getString(player, "tpa.request_cancel_btn"))
                .clickEvent(ClickEvent.runCommand("tpacancel $target"))
                .hoverEvent(Component.text(langManager.getString(player, "tpa.request_cancel_hover")))
        )
    }

    fun acceptTpa(player: Player, senderName: String) {
        val request = tpaRequests.find { it.sender == senderName && it.target == player.name }
        if (request == null) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.no_pending_request", mapOf("%p" to senderName)))
            return
        }

        val senderPlayer = Bukkit.getPlayer(senderName)
        if (senderPlayer == null) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.sender_offline", mapOf("%p" to senderName)))
            tpaRequests.remove(request)
            return
        }

        tpaRequests.remove(request)
        senderPlayer.sendMessage(LTCore.PREFIX + langManager.getString(senderPlayer, "tpa.request_accepted", mapOf("%p" to player.name)))
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.you_accepted", mapOf("%p" to senderName)))
        Teleportation(
            senderPlayer,
            player.location
        ).start {  }
    }

    fun denyTpa(player: Player, senderName: String) {
        val request = tpaRequests.find { it.sender == senderName && it.target == player.name }
        if (request == null) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.no_pending_request", mapOf("%p" to senderName)))
            return
        }

        tpaRequests.remove(request)
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.request_denied", mapOf("%p" to senderName)))
        Bukkit.getPlayer(senderName)?.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.request_denied_by", mapOf("%p" to player.name)))
    }

    fun cancelTpa(player: Player, target: String) {
        val request = tpaRequests.find { it.sender == player.name && it.target == target }
        if (request == null) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.no_pending_request", mapOf("%p" to target)))
            return
        }

        tpaRequests.remove(request)
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "tpa.request_canceled", mapOf("%p" to target)))
    }
}