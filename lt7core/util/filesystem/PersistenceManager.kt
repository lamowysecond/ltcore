package pl.lamas.lt7core.util.filesystem

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.bukkit.inventory.ItemStack
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.javaType
import kotlin.reflect.jvm.isAccessible
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

// --------------------- ADAPTERY ---------------------

class MaterialAdapter : JsonSerializer<Material>, JsonDeserializer<Material> {
    override fun serialize(src: Material, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.name)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Material {
        val materialName = json.asString
        val material = Material.getMaterial(materialName)
        if (material == null) {
            error("Unknown material: $materialName")
        }
        return material
    }
}

class LocationAdapter : JsonSerializer<Location>, JsonDeserializer<Location> {
    override fun serialize(src: Location, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            addProperty("world", src.world?.name)
            addProperty("x", src.x)
            addProperty("y", src.y)
            addProperty("z", src.z)
            addProperty("yaw", src.yaw)
            addProperty("pitch", src.pitch)
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Location {
        val obj = json.asJsonObject
        val world = Bukkit.getWorld(obj["world"].asString) ?: Bukkit.getWorlds()[0]
        return Location(
            world,
            obj["x"].asDouble,
            obj["y"].asDouble,
            obj["z"].asDouble,
            obj["yaw"]?.asFloat ?: 0f,
            obj["pitch"]?.asFloat ?: 0f
        )
    }
}

class ItemStackAdapter : JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    override fun serialize(src: ItemStack, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("type", src.type.name)
        obj.addProperty("amount", src.amount)

        val meta = src.itemMeta
        if (meta != null) {
            val metaObj = JsonObject()
            meta.displayName?.let { metaObj.addProperty("name", it) }
            if (meta.lore != null && meta.lore!!.isNotEmpty()) {
                val loreArray = JsonArray()
                meta.lore!!.forEach { loreArray.add(it) }
                metaObj.add("lore", loreArray)
            }
            if (meta.hasEnchants()) {
                val enchantsObj = JsonObject()
                meta.enchants.forEach { (enchant, level) ->
                    enchantsObj.addProperty(enchant.key.key, level)
                }
                metaObj.add("enchants", enchantsObj)
            }
            obj.add("meta", metaObj)
        }

        return obj
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ItemStack {
        if (!json.isJsonObject) throw JsonParseException("Expected JsonObject for ItemStack")
        val obj = json.asJsonObject

        val typeStr = obj.get("type")?.asString ?: throw JsonParseException("ItemStack missing 'type'")
        val amount = obj.get("amount")?.asInt ?: 1

        val material = try {
            Material.valueOf(typeStr)
        } catch (e: IllegalArgumentException) {
            throw JsonParseException("Invalid material: $typeStr")
        }

        val item = ItemStack(material, amount)

        val metaObj = obj.getAsJsonObject("meta")
        if (metaObj != null) {
            val meta = item.itemMeta ?: item.itemMeta!!

            metaObj.get("name")?.asString?.let { meta.setDisplayName(it) }

            val loreArray = metaObj.getAsJsonArray("lore")
            if (loreArray != null) {
                val lore = mutableListOf<String>()
                loreArray.forEach { lore.add(it.asString) }
                meta.lore = lore
            }

            val enchantsObj = metaObj.getAsJsonObject("enchants")
            enchantsObj?.entrySet()?.forEach { (key, value) ->
                val enchant = Enchantment.getByKey(NamespacedKey.minecraft(key))
                if (enchant != null) {
                    meta.addEnchant(enchant, value.asInt, true)
                }
            }

            item.itemMeta = meta
        }

        return item
    }
}

// --------------------- MANAGER ---------------------

object PersistenceManager {
    private val gson = GsonBuilder()
        .registerTypeAdapter(Location::class.java, LocationAdapter())
        .registerTypeAdapter(ItemStack::class.java, ItemStackAdapter())
        .registerTypeAdapter(Material::class.java, MaterialAdapter())
        .setPrettyPrinting()
        .create()

    private val tasks = mutableListOf<Int>()

    fun init(plugin: JavaPlugin, vararg targets: Any) {
        targets.forEach { target -> handle(plugin, target) }
    }

    fun shutdown() {
        tasks.forEach { Bukkit.getScheduler().cancelTask(it) }
        tasks.clear()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun handle(plugin: JavaPlugin, target: Any) {
        val clazz = target::class

        clazz.declaredMemberProperties.forEach { prop ->
            val ann = prop.findAnnotation<FilePersisted>() ?: return@forEach

            if (prop !is KMutableProperty1<*, *>) {
                Bukkit.getLogger().warning("[Persistence] Property ${prop.name} must be var.")
                return@forEach
            }

            prop.isAccessible = true
            val file = File(plugin.dataFolder, ann.filePath)
            file.parentFile.mkdirs()

            if (!file.exists()) {
                file.createNewFile()
                file.writeText(defaultJsonForProperty(prop))
                Bukkit.getLogger().info("[Persistence] Created ${file.name} with proper default format")
            }

            // LOAD
            if (ann.persistType == PersistType.READ || ann.persistType == PersistType.READ_SAVE) {
                try {
                    val type = TypeToken.get(prop.returnType.javaType).type
                    val value = gson.fromJson<Any>(file.readText(), type)

                    @Suppress("UNCHECKED_CAST")
                    (prop as KMutableProperty1<Any, Any?>).set(target, value)

                    Bukkit.getLogger().info("[Persistence] Loaded '${prop.name}' (${file.name})")
                } catch (ex: Exception) {
                    Bukkit.getLogger().severe("[Persistence] Failed to load ${prop.name} from ${file.name}")
                    ex.printStackTrace()
                }
            }

            // SAVE
            if (ann.persistType == PersistType.SAVE || ann.persistType == PersistType.READ_SAVE) {
                if (ann.autoSaveIntervalSeconds > 0) {
                    val task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                        plugin,
                        Runnable { saveProperty(file, prop, target) },
                        ann.autoSaveIntervalSeconds * 20L,
                        ann.autoSaveIntervalSeconds * 20L
                    ).taskId

                    tasks.add(task)
                    Bukkit.getLogger().info("[Persistence] Auto-saving '${prop.name}' every ${ann.autoSaveIntervalSeconds}s")
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun defaultJsonForProperty(prop: KMutableProperty1<*, *>): String {
        val type = prop.returnType.javaType

        if (type is ParameterizedType && type.rawType == List::class.java) return "[]"
        if (type is ParameterizedType && type.rawType == MutableList::class.java) return "[]"
        if (type is ParameterizedType && type.rawType == Map::class.java) return "{}"
        if (type is ParameterizedType && type.rawType == MutableMap::class.java) return "{}"

        // Dla klas (np. Config)
        return try {
            val clazz = Class.forName(prop.returnType.toString().removePrefix("class "))
            val instance = clazz.getDeclaredConstructor().newInstance()
            gson.toJson(instance)
        } catch (ex: Exception) {
            "null" // fallback
        }
    }

    private fun saveProperty(file: File, prop: KMutableProperty1<*, *>, target: Any) {
        try {
            val json = gson.toJson(prop.getter.call(target))
            file.writeText(json)
        } catch (ex: Exception) {
            Bukkit.getLogger().severe("[Persistence] Failed to save ${prop.name}")
            ex.printStackTrace()
        }
    }
}
