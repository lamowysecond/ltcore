package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.util.FriendsSystem

class FriendsCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        when (args.getOrNull(0)?.lowercase()) {
            "add" -> {
                if (args.getOrNull(1) == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/friends add <player>")))
                    return false
                }
                FriendsSystem.addFriend(sender as Player, args[1])
            }
            "remove" -> {
                if (args.getOrNull(1) == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/friends remove <player>")))
                    return false
                }
                FriendsSystem.removeFriend(sender as Player, args[1])
            }
            "cancel" -> {
                if (args.getOrNull(1) == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/friends cancel <player>")))
                    return false
                }
                FriendsSystem.cancelFriendRequest(sender as Player, args[1])
            }
            "list" -> FriendsSystem.listOfFriends(sender as Player)
            else -> sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/friends <add|remove|list|cancel> [player]")))
        }
        return true
    }
}