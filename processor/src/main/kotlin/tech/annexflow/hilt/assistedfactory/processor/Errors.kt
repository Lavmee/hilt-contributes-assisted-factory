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

import tech.annexflow.hilt.assistedfactory.AssistedKey

internal object Errors {
    fun missingBoundType(className: String): String {
        return "The @ContributesAssistedFactory annotation on class '$className' " +
                "must have a 'boundType' parameter"
    }

    fun missingScope(className: String): String {
        return "The @ContributesAssistedFactory annotation on class '$className' " +
                "must have a 'scope' parameter"
    }


    fun mustHaveSinglePrimaryConstructor(className: String): String {
        return "Class '$className' annotated with @ContributesAssistedFactory " +
                "must have a single primary constructor"
    }

    fun primaryConstructorMustBeAnnotatedWithAssistedInject(className: String): String {
        return "Class '$className' annotated with @ContributesAssistedFactory " +
                "must have its primary constructor annotated with @AssistedInject"
    }

    fun boundTypeMustBeAbstractOrInterface(boundTypeName: String, assistedFactoryName: String): String {
        return "The bound type '$boundTypeName' for @ContributesAssistedFactory on class " +
                "'$assistedFactoryName' must be an abstract class or interface"
    }

    fun boundTypeMustHasSingleAbstractMethod(boundType: String): String {
        return "The bound type '$boundType' for @ContributesAssistedFactory " +
                "must have a single abstract method"
    }

    fun parameterMismatch(boundTypeName: String, factoryMethodName: String, assistedFactoryName: String): String {
        return "The assisted factory method parameters in '$boundTypeName.$factoryMethodName' " +
                "must match the @Assisted parameters in the primary constructor of " +
                "'$assistedFactoryName'"
    }

    fun parameterMustBeAnnotatedWithAssistedKey(
        factoryParameterName: String,
        boundTypeName: String,
        factoryMethodName: String
    ): String {
        return "The parameter '${factoryParameterName}' in the factory method " +
                "'${boundTypeName}.${factoryMethodName}' must be annotated with " +
                "@${AssistedKey::class.simpleName} instead of @Assisted " +
                "to avoid conflicts with Dagger's @AssistedFactory annotation"
    }

    fun parameterDoesNotMatchAssistedParameter(factoryParameterName: String, assistedFactoryName: String): String {
        return "The factory method parameter '${factoryParameterName}' does not match any @Assisted parameter " +
                "in the primary constructor of '${assistedFactoryName}'"
    }

    fun boundTypeMustBeClassOrInterface(boundTypeName: String): String {
        return "Bound type $boundTypeName must be a class or interface"
    }

    fun scopeMustBeClassOrInterface(boundTypeName: String): String {
        return "Scope $boundTypeName must be a class or interface"
    }


}
