package pl.lamas.lt7core.util.config

import org.bukkit.Material
import pl.lamas.lt7core.util.drop.DropChances

data class Config(
    val maxItemInCart: Map<ServerRank, Int> = mapOf(
        ServerRank.DEFAULT to 10,
        ServerRank.LAMA to 15,
        ServerRank.CHRUMKA to 20,
        ServerRank.UTOPIEC to 35
    ),
    val maxInactivityForKeepingCartMinutes: Map<ServerRank, Int> = mapOf(
        ServerRank.DEFAULT to 0,
        ServerRank.LAMA to 5,
        ServerRank.CHRUMKA to 10,
        ServerRank.UTOPIEC to 40
    ),
    val checkingPlaceCoords: List<Double> = listOf(5.0, 5.0, 5.0, 0.0, 0.0),
    val antiLogoutTime: Int = 30,
    val chatCooldownDefault: Int = 60,
    val dropChances: List<DropChances> = listOf(
        DropChances(Material.COAL)
    ),
    val maxFriendsCount: Map<ServerRank, Int> = mapOf(
        ServerRank.DEFAULT to 10,
        ServerRank.LAMA to 15,
        ServerRank.CHRUMKA to 20,
        ServerRank.UTOPIEC to 40
    ),
    val friendsRequestTime: Int = 120,
    val maxActionHouseItems: Map<ServerRank, Int> = mapOf(
        ServerRank.DEFAULT to 12,
        ServerRank.LAMA to 16,
        ServerRank.CHRUMKA to 20,
        ServerRank.UTOPIEC to 30
    ),
    val lottoBet: Double = 5000.0,
    val lottoPuleForEach: Double = 8000.0,
    val maxLottoTicketsPerPlayer: Int = 3,
    val maxLottoNumber: Int = 40,
    val lottoHour: Int = 18,
    val guildsWarBuildRatio: Double = 0.33,
    val guildsOnlinePlayerDelay: Int = 10
)
