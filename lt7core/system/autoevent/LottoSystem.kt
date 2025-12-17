package pl.lamas.lt7core.system.autoevent

import kotlinx.datetime.LocalDateTime
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.system.economy.MoneySystem
import pl.lamas.lt7core.util.Utils

object LottoSystem {
    data class Lotto(
        val winners: List<String>,
        val winningNumber: Int,
        val totalPrize: Double,
        val dayOfMonth: Int = java.time.LocalDateTime.now().dayOfMonth
    )

    @FilePersisted("last_lotto.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var lastLotto: Lotto? = null

    @FilePersisted("lotto.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var lottoData: MutableMap<String, List<Int>> = mutableMapOf()

    fun getTotalLottoPrize(): Double {
        return lottoData.values.sumOf { it.size } * config.lottoPuleForEach.toDouble() + if (lastLotto?.winners?.isEmpty() == true) lastLotto?.totalPrize!! else 0.0
    }

    init {
        Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            if (java.time.LocalDateTime.ofEpochSecond(System.currentTimeMillis() / 1000L, 0, java.time.ZoneOffset.UTC).hour != config.lottoHour) {
                return@Runnable
            }
            if (lastLotto?.dayOfMonth == java.time.LocalDateTime.now().dayOfMonth) {
                return@Runnable
            }

            val winningNumber = (1..config.maxLottoNumber).random()
            val winners = lottoData.filter { it.value.contains(winningNumber) }.keys.toList()
            val totalPrize = getTotalLottoPrize()
            lastLotto = Lotto(
                winners,
                winningNumber,
                totalPrize
            )

            val prizePerWinner: Double = if (winners.isNotEmpty()) totalPrize / winners.size else 0.0

            if (winners.isNotEmpty()) {
                for (winner in winners) {
                    MoneySystem.addMoney(winner, prizePerWinner)
                }
            }

            lottoData.clear()

            Utils.broadcast("lotto.draw_announcement",
                mapOf(
                    "%n" to winningNumber.toString(),
                    "%c" to winners.size.toString(),
                    "%a" to totalPrize.toString(),
                    "%p" to if (prizePerWinner == 0.0) "-" else prizePerWinner.toString()
                )
            )
        }, 0L, 60 * 20L)
    }

    fun getLottoNumbers(playerName: String): List<Int>? {
        return lottoData[playerName]
    }

    fun addLottoEntry(sender: Player, number: Int) {
        if (lottoData.getOrDefault(sender.name, emptyList()).contains(number)) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.already_entered",
                mapOf(
                    "%n" to number.toString()
                )
            ))
            return
        }

        if (lottoData.getOrDefault(sender.name, emptyList()).size >= config.maxLottoTicketsPerPlayer) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.entry_limit"))
            return
        }

        if (number < 1 || number > config.maxLottoNumber) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.invalid_number",
                mapOf(
                    "%m" to "1",
                    "%x" to config.maxLottoNumber.toString()
                )
            ))
            return
        }

        if (!MoneySystem.hasEnoughMoney(sender.name, config.lottoBet)) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.not_enough_money",
                mapOf(
                    "%a" to config.lottoBet.toString()
                )
            ))
            return
        }

        MoneySystem.removeMoney(sender.name, config.lottoBet)

        val updatedNumbers = lottoData.getOrDefault(sender.name, emptyList()).toMutableList()
        updatedNumbers.add(number)

        lottoData[sender.name] = updatedNumbers
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.entry_added",
            mapOf(
                "%n" to number.toString(),
                "%a" to config.lottoBet.toString()
            )
        ))
    }

    fun removeEntries(sender: Player) {
        if (!lottoData.containsKey(sender.name)) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.no_entries"))
            return
        }

        MoneySystem.addMoney(sender.name, config.lottoBet* lottoData[sender.name]!!.size.toDouble())
        lottoData.remove(sender.name)
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.entries_removed"))
    }

    fun viewLottoInfo(sender: Player) {
        val entries = lottoData[sender.name]
        if (entries.isNullOrEmpty()) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.no_entries"))
            return
        }

        sender.sendMessage (LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.info_header"))
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.your_entries",
            mapOf(
                "%e" to entries.joinToString(", ")
            )
        ))
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.total_bet",
            mapOf(
                "%a" to (getTotalLottoPrize()).toString()
            )
        ))
        sender.sendMessage (LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.players_in_lotto",
            mapOf(
                "%c" to lottoData.size.toString()
            )
        ))
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.last_number",
            mapOf(
                "%n" to (lastLotto?.winningNumber?.toString() ?: "??"
            )
        )))
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.last_winners_count",
            mapOf(
                "%c" to (lastLotto?.winners?.size?.toString() ?: "??")
            )
        ))
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.last_prize",
            mapOf(
                "%a" to (lastLotto?.totalPrize?.toString() ?: "??")
            )
        ))
        sender.sendMessage (LTCore.PREFIX + LTCore.langManager.getString(sender, "lotto.last_prize_for_each_winner",
            mapOf(
                "%a" to (if (lastLotto?.winners?.isNotEmpty() == true) (lastLotto!!.totalPrize / lastLotto!!.winners.size).toInt().toString() else "??")
            )
        ))
    }
}