package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.util.EventsSystem

class EventCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        when (args.getOrNull(0)?.lowercase()) {
            "start" -> {
                if (args.getOrNull(1) == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/event start <name>")))
                    return true
                }
                EventsSystem.startEvent(sender as Player, args[1] )
            }
            "end" -> {
                EventsSystem.endEvent(sender as Player )
            }
            "join", "dolacz" -> {
                EventsSystem.joinEvent(sender as Player)
            }
            else -> {
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as Player, "command.usage", mapOf("%usage" to "/event <start|end|join> [name]")))
            }
        }
        return true
    }
}