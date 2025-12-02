package org.lamas.lt7core.util.filesystem

@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FilePersisted(
    val filePath: String,
    val persistType: PersistType,
    val autoSaveIntervalSeconds: Long = 0L
)

enum class PersistType {
    READ, SAVE, READ_SAVE
}