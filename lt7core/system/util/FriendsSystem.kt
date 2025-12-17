package pl.lamas.lt7core.system.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager

object FriendsSystem : Listener {
    data class FriendRequest(
        val fromPlayer: String,
        val toPlayer: String,
        val timestampCreated: Long = System.currentTimeMillis() / 1000L
    )

    @FilePersisted("friends.json", persistType = PersistType.READ_SAVE, autoSaveIntervalSeconds = 300L)
    var friends: MutableMap<String, MutableList<String>> = mutableMapOf()

    val pendingFriendRequests: MutableList<FriendRequest> = mutableListOf()

    fun addFriend(player: Player, friend: String) {
        if (pendingFriendRequests.any { it.fromPlayer == player.name && it.toPlayer == friend }) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.request_already_made"))
            return
        }

        if (pendingFriendRequests.any { it.fromPlayer == friend && it.toPlayer == player.name }) {
            pendingFriendRequests.removeIf { it.fromPlayer == friend && it.toPlayer == player.name }
            friends.getOrDefault(player.name, mutableListOf()).add(friend)
            friends.getOrDefault(friend, mutableListOf()).add(player.name)

            player.sendMessage(
                LTCore.PREFIX + langManager.getString(
                    player,
                    "friends.friend_added",
                    mapOf("%p" to friend)
                )
            )

            Bukkit.getPlayer(friend)?.sendMessage(
                LTCore.PREFIX + langManager.getString(
                    player,
                    "friends.friend_accepted",
                    mapOf("%p" to friend)
                )
            )
        }

        val friendPlayer = Bukkit.getPlayer(friend)
        if (friendPlayer == null) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.player_offline"))
            return
        }

        if (!IgnoresSystem.isIgnoring(friend, player.name)) {
            friendPlayer.sendMessage(LTCore.PREFIX + langManager.getString(friendPlayer, "friends.request_received", mapOf("%p" to player.name)))
            friendPlayer.sendMessage(Component.text(langManager.getString(friendPlayer, "friends.request_accept_btn"))
                .clickEvent(ClickEvent.runCommand("friend add ${player.name}"))
                .hoverEvent(Component.text(langManager.getString(friendPlayer, "friends.request_accept_hover")))

                .append(
                    Component.text(" §f| ")
                ).append(
                    Component.text(langManager.getString(friendPlayer, "friends.request_deny_btn"))
                        .clickEvent(ClickEvent.runCommand("friend remove ${player.name}"))
                        .hoverEvent(Component.text(langManager.getString(friendPlayer, "friends.request_deny_hover")))
                )
            )
        }
        pendingFriendRequests.add(FriendRequest(fromPlayer = player.name, toPlayer = friend))
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.request_sent", mapOf("%p" to friend)))
        player.sendMessage(Component.text(langManager.getString(player, "friends.request_cancel_btn"))
            .clickEvent(ClickEvent.runCommand("friend cancel $friend"))
            .hoverEvent(Component.text(langManager.getString(player, "friends.request_cancel_hover")))
        )
    }

    fun removeFriend(player: Player, friend: String) {
        if (pendingFriendRequests.any { it.fromPlayer == friend && it.toPlayer == player.name }) {
            pendingFriendRequests.removeIf { it.fromPlayer == friend && it.toPlayer == player.name }
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.request_denied", mapOf("%p" to friend)))
            Bukkit.getPlayer(friend)?.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.request_denied_by", mapOf("%p" to player.name)))
            return
        }

        val playerFriends = friends[player.name]
        if (playerFriends == null || !playerFriends.contains(friend)) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.not_your_friend", mapOf("%p" to friend)))
            return
        }

        playerFriends.remove(friend)
        friends[friend]?.remove(player.name)

        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.friend_removed", mapOf("%p" to friend)))
        Bukkit.getPlayer(friend)?.sendMessage(
            LTCore.PREFIX + langManager.getString(
                Bukkit.getPlayer(friend)!!,
                "friends.friend_removed_by",
                mapOf("%p" to player.name)
            )
        )
    }

    fun cancelFriendRequest(player: Player, friend: String) {
        val request = pendingFriendRequests.find { it.fromPlayer == player.name && it.toPlayer == friend }
        if (request == null) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.no_pending_request", mapOf("%p" to friend)))
            return
        }

        pendingFriendRequests.remove(request)
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.request_canceled", mapOf("%p" to friend)))
    }

    fun listOfFriends(player: Player) {
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "friends.list_header"))
        for (friend in friends.getOrDefault(player.name, mutableListOf())) {
            val online = Bukkit.getPlayer(friend) != null
            val coords = if (online) {
                if (SettingsSystem.getSetting(player.name, "friends_show_coords") != true) {
                    "§7[???]"
                }
                val friendPlayer = Bukkit.getPlayer(friend)!!
                "§a[${friendPlayer.location.blockX}, ${friendPlayer.location.blockY}, ${friendPlayer.location.blockZ}]"
            } else {
                "§c[Offline]"
            }

            player.sendMessage("§f$friend")
            player.sendMessage(langManager.getString(player, "friends.list_status", mapOf("%status" to if (online) "§aOnline" else "§cOffline")))
            player.sendMessage(langManager.getString(player, "friends.list_coords", mapOf("%coords" to coords)))
        }
    }
}