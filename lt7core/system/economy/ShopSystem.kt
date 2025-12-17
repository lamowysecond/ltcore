package pl.lamas.lt7core.system.economy

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import pl.lamas.lt7core.util.filesystem.PersistType
import pl.lamas.lt7core.util.filesystem.FilePersisted
import pl.lamas.lt7core.LTCore
import pl.lamas.lt7core.LTCore.Companion.config
import pl.lamas.lt7core.LTCore.Companion.langManager
import pl.lamas.lt7core.system.economy.MoneySystem.balances
import pl.lamas.lt7core.util.Utils.addItemOrDrop
import pl.lamas.lt7core.util.Utils.countExactItem
import pl.lamas.lt7core.util.Utils.getRank
import pl.lamas.lt7core.util.Utils.hasExactItem
import pl.lamas.lt7core.util.Utils.itemStack
import pl.lamas.lt7core.util.economy.CartItem
import pl.lamas.lt7core.util.economy.ShopCategory
import pl.lamas.lt7core.util.economy.ShopItem
import pl.lamas.lt7core.util.gui.Gui
import pl.lamas.lt7core.util.gui.GuiButton
import kotlin.math.abs

object ShopSystem {
    @FilePersisted(filePath = "shop_items.json", persistType = PersistType.READ)
    var shopItems: MutableList<ShopCategory> = mutableListOf()

    val carts: MutableMap<Player, MutableList<CartItem>> = mutableMapOf()

    init {
        Bukkit.getScheduler().runTaskLater(LTCore.instance, Runnable {
            for (category in shopItems) {
                category.items = category.items.filter {
                    it.buyPrice > 0
                }
            }
        }, 20 * 3L)
    }

    private fun openCart(
        player: Player
    ) {
        Gui(
            player.name,
            langManager.getString(player, "shop.gui.cart.title"),
            6,
            player,
            buttons = carts.getOrDefault(player, mutableListOf()).map {
                val x = carts.getOrDefault(player, mutableListOf()).indexOf(it)
                GuiButton(
                    slot = x + 10 + 3 * (x / 7),
                    icon = itemStack(it.shopItem.item, lore = listOf(
                        langManager.getString(player, "shop.gui.action" + if (it.quantity < 0) ".sell" else ".buy"),
                        langManager.getString(player, "shop.gui.totalprice", mapOf("%i" to if (it.quantity < 0) (it.shopItem.sellPrice * abs(it.quantity)).toString() else (it.shopItem.buyPrice * it.quantity).toString())),
                        langManager.getString(player, "shop.gui.cart.quantity", mapOf("%i" to abs(it.quantity).toString()))
                    )),
                    onClick = { event ->
                        if (event.click.isLeftClick) {
                            if (it.quantity < 0) {
                                val absQuantity = abs(it.quantity)
                                if (absQuantity > player.countExactItem(itemStack(it.shopItem.item))) {
                                    player.sendMessage(LTCore.PREFIX + langManager.getString(player, "shop.message.sellnotenough", mapOf("%i" to absQuantity.toString())))
                                    return@GuiButton
                                }
                                sellItem(player, it.shopItem, absQuantity)
                                carts[player]?.remove(it)
                                openCart(player)
                            } else {
                                val balance = balances[player.name] ?: 0.0
                                val totalPrice = it.shopItem.buyPrice * it.quantity
                                if (totalPrice > balance) {
                                    player.sendMessage(LTCore.PREFIX + langManager.getString(player, "shop.message.buynotenoughmoney", mapOf("%i" to totalPrice.toString())))
                                    return@GuiButton
                                }
                                buyItem(player, it.shopItem, it.quantity)
                                carts[player]?.remove(it)
                                openCart(player)
                            }
                        } else if (event.click == ClickType.RIGHT) {
                            carts[player]?.remove(it)
                            openCart(player)
                        }
                    }
                )
            }.toMutableList().apply {
                add(
                    GuiButton(
                        slot = 53,
                        icon = itemStack(org.bukkit.Material.PAPER, name = langManager.getString(player, "shop.gui.back")),
                        onClick = { _ -> getMainPage(player) }
                    )
                )
            }
        ).open()
    }

