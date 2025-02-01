package tech.annexflow.hilt.assistedfactory

import dagger.hilt.components.SingletonComponent
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
public annotation class ContributesAssistedFactory(
    val boundType: KClass<*>,
    val scope: KClass<*> = SingletonComponent::class,
)
