package tech.annexflow.hilt.factory.sample

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.components.ActivityComponent
import tech.annexflow.hilt.assistedfactory.ContributesAssistedFactory

interface Component {

    interface Factory {
        fun create(
            assistedArgument: String,
        ): Component
    }
}

@ContributesAssistedFactory(Component.Factory::class)
internal class SingletonComponentImpl @AssistedInject constructor(
    @Assisted private val assistedArgument: String,
) : Component

@ContributesAssistedFactory(Component.Factory::class, scope = ActivityComponent::class)
internal class ActivityComponentImpl @AssistedInject constructor(
    @Assisted private val assistedArgument: String,
) : Component
