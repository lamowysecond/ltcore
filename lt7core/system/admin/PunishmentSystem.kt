package pl.lamas.lt7core.system.admin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.lamas.lt7core.util.filesystem.PersistType
import org.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils.itemStack
import pl.lamas.lt7core.util.gui.Gui
import pl.lamas.lt7core.util.gui.GuiButton
import pl.lamas.lt7core.util.punishments.Ban
import pl.lamas.lt7core.util.punishments.Mute
import pl.lamas.lt7core.util.punishments.PunishmentReasonType
import pl.lamas.lt7core.util.punishments.Warn

object PunishmentSystem : Listener {

    @FilePersisted(filePath = "punishments/bans.json", persistType = PersistType.READ_SAVE)
    val bans: MutableList<Ban> = mutableListOf()

    @FilePersisted(filePath = "punishments/mutes.json", persistType = PersistType.READ_SAVE)
    val mutes: MutableList<Mute> = mutableListOf()

    @FilePersisted(filePath = "punishments/warns.json", persistType = PersistType.READ_SAVE)
    val warns: MutableList<Warn> = mutableListOf()

    @EventHandler
    private fun onChatEvent(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
        val mute = mutes.find { it.target == event.player.name.toString() }
        if (mute != null) {
            event.isCancelled = true
            event.player.sendMessage(LTCore.PREFIX + langManager.getString(event.player, "admin.mute.chat_blocked_message", mapOf("%r" to (mute.reason ?: langManager.getString(event.player, mute.reasonType.nameKey!!)), "%d" to (langManager.formatTimestamp(event.player, mute.timestampUntil.let { (mute.timestampUntil - (System.currentTimeMillis() / 1000L)).toString() }.toLong())))))
        }
    }

    @EventHandler
    private fun onLoginEvent(event: org.bukkit.event.player.PlayerLoginEvent) {
        val ban = bans.find { it.target == event.player.name.toString() }
        if (ban != null) {
            event.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_BANNED, langManager.getString(event.player, "admin.ban.login_blocked_message", mapOf("%r" to (ban.reason ?: langManager.getString(event.player, ban.reasonType.nameKey!!)), "%d" to (ban.timestampUntil?.let { langManager.formatTimestamp(event.player, it.let { (ban.timestampUntil - (System.currentTimeMillis() / 1000L)).toString() }.toLong()) } ?: langManager.getString(event.player, "admin.ban.permanent")))))
            return
        }
        val ipAddress = event.address.hostAddress ?: return
        val ipBan = bans.find { it.target == ipAddress }

