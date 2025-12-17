package pl.lamas.lt7core.system.admin

import io.papermc.paper.event.player.AsyncChatEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.util.IgnoresSystem

object ChatSystem : Listener {
    var isChatEnabled: Boolean = true
    var chatCooldown: Int = config.chatCooldownDefault

    val cooldownsPlayers: MutableMap<String, Int> = mutableMapOf()

    @EventHandler(priority = EventPriority.HIGHEST)
    private fun onChat(event: AsyncChatEvent) {
        if (!isChatEnabled && !event.player.hasPermission("lt7core.chat.bypass_blockade")) {
            event.player.sendMessage(LTCore.PREFIX + langManager.getString(event.player, "chat.blocked"))
            event.isCancelled = true
            return
        }

        if (cooldownsPlayers.containsKey(event.player.name)) {
            event.player.sendMessage(LTCore.PREFIX + langManager.getString(event.player, "chat.cooldown",
                mapOf(
                    "%i" to cooldownsPlayers[event.player.name].toString()
                )
            ))
            event.isCancelled = true
            return
        }

        cooldownsPlayers[event.player.name] = chatCooldown
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onChatLowest(event: AsyncChatEvent) {
        val message = event.message() as net.kyori.adventure.text.TextComponent
        val content = message.content().lowercase()

        for (part in content.split(" ")) {
            if (!part.contains("@")) continue
            val username = part.replace("@", "")
            val targetPlayer = Bukkit.getPlayerExact(username) ?: continue

            if (IgnoresSystem.isIgnoring(targetPlayer.name, event.player.name)) continue

            targetPlayer.sendActionBar(langManager.getString(targetPlayer, "chat.mentioned",
                mapOf(
                    "%s" to event.player.name
                )
            ))
            targetPlayer.playSound(targetPlayer.location, Sound.BLOCK_BELL_USE, 1.0f, 1.0f)
        }
    }

    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)

        Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            cooldownsPlayers.forEach {
                if (it.value == 0) {
                    cooldownsPlayers.remove(it.key)
                    return@forEach
                }
                cooldownsPlayers[it.key] = it.value - 1
            }
        }, 20L, 20L)
    }

    fun changeChatStatus(sender: Player) {
        if (!sender.hasPermission("lt7core.chat.toggle")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        if (isChatEnabled) {
            isChatEnabled = false
            Bukkit.broadcast(TextComponent(""))
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(LTCore.PREFIX + langManager.getString(sender, "chat.disabled_global", mapOf("%s" to sender.name)))
            }
            Bukkit.broadcast(TextComponent(""))
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "chat.disabled"))
        } else {
            isChatEnabled = true
            Bukkit.broadcast(TextComponent(""))
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(LTCore.PREFIX + langManager.getString(sender, "chat.enabled_global", mapOf("%s" to sender.name)))
            }
            Bukkit.broadcast(TextComponent(""))
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "chat.enabled"))
        }
    }

    fun clearChat(sender: Player) {
        if (!sender.hasPermission("lt7core.chat.clear")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }
        for (i in 1..100) {
            Bukkit.broadcast(TextComponent(""))
        }
        Bukkit.broadcast(TextComponent(""))
        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(LTCore.PREFIX + langManager.getString(sender, "chat.cleared_global", mapOf("%s" to sender.name)))
        }
        Bukkit.broadcast(TextComponent(""))
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "chat.cleared"))
    }
}