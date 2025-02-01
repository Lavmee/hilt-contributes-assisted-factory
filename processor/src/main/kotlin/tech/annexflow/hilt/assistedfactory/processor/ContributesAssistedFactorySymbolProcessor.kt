/*
 * Copyright 2025 Saldo Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.annexflow.hilt.assistedfactory.processor

import tech.annexflow.hilt.assistedfactory.processor.internal.ErrorLoggingSymbolProcessor
import tech.annexflow.hilt.assistedfactory.processor.internal.SymbolProcessingException
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.isDefault
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.impl.hasAnnotation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import tech.annexflow.hilt.assistedfactory.AssistedKey
import tech.annexflow.hilt.assistedfactory.ContributesAssistedFactory

private val contributesAssistedFactoryFqName = ContributesAssistedFactory::class.asClassName()

internal class ContributesAssistedFactorySymbolProcessor(
    override val env: SymbolProcessorEnvironment,
) : ErrorLoggingSymbolProcessor() {

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            return ContributesAssistedFactorySymbolProcessor(environment)
        }
    }

    // Track processed declarations to avoid duplicate generation
    private val processedDeclarations = mutableSetOf<String>()

    override fun processChecked(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver.getSymbolsWithAnnotation(contributesAssistedFactoryFqName.reflectionName())
                .filterIsInstance<KSClassDeclaration>()
                .filterNot { it.qualifiedName?.asString() in processedDeclarations }
                .toList()

        val deferredSymbols = mutableListOf<KSAnnotated>()

        symbols.forEach { annotated ->
            if (!annotated.isProcessable()) {
                deferredSymbols.add(annotated)
                return@forEach
            }

            val dependencies = annotated.containingFile?.let { listOf(it) } ?: emptyList()
            val assistedFactoryFileSpec = generateAssistedFactory(annotatedClass = annotated)

            assistedFactoryFileSpec.writeTo(
                codeGenerator = env.codeGenerator,
                dependencies = Dependencies(aggregating = true, sources = dependencies.toTypedArray()),
            )

            generateAssistedFactoryBindingModule(
                annotatedClass = annotated,
                assistedFactoryFileSpec = assistedFactoryFileSpec
            ).writeTo(
                codeGenerator = env.codeGenerator,
                dependencies = Dependencies(aggregating = true, sources = dependencies.toTypedArray()),
            )

            processedDeclarations.add(annotated.qualifiedName?.asString() ?: return@forEach)
        }

        return deferredSymbols
    }

    private fun generateAssistedFactory(
        annotatedClass: KSClassDeclaration,
    ): FileSpec {
        val annotation = annotatedClass.annotations.single {
            it.annotationType.resolve().toClassName() == contributesAssistedFactoryFqName
        }

        val generationDetails = ContributesAssistedFactoryValidator(
            annotation = annotation,
            assistedFactoryClass = annotatedClass
        ).validate()

        val factory = createAssistedFactory(
            annotatedName = annotatedClass.toClassName(),
            boundType = generationDetails.boundType.run {
                BoundType(
                    name = toClassName(),
                    isInterface = classKind == INTERFACE,
                    visibility = annotatedClass.visibilityKModifier(),
                )
            },
            factoryMethod = generationDetails.factoryMethod.run {
                FactoryMethod(
                    name = simpleName.asString(),
                    parameters = parameters.map { parameter ->
                        FactoryMethodParameter(
                            type = parameter.type.toTypeName(),
                            name = parameter.name!!.asString(),
                            assistedKeyValue = parameter.assistedKeyValue(),
                        )
                    }
                )
            },
        )
        return factory.spec
    }

    private fun generateAssistedFactoryBindingModule(
        annotatedClass: KSClassDeclaration,
        assistedFactoryFileSpec: FileSpec,
    ): FileSpec {

        val annotation = annotatedClass.annotations.single {
            it.annotationType.resolve().toClassName() == contributesAssistedFactoryFqName
        }

        val generationDetails = ContributesAssistedFactoryValidator(
            annotation = annotation,
            assistedFactoryClass = annotatedClass
        ).validate()

        val module = createBindingModule(
            factoryName = generationDetails.boundType.toClassName(),
            installIn = generationDetails.scope.toClassName(),
            implementationName = ClassName(
                packageName = assistedFactoryFileSpec.packageName,
                assistedFactoryFileSpec.name,
            ),
            visibility = annotatedClass.visibilityKModifier(),
        )
        return module.spec
    }

    internal class ContributesAssistedFactoryValidator(
        private val annotation: KSAnnotation,
        private val assistedFactoryClass: KSClassDeclaration,
    ) {

        @OptIn(KspExperimental::class)
        fun validate(): GenerationDetails {
            val boundType = annotation.boundTypeOrNull()?.declaration
            val scope = annotation.scopeOrNull()?.declaration

            boundType ?: throw SymbolProcessingException(
                node = annotation,
                message = Errors.missingBoundType(assistedFactoryClass.simpleName.asString()),
            )

            if (boundType !is KSClassDeclaration) {
                throw SymbolProcessingException(
                    node = annotation,
                    message = Errors.boundTypeMustBeClassOrInterface(boundType.simpleName.asString()),
                )
            }

            scope ?: throw SymbolProcessingException(
                node = annotation,
                message = Errors.missingScope(assistedFactoryClass.simpleName.asString()),
            )

            if (scope !is KSClassDeclaration) {
                throw SymbolProcessingException(
                    node = annotation,
                    message = Errors.scopeMustBeClassOrInterface(boundType.simpleName.asString()),
                )
            }

            val primaryConstructor = assistedFactoryClass.primaryConstructor
            val hasMoreThanOneConstructor =
                assistedFactoryClass.getConstructors().toList().size != 1

            if (primaryConstructor == null || hasMoreThanOneConstructor) {
                throw SymbolProcessingException(
                    assistedFactoryClass,
                    Errors.mustHaveSinglePrimaryConstructor(
                        className = assistedFactoryClass.simpleName.asString()
                    ),
                )
            }

            if (!primaryConstructor.hasDaggerAssistedInjectAnnotation()) {
                throw SymbolProcessingException(
                    primaryConstructor,
                    Errors.primaryConstructorMustBeAnnotatedWithAssistedInject(
                        className = assistedFactoryClass.simpleName.asString()
                    ),
                )
            }

            if (!boundType.isAbstract()) {
                throw SymbolProcessingException(
                    annotation,
                    Errors.boundTypeMustBeAbstractOrInterface(
                        boundType.simpleName.asString(),
                        assistedFactoryClass.simpleName.asString(),
                    ),
                )
            }

            val factoryMethod = boundType.getAllFunctions().singleOrNull { it.isAbstract }

            factoryMethod ?: throw SymbolProcessingException(
                boundType,
                Errors.boundTypeMustHasSingleAbstractMethod(boundType.simpleName.asString()),
            )
            val factoryMethodParameters = factoryMethod.parameters
            val constructorParameters = primaryConstructor.parameters
                .filter { it.hasDaggerAssistedAnnotation() }
                .associateBy { ParameterKey(it.type.resolve().toTypeName(), it.assistedValue()) }

            if (constructorParameters.size != factoryMethodParameters.size) {
                throw SymbolProcessingException(
                    factoryMethod,
                    Errors.parameterMismatch(
                        boundType.simpleName.asString(),
                        factoryMethod.simpleName.asString(),
                        assistedFactoryClass.simpleName.asString(),
                    ),
                )
            }

            factoryMethodParameters.forEach { factoryParameter ->
                val isAnnotatedWithDaggerAssisted =
                    factoryParameter.hasDaggerAssistedAnnotation()
                val isAnnotatedWithAssistedKey =
                    factoryParameter.isAnnotationPresent(AssistedKey::class)
                if (isAnnotatedWithDaggerAssisted && !isAnnotatedWithAssistedKey) {
                    throw SymbolProcessingException(
                        factoryParameter,
                        Errors.parameterMustBeAnnotatedWithAssistedKey(
                            factoryParameter.name!!.asString(),
                            boundType.simpleName.asString(),
                            factoryMethod.simpleName.asString(),
                        ),
                    )
                }

                val assistedKey = factoryParameter.assistedKeyValue()
                val constructorParameter =
                    constructorParameters[factoryParameter.asParameterKey { assistedKey }]

                constructorParameter ?: throw SymbolProcessingException(
                    factoryParameter,
                    Errors.parameterDoesNotMatchAssistedParameter(
                        factoryParameter.name!!.asString(),
                        assistedFactoryClass.simpleName.asString(),
                    ),
                )
            }

            return GenerationDetails(
                scope = scope,
                boundType = boundType,
                factoryMethod = factoryMethod,
                factoryParameters = constructorParameters,
            )
        }
    }

    private fun KSClassDeclaration.isProcessable(): Boolean {
        return try {
            val unresolvedTypes = mutableListOf<String>()

            // Check constructor parameters
            primaryConstructor?.parameters?.forEach { param ->
                if (!param.type.resolve().isResolvable()) {
                    unresolvedTypes.add(
                        "constructor parameter ${param.name?.asString()}: ${param.type}"
                    )
                }
            }

            // Check annotation types
            annotations.firstOrNull {
                it.annotationType.resolve().toClassName() == contributesAssistedFactoryFqName
            }?.let { annotation ->

                // Check bound type
                annotation.boundTypeOrNull()?.let {
                    if (!it.isResolvable()) {
                        unresolvedTypes.add("bound type: $it")
                    }
                }
            }

            // Check supertype
            superTypes.forEach { superType ->
                if (!superType.resolve().isResolvable()) {
                    unresolvedTypes.add("supertype: $superType")
                }
            }

            if (unresolvedTypes.isNotEmpty()) {
                env.logger.info(
                    message = "Deferring processing of ${simpleName.asString()}: " +
                            "unresolved types: $unresolvedTypes"
                )
                false
            } else {
                true
            }
        } catch (e: Exception) {
            env.logger.info("Deferring processing of ${simpleName.asString()}: ${e.message}")
            false
        }
    }

    private fun KSType.isResolvable(): Boolean {
        return !isError && declaration.qualifiedName != null
    }
}

internal data class GenerationDetails(
    val scope: KSClassDeclaration,
    val boundType: KSClassDeclaration,
    val factoryMethod: KSFunctionDeclaration,
    val factoryParameters: Map<ParameterKey, KSValueParameter>,
)

private fun KSAnnotated.hasDaggerAssistedAnnotation() = hasAnnotation(DaggerAssistedAnnotation)
private fun KSAnnotated.hasDaggerAssistedInjectAnnotation() = hasAnnotation(DaggerAssistedInjectAnnotation)

private fun KSAnnotated.hasAnnotation(className: ClassName) = hasAnnotation(className.canonicalName)

private fun KSValueParameter.asParameterKey(keyFactory: (KSValueParameter) -> String?): ParameterKey {
    return ParameterKey(type.resolve().toTypeName(), keyFactory(this))
}

private fun KSValueParameter.assistedValue(): String? {
    return annotationByNameStringValue(name = DaggerAssistedAnnotation)
}

private fun KSValueParameter.assistedKeyValue(): String? {
    return annotationStringValue<AssistedKey>()
}

private inline fun <reified T> KSAnnotated.annotationStringValue(): String? {
    val value = annotations
        .singleOrNull { it.annotationType.resolve().toClassName() == T::class.asClassName() }
        ?.argumentAt("value")
        ?.value
    return (value as String?)?.takeIf { it.isNotBlank() }
}

private fun KSAnnotated.annotationByNameStringValue(name: ClassName): String? {
    val value = annotations
        .singleOrNull { it.annotationType.resolve().toClassName() == name }
        ?.argumentAt("value")
        ?.value
    return (value as String?)?.takeIf { it.isNotBlank() }
}

internal fun KSAnnotation.argumentAt(
    name: String,
): KSValueArgument? = arguments
    .find { it.name?.asString() == name }
    ?.takeUnless { it.isDefault() }

internal fun KSAnnotation.boundTypeOrNull(): KSType? = argumentAt("boundType")?.value as? KSType?
internal fun KSAnnotation.scopeOrNull(): KSType? =
    arguments.find { it.name?.asString() == "scope" }?.value as? KSType?

private fun KSClassDeclaration.visibilityKModifier(): KModifier = this.getVisibility().toKModifier() ?: KModifier.PUBLIC
