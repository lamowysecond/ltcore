package pl.lamas.lt7core.util.bilingual

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.Yaml
import java.io.File

class LanguageManager(plugin: JavaPlugin) {
    private val languages = mutableMapOf<String, Map<String, Any>>()
    private val yaml = Yaml()

    init {
        val langFolder = File(plugin.dataFolder, "languages")
        if (!langFolder.exists()) langFolder.mkdirs()

        langFolder.listFiles { f -> f.extension == "yml" }?.forEach { file ->
            val map = yaml.load<Map<String, Any>>(file.readText()) ?: emptyMap()
            languages[file.nameWithoutExtension] = map
        }
    }

    fun formatTimestamp(player: Player, timestamp: Long): String {
        val lang = player.locale
        return formatTimestamp(lang, timestamp)
    }

    fun formatTimestamp(lang: String, timestamp: Long): String {
        val dateFormat = getValue(lang, "date-format") ?: "yyyy-MM-dd HH:mm:ss"
        val sdf = java.text.SimpleDateFormat(dateFormat)
        val date = java.util.Date(timestamp * 1000L)
        return sdf.format(date)
    }

    fun getString(player: Player, key: String, placeholders: Map<String, String> = emptyMap()): String {
        val lang = player.locale
        return getString(lang, key, placeholders)
    }

    fun getString(lang: String, key: String, placeholders: Map<String, String> = emptyMap()): String {
        var text = getValue(lang, key) ?: key
        placeholders.forEach { (k, v) -> text = text.replace(k, v) }
        return text
    }

    private fun getValue(lang: String, key: String): String? {
        val data = languages[lang] ?: return null
        return resolveKey(data, key)
    }

    private fun resolveKey(map: Map<String, Any>, key: String): String? {
        return map[key]?.toString() ?: run {
            var current: Any? = map
            for (part in key.split(".")) {
                if (current !is Map<*, *>) return null
                current = current[part]
            }
            current?.toString()
        }
    }
}