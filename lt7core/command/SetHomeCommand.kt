package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.command.HomesSystem

class SetHomeCommand  : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/sethome <name>")))
            return false
        }
        HomesSystem.setHome(sender as Player, args[0])
        return true
    }
}