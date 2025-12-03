package pl.lamas.lt7core.util.bilingual

enum class LangOption : SettingsSystem.SettingsOption<LangOption> {
    POLISH,
    HUNGARIAN;
    override val defaultValue: LangOption
        get() = POLISH
    override var value: LangOption = defaultValue

    override fun nextValue(currentValue: LangOption): LangOption {
        TODO("Not yet implemented")
    }
}