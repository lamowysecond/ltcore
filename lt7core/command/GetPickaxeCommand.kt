package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.system.util.StoneGeneratorsSystem

class GetPickaxeCommand  : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val speed: Int = if (args.isNotEmpty()) {
            try {
                args[0].toInt()
            } catch (e: NumberFormatException) {
                40
            }
        } else {
            40
        }
        val isAdmin = label == "getadminpickaxe" || label == "gapickaxe"
        StoneGeneratorsSystem.giveGenerator(sender as Player, speed, isAdmin)
        return true
    }
}