package pl.lamas.lt7core

import org.bukkit.plugin.java.JavaPlugin
import pl.lamas.lt7core.command.TabCompleter
import pl.lamas.lt7core.system.admin.ChatSystem
import pl.lamas.lt7core.system.admin.CheckingSystem
import pl.lamas.lt7core.system.admin.PunishmentSystem
import pl.lamas.lt7core.system.autoevent.LottoSystem
import pl.lamas.lt7core.system.command.HomesSystem
import pl.lamas.lt7core.system.command.KitsSystem
import pl.lamas.lt7core.system.command.MsgSystem
import pl.lamas.lt7core.system.command.RtpSystem
import pl.lamas.lt7core.system.command.SpawnSystem
import pl.lamas.lt7core.system.command.TpaSystem
import pl.lamas.lt7core.system.command.WarpsSystem
import pl.lamas.lt7core.system.economy.ActionHouseSystem
import pl.lamas.lt7core.system.economy.ChangesSystem
import pl.lamas.lt7core.system.economy.JobSystem
import pl.lamas.lt7core.system.economy.JobUpgradesEventsSystem
import pl.lamas.lt7core.system.economy.MoneySystem
import pl.lamas.lt7core.system.economy.ShopSystem
import pl.lamas.lt7core.system.util.AntiLogoutSystem
import pl.lamas.lt7core.system.util.DailyQuestsSystem
import pl.lamas.lt7core.system.util.DropSystem
import pl.lamas.lt7core.system.util.EntrancesSystem
import pl.lamas.lt7core.system.util.EventsSystem
import pl.lamas.lt7core.system.util.FriendsSystem
import pl.lamas.lt7core.system.util.IgnoresSystem
import pl.lamas.lt7core.system.util.SettingsSystem
import pl.lamas.lt7core.system.util.StatsSystem
import pl.lamas.lt7core.system.util.StoneGeneratorsSystem
import pl.lamas.lt7core.util.bilingual.LanguageManager
import pl.lamas.lt7core.util.config.Config
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.util.filesystem.PersistenceManager

class LTCore : JavaPlugin() {
    companion object {
        const val PREFIX = "§8[§6L§bT§8] "
        lateinit var instance: LTCore
            private set

        lateinit var langManager: LanguageManager

        @FilePersisted("config.json", persistType = PersistType.READ)
        var config: Config = Config()
    }

