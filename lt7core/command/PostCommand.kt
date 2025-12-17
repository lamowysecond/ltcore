package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.economy.ActionHouseSystem

class PostCommand  : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val price = args.getOrNull(0)?.toDoubleOrNull()
            ?: run {
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as org.bukkit.entity.Player, "command.usage", mapOf("%usage" to "/post <price>")))
                return false
            }
        ActionHouseSystem.addItemToActionHouse(sender as Player, price)
        return true
    }
}