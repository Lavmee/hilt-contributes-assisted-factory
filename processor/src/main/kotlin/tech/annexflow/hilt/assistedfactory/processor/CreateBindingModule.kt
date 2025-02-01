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
import com.squareup.kotlinpoet.TypeSpec

internal fun createBindingModule(
    factoryName: ClassName,
    implementationName: ClassName,
    installIn: ClassName,
    visibility: KModifier,
): AssistedFactoryModuleSpec {
    val moduleClassName = "${implementationName.simpleName}_Module"

    val spec = FileSpec
        .builder(
            className = ClassName(implementationName.packageName, moduleClassName),
        )
        .addType(
            TypeSpec
                .interfaceBuilder(name = moduleClassName)
                .addModifiers(visibility)
                .addAnnotation(ModuleAnnotationSpec)
                .addAnnotation(
                    InstallInSingletonAnnotationSpecBuilder
                        .addMember("${installIn.canonicalName}::class")
                        .build()
                )
                .addFunction(
                    FunSpec
                        .builder(name = "factory")
                        .addModifiers(KModifier.ABSTRACT)
                        .addAnnotation(BindsAnnotationSpec)
                        .addParameter(
                            name = "implementation",
                            type = implementationName,
                        )
                        .returns(returnType = factoryName)
                        .build()
                )
                .build()
        )
        .build()

    return AssistedFactoryModuleSpec(
        name = moduleClassName,
        packageName = implementationName.packageName,
        spec = spec,
    )
}

private val ModuleAnnotationSpec = AnnotationSpec
    .builder(type = DaggerModuleAnnotation)
    .build()

private val InstallInSingletonAnnotationSpecBuilder = AnnotationSpec
    .builder(type = DaggerHiltModuleAnnotation)

private val BindsAnnotationSpec = AnnotationSpec
    .builder(type = DaggerBindsAnnotation)
    .build()

internal data class AssistedFactoryModuleSpec(
    val name: String,
    val packageName: String,
    val spec: FileSpec,
)
