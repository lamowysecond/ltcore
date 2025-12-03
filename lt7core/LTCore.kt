package pl.lamas.lt7core

import org.bukkit.plugin.java.JavaPlugin
import pl.lamas.lt7core.util.bilingual.LanguageManager
import pl.lamas.lt7core.util.config.Config

class LTCore : JavaPlugin() {
    companion object {
        const val PREFIX = "§8[§6L§dT§8] "
        lateinit var instance: LTCore
            private set

        val langManager: LanguageManager = LanguageManager(instance)
        val config: Config
            get() {
                TODO()
            }
    }

    override fun onEnable() {
        instance = this
    }

    override fun onDisable() {

    }
}
