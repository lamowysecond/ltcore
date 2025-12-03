package pl.lamas.lt7core.system.command

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.util.IgnoresSystem

object MsgSystem {
    val msgRepliers: MutableMap<String, String> = mutableMapOf()

    fun reply(sender: Player, message: String) {
        val recieverName = msgRepliers[sender.name]
        if (recieverName == null) {
            sender.sendMessage(langManager.getString(sender, "msg.no-reply-target"))
            return
        }
        msg(sender, recieverName, message)
    }

    fun msg(sender: Player, reciever: String, message: String) {
        val playerReciever = Bukkit.getPlayer(reciever)
        if (playerReciever == null) {
            sender.sendMessage(langManager.getString(sender, "msg.player-not-found"))
            return
        }

        if (!IgnoresSystem.isIgnoring(reciever, sender.name)) {
            playerReciever.sendMessage(
                langManager.getString(sender, "msg.reciever", mapOf("%s" to sender.name, "%m" to message))
            )
            return
        }
        sender.sendMessage(
            langManager.getString(sender, "msg.sender", mapOf("%r" to playerReciever.name, "%m" to message))
        )

        msgRepliers[playerReciever.name] = sender.name
        msgRepliers[sender.name] = playerReciever.name
    }
}