    private fun getCategoryPage(
        player: Player,
        category: ShopCategory
    ) {
        Gui(
            player.name,
            langManager.getString(player, category.displayNameKey),
            6,
            player,
            buttons = category.items.map {
                val x = category.items.indexOf(it)
                GuiButton(
                    slot = x + 10 + 2 * (x / 7),
                    icon = itemStack(it.item, lore = listOf(
                        langManager.getString(player, "shop.gui.item.buyprice", mapOf("%i" to it.buyPrice.toString())),
                        langManager.getString(player, "shop.gui.item.sellprice", mapOf("%i" to it.sellPrice.toString()))
                    )),
                    onClick = { _ -> getPaymentPage(player, it, 1) }
                )
            }.toMutableList().apply {
                add(
                    GuiButton(
                        slot = 53,
                        icon = itemStack(org.bukkit.Material.PAPER, name = langManager.getString(player, "shop.gui.back")),
                        onClick = { _ -> getMainPage(player) }
                    )
                )
            }
        ).open()
    }

    private fun sellItem(
        player: Player,
        item: ShopItem,
        quantity: Int
    ) {
        val itemStack = itemStack(item.item)
        val quantityInInventory = player.countExactItem(itemStack)
        if (quantity > quantityInInventory) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "shop.message.sellnotenough", mapOf("%i" to quantity.toString())))
            return
        }

        val moneyEarned = item.sellPrice * quantity
        // Usuwanie przedmiotów z ekwipunku gracza
        var remainingToRemove = quantity
        for (invItem in player.inventory.contents) {
            if (invItem != null &&
                invItem.type == item.item &&
                invItem.itemMeta?.displayName == itemStack.itemMeta?.displayName &&
                invItem.itemMeta?.lore == itemStack.itemMeta?.lore
            ) {
                val removeAmount = minOf(invItem.amount, remainingToRemove)
                invItem.amount -= removeAmount
                remainingToRemove -= removeAmount
                if (remainingToRemove <= 0) break
            }
        }
        // Dodawanie pieniędzy do salda gracza
        val currentBalance = balances[player.name] ?: 0.0
        balances[player.name] = currentBalance + moneyEarned

        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "shop.message.sellsuccess", mapOf("%s" to quantity.toString(), "%m" to moneyEarned.toString())))
    }

    private fun buyItem(
        player: Player,
        item: ShopItem,
        quantity: Int
    ) {
        val balance = balances[player.name] ?: 0.0
        val totalPrice = item.buyPrice * quantity
        if (totalPrice > balance) {
            player.sendMessage(LTCore.PREFIX + langManager.getString(player, "shop.message.buynotenoughmoney", mapOf("%i" to totalPrice.toString())))
            return
        }

        balances[player.name] = balance - totalPrice
        player.addItemOrDrop(itemStack(item.item).clone().apply { this.amount = quantity })
        player.sendMessage(LTCore.PREFIX + langManager.getString(player, "shop.message.buysuccess", mapOf("%s" to quantity.toString(), "%m" to totalPrice.toString())))
    }

    private fun getPaymentPage(
        player: Player,
        item: ShopItem,
        quantity: Int
    ) {
        Gui(
            player.name,
            langManager.getString(player, "shop.gui.payment.title"),
            6,
            player,
            buttons = mutableListOf(
                GuiButton(
                    slot = 13,
                    icon = itemStack(item.item, lore = listOf(
                        langManager.getString(player, "shop.gui.item.buyprice", mapOf("%i" to item.buyPrice.toString())),
                        langManager.getString(player, "shop.gui.item.sellprice", mapOf("%i" to item.sellPrice.toString())),
                        if (quantity >= 0) {
                            langManager.getString(player, "shop.gui.payment.quantity", mapOf("%i" to quantity.toString()))
                        } else {
                            langManager.getString(player, "shop.gui.payment.quantity.sell", mapOf("%i" to abs(quantity).toString()))
                        },
                        if (
                            (quantity > 0 && (balances[player.name] ?: 0.0) >= item.buyPrice * quantity)
                        ) {
                            langManager.getString(player, "shop.gui.payment.totalprice", mapOf("%i" to (item.buyPrice * quantity).toString()))
                        } else if (
                            (quantity < 0 && abs(quantity) <= player.countExactItem(itemStack(item.item)))
                        ) {
                            langManager.getString(player, "shop.gui.payment.totalprice.sell", mapOf("%i" to (item.sellPrice * abs(quantity)).toString()))
                        } else {
                            ""
                        }
                    ))
                ),
                GuiButton(
                    slot = 19,
                    icon = itemStack(Material.RED_WOOL, 64, name = langManager.getString(player, "shop.gui.payment.buy64")),
                    onClick = { _ -> getPaymentPage(player, item, quantity + 64) }
                ),
                GuiButton(
                    slot = 20,
                    icon = itemStack(Material.RED_WOOL, 16, name = langManager.getString(player, "shop.gui.payment.buy16")),
                    onClick = { _ -> getPaymentPage(player, item, quantity + 16) }
                ),
                GuiButton(
                    slot = 21,
                    icon = itemStack(Material.RED_WOOL, 1, name = langManager.getString(player, "shop.gui.payment.buy1")),
                    onClick = { _ -> getPaymentPage(player, item, quantity + 1) }
                ),
                GuiButton(
                    slot = 23,
                    icon = itemStack(Material.GREEN_WOOL, 1, name = langManager.getString(player, "shop.gui.payment.remove1")),
                    onClick = { _ ->
                        getPaymentPage(player, item, quantity - 1)
                    }
                ),
                GuiButton(
                    slot = 24,
                    icon = itemStack(Material.GREEN_WOOL, 16, name = langManager.getString(player, "shop.gui.payment.remove16")),
                    onClick = { _ ->
                        getPaymentPage(player, item, quantity - 16)
                    }
                ),
                GuiButton(
                    slot = 25,
                    icon = itemStack(Material.GREEN_WOOL, 64, name = langManager.getString(player, "shop.gui.payment.remove64")),
                    onClick = { _ ->
                        getPaymentPage(player, item, quantity - 64)
                    }
                ),
                GuiButton(
                    slot = 45,
                    icon = itemStack(Material.PAPER, name = langManager.getString(player, "shop.gui.back")),
                    onClick = { _ -> getCategoryPage(player, shopItems.first { it.items.contains(item) }) }
                )
            ).apply {
                if (player.hasExactItem(itemStack(item.item))) {
                    add(
                        GuiButton(
                            slot = 52,
                            icon = itemStack(Material.LIME_WOOL, name = langManager.getString(player, "shop.gui.payment.sellall", mapOf("%i" to player.countExactItem(itemStack(item.item)).toString()))),
                            onClick = { _ ->
                                sellItem(player, item, player.countExactItem(itemStack(item.item)))
                            }
                        )
                    )
                }
                if (quantity == 0) return@apply
                add(
                    GuiButton(
                        slot = 51,
                        icon = itemStack(Material.YELLOW_WOOL, name = langManager.getString(player, "shop.gui.payment.addtocart")),
                        onClick = { _ ->
                            if ((carts[player]?.size ?: 0) >= (config.maxItemInCart[player.getRank()] ?: 0)) return@GuiButton
                            carts.getOrDefault(player, mutableListOf()).add(
                                CartItem(
                                    item, quantity
                                )
                            )
                        }
                    )
                )
                if (quantity < 0) {
                    val absQuantity = abs(quantity)
                    if (absQuantity > player.countExactItem(itemStack(item.item))) return@apply
                } else {
                    if ((balances[player.name] ?: 0.0) < item.buyPrice * quantity) return@apply
                }

                add(
                    GuiButton(
                        53,
                        icon = itemStack(Material.GREEN_WOOL, name = langManager.getString(player, "shop.gui.payment.confirm")),
                        onClick = { _ ->
                            if (quantity < 0) {
                                val absQuantity = abs(quantity)
                                sellItem(player, item, absQuantity)
                            } else {
                                buyItem(player, item, quantity)
                            }
                        }
                    )
                )
            }
        ).open()
    }

    fun getMainPage(player: Player) {
        Gui(
            player.name,
            langManager.getString(player, "shop.gui.title"),
            6,
            player,
            buttons = shopItems.map {
                GuiButton(
                    slot = it.slot,
                    icon = itemStack(it.icon, name = langManager.getString(player, it.displayNameKey)),
                    onClick = { _ ->
                        getCategoryPage(player, it)
                    }
                )
            }.toMutableList().apply {
                addAll(
                    listOf(
                        GuiButton(
                            slot = 53,
                            icon = itemStack(org.bukkit.Material.BARRIER, name = langManager.getString(player, "shop.gui.close")),
                            onClick = { event ->
                                event.whoClicked.closeInventory()
                            }
                        ),
                        GuiButton(
                            slot = 52,
                            icon = itemStack(org.bukkit.Material.CHEST, name = langManager.getString(player, "shop.gui.storage")),
                            onClick = { event ->
                                event.whoClicked.closeInventory()
                                openCart(player)
                            }
                        ),
                    )
                )
            }
        ).open()
    }
}