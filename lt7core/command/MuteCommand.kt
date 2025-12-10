package pl.lamas.lt7core.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.admin.PunishmentSystem
import pl.lamas.lt7core.util.Utils
import pl.lamas.lt7core.util.punishments.PunishmentReasonType

class MuteCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val senderP = sender as Player
        if (args.size < 3) {
            senderP.sendMessage(LTCore.PREFIX + langManager.getString(senderP, "command.usage", mapOf("%usage" to "/mute <player> <time> <reason>")))
            return false
        }
        val targetName = args[0]
        val time = Utils.parseTime(args[1])
        val reasonType = PunishmentReasonType.entries.find { it.name.contentEquals(args.slice(2 until args.size).joinToString(" "), true) } ?: PunishmentReasonType.OTHER
        val reason = if (reasonType == PunishmentReasonType.OTHER) { args.slice(2 until args.size).joinToString(" ")} else null

        PunishmentSystem.mutePlayer(senderP, targetName, reasonType, reason, time)
        return true
    }
}