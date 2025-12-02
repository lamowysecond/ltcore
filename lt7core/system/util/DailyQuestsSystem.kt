package pl.lamas.lt7core.system.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.lamas.lt7core.util.filesystem.FilePersisted
import org.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils.addItemOrDrop
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

    val questDays: List<DailyQuestDay> = mutableListOf()

    @FilePersisted("questStreaks.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    val questStreaks: MutableMap<String, DailyQuestStreak> = mutableMapOf()

    @FilePersisted("lastQuestReset.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var lastQuestResetDayOfYear: Int = LocalDateTime.now().dayOfYear

    init {
        Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            if (LocalDateTime.now().hour == 0 && LocalDateTime.now().minute == 0 && LocalDateTime.now().dayOfYear != lastQuestResetDayOfYear) {
                questStreaks.forEach { _, streak ->
                    if (!streak.hasCompletedToday) {
                        streak.streakNumber = 0
                    }

                    streak.hasCompletedToday = false
                }
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

        sender.addItemOrDrop(dailyQuestDay.items.random())
        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "daily-quests.item_drawn", mapOf("%d" to dayNumber.toString())))
    }
}