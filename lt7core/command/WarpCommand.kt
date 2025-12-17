package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.system.command.WarpsSystem

class WarpCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        when (args.getOrNull(0)) {
            "create" -> {
                WarpsSystem.addWarp(sender as Player, args.getOrNull(1))
            }
            "delete" -> {
                WarpsSystem.removeWarp(sender as Player, args.getOrNull(1))
            }
            else -> {
                WarpsSystem.warp(sender as Player, args.getOrNull(0))
            }
        }
        return true
    }
}