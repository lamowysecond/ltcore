package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.admin.PunishmentSystem

class UnbanIPCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val senderP = sender as Player
        if (args.isEmpty()) {
            senderP.sendMessage(LTCore.PREFIX + langManager.getString(senderP, "command.usage", mapOf("%usage" to "/unbanIP <ip>")))
            return false
        }
        val targetName = args[0]

        PunishmentSystem.unbanIP(senderP, targetName)
        return true
    }
}