/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.lightClasses.accessors;

import com.intellij.testFramework.LightProjectDescriptor
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.caches.resolve.IDELightClassGenerationSupport
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasShortNameIndex
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.junit.runner.RunWith;

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class LightClassAccessorsTest : KotlinLightCodeInsightFixtureTestCase() {
    fun testStressPropertyAccessor() {
        val file = myFixture.configureByText(
            "test.kt",
            //language=kotlin
            """
            class A {
                val a = 1
                val b = 2
            }
            """.trimIndent()
        )
        val declarations = mutableListOf<KtDeclaration>()
        file.accept(namedDeclarationRecursiveVisitor { declaration ->
            declarations.add(declaration)
        })
        val declarationWithMethods = declarations.map { Pair(it, it.toLightMethods())}
        for ((declaration, methods) in declarationWithMethods) {
            when (declaration) {
                is KtPropertyAccessor, is KtFunction, is KtProperty, is KtParameter -> {
                    val allMethods = LightClassUtil.getWrappingClasses(declaration).flatMap { it.methods.asSequence() }
                    val sequence = allMethods.filterIsInstance<KtLightMethod>().filter { it.kotlinOrigin === declaration }.toList()
                    assertThat(methods).isEqualTo(sequence)
                }
            }
        }
    }

    fun testLightAnnotationResolver() { // rename to findDeprecated
        val file = myFixture.configureByText(
            "test.kt",
            //language=kotlin
            """
            import org.junit.Test
            import kotlin.Deprecated as Test1
            import kotlin.Deprecated as D1
            import D1 as D2
            import org.D3 as NONEXIST
        
            class A { 
                @NONEXIST
                fun testFalse3() {}

                @D2
                fun testFalse() {}

                @org.junit.Test
                fun testFalse1() {}

                @Deprecated
                fun testFalse2() {}
                

                @Test1(level = DeprecationLevel.HIDDEN)
                fun testTrue() {}

                @Deprecated(level = DeprecationLevel.HIDDEN)
                fun testTrue1() {}
                
                @B(level = DeprecationLevel.HIDDEN)
                fun testTrue2() {}
                
                @C(level = DeprecationLevel.HIDDEN)
                fun testTrue3() {} 
            }
            
            typealias B = Test1
            typealias C = kotlin.Deprecated
            """.trimIndent()
        )
        file.accept(namedDeclarationRecursiveVisitor { declaration ->
            val classes = LightClassUtil.getWrappingClasses(declaration)
            for (clazz in classes) {
                if (clazz is KtUltraLightClass) {
                    val hidden = clazz.isHiddenByDeprecation(declaration)
                    if (declaration.nameAsSafeName.asString().contains("testTruth"))
                        assertThat(hidden).describedAs(declaration.text).isTrue()
                    else if (declaration.nameAsSafeName.asString().contains("testFalse"))
                        assertThat(hidden).describedAs(declaration.text).isFalse()
                }
            }
        })
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }
}