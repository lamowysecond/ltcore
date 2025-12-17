package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.economy.ChangesSystem

class ExchangeCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.size < 2) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/change <player> <amount>")))
            return false
        }
        val targetPlayerName = args[0]
        val amount = args[1].replace(",", ".").toDoubleOrNull()

        if (amount == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.invalid_amount"))
            return false
        }
        ChangesSystem.sendChangeRequest(sender as Player, targetPlayerName, amount)
        return true
    }
}