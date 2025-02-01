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

package tech.annexflow.hilt.assistedfactory.processor.internal

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode

internal abstract class ErrorLoggingSymbolProcessor : SymbolProcessor {
    abstract val env: SymbolProcessorEnvironment

    final override fun process(resolver: Resolver): List<KSAnnotated> {
        return try {
            processChecked(resolver)
        } catch (e: SymbolProcessingException) {
            env.logger.error(e.message, e.node)
            e.cause?.let(env.logger::exception)
            emptyList()
        }
    }

    protected abstract fun processChecked(resolver: Resolver): List<KSAnnotated>
}

internal class SymbolProcessingException(
    val node: KSNode,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception()
