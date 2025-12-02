package pl.lamas.lt7core.system.economy

import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.lamas.lt7core.util.filesystem.PersistType
import org.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.LTCore

object MoneySystem {
    @FilePersisted("balances.json", persistType = PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    var balances: MutableMap<String, Double> = mutableMapOf()

    fun getBalance(playerName: String): Double {
        return balances.getOrDefault(playerName, 0.0)
    }

    fun setBalance(playerName: String, amount: Double) {
        balances[playerName] = amount
    }

    fun addMoney(playerName: String, amount: Double) {
        val currentBalance = getBalance(playerName)
        setBalance(playerName, currentBalance + amount)
    }

    fun removeMoney(playerName: String, amount: Double) {
        val currentBalance = getBalance(playerName)
        setBalance(playerName, currentBalance - amount)
    }

    fun hasEnoughMoney(playerName: String, amount: Double): Boolean {
        return getBalance(playerName) >= amount
    }

    init {
        Bukkit.getScheduler().runTaskTimer(LTCore.instance, Runnable {
            balances = balances.entries.sortedByDescending { it.value }
                .associate { it.toPair() }
                .toMutableMap()
        }, 0L, 60 * 10L)
    }

    fun checkMoney(sender: Player, targetPlayerName: String?) {
        val balance = getBalance(targetPlayerName ?: sender.name)
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.money-check", mapOf("%p" to (targetPlayerName ?: sender.name), "%b" to balance.toString())))
    }

    fun balanceTop(sender: Player, page: Int = 1) {
        val entriesPerPage = 15
        val totalPages = (balances.size + entriesPerPage - 1) / entriesPerPage
        val currentPage = page.coerceIn(1, totalPages)
        val startIndex = (currentPage - 1) * entriesPerPage
        val endIndex = (startIndex + entriesPerPage).coerceAtMost(balances.size)

        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.balance-top-header", mapOf("%p" to currentPage.toString(), "%t" to totalPages.toString())))

        for ((index, entry) in balances.entries.toList().subList(startIndex, endIndex).withIndex()) {
            val rank = startIndex + index + 1
            sender.sendMessage(LTCore.langManager.getString(sender, "economy.balance-top-entry", mapOf("%i" to rank.toString(), "%p" to entry.key, "%b" to entry.value.toString())))
        }

        val beginning = if (page > 1) TextComponent("§f§l<") else TextComponent("§8<")
        if (page > 1) {
            beginning.clickEvent = net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "balancetop ${currentPage - 1}"
            )
        }

        val end = if (page < totalPages) TextComponent("§f§l>") else TextComponent("§8>")
        if (page < totalPages) {
            end.clickEvent = net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "balancetop ${currentPage + 1}"
            )
        }

        val middle = TextComponent(" §7(§f${currentPage}§7/§f${totalPages}§7) ")

        sender.spigot().sendMessage(beginning.apply { middle }.apply { end })
    }

    fun addMoneyCommand(sender: Player, targetPlayerName: String, amount: Double) {
        if (!sender.hasPermission("economy.edit_money")) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "no_permission"))
            return
        }

        if (!balances.containsKey(targetPlayerName)) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.player-not-found", mapOf("%p" to targetPlayerName)))
            return
        }

        addMoney(targetPlayerName, amount)
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.money-added", mapOf("%p" to targetPlayerName, "%i" to amount.toString())))

        val player = Bukkit.getPlayer(targetPlayerName)
        player?.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(player, "economy.money-received", mapOf("%i" to amount.toString(), "%a" to sender.name)))
    }

    fun removeMoneyCommand(sender: Player, targetPlayerName: String, amount: Double) {
        if (!sender.hasPermission("economy.edit_money")) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "no_permission"))
            return
        }

        if (!balances.containsKey(targetPlayerName)) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.player-not-found", mapOf("%p" to targetPlayerName)))
            return
        }

        removeMoney(targetPlayerName, amount)
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.money-removed", mapOf("%p" to targetPlayerName, "%i" to amount.toString())))

        val player = Bukkit.getPlayer(targetPlayerName)
        player?.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(player, "economy.money-deducted", mapOf("%i" to amount.toString(), "%a" to sender.name)))
    }

    fun clearMoneyCommand(sender: Player, targetPlayerName: String) {
        if (!balances.containsKey(targetPlayerName)) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.player-not-found", mapOf("%p" to targetPlayerName)))
            return
        }

        setBalance(targetPlayerName, 0.0)
        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.money-cleared", mapOf("%p" to targetPlayerName)))

        val player = Bukkit.getPlayer(targetPlayerName)
        player?.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(player, "economy.money-cleared-by", mapOf("%a" to sender.name)))
    }

    fun payCommand(sender: Player, target: String, amount: Double) {
        val senderBalance = getBalance(sender.name)

        if (amount <= 0) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.invalid-amount"))
            return
        }

        if (senderBalance < amount) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.insufficient-funds", mapOf("%b" to senderBalance.toString(), "%i" to amount.toString())))
            return
        }

        if (!balances.containsKey(target)) {
            sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.player-not-found", mapOf("%p" to target)))
            return
        }

        val targetPlayer = Bukkit.getPlayer(target)

        removeMoney(sender.name, amount)
        addMoney(target, amount)

        sender.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(sender, "economy.payment-sent", mapOf("%i" to amount.toString(), "%p" to target)))
        targetPlayer?.sendMessage(LTCore.PREFIX + LTCore.langManager.getString(targetPlayer, "economy.payment-received", mapOf("%i" to amount.toString(), "%p" to sender.name)))
    }
}