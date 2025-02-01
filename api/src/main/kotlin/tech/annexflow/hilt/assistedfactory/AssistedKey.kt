package tech.annexflow.hilt.assistedfactory

@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class AssistedKey(
    val value: String,
)
