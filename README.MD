[![Maven Central](https://img.shields.io/maven-central/v/tech.annexflow.hilt/assisted-factory)](https://search.maven.org/search?q=g:tech.annexflow.hilt)
![license](https://img.shields.io/github/license/Lavmee/hilt-contributes-assisted-factory)

![badge-Android](https://img.shields.io/badge/Platform-Android-brightgreen)

# Hilt-Contributes Assisted Factory [KSP-Plugin]

**Hilt-Contributes Assisted Factory** is a plugin for automatically generating `AssistedFactory` factories and Dagger Hilt modules. With the provided annotation, you can focus on implementing your classes while the plugin takes care of generating the necessary boilerplate code.

## Key Features

- Automatic generation of `@AssistedFactory` interfaces.
- Creation of Dagger modules (`@Module`) for binding factories.
- Simplifies working with Dagger Hilt and Assisted Injection by eliminating boilerplate code.

## How It Works

Annotate a class with `@ContributesAssistedFactory`, specifying the factory class.  
The plugin will automatically generate the required factory interfaces and corresponding Dagger modules.

### Usage Example

**Your Code:**

```kotlin
interface Component {
    interface Factory {
        fun create(assistedArgument: Any): Component
    }
}

@ContributesAssistedFactory(boundType = Component.Factory::class)
class DefaultComponent @AssistedInject constructor(
    @Assisted private val assistedArgument: Any
) : Component
```

***Plugin-Generated Code:***

```kotlin
@AssistedFactory
interface Component_AssistedFactory : Component.Factory {
    override fun create(assistedArgument: Any): Component
}

@Module
@InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DefaultComponent_AssistedFactory_Module {
    @Binds
    fun factory(implementation: DefaultComponent_AssistedFactory): Component.Factory
}
```

## Usage

- Annotate your class with `@ContributesAssistedFactory`.
- Specify the `boundType` parameter – the factory marker class.
- After building the project, all necessary interfaces and modules will be generated automatically.

## Download

[![Maven Central](https://img.shields.io/maven-central/v/tech.annexflow.hilt/assisted-factory)](https://central.sonatype.com/namespace/tech.annexflow.hilt)

```kotlin
dependencies {
    implementation("tech.annexflow.hilt:assisted-factory:0.0.1")
    ksp("tech.annexflow.hilt:assisted-factory-processor:0.0.1")
}
```

# Special Thanks
This project is a modified fork of the [anvil-utils](https://github.com/IlyaGulya/anvil-utils) repository by [IlyaGulya](https://github.com/IlyaGulya). Special thanks for the original work and inspiration that contributed to this implementation.
