package pl.lamas.lt7core.system.guild

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.util.FriendsSystem.FriendRequest
import pl.lamas.lt7core.system.util.FriendsSystem.friends
import pl.lamas.lt7core.system.util.FriendsSystem.pendingFriendRequests
import pl.lamas.lt7core.util.Utils
import pl.lamas.lt7core.util.config.Config

object GuildSystem {
    data class WarDeclaration(
        val declaringGuild: String,
        val targetGuild: String,
        val timestampDeclared: Long = System.currentTimeMillis() / 1000L
    )

    data class Plot(
        val id: String,
        val location: Location,
        val radius: Int,
        val isCapital: Boolean = false
    )

    data class PeaceRequest(
        val declaringGuild: String,
        val targetGuild: String,
        val timestampDeclared: Long = System.currentTimeMillis() / 1000L
    )

    data class Guild(
        var name: String,
        val tag: String,
        var owner: String,
        val members: MutableList<String> = mutableListOf(owner),
        val wars: MutableList<String> = mutableListOf(),
        val warDeclarations: MutableList<WarDeclaration> = mutableListOf(),
        val plots: MutableList<Plot> = mutableListOf(),
        val createdTimestamp: Long = System.currentTimeMillis() / 1000L,
        var bankBalance: Double = 15000.0,
        var isBankBlocked: Boolean = false
    ) {
        var onlinePlayers: MutableList<Player> = mutableListOf()

        fun isInGuildArea(location: Location): Boolean {
            for (plot in plots) {
                val dx = plot.location.x - location.x
                val dz = plot.location.z - location.z
                if (dx * dx + dz * dz <= plot.radius * plot.radius) {
                    return true
                }
            }
            return false
        }
    }

    data class GuildRequest(
        val fromGuild: Guild,
        val toPlayer: String,
        val timestampCreated: Long = System.currentTimeMillis() / 1000L
    )

    val pendingGuildRequests: MutableList<GuildRequest> = mutableListOf()
    val pendingPeaceRequests: MutableList<PeaceRequest> = mutableListOf()