    override fun onEnable() {
        instance = this
        langManager = LanguageManager(instance) // <- tutaj

        instance.getCommand("actionhouse")?.setExecutor(pl.lamas.lt7core.command.ActionHouseCommand())
        instance.getCommand("addmoney")?.setExecutor(pl.lamas.lt7core.command.AddMoneyCommand())
        instance.getCommand("baltop")?.setExecutor(pl.lamas.lt7core.command.BaltopCommand())
        instance.getCommand("ban")?.setExecutor(pl.lamas.lt7core.command.BanCommand())
        instance.getCommand("banip")?.setExecutor(pl.lamas.lt7core.command.BanIPCommand())
        instance.getCommand("chat")?.setExecutor(pl.lamas.lt7core.command.ChatCommand())
        instance.getCommand("check")?.setExecutor(pl.lamas.lt7core.command.CheckCommand())
        instance.getCommand("collect")?.setExecutor(pl.lamas.lt7core.command.CollectCommand())
        instance.getCommand("delhome")?.setExecutor(pl.lamas.lt7core.command.DelHomeCommand())
        instance.getCommand("event")?.setExecutor(pl.lamas.lt7core.command.EventCommand())
        instance.getCommand("exchange")?.setExecutor(pl.lamas.lt7core.command.ExchangeCommand())
        instance.getCommand("free")?.setExecutor(pl.lamas.lt7core.command.FreeCommand())
        instance.getCommand("friend")?.setExecutor(pl.lamas.lt7core.command.FriendsCommand())
        instance.getCommand("getpickaxe")?.setExecutor(pl.lamas.lt7core.command.GetPickaxeCommand())
        instance.getCommand("home")?.setExecutor(pl.lamas.lt7core.command.HomeCommand())
        instance.getCommand("ignore")?.setExecutor(pl.lamas.lt7core.command.IgnoreCommand())
        instance.getCommand("job")?.setExecutor(pl.lamas.lt7core.command.JobCommand())
        instance.getCommand("kit")?.setExecutor(pl.lamas.lt7core.command.KitCommand())
        instance.getCommand("lotto")?.setExecutor(pl.lamas.lt7core.command.LottoCommand())
        instance.getCommand("money")?.setExecutor(pl.lamas.lt7core.command.MoneyCommand())
        instance.getCommand("msg")?.setExecutor(pl.lamas.lt7core.command.MsgCommand())
        instance.getCommand("mute")?.setExecutor(pl.lamas.lt7core.command.MuteCommand())
        instance.getCommand("pay")?.setExecutor(pl.lamas.lt7core.command.PayCommand())
        instance.getCommand("post")?.setExecutor(pl.lamas.lt7core.command.PostCommand())
        instance.getCommand("punishments")?.setExecutor(pl.lamas.lt7core.command.PunishmentsCommand())
        instance.getCommand("removemoney")?.setExecutor(pl.lamas.lt7core.command.RemoveMoneyCommand())
        instance.getCommand("reply")?.setExecutor(pl.lamas.lt7core.command.ReplyCommand())
        instance.getCommand("resetmoney")?.setExecutor(pl.lamas.lt7core.command.ResetMoneyCommand())
        instance.getCommand("randomteleport")?.setExecutor(pl.lamas.lt7core.command.RtpCommand())
        instance.getCommand("sethome")?.setExecutor(pl.lamas.lt7core.command.SetHomeCommand())
        instance.getCommand("settings")?.setExecutor(pl.lamas.lt7core.command.SettingsCommand())
        instance.getCommand("shop")?.setExecutor(pl.lamas.lt7core.command.ShopCommand())
        instance.getCommand("spawn")?.setExecutor(pl.lamas.lt7core.command.SpawnCommand())
        instance.getCommand("stats")?.setExecutor(pl.lamas.lt7core.command.StatsCommand())
        instance.getCommand("tempban")?.setExecutor(pl.lamas.lt7core.command.TempBanCommand())
        instance.getCommand("tempbanip")?.setExecutor(pl.lamas.lt7core.command.TempBanIPCommand())
        instance.getCommand("tpaaccept")?.setExecutor(pl.lamas.lt7core.command.TpaAcceptCommand())
        instance.getCommand("tpacancel")?.setExecutor(pl.lamas.lt7core.command.TpaCancelCommand())
        instance.getCommand("tpa")?.setExecutor(pl.lamas.lt7core.command.TpaCommand())
        instance.getCommand("tpdeny")?.setExecutor(pl.lamas.lt7core.command.TpDenyCommand())
        instance.getCommand("unbanip")?.setExecutor(pl.lamas.lt7core.command.UnbanIPCommand())
        instance.getCommand("warn")?.setExecutor(pl.lamas.lt7core.command.WarnCommand())
        instance.getCommand("warp")?.setExecutor(pl.lamas.lt7core.command.WarpCommand())

        val tabCompleter = TabCompleter() // Twój jedyny TabCompleter

        instance.getCommand("home")?.setTabCompleter(tabCompleter)
        instance.getCommand("delhome")?.setTabCompleter(tabCompleter)
        instance.getCommand("addmoney")?.setTabCompleter(tabCompleter)
        instance.getCommand("removemoney")?.setTabCompleter(tabCompleter)
        instance.getCommand("resetmoney")?.setTabCompleter(tabCompleter)
        instance.getCommand("tpdeny")?.setTabCompleter(tabCompleter)
        instance.getCommand("tpaccept")?.setTabCompleter(tabCompleter)
        instance.getCommand("tpacancel")?.setTabCompleter(tabCompleter)
        instance.getCommand("pay")?.setTabCompleter(tabCompleter)
        instance.getCommand("msg")?.setTabCompleter(tabCompleter)
        instance.getCommand("tpa")?.setTabCompleter(tabCompleter)
        instance.getCommand("exchange")?.setTabCompleter(tabCompleter)
        instance.getCommand("ignore")?.setTabCompleter(tabCompleter)
        instance.getCommand("baltop")?.setTabCompleter(tabCompleter)
        instance.getCommand("ban")?.setTabCompleter(tabCompleter)
        instance.getCommand("warn")?.setTabCompleter(tabCompleter)
        instance.getCommand("banip")?.setTabCompleter(tabCompleter)
        instance.getCommand("mute")?.setTabCompleter(tabCompleter)
        instance.getCommand("tempban")?.setTabCompleter(tabCompleter)
        instance.getCommand("tempbanip")?.setTabCompleter(tabCompleter)
        instance.getCommand("kit")?.setTabCompleter(tabCompleter)
        instance.getCommand("chat")?.setTabCompleter(tabCompleter)
        instance.getCommand("warp")?.setTabCompleter(tabCompleter)
        instance.getCommand("event")?.setTabCompleter(tabCompleter)
        instance.getCommand("friend")?.setTabCompleter(tabCompleter)
        instance.getCommand("lotto")?.setTabCompleter(tabCompleter)

        PersistenceManager.init(
            this,
            Companion,
            ChatSystem,
            CheckingSystem,
            PunishmentSystem,
            LottoSystem,
            HomesSystem,
            KitsSystem,
            MsgSystem,
            RtpSystem,
            SpawnSystem,
            TpaSystem,
            WarpsSystem,
            ActionHouseSystem,
            ChangesSystem,
            JobUpgradesEventsSystem,
            JobSystem,
            MoneySystem,
            MoneySystem,
            ShopSystem,
            //GuildSystem,
            AntiLogoutSystem,
            DropSystem,
            EntrancesSystem,
            EventsSystem,
            FriendsSystem,
            IgnoresSystem,
            SettingsSystem,
            StatsSystem,
            StoneGeneratorsSystem,
            DailyQuestsSystem,
        )
    }

    override fun onDisable() {
        PersistenceManager.shutdown()
    }
}
