package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.admin.ChatSystem

class ChatCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val player = sender as Player
        if (args.isEmpty()) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(player, "command.usage", mapOf("%c" to "/chat <clear|toggle>")))
            return false
        }
        when (args[0]) {
            "clear" -> {
                ChatSystem.clearChat(player)
            }
            "toggle" -> {
                ChatSystem.changeChatStatus(player)
            }
            else -> {
                sender.sendMessage(LTCore.PREFIX + langManager.getString(player, "command.usage", mapOf("%c" to "/chat <clear|toggle>")))
                return false
            }
        }
        return true
    }
}