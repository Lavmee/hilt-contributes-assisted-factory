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

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal fun createAssistedFactory(
    annotatedName: ClassName,
    boundType: BoundType,
    factoryMethod: FactoryMethod,

    ): AssistedFactorySpec {
    val factoryClassName = "${annotatedName.simpleName}_AssistedFactory"

    val typeBuilder =
        if (boundType.isInterface) {
            TypeSpec
                .interfaceBuilder(factoryClassName)
                .addSuperinterface(boundType.name)
        } else {
            TypeSpec
                .classBuilder(factoryClassName)
                .addModifiers(KModifier.ABSTRACT)
                .superclass(boundType.name)
        }

    val spec = FileSpec
        .builder(
            className = ClassName(annotatedName.packageName, factoryClassName),
        )
        .addType(
            typeBuilder
                .addAnnotation(AssistedFactoryAnnotation)
                .addModifiers(boundType.visibility)
                .addFunction(
                    FunSpec
                        .builder(factoryMethod.name)
                        .addModifiers(KModifier.OVERRIDE, KModifier.ABSTRACT)
                        .apply {
                            factoryMethod.parameters.forEach { parameter ->
                                addParameter(
                                    ParameterSpec
                                        .builder(parameter.name, parameter.type)
                                        .assisted(parameter.assistedKeyValue)
                                        .build(),
                                )
                            }
                        }
                        .returns(annotatedName)
                        .build(),
                )
                .build(),
        )
        .build()

    return AssistedFactorySpec(
        name = factoryClassName,
        packageName = annotatedName.packageName,
        spec = spec,
    )
}

internal data class AssistedFactorySpec(
    val name: String,
    val packageName: String,
    val spec: FileSpec,
)

internal data class BoundType(
    val name: ClassName,
    val isInterface: Boolean,
    val visibility: KModifier,
)

internal data class FactoryMethod(
    val name: String,
    val parameters: List<FactoryMethodParameter>,
)

internal data class FactoryMethodParameter(
    val type: TypeName,
    val name: String,
    val assistedKeyValue: String?
)

private fun ParameterSpec.Builder.assisted(value: String?): ParameterSpec.Builder {
    if (value == null) return this
    addAnnotation(
        AnnotationSpec
            .builder(type = DaggerAssistedAnnotation)
            .addMember("%S", value)
            .build(),
    )
    return this
}

private val AssistedFactoryAnnotation = AnnotationSpec
    .builder(type = DaggerAssistedFactoryAnnotation)
    .build()
