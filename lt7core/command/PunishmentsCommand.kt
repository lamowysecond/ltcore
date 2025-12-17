package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.system.admin.PunishmentSystem

class PunishmentsCommand  : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        PunishmentSystem.punishmentsList(sender as Player, args[0])
        return true
    }
}