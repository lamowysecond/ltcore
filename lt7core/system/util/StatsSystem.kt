package pl.lamas.lt7core.system.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.LTCore

object StatsSystem {
    data class Stats(
        var kills: Int,
        var deaths: Int,
        var playTimeSeconds: Long,
        var playTimeSecondsThisWeek: Long
    )

    @FilePersisted(filePath = "stats.json", persistType = pl.lamas.lt7core.util.filesystem.PersistType.READ_SAVE, autoSaveIntervalSeconds = 300L)
    var stats: MutableMap<String, Stats> = mutableMapOf()

    @EventHandler(ignoreCancelled = true)
    private fun onDeath(event: PlayerDeathEvent) {
        val victimName = event.entity.name
        val killerName = event.entity.killer?.name

        if (event.entity.killer !is Player) {
            return
        }

        val victimStats = stats.getOrPut(victimName) { Stats(0, 0, 0L, 0L) }
        victimStats.deaths += 1

        if (killerName != null) {
            val killerStats = stats.getOrPut(killerName) { Stats(0, 0, 0L, 0L) }
            killerStats.kills += 1
        }
    }

    init {
        Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            stats.forEach {
                _, stat ->
                stat.playTimeSeconds += 31L
                stat.playTimeSecondsThisWeek += 31L
            }

            val now = java.time.LocalDateTime.now()
            if (now.dayOfWeek == java.time.DayOfWeek.MONDAY && now.hour == 0 && now.minute == 0) {
                stats.values.forEach { it.playTimeSecondsThisWeek = 0L }
            }
        }, 0L, 31 * 20L)
    }

    private fun formatPlayTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return String.format("%02dh %02dm", hours, minutes)
    }

    fun sendStats(sender: Player) {
        val playerStats = stats.getOrPut(sender.name) { Stats(0, 0, 0L, 0L) }
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "stats.info", mapOf(
            "%k" to playerStats.kills.toString(),
            "%d" to playerStats.deaths.toString(),
            "%p" to formatPlayTime(playerStats.playTimeSeconds),
            "%w" to formatPlayTime(playerStats.playTimeSecondsThisWeek)
        )))
    }
}