        if (ipBan != null) {
            event.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_BANNED, langManager.getString(event.player, "admin.ban.login_blockedip_message", mapOf("%r" to (ipBan.reason ?: langManager.getString(event.player, ipBan.reasonType.nameKey!!)), "%d" to (ipBan.timestampUntil?.let { langManager.formatTimestamp(event.player, it.let { (ipBan.timestampUntil - (System.currentTimeMillis() / 1000L)).toString() }.toLong()) } ?: langManager.getString(event.player, "admin.ban.permanent")))))
        }
    }

    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
    }

    fun punishmentsList(sender: Player, targetNickname: String?) {
        val target = targetNickname ?: sender.name
        val targetWarns = warns.filter { it.target == target }

        Gui(
            id = "punishments_list_$target",
            title = langManager.getString(sender, "admin.punishments_list.gui_title", mapOf("%t" to target)),
            rows = 6,
            holder = sender,
            buttons = targetWarns.map {
                GuiButton(
                    icon = itemStack(Material.GRAY_WOOL, name = langManager.getString(sender, "admin.punishments.warn_wool_name"),
                        lore = listOf(
                            langManager.getString(sender, "admin.punishments.warn_wool_lore_line_1", mapOf("%r" to (it.reason ?: langManager.getString(sender, it.reasonType.nameKey!!)))),
                            langManager.getString(sender, "admin.punishments.warn_wool_lore_line_2", mapOf("%a" to it.adminName)),
                            langManager.getString(sender, "admin.punishments.warn_wool_lore_line_3", mapOf("%d" to langManager.formatTimestamp(sender, it.timestampCreated)))
                        )
                    ),
                    slot = targetWarns.indexOf(it) + 10,
                    onClick = { event ->
                        if (event.whoClicked.hasPermission("lt7core.admin.unwarn")) {
                            warns.remove(it)
                            event.whoClicked.sendMessage(LTCore.PREFIX + langManager.getString(event.whoClicked as Player, "admin.unmute.success", mapOf("%t" to target)))
                            punishmentsList(event.whoClicked as Player, target)
                        } else {
                            event.whoClicked.sendMessage(LTCore.PREFIX + langManager.getString(event.whoClicked as Player, "no_permission"))
                        }
                    }
                )
            }.toMutableList().apply {
                val ban = bans.findLast { it.target == target }
                if (ban != null) {
                    add(
                        GuiButton(
                            icon = itemStack(Material.RED_WOOL, name = langManager.getString(sender, "admin.punishments.ban_wool_name"),
                                lore = listOf(
                                    langManager.getString(sender, "admin.punishments.ban_wool_lore_line_1", mapOf("%r" to (ban.reason ?: langManager.getString(sender, ban.reasonType.nameKey!!)))),
                                    langManager.getString(sender, "admin.punishments.ban_wool_lore_line_2", mapOf("%a" to ban.adminName)),
                                    langManager.getString(sender, "admin.punishments.ban_wool_lore_line_3", mapOf("%d" to (ban.timestampUntil?.let { langManager.formatTimestamp(sender, (it - (System.currentTimeMillis() / 1000L)).toLong()) } ?: langManager.getString(sender, "admin.date_never"))))
                                )
                            ),
                            slot = 16,
                            onClick = { event ->
                                if (event.whoClicked.hasPermission("lt7core.admin.unban")) {
                                    bans.remove(ban)
                                    event.whoClicked.sendMessage(LTCore.PREFIX + langManager.getString(event.whoClicked as Player, "admin.unban.success", mapOf("%t" to target)))
                                    punishmentsList(event.whoClicked as Player, target)
                                } else {
                                    event.whoClicked.sendMessage(LTCore.PREFIX + langManager.getString(event.whoClicked as Player, "no_permission"))
                                }
                            }
                        )
                    )
                    return@apply
                }
                val mute = mutes.findLast { it.target == target }
                if (mute != null) {
                    add(
                        GuiButton(
                            icon = itemStack(Material.YELLOW_WOOL, name = langManager.getString(sender, "admin.punishments.mute_wool_name"),
                                lore = listOf(
                                    langManager.getString(sender, "admin.punishments.mute_wool_lore_line_1", mapOf("%r" to (mute.reason ?: langManager.getString(sender, mute.reasonType.nameKey!!)))),
                                    langManager.getString(sender, "admin.punishments.mute_wool_lore_line_2", mapOf("%a" to mute.adminName)),
                                    langManager.getString(sender, "admin.punishments.mute_wool_lore_line_3", mapOf("%d" to langManager.getString(sender, mute.timestampUntil.let { (mute.timestampUntil - (System.currentTimeMillis() / 1000L)).toString() })))
                                )
                            ),
                            slot = 16,
                            onClick = { event ->
                                if (event.whoClicked.hasPermission("lt7core.admin.unmute")) {
                                    mutes.remove(mute)
                                    event.whoClicked.sendMessage(LTCore.PREFIX + langManager.getString(event.whoClicked as Player, "admin.unmute.success", mapOf("%t" to target)))
                                    punishmentsList(event.whoClicked as Player, target)

                                    val player = Bukkit.getPlayer(mute.target)
                                    if (player != null && player.isOnline) {
                                        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "admin.unmute.notify_message", mapOf("%a" to event.whoClicked.name)))
                                    }
                                } else {
                                    event.whoClicked.sendMessage(LTCore.PREFIX + langManager.getString(event.whoClicked as Player, "no_permission"))
                                }
                            }
                        )
                    )
                } else {
                    add(
                        GuiButton(
                            icon = itemStack(Material.LIME_WOOL, name = langManager.getString(sender, "admin.punishments.no_active_punishments_wool_name")),
                            slot = 16,
                        )
                    )
                }
            }
        ).open()
    }

    fun unbanIP(sender: Player, targetIP: String) {
        if (!sender.hasPermission("lt7core.admin.unbanip")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        val ban = bans.find { it.target == targetIP }
        if (ban != null) {
            bans.remove(ban)
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "admin.unbanip.success", mapOf("%t" to targetIP)))
        } else {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "admin.unbanip.not_found"))
        }
    }

    fun banIP(sender: Player, targetIP: String, reasonType: PunishmentReasonType, reason: String?, durationSeconds: Long?) {
        if (!sender.hasPermission("lt7core.admin.banip")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        val ban = Ban(
            target = targetIP,
            reason = reason,
            adminName = sender.name,
            reasonType = reasonType,
            timestampUntil = durationSeconds?.let { (System.currentTimeMillis() / 1000L) + it }
        )
        bans.add(ban)
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "admin.banip.success", mapOf("%t" to targetIP)))

        LTCore.instance.server.onlinePlayers.forEach { player ->
            val playerIP = player.address?.address?.hostAddress
            if (playerIP == targetIP) {
                player.kickPlayer(langManager.getString(player, "admin.banip.kick_message", mapOf("%r" to (reason ?: langManager.getString(player, reasonType.nameKey!!)), "%a" to sender.name, "%d" to (durationSeconds?.toString() ?: langManager.getString(player, "admin.ban.permanent")))))
            }
        }
    }

    fun banPlayer(sender: Player, target: Player, reasonType: PunishmentReasonType, reason: String?, durationSeconds: Long?) {
        if (!sender.hasPermission("lt7core.admin.ban")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        val ban = Ban(
            target = target.uniqueId.toString(),
            reason = reason,
            adminName = sender.name,
            reasonType = reasonType,
            timestampUntil = durationSeconds?.let { (System.currentTimeMillis() / 1000L) + it }
        )
        bans.add(ban)
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "admin.ban.success", mapOf("%t" to target.name)))

        if (target.isOnline) {
            target.kickPlayer(langManager.getString(target, "admin.ban.kick_message", mapOf("%r" to (reason ?: langManager.getString(target, reasonType.nameKey!!)), "%a" to sender.name, "%d" to (durationSeconds?.toString() ?: langManager.getString(target, "admin.ban.permanent")))))
        }
    }

    fun mutePlayer(sender: Player, target: Player, reasonType: PunishmentReasonType, reason: String?, durationSeconds: Long) {
        if (!sender.hasPermission("lt7core.admin.mute")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        val mute = Mute(
            target = target.uniqueId.toString(),
            reason = reason,
            adminName = sender.name,
            reasonType = reasonType,
            timestampUntil = durationSeconds.let { (System.currentTimeMillis() / 1000L) + it }
        )
        mutes.add(mute)
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "admin.mute.success", mapOf("%t" to target.name)))

        if (target.isOnline) {
            target.sendMessage(LTCore.PREFIX + langManager.getString(target, "admin.mute.notify_message", mapOf("%r" to (reason ?: langManager.getString(target, reasonType.nameKey!!)), "%a" to sender.name, "%d" to (durationSeconds?.toString()!!))))
        }
    }

    fun warnPlayer(sender: Player, target: Player, reasonType: PunishmentReasonType, reason: String?) {
        if (!sender.hasPermission("lt7core.admin.warn")) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "no_permission"))
            return
        }

        val warn = Warn(
            target = target.uniqueId.toString(),
            reason = reason ?: langManager.getString(target, reasonType.nameKey!!),
            adminName = sender.name,
            reasonType = reasonType
        )
        warns.add(warn)
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "admin.warn.success", mapOf("%t" to target.name)))

        if (target.isOnline) {
            target.sendMessage(LTCore.PREFIX + langManager.getString(target, "admin.warn.notify_message", mapOf("%r" to (reason ?: langManager.getString(target, reasonType.nameKey!!)), "%a" to sender.name)))
        }
    }
}