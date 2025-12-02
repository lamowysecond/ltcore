package pl.lamas.lt7core.util.punishments

abstract class Punishment(
    open val target: String,
    open val reason: String?,
    open val adminName: String,
    open val reasonType: PunishmentReasonType,
    open val timestampUntil: Long?,
    open val timestampCreated: Long = System.currentTimeMillis() / 1000L
) {
}