package pl.lamas.lt7core.util.punishments

data class Ban(
    override val target: String,
    override val reason: String?,
    override val adminName: String,
    override val reasonType: PunishmentReasonType,
    override val timestampUntil: Long?,
    override val timestampCreated: Long = System.currentTimeMillis() / 1000L
) : Punishment(
    target,
    reason,
    adminName,
    reasonType,
    timestampUntil,
    timestampCreated
)
