package pl.lamas.lt7core.system.util

import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.scheduler.BukkitTask
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.LTCore.Companion.langManager

object AntiLogoutSystem : Listener {

    val playersInBattle: MutableList<Player> = mutableListOf()

    @EventHandler(ignoreCancelled = true)
    private fun onKill(event: PlayerDeathEvent) {
        playersInBattle.removeIf { it.name == event.entity.name }
        event.player.damage(1000.0, event.damageSource)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onDamage(event: EntityDamageByEntityEvent) {
        if (event.damager !is Player || event.entity !is Player) return
        val damager = event.damager as Player
        val entity = event.entity as Player

        if (playersInBattle.all { damager.name != it.name}) {
            handleHit(damager)
        }

        if (playersInBattle.all { entity.name != it.name }) {
            handleHit(entity)
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onPlayerQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
        val player = event.player
        if (playersInBattle.any { it.name == player.name }) {
            player.damage(1000.0)
            playersInBattle.removeIf { it.name == player.name }
        }
    }


    init {
        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
    }

    private fun handleHit(entity: Player) {
        playersInBattle.add(entity)
        entity.sendMessage(LTCore.PREFIX + langManager.getString(entity, "antilogout.inbattle"))

        var secondsLeft: Int = config.antiLogoutTime + 1

        var task: BukkitTask? = null
        task = Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            if (playersInBattle.none { entity.name == it.name }) {
                task?.cancel()
                return@Runnable
            }
            if (secondsLeft == 0) {
                entity.sendMessage(LTCore.PREFIX + langManager.getString(entity, "antilogout.outbattle"))
                entity.sendActionBar(TextComponent(langManager.getString(entity, "antilogout.outbattle_actionbar")))
                task?.cancel()
                return@Runnable
            }
            entity.sendActionBar(TextComponent(langManager.getString(entity, "antilogout.actionbar", mapOf(
                "%i" to secondsLeft.toString()
            ))))

            secondsLeft -= 1
        }, 0L, 20L)
    }
}