package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.command.MsgSystem

class ReplyCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/reply <message>")))
        }
        MsgSystem.reply(sender as Player, args.joinToString(" "))
        return true
    }
}