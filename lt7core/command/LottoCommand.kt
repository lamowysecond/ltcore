package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.autoevent.LottoSystem

class LottoCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as org.bukkit.entity.Player, "command.usage", mapOf("%usage" to "/lotto <draw|leave|info>")))
            return true
        }

        when (args[0].lowercase()) {
            "draw" -> {
                if (args.size < 2) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as org.bukkit.entity.Player, "command.usage", mapOf("%usage" to "/lotto draw <number-of-tickets>")))
                    return false
                }
                LottoSystem.addLottoEntry(sender as org.bukkit.entity.Player, args[1].toIntOrNull()!!)
                return true
            }
            "leave" -> {
                LottoSystem.removeEntries(sender as org.bukkit.entity.Player)
                return true
            }
            "info" -> {
                LottoSystem.viewLottoInfo(sender as org.bukkit.entity.Player)
                return true
            }
            else -> {
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender as org.bukkit.entity.Player, "command.usage", mapOf("%usage" to "/lotto <draw|leave|info>")))
                return true
            }
        }
    }
}