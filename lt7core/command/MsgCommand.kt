package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.command.MsgSystem

class MsgCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.size < 2) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/msg <player> <message>")))
            return false
        }
        MsgSystem.msg(sender as Player, args[0], args.slice(1 until args.size).joinToString(" "))
        return true
    }
}