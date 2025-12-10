package pl.lamas.lt7core.system.economy

import io.papermc.paper.event.block.PlayerShearBlockEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityDeathEvent
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.util.filesystem.PersistType
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.SmithItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.BrewerInventory
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.util.Utils.itemStack
import pl.lamas.lt7core.util.gui.Gui
import pl.lamas.lt7core.util.gui.GuiButton
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object JobSystem : Listener {
    enum class Job(val unlockCost: Long? = null) {
        MINER,
        LUMBERJACK,
        FARMER(unlockCost = 200),
        HUNTER(unlockCost = 450),
        BLACKSMITH(unlockCost = 650),
        ALCHEMIST(unlockCost = 550),
        TRADER(unlockCost = 800),
        EXPLORER(unlockCost = 1000),
        BUILDER(unlockCost = 1000),
        FISHERMAN(unlockCost = 750)
    }

    enum class Upgrade(
        val job: Job?,
        val icon: Material,
        val cost: Long,
        val requiredJobLevel: Int = 0,
        val requiredUpgrade: Upgrade? = null
    ) {

    }

    data class JobObject(
        val job: Job,
        var level: Int,
        var exp: Double
    )

    data class PlayerJobs(
        val jobsUnlocked: MutableSet<JobObject> = mutableSetOf(
            JobObject(Job.MINER, 1, 0.0),
            JobObject(Job.LUMBERJACK, 1, 0.0)
        ),
        var activeJob: Job = Job.MINER,
        var secondJob: Job = Job.LUMBERJACK,
        val secondJobUnlocked: Boolean = false,
        val upgradesUnlocked: MutableSet<Upgrade> = mutableSetOf()
    ) {
        fun isJobActive(player: Player, job: Job): Boolean {
            return activeJob == job || ((secondJobUnlocked || player.hasPermission("lt7core.job.second")) && secondJob == job)
        }
    }

    @FilePersisted("player_jobs.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    val playerJobs: MutableMap<String, PlayerJobs> = mutableMapOf()

    @FilePersisted("job_gems.json", PersistType.READ_SAVE, autoSaveIntervalSeconds = 60L)
    val playerJobGems: MutableMap<String, Long> = mutableMapOf()

    private fun getExpToNextLevel(level: Int): Double {
        return level * 50.0 + (level - 1) * 15.0
    }

    private fun getJobButton(slot: Int, material: Material, job: Job, sender: Player): GuiButton {
        val jobs = playerJobs.getOrDefault(sender.name, PlayerJobs())
        val jobObject = jobs.jobsUnlocked.find { it.job == job}
        return GuiButton(
            slot = slot,
            icon = itemStack(
                material,
                amount = jobObject?.level ?: 1,
                name = langManager.getString(sender, "jobs.gui.${job.name.lowercase()}"),
                lore = if (playerJobs.getOrDefault(sender.name, PlayerJobs()).jobsUnlocked.any { it.job == job }) {
                    listOf(langManager.getString(sender, "jobs.gui.job_unlocked_1", mapOf("%l" to jobObject!!.level.toString())))
                    listOf(langManager.getString(sender, "jobs.gui.job_unlocked_2", mapOf("%e" to jobObject.exp.toString(), "%n" to getExpToNextLevel(jobObject.level + 1).toString())))
                    listOf(langManager.getString(sender, "jobs.gui.job_unlocked_3", mapOf("%p" to (jobObject.exp / getExpToNextLevel(jobObject.level + 1)).times(100.0).toString())))
                    if (playerJobs.getOrDefault(sender.name, PlayerJobs()).isJobActive(sender, job)) {
                        listOf(langManager.getString(sender, "jobs.gui.job_active"))
                    } else {
                        listOf(langManager.getString(sender, "jobs.gui.job_inactive"))
                    }
                    listOf(langManager.getString(sender, "jobs.gui.job_info_1"))
                } else {
                    listOf(langManager.getString(sender, "jobs.gui.job_locked", mapOf("%c" to job.unlockCost.toString())))
                }
            ),
            onClick = { event ->
                if (!event.isShiftClick) {
                    if (!jobs.jobsUnlocked.any {it.job == job}) {
                        if (job.unlockCost != null) {
                            val playerGems = playerJobGems.getOrDefault(sender.name, 0L)
                            if (playerGems >= job.unlockCost) {
                                playerJobGems[sender.name] = playerGems - job.unlockCost
                                jobs.jobsUnlocked.add(JobObject(job, 1, 0.0))
                                playerJobs[sender.name] = jobs
                                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.job_unlocked", mapOf("%j" to langManager.getString(sender, "jobs.gui.${job.name.lowercase()}"))))
                            } else {
                                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.not_enough_job_gems", mapOf("%c" to job.unlockCost.toString(), "%i" to playerGems.toString())))
                            }
                        } else {
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.cannot_unlock_job", mapOf("%j" to langManager.getString(sender, "jobs.gui.${job.name.lowercase()}"))))
                        }
                        return@GuiButton
                    }
                    if (jobs.isJobActive(sender, job)) {
                        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.is_already_active", mapOf("%j" to langManager.getString(sender, "jobs.gui.${job.name.lowercase()}"))))
                    } else {
                        if (event.isRightClick) {
                            if (!jobs.secondJobUnlocked && !sender.hasPermission("lt7core.job.second")) {
                                sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.second_job_not_unlocked"))
                                return@GuiButton
                            }
                            jobs.secondJob = job
                            playerJobs[sender.name] = jobs
                        } else {
                            jobs.activeJob = job
                            playerJobs[sender.name] = jobs
                        }
                        sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.job_activated", mapOf("%j" to langManager.getString(sender, "jobs.gui.${job.name.lowercase()}"))))
                    }
                    return@GuiButton
                }
                generateUpgradesGui(sender, job).open()
            }
        )
    }

    private fun generateUpgradesGui(sender: Player, job: Job?): Gui {
        val jobs = playerJobs.getOrDefault(sender.name, PlayerJobs())
        return Gui(
            "jobs_info_gui_${job?.name?.lowercase() ?: "global"}",
            langManager.getString(sender, "jobs.gui.${job?.name?.lowercase() ?: "global"}_upgrades"),
            1,
            sender,
            Upgrade.entries.filter { it.job == job}.map {
                GuiButton(
                    slot = Upgrade.entries.filter { it.job == job}.indexOf(it),
                    icon = itemStack(
                        it.icon,
                        name = langManager.getString(sender, "jobs.gui.upgrade.${it.name.lowercase()}"),
                        lore = listOf(
                            langManager.getString(sender, "jobs.gui.upgrade.cost", mapOf("%c" to it.cost.toString())),
                            langManager.getString(sender, "jobs.gui.upgrade.required_level", mapOf("%l" to it.requiredJobLevel.toString())),
                            if (it.requiredUpgrade != null) {
                                langManager.getString(sender, "jobs.gui.upgrade.required_upgrade", mapOf("%u" to langManager.getString(sender, "jobs.gui.upgrade.${it.requiredUpgrade.name.lowercase()}")))
                            } else {
                                ""
                            },
                            if (jobs.upgradesUnlocked.contains(it)) {
                                langManager.getString(sender, "jobs.gui.upgrade.unlocked")
                            } else if (jobs.jobsUnlocked.any { j -> j.job == job }) {
                                langManager.getString(sender, "jobs.gui.upgrade.locked")
                            } else {
                                langManager.getString(sender, "jobs.gui.upgrade.job_locked")
                            }
                        ).filter { loreLine -> loreLine.isNotEmpty() }
                    ),
                    onClick = { event ->
                        if (!jobs.jobsUnlocked.any { j -> j.job == job }) {
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.cannot_upgrade_locked_job", mapOf("%j" to langManager.getString(sender, "jobs.gui.${job?.name?.lowercase() ?: "global"}"))))
                            return@GuiButton
                        }
                        val jobObj = jobs.jobsUnlocked.find { j -> j.job == job }!!
                        if (jobObj.level < it.requiredJobLevel) {
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.cannot_upgrade_insufficient_level", mapOf("%l" to it.requiredJobLevel.toString(), "%j" to langManager.getString(sender, "jobs.gui.${job?.name?.lowercase() ?: "global"}"))))
                            return@GuiButton
                        }
                        if (it.requiredUpgrade != null && !jobs.upgradesUnlocked.contains(it.requiredUpgrade)) {
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.cannot_upgrade_missing_upgrade", mapOf("%u" to langManager.getString(sender, "jobs.gui.upgrade.${it.requiredUpgrade.name.lowercase()}"))))
                            return@GuiButton
                        }
                        val playerGems = playerJobGems.getOrDefault(sender.name, 0L)
                        if (playerGems >= it.cost) {
                            playerJobGems[sender.name] = playerGems - it.cost
                            jobs.upgradesUnlocked.add(it)
                            playerJobs[sender.name] = jobs
                            generateUpgradesGui(sender, job).open()
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.upgrade_purchased", mapOf("%u" to langManager.getString(sender, "jobs.gui.upgrade.${it.name.lowercase()}"))))
                        } else {
                            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.not_enough_job_gems", mapOf("%c" to it.cost.toString(), "%i" to playerGems.toString())))
                        }
                    }
                )
            }.toMutableList().apply {
                add(
                    GuiButton(
                        8,
                        itemStack(Material.BARRIER, name = langManager.getString(sender, "jobs.gui.close_button")),
                        onClick = { event ->
                            JobsGui(sender)
                        }
                    )
                )
            }
        )
    }

    private fun doJob(sender: Player, job: Job, multiplier: Double) {
        if (!playerJobs.containsKey(sender.name)) return
        if (!playerJobs[sender.name]!!.isJobActive(sender, job)) return
        val jobs = playerJobs.getOrDefault(sender.name, PlayerJobs())
        val jobObject = jobs.jobsUnlocked.find { it.job == job} ?: return
        val expGained = SecureRandom().nextDouble(0.2, 0.4) * multiplier
        var newExp = jobObject.exp + expGained
        var newLevel = jobObject.level
        var leveledUp = false
        while (newExp >= getExpToNextLevel(newLevel + 1)) {
            newExp -= getExpToNextLevel(newLevel + 1)
            newLevel += 1
            leveledUp = true
        }
        if (leveledUp) {
            sender.playSound(sender.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
            sender.sendMessage(LTCore.PREFIX + langManager.getString(sender, "jobs.level_up", mapOf("%j" to langManager.getString(sender, "jobs.gui.${job.name.lowercase()}"), "%l" to newLevel.toString())))
        }
        playerJobs[sender.name] = jobs.apply {
            val jobUnlocked = jobs.jobsUnlocked.find { it.job == job }
            jobUnlocked?.level = newLevel
            jobUnlocked?.exp = newExp
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onMine(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        when (block.type) {
            Material.STONE, Material.DEEPSLATE -> {
                doJob(player, Job.MINER, 1.0)
            }
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG, Material.CRIMSON_STEM, Material.WARPED_STEM,
            Material.PALE_OAK_LOG -> {
                doJob(player, Job.LUMBERJACK, 1.15)
            }
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.SUGAR_CANE, Material.NETHER_WART,
            Material.MELON_STEM, Material.PUMPKIN_STEM -> {
                doJob(player, Job.FARMER, 1.3)
            }
            Material.SWEET_BERRIES, Material.COCOA_BEANS, Material.GLOW_BERRIES -> {
                doJob(player, Job.FARMER, 1.4)
            }
            else -> {}
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onKill(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        when (event.entity.type) {
            EntityType.COW,
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.CHICKEN,
            EntityType.HORSE,
            EntityType.RABBIT,
            EntityType.MOOSHROOM,
            EntityType.GOAT,
            EntityType.SQUID,
            EntityType.COD,
            EntityType.SALMON,
            EntityType.TROPICAL_FISH,
            EntityType.PUFFERFISH -> {
                doJob(killer, Job.FARMER, 1.2)
            }
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.CREEPER,
            EntityType.SPIDER,
            EntityType.ENDERMAN,
            EntityType.WITCH,
            EntityType.DROWNED,
            EntityType.HUSK,
            EntityType.STRAY,
            EntityType.BOGGED -> {
                doJob(killer, Job.HUNTER, 1.5)
            }
            EntityType.PIGLIN,
            EntityType.BLAZE,
            EntityType.BREEZE,
            EntityType.GHAST,
            EntityType.MAGMA_CUBE,
            EntityType.WITHER_SKELETON,
            EntityType.ZOMBIFIED_PIGLIN -> {
                doJob(killer, Job.HUNTER, 1.7)
            }
            EntityType.CAVE_SPIDER,
            EntityType.ELDER_GUARDIAN,
            EntityType.SHULKER,
            EntityType.GUARDIAN,
            EntityType.PILLAGER,
            EntityType.VINDICATOR,
                     -> {
                doJob(killer, Job.HUNTER, 2.0)
            }
            EntityType.WITHER,
            EntityType.RAVAGER -> {
                doJob(killer, Job.HUNTER, 20.0)
            }
            EntityType.WARDEN,
            EntityType.ENDER_DRAGON -> {
                doJob(killer, Job.HUNTER, 35.0)
            }
            else -> {

            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreed(event: EntityBreedEvent) {
        val player = event.breeder ?: return  // gracz, który spowodował rozmnożenie
        doJob(player as Player, Job.FARMER, 4.0)
    }

    @EventHandler(ignoreCancelled = true)
    fun onStripLog(event: BlockDamageEvent) {
        val block = event.block
        val player = event.player

        // Sprawdzamy, czy blok jest typu drewno
        if (block.type.name.endsWith("_LOG")) {
            val itemInHand = player.inventory.itemInMainHand
            if (itemInHand.type.name.contains("AXE")) {
                doJob(player, Job.LUMBERJACK, 4.0)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onEnchant(event: EnchantItemEvent) {
        val player = event.enchanter
        doJob(player, Job.BLACKSMITH, 2.0 * event.enchantsToAdd.map { it.value }.sum())
    }

    @EventHandler(ignoreCancelled = true)
    private fun onFish(event: org.bukkit.event.player.PlayerFishEvent) {
        val player = event.player
        if (event.state == org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_FISH) {
            val item = (event.caught as Item).itemStack
            when (item.type) {
                Material.COD,
                Material.SALMON,
                Material.TROPICAL_FISH,
                Material.PUFFERFISH -> {
                    doJob(player, Job.FISHERMAN, 3.7)
                }
                Material.FISHING_ROD,
                Material.ENCHANTED_BOOK,
                Material.BOW,
                Material.NAUTILUS_SHELL,
                Material.SADDLE,
                Material.NAME_TAG -> {
                    doJob(player, Job.FISHERMAN, 4.15)
                }
                else -> {
                    doJob(player, Job.FISHERMAN, 2.15)
                }
            }
        }
    }

    private val brewerStorage = ConcurrentHashMap<String, UUID>()

    // zapisujemy gracza tylko gdy kliknie stojak (czyli faktycznie zaczyna z nim pracować)
    @EventHandler
    fun onClick(e: PlayerInteractEvent) {
        val block = e.clickedBlock ?: return
        if (e.action == Action.RIGHT_CLICK_BLOCK && (block.type == Material.ANVIL || block.type == Material.CHIPPED_ANVIL || block.type == Material.DAMAGED_ANVIL
                    || block.type == Material.SMITHING_TABLE)) {
                blacksmithStorage[block.location.toString()] = e.player.uniqueId
            return
        }
        if (e.action == Action.RIGHT_CLICK_BLOCK && e.clickedBlock?.type == Material.BREWING_STAND) {
            val loc = e.clickedBlock!!.location
            brewerStorage[loc.toString()] = e.player.uniqueId
        }
    }

    // nagroda przy zakończeniu warzenia
    @EventHandler(ignoreCancelled = true)
    fun onBrew(event: BrewEvent) {
        val inv = event.contents
        val ingredientBefore = inv.ingredient ?: return

        // jeśli składnik został zużyty, to warzenie się skończyło
        // BrewEvent odpala się kilka razy, ale ten warunek łapie tylko koniec
        if (ingredientBefore.type == Material.AIR) {
            val loc = event.block.location.toString()
            val brewerId = brewerStorage[loc] ?: return
            val brewer = Bukkit.getPlayer(brewerId) ?: return

            doJob(brewer, Job.ALCHEMIST, 4.0)

            brewerStorage.remove(loc)
        }
    }

    private val blacksmithStorage = ConcurrentHashMap<String, UUID>()

    // KOWADŁO: wykrywanie finalizacji craftu (rename, repair, combine)
    @EventHandler(ignoreCancelled = true)
    fun onAnvilUse(e: InventoryClickEvent) {
        val inv = e.inventory
        if (inv.type != InventoryType.ANVIL) return

        // slot 2 = wynik
        if (e.slot != 2) return
        val result = e.currentItem ?: return
        if (result.type == Material.AIR) return

        val holder = inv.holder ?: return
        val loc =  (holder as? Player)?.toString() ?: return

        val uuid = blacksmithStorage[loc] ?: return
        val player = e.whoClicked.server.getPlayer(uuid) ?: return

        doJob(player, Job.BLACKSMITH, 4.0)

        blacksmithStorage.remove(loc)
    }

    @EventHandler(ignoreCancelled = true)
    fun onSmith(e: SmithItemEvent) {
        val loc = (e.inventory.holder as? Player)?.location?.toString() ?: return
        val uuid = blacksmithStorage[loc] ?: return
        val player = e.whoClicked.server.getPlayer(uuid) ?: return

        doJob(player, Job.BLACKSMITH, 4.0)

        blacksmithStorage.remove(loc)
    }


    @EventHandler(ignoreCancelled = true)
    private fun onPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        val type = block.type
        val name = type.name
        when {
            name.contains("PURPUR")
                    || name.contains("QUARTZ") -> {
                doJob(player, Job.BUILDER, 1.35)
            }

            name.contains("SANDSTONE")
                    || name.contains("END_STONE")
                    || type == Material.NETHERRACK
                -> {
                doJob(player, Job.BUILDER, 1.05)
            }

            name.contains("BRICK")
                    || name.contains("STONE")
                    || name.contains("CONCRETE")
                    || name.contains("TERRACOTTA")
                    || name.contains("GLASS")
                    || name.contains("OBSIDIAN")
                    || type  == Material.CLAY
                    || type == Material.COBBLESTONE
                    || type == Material.GRAVEL
                -> {
                doJob(player, Job.BUILDER, 0.85)
            }

            name.contains("LOG")
            || name.contains("WOOD")
            || name.contains("STEM")
            || name.contains("FENCE")
            || name.contains("PLANKS")
            || name.contains("DOOR")
            || name.contains("TRAPDOOR")
            || name.contains("SLAB")
            || name.contains("STAIRS")
            || type == Material.BOOKSHELF
            || type == Material.SAND
            || type == Material.RED_SAND
            || type == Material.DIRT
            || type == Material.GRASS_BLOCK
                     -> {
                doJob(player, Job.BUILDER, 0.6)
            }
            else -> {}
        }
    }

    fun JobsGui(sender: Player) {
        Gui(
            "jobs_main_gui",
            langManager.getString(sender, "jobs.gui.title"),
            6,
            sender,
            listOf(
                GuiButton(4, icon = itemStack(
                    Material.SUNFLOWER,
                    name = langManager.getString(sender, "jobs.gui.job_gems", mapOf("%i" to playerJobGems.getOrDefault(sender.name, 0).toString())),
                ),
                    onClick = {
                        generateUpgradesGui(sender, null)
                    }
                ),
                getJobButton(10, Material.IRON_PICKAXE, Job.MINER, sender),
                getJobButton(11, Material.IRON_AXE, Job.LUMBERJACK, sender),
                getJobButton(12, Material.WHEAT, Job.FARMER, sender),
                getJobButton(14, Material.BOW, Job.HUNTER, sender),
                getJobButton(15, Material.SMITHING_TABLE, Job.BLACKSMITH, sender),
                getJobButton(16, Material.BREWING_STAND, Job.ALCHEMIST, sender),
                getJobButton(20, Material.EMERALD, Job.TRADER, sender),
                getJobButton(21, Material.COMPASS, Job.EXPLORER, sender),
                getJobButton(23, Material.BRICKS, Job.BUILDER, sender),
                getJobButton(24, Material.FISHING_ROD, Job.FISHERMAN, sender),
            )
        ).open()
    }
}