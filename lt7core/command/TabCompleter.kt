package pl.lamas.lt7core.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import pl.lamas.lt7core.system.command.HomesSystem
import pl.lamas.lt7core.system.command.KitsSystem
import pl.lamas.lt7core.system.command.TpaSystem
import pl.lamas.lt7core.system.command.WarpsSystem
import pl.lamas.lt7core.system.economy.MoneySystem
import pl.lamas.lt7core.util.punishments.PunishmentReasonType

class TabCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String?>? {
        return when (command.name) {
            "home",
            "delhome" -> {
                if (args.size == 1) {
                    HomesSystem.homes.filter { it.key == sender.name }
                }
                null
            }
            "addmoney",
            "removemoney",
            "resetmoney" -> {
                if (args.size == 1 && sender.hasPermission("lt7core.economy.editmoney")) {
                    MoneySystem.balances.keys
                }
                null
            }
            "tpdeny", "tpaccept" -> {
                if (args.size == 1) {
                    TpaSystem.tpaRequests.filter {it.target == sender.name}.map {it.sender}
                }
                null
            }
            "tpacancel" -> {
                if (args.size == 1) {
                    TpaSystem.tpaRequests.filter {it.sender == sender.name}.map {it.target}
                }
                null
            }
            "pay", "msg", "tpa", "exchange", "ignore" -> {
                if (args.size == 1) {
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                null
            }
            "baltop" -> {
                if (args.size == 1) {
                    1..MoneySystem.balances.size.div(10)
                }
                null
            }
            "ban", "warn", "banip" -> {
                if (!sender.hasPermission("lt7core.admin.${command.name.replace("temp", "")}")) null
                if (args.size == 1) {
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                if (args.size == 2) {
                    PunishmentReasonType.entries.map { it.name.replace("_", " ") }
                }
                null
            }
            "mute", "tempban", "tempbanip" -> {
                if (!sender.hasPermission("lt7core.admin.${command.name.replace("temp", "")}")) null
                if (args.size == 1) {
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                if (args.size == 2) {
                    listOf("1m", "5m", "10m", "30m", "1h", "3h", "6h", "12h", "1d", "3d", "7d", "14d", "30d")
                }
                if (args.size == 3) {
                    PunishmentReasonType.entries.map { it.name.replace("_", " ") }
                }
                null
            }
            "kit" -> {
                if (args.size == 1) {
                    KitsSystem.kits.filter {sender.hasPermission(it.permission)}.map { it.name }
                }
                null
            }
            "chat" -> {
                if (args.size == 1) {
                    listOf("clear", "toggle")
                }
                null
            }
            "warp" -> {
                if (args.size == 1) {
                    WarpsSystem.warps.map { it.name }.toMutableList().apply {
                        if (sender.hasPermission("lt7core.warp.edit")) {
                            add("create")
                            add("delete")
                        }
                    }
                }
                null
            }
            "event" -> {
                if (args.size == 1) {
                    listOf("start", "end", "join")
                }
                null
            }
            "friend" -> {
                if (args.size == 1) {
                    listOf("add", "remove", "cancel", "list")
                }
                null
            }
            else -> null
        }
    }
}