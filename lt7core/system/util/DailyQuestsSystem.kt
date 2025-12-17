package pl.lamas.lt7core.system.util

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils.addItemOrDrop
import pl.lamas.lt7core.util.Utils.itemStack
import java.time.LocalDateTime

object DailyQuestsSystem {
    data class DailyQuestDay(
        val dayNumber: Int,
        val items: List<ItemStack>
    )

    data class DailyQuestStreak(
        var streakNumber: Int,
        var hasCompletedToday: Boolean = false
    )

    val questDays = listOf(
        DailyQuestDay(1, listOf(
            itemStack(Material.IRON_INGOT, 32),
            itemStack(Material.COOKED_BEEF, 32),
            itemStack(Material.IRON_PICKAXE)
        )),
        DailyQuestDay(2, listOf(
            itemStack(Material.IRON_SWORD),
            itemStack(Material.COAL, 32),
            itemStack(Material.BOW, enchantments = mapOf(Enchantment.POWER to 2)),
        )),
        DailyQuestDay(3, listOf(
            StoneGeneratorsSystem.betterGeneratorItem.clone().apply {
                this.amount = 4
            },
            itemStack(Material.GOLD_INGOT, 32),
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(Enchantment.MENDING to 1))
        )),
        DailyQuestDay(4, listOf(
            itemStack(Material.DIAMOND),
            itemStack(Material.LAPIS_LAZULI, 16),
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(Enchantment.EFFICIENCY to 3))
        )),
        DailyQuestDay(5, listOf(
            itemStack(Material.DIAMOND, 8),
            itemStack(Material.ANVIL),
            itemStack(Material.LAPIS_LAZULI, 16)
        )),
        DailyQuestDay(6, listOf(
            itemStack(Material.DIAMOND_PICKAXE, enchantments = mapOf(Enchantment.UNBREAKING to 2)),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.SWIFTNESS
                this.itemMeta = meta
            },
            itemStack(Material.GOLD_INGOT, 32)
        )),
        DailyQuestDay(7, listOf(
            itemStack(Material.DIAMOND_SWORD, enchantments = mapOf(Enchantment.SHARPNESS to 3)),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.REGENERATION
                this.itemMeta = meta
            },
            itemStack(Material.OBSIDIAN, 8)
        )),
        DailyQuestDay(8, listOf(
            itemStack(Material.DIAMOND, 8),
            StoneGeneratorsSystem.betterGeneratorItem.clone().apply {
                this.amount = 4
            },
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.STRENGTH
                this.itemMeta = meta
            }
        )),
        DailyQuestDay(9, listOf(
            itemStack(Material.OBSIDIAN, 32),
            itemStack(Material.LAVA_BUCKET),
            itemStack(Material.WATER_BUCKET)
        )),
        DailyQuestDay(10, listOf(
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(
                Enchantment.EFFICIENCY to 4,
                Enchantment.UNBREAKING to 3
            )),
            itemStack(Material.GOLDEN_APPLE, 8),
            itemStack(Material.ENDER_PEARL, 16)
        )),
        DailyQuestDay(11, listOf(
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.FIRE_RESISTANCE
                this.itemMeta = meta
            },
            itemStack(Material.NAME_TAG),
            StoneGeneratorsSystem.betterGeneratorItem.clone().apply {
                this.amount = 4
            },
        )),
        DailyQuestDay(12, listOf(
            itemStack(Material.TOTEM_OF_UNDYING),
            itemStack(Material.EMERALD, 16),
            itemStack(Material.SPLASH_POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.INVISIBILITY
                this.itemMeta = meta
            }
        )),
        DailyQuestDay(13, listOf(
            itemStack(Material.GOLD_INGOT, 48),
            itemStack(Material.EMERALD, 64),
            itemStack(Material.SLIME_BALL, 16)
        )),
        DailyQuestDay(14, listOf(
            itemStack(Material.DIAMOND, 5),
            itemStack(Material.ENDER_CHEST),
            itemStack(Material.FIREWORK_ROCKET, 32)
        )),
        DailyQuestDay(15, listOf(
            itemStack(Material.DIAMOND_PICKAXE, enchantments = mapOf(Enchantment.FORTUNE to 2)),
            StoneGeneratorsSystem.betterGeneratorItem.clone().apply {
                this.amount = 4
            },
            itemStack(Material.ENDER_PEARL, 16)
        )),
        DailyQuestDay(16, listOf(
            itemStack(Material.DIAMOND_CHESTPLATE, enchantments = mapOf(Enchantment.PROTECTION to 2)),
            StoneGeneratorsSystem.betterGeneratorItem.clone().apply {
                this.amount = 4
            },
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.REGENERATION
                this.itemMeta = meta
            }
        )),
        DailyQuestDay(17, listOf(
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(Enchantment.MENDING to 1)),
            itemStack(Material.BOW, enchantments = mapOf(Enchantment.POWER to 2)),
            StoneGeneratorsSystem.betterGeneratorItem.clone().apply {
                this.amount = 4
            },
        )),
        DailyQuestDay(18, listOf(
            itemStack(Material.DIAMOND_SWORD, enchantments = mapOf(
                Enchantment.KNOCKBACK to 2,
                Enchantment.SHARPNESS to 1
            )),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.STRENGTH
                this.itemMeta = meta
            },
            itemStack(Material.GOLD_INGOT, 16)
        )),
        DailyQuestDay(19, listOf(
            itemStack(Material.BOW, enchantments = mapOf(
                Enchantment.POWER to 4,
                Enchantment.UNBREAKING to 3,
                Enchantment.MENDING to 1
            )),
            itemStack(Material.ENDER_CHEST),
            itemStack(Material.OBSIDIAN, 64)
        )),
        DailyQuestDay(20, listOf(
            itemStack(Material.NAME_TAG),
            itemStack(Material.ANVIL),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.STRENGTH
                this.itemMeta = meta
            }
        )),
        DailyQuestDay(21, listOf(
            itemStack(
                listOf(
                    Material.DIAMOND_BOOTS,
                    Material.DIAMOND_LEGGINGS,
                    Material.DIAMOND_HELMET,
                    Material.DIAMOND_CHESTPLATE
                ).random(),
                enchantments = mapOf(
                    Enchantment.PROTECTION to 2,
                    Enchantment.UNBREAKING to 1
                )
            ),
            itemStack(Material.DIAMOND, 4),
            itemStack(Material.GOLDEN_CARROT, 32)
        )),
        DailyQuestDay(22, listOf(
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.FIRE_RESISTANCE
                this.itemMeta = meta
            },
            itemStack(Material.EMERALD, 16),
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(
                Enchantment.PROTECTION to 1
            )),
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(
                Enchantment.POWER to 2,
            ))
        )),
        DailyQuestDay(23, listOf(
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(
                Enchantment.FORTUNE to 2
            )),
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(
                Enchantment.SHARPNESS to 2
            )),
            itemStack(Material.SADDLE),
            itemStack(Material.FIREWORK_ROCKET, 64)
        )),
        DailyQuestDay(24, listOf(
            itemStack(Material.DIAMOND_PICKAXE, enchantments = mapOf(Enchantment.UNBREAKING to 2)),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.STRENGTH
                this.itemMeta = meta
            },
            itemStack(Material.BLAZE_ROD, 16)
        )),
        DailyQuestDay(25, listOf(
            itemStack(Material.TOTEM_OF_UNDYING),
            itemStack(Material.DIAMOND, 6),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.REGENERATION
                this.itemMeta = meta
            }
        )),
        DailyQuestDay(26, listOf(
            StoneGeneratorsSystem.betterGeneratorItem.clone().apply {
                this.amount = 4
            },
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(Enchantment.MENDING to 1)),
            itemStack(Material.ENDER_CHEST)
        )),
        DailyQuestDay(27, listOf(
            itemStack(Material.DIAMOND_SWORD, enchantments = mapOf(
                Enchantment.SHARPNESS to 3,
                Enchantment.UNBREAKING to 2
            )),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.SLOW_FALLING
                this.itemMeta = meta
            },
            itemStack(Material.ENDER_PEARL, 16)
        )),
        DailyQuestDay(28, listOf(
            itemStack(Material.DIAMOND, 4),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.STRENGTH
                this.itemMeta = meta
            },
            itemStack(Material.GOLDEN_CARROT, 32)
        )),
        DailyQuestDay(29, listOf(
            itemStack(Material.ENCHANTED_BOOK, enchantments = mapOf(
                Enchantment.EFFICIENCY to 3,
                Enchantment.PROTECTION to 3
            )),
            itemStack(Material.ANVIL),
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.SWIFTNESS
                this.itemMeta = meta
            }
        )),
        DailyQuestDay(30, listOf(
            itemStack(Material.DIAMOND, 24),
            StoneGeneratorsSystem.betterGeneratorItem.clone().apply {
                this.amount = 8
            },
            itemStack(Material.POTION).apply {
                val meta = this.itemMeta as? org.bukkit.inventory.meta.PotionMeta
                meta?.basePotionType = org.bukkit.potion.PotionType.REGENERATION
                this.itemMeta = meta
            }
        ))
    )


    @FilePersisted("questStreaks.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var questStreaks: MutableMap<String, DailyQuestStreak> = mutableMapOf()

    @FilePersisted("lastQuestReset.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var lastQuestResetDayOfYear: Int = LocalDateTime.now().dayOfYear

    init {
        Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            if (LocalDateTime.now().dayOfYear != lastQuestResetDayOfYear) {
                questStreaks.forEach { _, streak ->
                    if (!streak.hasCompletedToday) {
                        streak.streakNumber = 0
                    }

                    streak.hasCompletedToday = false
                }
                lastQuestResetDayOfYear = LocalDateTime.now().dayOfYear
            }
        }, 0L, 60 * 20L)
    }

    fun getDailyQuestForPlayer(sender: Player) {
        val streak = questStreaks.getOrPut(sender.name) { DailyQuestStreak(0, false) }
        val dayNumber = (streak.streakNumber % questDays.size) + 1
        val dailyQuestDay = questDays.firstOrNull { it.dayNumber == dayNumber }

        if (dailyQuestDay == null) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "daily-quests.no_quest_found"))
            return
        }

        if (streak.hasCompletedToday) {
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "daily-quests.already_completed_today"))
            return
        }

        sender.addItemOrDrop(dailyQuestDay.items.random())
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "daily-quests.item_drawn", mapOf("%d" to dayNumber.toString())))
        streak.streakNumber += 1
        streak.hasCompletedToday = true
    }
}