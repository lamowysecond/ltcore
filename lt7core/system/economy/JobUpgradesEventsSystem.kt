package pl.lamas.lt7core.system.economy

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Animals
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.BrewerInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.system.economy.JobSystem.Upgrade
import pl.lamas.lt7core.util.Utils.itemStack
import java.util.UUID

object JobUpgradesEventsSystem : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: org.bukkit.event.block.BlockBreakEvent) {
        val player = event.player
        val job = JobSystem.getPlayerJob(player)
    }

    /* =========================
       WOOD STRIPPING
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onWoodStrip(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val block = event.clickedBlock ?: return
        val item = player.inventory.itemInMainHand

        if (!item.type.name.endsWith("_AXE")) return
        if (!block.type.name.endsWith("_LOG")) return
        if (!hasUpgrade(player, Upgrade.LUMBERJACK_WOOD_STRIPPING)) return

        val stripped = getStripped(block) ?: return

        block.type = stripped
        item.damage(1, player)
    }

    /* =========================
       APPLES & GOLDEN APPLES
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onLeavesDrop(event: BlockDropItemEvent) {
        if (!event.blockState.type.name.endsWith("_LEAVES")) return

        val player = event.player ?: return
        val job = JobSystem.getPlayerJob(player)

        if (Upgrade.LUMBERJACK_APPLES_DROPPING !in job.upgradesUnlocked) return

        val appleChance =
            if (Upgrade.LUMBERJACK_APPLES_DROP_INCREASE in job.upgradesUnlocked) 0.05
            else 0.02

        val goldenChance =
            if (Upgrade.LUMBERJACK_GOLDEN_DROPPING in job.upgradesUnlocked) 0.005
            else 0.0

        if (Math.random() < appleChance) {
            dropItem(event, Material.APPLE)
        }

        if (Math.random() < goldenChance) {
            dropItem(event, Material.GOLDEN_APPLE)
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun dropItem(event: BlockDropItemEvent, material: Material) {
        val item = ItemStack(material)
        val drop = event.block.world.dropItemNaturally(event.block.location, item)
        event.items.add(drop)
    }

    /* =========================
       AXE DAMAGE BONUS
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onAxeDamage(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val weapon = player.inventory.itemInMainHand

        if (!weapon.type.name.endsWith("_AXE")) return

        val job = JobSystem.getPlayerJob(player)

        var multiplier = 1.0

        if (Upgrade.LUMBERJACK_AXE_DAMAGE_INCREASE in job.upgradesUnlocked) {
            multiplier += 0.03
        }

        if (Upgrade.LUMBERJACK_AXE_DAMAGE_INCREASE_TWO in job.upgradesUnlocked) {
            multiplier += 0.075
        }

        if (multiplier == 1.0) return

        event.damage *= multiplier
    }

    /* =========================
       HELPERS
       ========================= */
    private fun hasUpgrade(player: Player, upgrade: Upgrade): Boolean {
        val job = JobSystem.getPlayerJob(player)
        return upgrade in job.upgradesUnlocked
    }

    private fun getStripped(block: Block): Material? {
        return try {
            Material.valueOf("STRIPPED_${block.type.name}")
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /* =========================
       MORE CROPS (3 TIERS)
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onCropDrop(event: BlockDropItemEvent) {
        val player = event.player ?: return
        val blockType = event.blockState.type

        // interesują nas tylko uprawy
        if (!isCrop(blockType)) return

        val job = JobSystem.getPlayerJob(player)

        val chance = when {
            Upgrade.FARMER_MORE_CROPS_THIRD_TIER in job.upgradesUnlocked -> 0.30
            Upgrade.FARMER_MORE_CROPS_SECOND_TIER in job.upgradesUnlocked -> 0.20
            Upgrade.FARMER_MORE_CROPS_FIRST_TIER in job.upgradesUnlocked -> 0.10
            else -> 0.0
        }

        if (chance == 0.0) return

        if (Math.random() < chance) {
            val extra = ItemStack(blockType)
            val drop = event.block.world.dropItemNaturally(
                event.block.location, extra
            )
            event.items.add(drop)
        }
    }

    /* =========================
       ANIMAL FOOD (MEAT BONUS)
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onAnimalDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        val entity = event.entity

        if (entity !is Animals) return

        val job = JobSystem.getPlayerJob(killer)

        val multiplier = when {
            Upgrade.FARMER_ANIMAL_FOOD_SECOND_TIER in job.upgradesUnlocked -> (1..2).random()
            Upgrade.FARMER_ANIMAL_FOOD_FIRST_TIER in job.upgradesUnlocked -> 1
            else -> 0
        }

        if (multiplier == 0) return

        event.drops
            .filter { it.type.isEdible }
            .forEach { it.amount += multiplier }
    }

    /* =========================
       ADDITIONAL RANDOM CROP
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onAdditionalCrop(event: BlockDropItemEvent) {
        val player = event.player
        val blockType = event.blockState.type

        if (!isCrop(blockType)) return

        val job = JobSystem.getPlayerJob(player)
        if (Upgrade.FARMER_CROPS_ADDITIONAL_CROP !in job.upgradesUnlocked) return

        val extra = ItemStack(blockType)
        val drop = event.block.world.dropItemNaturally(
            event.block.location, extra
        )
        event.items.add(drop)
    }

    /* =========================
       HELPERS
       ========================= */
    private fun isCrop(material: Material): Boolean {
        return material == Material.WHEAT ||
                material == Material.CARROTS ||
                material == Material.POTATOES ||
                material == Material.BEETROOTS ||
                material == Material.NETHER_WART ||
                material == Material.SWEET_BERRIES ||
                material == Material.COCOA ||
                material == Material.MELON ||
                material == Material.PUMPKIN
    }

    /* =========================
       MORE MOB DROPS (2 TIERS)
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onMobDrops(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        val job = JobSystem.getPlayerJob(killer)

        // tier 1
        var dropChance = if (Upgrade.HUNTER_MORE_MOB_DROPS_FIRST_TIER in job.upgradesUnlocked) 0.10 else 0.0
        // tier 2
        if (Upgrade.HUNTER_MORE_MOB_DROPS_SECOND_TIER in job.upgradesUnlocked) dropChance = 0.20

        if (dropChance == 0.0) return

        // dodanie kości do dropów
        if (Math.random() < dropChance) {
            event.drops.forEach {
                it.amount += 1
            }
        }
    }

    /* =========================
       WEAPON DAMAGE BONUS vs MOBS
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onWeaponDamage(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val weapon = player.inventory.itemInMainHand
        if (!weapon.type.name.contains("SWORD")) return

        val job = JobSystem.getPlayerJob(player)
        if (Upgrade.HUNTER_WEAPON_DAMAGE_INCREASE_FOR_MOBS !in job.upgradesUnlocked) return
        if (event.entity !is Player) { // tylko moby
            event.damage *= 1.10
        }
    }

    /* =========================
       MOB EXP INCREASE
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onMobExp(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        val job = JobSystem.getPlayerJob(killer)

        if (Upgrade.HUNTER_MOBS_EXP_INCREASE !in job.upgradesUnlocked) return

        // Paper pozwala modyfikować exp:
        event.droppedExp = (event.droppedExp * 1.15).toInt()
    }

    data class ComboData(
        var hits: Int,
        var lastHitTime: Long
    )

    // mapujemy gracza → combo
    private val comboMap = mutableMapOf<UUID, ComboData>()
    private val COMBO_WINDOW_MS = 60_000L // 60 sekund
    private val REQUIRED_HITS = 3

    @EventHandler(ignoreCancelled = true)
    fun onHunterHit(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val job = JobSystem.getPlayerJob(player)
        if (Upgrade.HUNTER_COMBO_STRENGTH_EFFECT !in job.upgradesUnlocked) return

        // reset jeśli sam został uderzony
        if (event.entity is Player) {
            val victim = event.entity as Player
            if (comboMap.containsKey(victim.uniqueId)) {
                comboMap.remove(victim.uniqueId)
            }
        }

        val now = System.currentTimeMillis()
        val combo = comboMap[player.uniqueId]

        if (combo == null || now - combo.lastHitTime > COMBO_WINDOW_MS) {
            // pierwszy hit
            comboMap[player.uniqueId] = ComboData(1, now)
        } else {
            // kolejne hity
            combo.hits += 1
            combo.lastHitTime = now

            if (combo.hits >= REQUIRED_HITS) {
                applyStrength(player)
                comboMap.remove(player.uniqueId) // reset po udanym combo
            } else {
                comboMap[player.uniqueId] = combo
            }
        }

        // scheduler, który usuwa combo po 60s
        object : BukkitRunnable() {
            override fun run() {
                val c = comboMap[player.uniqueId] ?: return
                if (System.currentTimeMillis() - c.lastHitTime > COMBO_WINDOW_MS) {
                    comboMap.remove(player.uniqueId)
                    cancel()
                }
            }
        }.runTaskLater(LTCore.instance, COMBO_WINDOW_MS / 50) // zamiana ms → tick (1 tick = 50ms)
    }

    private fun applyStrength(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, 20 * 10, 1)) // 10s strength II
        player.location.world.playSound(player.location, org.bukkit.Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.0f, 1.0f)
        player.location.world.spawnParticle(
            org.bukkit.Particle.SONIC_BOOM,
            player.location.add(0.0, 1.0, 0.0),
            5,
            0.5,
            1.0,
            0.5,
            0.1
        )
    }

    /* =========================
       ANVIL DAMAGE DECREASE
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onAnvilDamage(event: PrepareAnvilEvent) {
        val inv = event.inventory
        val player = inv.viewers.firstOrNull() as? Player ?: return
        val job = JobSystem.getPlayerJob(player)

        if (Upgrade.BLACKSMITH_ANVIL_DAMAGE_DECREASE !in job.upgradesUnlocked) return

        val result: ItemStack = inv.result ?: return
        // zminimalizowanie szansy na uszkodzenie narzędzia
        inv.result = result.clone() // Paper nie ma bezpośredniego dmg API → placeholder
    }

    /* =========================
       ANVIL COST DECREASE (2 TIERS)
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onAnvilCost(event: PrepareAnvilEvent) {
        val inv = event.inventory
        val player = inv.viewers.firstOrNull() as? Player ?: return
        val job = JobSystem.getPlayerJob(player)

        var multiplier = 1.0
        if (Upgrade.BLACKSMITH_ANVIL_COST_DECREASE in job.upgradesUnlocked) multiplier -= 0.10
        if (Upgrade.BLACKSMITH_ANVIL_COST_DECREASE_TWO in job.upgradesUnlocked) multiplier -= 0.15

        if (multiplier < 1.0) {
            event.view.repairCost = (event.view.repairCost * multiplier).toInt().coerceAtLeast(0)
        }
    }

    /* =========================
       MAX LEVEL ENCHANTS
       ========================= */
    @EventHandler(ignoreCancelled = true)
    fun onEnchantItem(event: EnchantItemEvent) {
        val player = event.enchanter
        val job = JobSystem.getPlayerJob(player)

        if (Upgrade.BLACKSMITH_MAX_LEVEL_ENCHANTS in job.upgradesUnlocked) return

        // ograniczamy poziom enchantów do max 5 zamiast vanilla
        val newLevels = event.enchantsToAdd.mapValues { (_, level) ->
            level.coerceAtMost(4)
        }
        event.enchantsToAdd.clear()
        event.enchantsToAdd.putAll(newLevels)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val player = event.viewers.firstOrNull() ?: return
        val inv = event.inventory
        val left = inv.getItem(0) ?: return
        val right = inv.getItem(1) ?: return
        val result = inv.result ?: return
        val job = JobSystem.getPlayerJob(player as? Player ?: return)

        if (Upgrade.BLACKSMITH_MAX_LEVEL_ENCHANTS in job.upgradesUnlocked) return

        // kopiujemy enchanty z lewej i prawej strony
        val combinedEnchants = mutableMapOf<Enchantment, Int>()
        for ((ench, lvl) in left.enchantments) combinedEnchants[ench] = lvl
        for ((ench, lvl) in right.enchantments) {
            val current = combinedEnchants.getOrDefault(ench, 0)
            // jeśli oba IV, nie pozwalamy na V
            val newLvl = if (current == 4 && lvl == 4) 4 else maxOf(current, lvl)
            combinedEnchants[ench] = newLvl.coerceAtMost(4)
        }

        // ustawiamy wynikowy item
        val meta = result.itemMeta
        meta?.enchants?.keys?.forEach { meta.removeEnchant(it) }
        for ((ench, lvl) in combinedEnchants) {
            meta?.addEnchant(ench, lvl, true)
        }
        result.itemMeta = meta
        inv.result = result
    }


    // =========================
    // BREWING SPEED INCREASE
    // =========================
    @EventHandler(ignoreCancelled = true)
    fun onBrewTwo(event: BrewEvent) {
        val brewer = event.contents.holder as? Player ?: return // jeśli chcesz, możesz targetować tylko gracza
        val job = JobSystem.getPlayerJob(brewer)
        var multiplier = 1.0
        if (Upgrade.ALCHEMIST_BREWING_SPEED_INCREASE in job.upgradesUnlocked) multiplier *= 0.8
        if (Upgrade.ALCHEMIST_BREWING_SPEED_INCREASE_TWO in job.upgradesUnlocked) multiplier *= 0.7

        // Paper nie daje bezpośrednio "speed", więc podmieniamy ticki lub czas symulowany
        // tutaj pokazuję symbolicznie – w praktyce trzeba by użyć własnego systemu schedulera
        // event.brewingTime = (event.brewingTime * multiplier).toInt() // deprecated w wielu wersjach
    }

    @EventHandler(ignoreCancelled = true)
    fun onBrew(event: BrewEvent) {
        val brewer = event.contents.holder as? Player ?: return
        val job = JobSystem.getPlayerJob(brewer)

        var durationMultiplier = 1.0
        if (Upgrade.ALCHEMIST_POTION_EFFECT_INCREASE in job.upgradesUnlocked) durationMultiplier *= 1.25
        if (Upgrade.ALCHEMIST_POTION_EFFECT_INCREASE_TWO in job.upgradesUnlocked) durationMultiplier *= 1.5

        val inv: BrewerInventory = event.contents

        for (i in 0 until inv.size) {
            val item = inv.getItem(i) ?: continue
            if (item.type != Material.POTION && item.type != Material.SPLASH_POTION && item.type != Material.LINGERING_POTION) continue
            val meta = item.itemMeta as? PotionMeta ?: continue

            // modyfikujemy wszystkie efekty mikstury
            meta.customEffects.forEach { effect ->
                val newEffect = PotionEffect(
                    effect.type,
                    (effect.duration * durationMultiplier).toInt(),
                    effect.amplifier,
                    effect.isAmbient,
                    effect.hasParticles(),
                    effect.hasIcon()
                )
                meta.removeCustomEffect(effect.type)
                meta.addCustomEffect(newEffect, true)
            }

            item.itemMeta = meta
            inv.setItem(i, item)
        }
    }

    // =========================
    // ITEMS DROP INCREASE
    // =========================
    private val alchemyDrops = setOf(
        Material.BLAZE_ROD,
        Material.GHAST_TEAR,
        Material.GLOWSTONE_DUST,
        Material.NETHER_WART
    )

    @EventHandler(ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer as? Player ?: return
        val job = JobSystem.getPlayerJob(killer)
        if (Upgrade.ALCHEMIST_ITEMS_DROP_INCREASE !in job.upgradesUnlocked) return

        event.drops.replaceAll { drop ->
            if (drop.type in alchemyDrops) {
                val amount = (drop.amount * 1.5).toInt().coerceAtLeast(1)
                drop.clone().apply { this.amount = amount }
            } else drop
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBroke(event: BlockBreakEvent) {
        val player = event.player
        val job = JobSystem.getPlayerJob(player)
        if (Upgrade.ALCHEMIST_ITEMS_DROP_INCREASE !in job.upgradesUnlocked) return

        when (event.block.type) {
            Material.NETHER_WART -> {
                val extra = (1..event.block.getDrops(player.inventory.itemInMainHand).size) .random()
                event.block.world.dropItemNaturally(event.block.location, itemStack(Material.NETHER_WART, extra))
            }
            else -> return
        }
    }
}