    @FilePersisted("guilds.json", persistType = pl.lamas.lt7core.util.filesystem.PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    val guilds: MutableMap<String, Guild> = mutableMapOf()

    @FilePersisted("guild_wars.json", persistType = pl.lamas.lt7core.util.filesystem.PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    val guildWars: MutableList<WarDeclaration> = mutableListOf()

    private fun isLocationInRadius(loc1: Location, loc2: Location, radius: Int): Boolean {
        val dx = loc1.x - loc2.x
        val dz = loc1.z - loc2.z
        return dx * dx + dz * dz <= radius * radius
    }

    private fun checkPermsToManageCore(player: Player, location: Location): Boolean {
        val plot = guilds.filter { it.value.plots.any { it.location == location } }
            .map { it.value }
            .firstOrNull() ?: return false

        if (!plot.wars.contains(getGuildByMember(player.name)?.name)) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "guild.plot_no_core_permission"))
            return true
        }
        return false
    }

    private fun checkPermsToManage(player: Player, location: Location): Boolean {
        val plot = guilds.filter { it.value.plots.any { isLocationInRadius(it.location, location, it.radius) } }
            .map { it.value }
            .firstOrNull() ?: return false

        if (!plot.members.contains(player.name) && (
                    plot.wars.none { guildTag -> guilds.filter { it.value.tag == guildTag }.values.firstOrNull()?.members?.contains(player.name) == true }
                            && plot.onlinePlayers.size / plot.members.size.toDouble() < config.guildsWarBuildRatio
                    )) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "guild.plot_no_build_permission"))
            return true
        }
        return false
    }

    @EventHandler(ignoreCancelled = true)
    private fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        Bukkit.getScheduler().runTaskLater(LTCore.instance, Runnable {
            if (Bukkit.getPlayer(player.name) != null) {
                guilds.values.forEach { guild ->
                    guild.onlinePlayers.removeIf { it.name == player.name }
                }
            }
        }, 20L * config.guildsOnlinePlayerDelay)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onBlockBuild(event: BlockPlaceEvent) {
        event.isCancelled = checkPermsToManage(event.player, event.block.location)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onBlockBreak(event: BlockBreakEvent) {
        if (checkPermsToManageCore(event.player, event.block.location)) {
            event.isCancelled = true
            val guild = guilds.filter { it.value.plots.any { event.block.location == it.location } }
                .map { it.value }
                .firstOrNull() ?: return

            val plot = guild.plots.firstOrNull { event.block.location == it.location } ?: return

            guild.plots.removeIf { it.location == plot.location }
            if (plot.isCapital) {
                guilds.remove(guild.name)

                event.player.sendMessage(LTCore.PREFIX + langManager.getString(event.player, "guild.capital_core_destroyed"))
                Utils.broadcast("guild.capital_core_destroyed_broadcast", mapOf("%g" to guild.name))
                return
            }
            event.player.sendMessage(LTCore.PREFIX + langManager.getString(event.player, "guild.plot_core_destroyed"))
            Utils.broadcast("guild.plot_core_destroyed_broadcast", mapOf("%g" to guild.name))
            return
        }
        event.isCancelled = checkPermsToManage(event.player, event.block.location)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onInteract(event: PlayerInteractEvent) {
        event.isCancelled = checkPermsToManage(event.player, event.player.location)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        if (event.damageSource is Player) {
            val damager = event.damageSource as Player
            event.isCancelled = checkPermsToManage(damager, player.location)
        }
    }

    fun getGuildByMember(playerName: String): Guild? {
        return guilds.values.firstOrNull { guild -> guild.members.contains(playerName) }
    }

    fun guildCommand(sender: Player, subArg: String, args: Array<String>) {
        when (subArg) {
            "info" -> {
                var guild = getGuildByMember(sender.name)
                if (guild == null && args.isEmpty()) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }
                guild = guilds.filter { it.value.tag == args.getOrNull(0) }
                    .map { it.value }
                    .firstOrNull() ?: return

                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.info.header", mapOf("%g" to guild.name)))
                sender.sendMessage(langManager.getString(sender, "guild.info.tag", mapOf("%t" to guild.tag)))
                sender.sendMessage(langManager.getString(sender, "guild.info.owner", mapOf("%p" to guild.owner)))
                sender.sendMessage(langManager.getString(sender, "guild.info.members_count", mapOf("%c" to guild.members.size.toString())))
                sender.sendMessage(langManager.getString(sender, "guild.info.bank_balance", mapOf("%b" to guild.bankBalance.toString())))
            }
            "delete" -> {
                val guild = getGuildByMember(sender.name)
                if (guild == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }

                val confirm = args.getOrNull(0)
                if (confirm != "confirm") {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.usage_delete", mapOf("%cmd" to "/guild delete confirm")))
                    return
                }

                if (guild.owner != sender.name) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_permission"))
                    return
                }

                guilds.remove(guild.name)
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.deleted_successfully", mapOf("%g" to guild.name)))
            }
            "create" -> {
                val guildName = args.getOrNull(0)
                val guildTag = args.getOrNull(1)

                if (guildName == null || guildTag == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.usage_create", mapOf("%cmd" to "/guild create <nazwa> <tag>")))
                    return
                }

                if (getGuildByMember(sender.name) != null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.already_in_guild"))
                    return
                }

                if (guilds.containsKey(guildName)) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.name_taken", mapOf("%g" to guildName)))
                    return
                }

                if (guilds.values.any { it.tag == guildTag }) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.tag_taken", mapOf("%t" to guildTag)))
                    return
                }

                for (guild in guilds.values) {
                    for (plot in guild.plots) {
                        if (isLocationInRadius(plot.location, sender.location, plot.radius + config.guildsMinDistanceBetweenPlots)) {
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.too_close_to_other_guild_plot"))
                            return
                        }
                    }
                }

                if (guildTag.length >= 5 || guildTag.length <= 1 || !guildTag.contains(Regex("^[A-Za-z]+$"))) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.tag_invalid_length"))
                    return
                }

                val newGuild = Guild(
                    name = guildName,
                    tag = guildTag,
                    owner = sender.name,
                    plots = mutableListOf(
                        Plot(
                            id = "Plot 1",
                            location = sender.location.clone().add(0.0, -1.0, 0.0),
                            radius = config.guildsPlotSize,
                            isCapital = true
                        )
                    )
                )

                sender.location.clone().apply {
                    y = 65.0
                }.block.type = Material.REINFORCED_DEEPSLATE

                guilds[guildName] = newGuild
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.created_successfully", mapOf("%g" to guildName, "%t" to guildTag)))
            }
            "war" -> {
                val guild = guilds.values.firstOrNull { it.tag == args.getOrNull(0) }
                if (guild == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }

                val guildByMember = getGuildByMember(sender.name)
                if (guildByMember == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }

                if (guildByMember.owner != sender.name) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_permission"))
                    return
                }

                if (guildWars.none { it.declaringGuild == guildByMember.tag && it.targetGuild == guild.tag }) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.already_at_war", mapOf("%g" to guild.name)))
                    return
                }

                guildWars.add(WarDeclaration(
                    declaringGuild = guildByMember.tag,
                    targetGuild = guild.tag
                ))
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.war_declaration_ready", mapOf("%g" to guild.name)))
            }
            "peace" -> {
                val guild = guilds.values.firstOrNull { it.tag == args.getOrNull(0) }
                if (guild == null) {
                   sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                   return
                }

                val guildByMember = getGuildByMember(sender.name)
                if (guildByMember == null) {
                   sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                   return
                }

                if (guildByMember.owner != sender.name) {
                   sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_permission"))
                   return
                }

                if (guildWars.none { it.declaringGuild == guildByMember.tag && it.targetGuild == guild.tag }) {
                   sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.not_at_war", mapOf("%g" to guild.name)))
                   return
                }

                // jeśli druga strona już wysłała prośbę o pokój — zaakceptuj
                val reversePending = pendingPeaceRequests.any { it.declaringGuild == guild.tag && it.targetGuild == guildByMember.tag }
                if (reversePending) {
                   guildWars.removeIf { it.declaringGuild == guildByMember.tag && it.targetGuild == guild.tag }
                   pendingPeaceRequests.removeIf { it.declaringGuild == guild.tag && it.targetGuild == guildByMember.tag }
                   sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.peace_declared", mapOf("%g" to guild.name)))
                   Bukkit.getPlayer(guild.owner)?.sendMessage(LTCore.PREFIX + langManager.getString(Bukkit.getPlayer(guild.owner)!!, "guild.peace_declared_by", mapOf("%g" to guildByMember.name)))
                   return
                }

                // utwórz żądanie pokoju i powiadom właściciela drugiej gildii
                pendingPeaceRequests.add(PeaceRequest(declaringGuild = guildByMember.tag, targetGuild = guild.tag))
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.peace_request_sent", mapOf("%g" to guild.name)))

                Bukkit.getPlayer(guild.owner)?.let { ownerPlayer ->
                   ownerPlayer.sendMessage(LTCore.PREFIX + langManager.getString(ownerPlayer, "guild.peace_request_received", mapOf("%g" to guildByMember.name)))
                   ownerPlayer.sendMessage(
                       Component.text(langManager.getString(ownerPlayer, "guild.accept_peace_btn"))
                           .clickEvent(ClickEvent.runCommand("guild peace ${guildByMember.tag}"))
                           .hoverEvent(Component.text(langManager.getString(ownerPlayer, "guild.accept_peace_hover")))
                   )
                }

                Bukkit.getScheduler().runTaskLater(LTCore.instance, Runnable {
                   pendingPeaceRequests.removeIf { it.declaringGuild == guildByMember.tag && it.targetGuild == guild.tag }
                }, 20L * config.friendsRequestTime)
            }
            "cancelwar" -> {
                val guild = guilds.values.firstOrNull { it.tag == args.getOrNull(0) }
                if (guild == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }

                val guildByMember = getGuildByMember(sender.name)
                if (guildByMember == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }

                if (guildByMember.owner != sender.name) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_permission"))
                    return
                }

                if (guildWars.none { it.declaringGuild == guildByMember.tag && it.targetGuild == guild.tag && it.timestampDeclared + config.guildsDeclarationTime * 60 <= System.currentTimeMillis() / 1000L }) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.not_at_war", mapOf("%g" to guild.name)))
                    return
                }

                guildWars.removeIf {
                    it.declaringGuild == guildByMember.tag && it.targetGuild == guild.tag
                }
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.war_canceled", mapOf("%g" to guild.name)))
            }
            "invite" -> {
                val target = args.getOrNull(0)
                if (target == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.usage_invite", mapOf("%cmd" to "/guild invite <gracz>")))
                    return
                }

                val guild = getGuildByMember(sender.name)
                if (guild == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }

                if (guild.owner != sender.name) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_permission"))
                    return
                }

                if (guild.members.contains(target)) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.already_member", mapOf("%p" to target)))
                    return
                }

                val targetPlayer = Bukkit.getPlayer(target)
                if (targetPlayer == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.player_offline", mapOf("%p" to target)))
                    return
                }

                targetPlayer.sendMessage(LTCore.PREFIX + langManager.getString(targetPlayer, "guild.invite_received", mapOf("%p" to sender.name, "%g" to guild.name)))
                targetPlayer.sendMessage(
                    Component.text(langManager.getString(targetPlayer, "guild.invite_accept_btn"))
                        .clickEvent(ClickEvent.runCommand("guild join ${guild.name}"))
                        .hoverEvent(Component.text(langManager.getString(targetPlayer, "guild.invite_accept_hover")))
                        .append(Component.text(" §f| "))
                        .append(
                            Component.text(langManager.getString(targetPlayer, "guild.invite_deny_btn"))
                                .clickEvent(ClickEvent.runCommand("guild deny ${guild.name}"))
                                .hoverEvent(Component.text(langManager.getString(targetPlayer, "guild.invite_deny_hover")))
                        )
                )

                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.invite_sent", mapOf("%p" to target, "%g" to guild.name)))
                sender.sendMessage(Component.text(langManager.getString(sender, "friends.request_cancel_btn"))
                    .clickEvent(ClickEvent.runCommand("guild cancel ${target}"))
                    .hoverEvent(Component.text(langManager.getString(sender, "friends.request_cancel_hover")))
                )

                pendingGuildRequests.add(GuildRequest(fromGuild = guild, toPlayer = target))

                Bukkit.getScheduler().runTaskLater(LTCore.instance, Runnable {
                    pendingGuildRequests.removeIf { it.fromGuild == guild && it.toPlayer == target }

                }, 20L * config.friendsRequestTime)
            }
            "accept" -> {
                val guildName = args.getOrNull(0)
                if (guildName == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.usage_accept"))
                    return
                }

                val guild = guilds[guildName]
                if (guild == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.guild_not_found", mapOf("%g" to guildName)))
                    return
                }

                if (guild.members.contains(sender.name)) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.already_member", mapOf("%p" to sender.name)))
                    return
                }

                val pendingRequest = pendingGuildRequests.any { it.fromGuild == guild && it.toPlayer == sender.name }
                if (!pendingRequest) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_invite_found", mapOf("%g" to guildName)))
                    return
                }

                pendingFriendRequests.removeIf { it.fromPlayer == guild.owner && it.toPlayer == sender.name }
                guild.members.add(sender.name)

                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.joined_guild", mapOf("%g" to guild.name)))
                Bukkit.getPlayer(guild.owner)?.sendMessage(
                    LTCore.PREFIX + langManager.getString(
                        sender,
                        "guild.member_joined",
                        mapOf("%p" to sender.name)
                    )
                )
            }
            "deny" -> {
                val guildName = args.getOrNull(0)
                if (guildName == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.usage_deny"))
                    return
                }

                val guild = guilds[guildName]
                if (guild == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.guild_not_found", mapOf("%g" to guildName)))
                    return
                }

                val pendingRequest = pendingGuildRequests.any { it.fromGuild == guild && it.toPlayer == sender.name }
                if (!pendingRequest) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_invite_found", mapOf("%g" to guildName)))
                    return
                }

                pendingGuildRequests.removeIf { it.fromGuild == guild && it.toPlayer == sender.name }
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.invite_denied", mapOf("%g" to guild.name)))
                Bukkit.getPlayer(guild.owner)?.sendMessage(
                    LTCore.PREFIX + langManager.getString(
                        sender,
                        "guild.invite_denied_by",
                        mapOf("%p" to sender.name)
                    )
                )
            }
            "cancel" -> {
                val target = args.getOrNull(0)
                if (target == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.usage_cancel"))
                    return
                }

                val guild = getGuildByMember(sender.name)
                if (guild == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }

                val pendingRequest = pendingGuildRequests.any { it.fromGuild == guild && it.toPlayer == target }
                if (!pendingRequest) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_invite_found", mapOf("%g" to guild.name)))
                    return
                }

                pendingGuildRequests.removeIf { it.fromGuild == guild && it.toPlayer == target }
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.invite_canceled", mapOf("%p" to target)))
            }
            "remove" -> {
                val target = args.getOrNull(0)
                if (target == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.usage_remove"))
                    return
                }

                val guild = getGuildByMember(sender.name)
                if (guild == null) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_guild"))
                    return
                }

                if (guild.owner != sender.name) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.no_permission"))
                    return
                }

                if (!guild.members.contains(target)) {
                    sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.not_a_member", mapOf("%p" to target)))
                    return
                }

                guild.members.remove(target)
                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "guild.member_removed", mapOf("%p" to target)))
                Bukkit.getPlayer(target)?.sendMessage(
                    LTCore.PREFIX + langManager.getString(
                        sender,
                        "guild.removed_from_guild",
                        mapOf("%g" to guild.name)
                    )
                )
            }
        }
    }
}