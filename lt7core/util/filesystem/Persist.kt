package pl.lamas.lt7core.util.filesystem

import pl.lamas.lt7core.util.filesystem.PersistType

data class Persist(
    val filePath: String,
    val persistType: PersistType,
    var variable: Any
) {
    override fun toString(): String {
        return "Persist(filePath='$filePath', persistType=$persistType, variable=$variable)"
    }

    fun isForRead() : Boolean {
        return persistType == PersistType.READ || persistType == PersistType.READ_SAVE
    }

    fun isForSave() : Boolean {
        return persistType == PersistType.SAVE || persistType == PersistType.READ_SAVE
    }
}
