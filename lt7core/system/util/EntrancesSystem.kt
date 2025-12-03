package pl.lamas.lt7core.system.util

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.lamas.lt7core.util.filesystem.FilePersisted
import org.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils.hasPersistentKey
import pl.lamas.lt7core.util.Utils.itemStack

object EntrancesSystem : Listener {

    data class EntranceEffects(
        var netherTimeEnd: Long = 0,
        var endTimeEnd: Long = 0
    )

    val endEntranceItem = itemStack(
        Material.PAPER,
        name = "§6§l§kk §r§eEnd Ticket §6§l§kk",
        lore = listOf(
            "§7Base time: &810 minutes&7.",
            "§7Use this ticket to enter the §eEnd§7 dimension.",
        )
    ).apply {
        val meta = this.itemMeta
        meta!!.persistentDataContainer.set(
            org.bukkit.NamespacedKey(LTCore.instance, "entrance.end_ticket"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "time:10"
        )
        this.itemMeta = meta
    }

    val netherEntranceItem = itemStack(
        Material.PAPER,
        name = "§4§l§kk §r§cNether Ticket §4§l§kk",
        lore = listOf(
            "§7Base time: &810 minutes&7.",
            "§7Use this ticket to enter the §cNether§7 dimension.",
        )
    ).apply {
        val meta = this.itemMeta
        meta!!.persistentDataContainer.set(
            org.bukkit.NamespacedKey(LTCore.instance, "entrance.nether_ticket"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "time:10"
        )
        this.itemMeta = meta
    }

    @FilePersisted("entrance_tickets.json", persistType = PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    val entranceEffects: MutableMap<String, EntranceEffects> = mutableMapOf()

    @EventHandler(ignoreCancelled = true)
    private fun onClick(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        if (item.hasPersistentKey("entrance.nether_ticket")) {
            val effects = entranceEffects.getOrPut(player.name) { EntranceEffects() }
            val currentTime = System.currentTimeMillis()
            effects.netherTimeEnd = currentTime + 10 * 60 * 1000L
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "entrances.used.nether_ticket"))
            item.amount -= 1
            event.isCancelled = true
        } else if (item.hasPersistentKey("entrance.end_ticket")) {
            val effects = entranceEffects.getOrPut(player.name) { EntranceEffects() }
            val currentTime = System.currentTimeMillis()
            effects.endTimeEnd = currentTime + 10 * 60 * 1000L
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "entrances.used.end_ticket"))
            item.amount -= 1
            event.isCancelled = true
        }
    }


    init {
        Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            Bukkit.getOnlinePlayers().forEach {
                player ->
                val effects = entranceEffects.getOrPut(player.name) { EntranceEffects() }
                val currentTime = System.currentTimeMillis()

                if (effects.netherTimeEnd < currentTime && effects.netherTimeEnd != 0L) {
                    effects.netherTimeEnd = 0L
                    player.sendMessage(LTCore.PREFIX + langManager.getString(player, "entrances.system.nether_time_expired"))
                    return@forEach
                }

                if (effects.endTimeEnd < currentTime && effects.endTimeEnd != 0L) {
                    effects.endTimeEnd = 0L
                    player.sendMessage(LTCore.PREFIX + langManager.getString(player, "entrances.system.end_time_expired"))
                    return@forEach
                }

                if (effects.netherTimeEnd < currentTime && player.world.environment == org.bukkit.World.Environment.NETHER) {
                    player.addPotionEffect(PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 10 * 20, 1, false, true, true))
                    player.addPotionEffect(PotionEffect(org.bukkit.potion.PotionEffectType.WITHER, 20, 0, false, true, true))
                    player.addPotionEffect(PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 7 * 20, 2, false, true, true))
                } else if (effects.endTimeEnd < currentTime && player.world.environment == org.bukkit.World.Environment.THE_END) {
                    player.addPotionEffect(PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 10 * 20, 1, false, true, true))
                    player.addPotionEffect(PotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION, 20, 0, false, true, true))
                    player.addPotionEffect(PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 7 * 20, 2, false, true, true))
                }
            }
        }, 0L, 5 * 20L)

        LTCore.instance.server.pluginManager.registerEvents(this, LTCore.instance)
    }
}