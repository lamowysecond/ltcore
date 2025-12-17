package pl.lamas.lt7core.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.admin.CheckingSystem

class FreeCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty() || Bukkit.getPlayer(args[0]) == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/free <player>")))
            return false
        }
        CheckingSystem.freePlayer(sender as Player, Bukkit.getPlayer(args[0])!!)
        return true
    }
}