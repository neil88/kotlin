/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve.util

import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.KtUltraLightSupport
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.EnumValue

/**
 * Search for FQN of annotation defined in the same file.
 */
class AnnotationLightResolver(val fqName: FqName) {
    fun isSame(annotationEntry: KtAnnotationEntry): SearchResult {
        val ktFile = annotationEntry.containingKtFile
        val shortName = annotationEntry.shortName?.asString() ?: return SearchResult.NOT_FOUND
        val result = findCandidatesFor(shortName, ktFile)
        return result.first
    }

    private fun findCandidatesFor(shortName: String, ktFile: KtFile): Pair<SearchResult, KtImportDirective?> {
        val matchedImport = ktFile.importDirectives
            .filter {
                if (it.alias != null)
                    it.alias?.name == shortName
                else
                    it.importedName?.asString() == shortName || it.importedFqName?.asString() == shortName
            }
        return when (matchedImport.size) {
            0 -> Pair(SearchResult.NOT_FOUND, null)
            1 -> Pair(SearchResult.FOUND, matchedImport.first())
            else -> Pair(SearchResult.ITS_COMPLICATED, null)
        }
    }
}

fun KtUltraLightSupport.findAnnotationNoResolve(annotated: KtAnnotated, fqName: FqName): Pair<SearchResult, KtAnnotationEntry?> {
    val annotationLightResolver = AnnotationLightResolver(fqName)
    val candidate = annotated.annotationEntries.filter {
        annotationLightResolver.isSame(it) == SearchResult.FOUND
    }
    return when (candidate.size) {
        0 -> Pair(SearchResult.NOT_FOUND, null)
        1 -> Pair(SearchResult.FOUND, candidate.first())
        else -> Pair(SearchResult.ITS_COMPLICATED, null)
    }
}

fun KtUltraLightClass.isNewHiddenByDeprecation(declaration: KtDeclaration): Boolean {
    val deprecated = support.findAnnotationNoResolve(declaration, KotlinBuiltIns.FQ_NAMES.deprecated)
    return when (deprecated.first) {
        SearchResult.NOT_FOUND -> false
        SearchResult.FOUND -> {
            val valueArgumentList = deprecated.second?.valueArguments
            val typeArgumentList = deprecated.second?.typeArguments
            // (deprecated?.argumentValue("level") as? EnumValue)?.enumEntryName?.asString() == "HIDDEN"
            return false
        }
        else -> {
            support.isHiddenByDeprecation(declaration)
        }
    }
}

enum class SearchResult {
    FOUND, NOT_FOUND, ITS_COMPLICATED
}