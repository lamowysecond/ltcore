package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.system.economy.MoneySystem

class PayCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.size < 2) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/pay <player> <amount>")))
            return false
        }
        val money = args[1].toDoubleOrNull()
        if (money == null || money <= 0) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender as Player, "command.invalid_amount"))
            return false
        }
        MoneySystem.payCommand(sender as Player, args[0], money)
        return true
    }
}