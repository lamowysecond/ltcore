package pl.lamas.lt7core.util.punishments

enum class PunishmentReasonType(
    val nameKey: String?
) {
    CHAT_TRASHING("punishment.reason.chat_trashing"),
    CHEATING("punishment.reason.cheating"),
    ADVERTISING("punishment.reason.advertising"),
    EXPLOITING_BUGS("punishment.reason.exploiting_bugs"),
    INAPPROPRIATE_NICKNAME("punishment.reason.inappropriate_nickname"),
    INAPPROPRIATE_SKIN("punishment.reason.inappropriate_skin"),
    INAPPROPRIATE_CONTENT("punishment.reason.inappropriate_content"),
    INAPPROPRIATE_BUILDS("punishment.reason.inappropriate_builds"),
    DOXXING("punishment.reason.doxxing"),
    LANGUAGE_VIOLATIONS("punishment.reason.language_violations"),
    MULTIACCOUNTING("punishment.reason.multiaccounting"),
    ENCOURAGING_RULE_VIOLATIONS("punishment.reason.encouraging_rule_violations"),
    ENCOURAGING_SELFHARM("punishment.reason.encouraging_selfharm"),
    INVOKING_SPAM("punishment.reason.invoking_spam"),
    QUITTING_WHILE_BEING_CHECKED("punishment.reason.quitting_while_checked"),
    OTHER(null)
}