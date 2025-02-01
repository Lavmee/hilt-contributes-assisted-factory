package tech.annexflow.hilt.assistedfactory.processor

import com.squareup.kotlinpoet.ClassName

internal val DaggerModuleAnnotation = ClassName(packageName = "dagger", "Module")
internal val DaggerBindsAnnotation = ClassName(packageName = "dagger", "Binds")

internal val DaggerHiltModuleAnnotation = ClassName(packageName = "dagger.hilt", "InstallIn")

internal val DaggerAssistedAnnotation = ClassName(packageName = "dagger.assisted", "Assisted")
internal val DaggerAssistedFactoryAnnotation = ClassName(packageName = "dagger.assisted", "AssistedFactory")
internal val DaggerAssistedInjectAnnotation = ClassName(packageName = "dagger.assisted", "AssistedInject")
