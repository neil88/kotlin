/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class ModuleData(
    val name: String,
    val qualifiedName: String,
    val classpath: List<File>,
    val sources: List<File>,
    val javaSourceRoots: List<File>
)

private fun NodeList.toList(): List<Node> {
    val list = mutableListOf<Node>()
    for (index in 0 until this.length) {
        list += item(index)
    }
    return list
}


private val Node.childNodesList get() = childNodes.toList()

abstract class AbstractModularizedTest : KtUsefulTestCase() {
    override fun setUp() {
        super.setUp()
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = false
    }

    override fun tearDown() {
        super.tearDown()
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true
    }

    private fun loadModule(file: File): ModuleData {

        val factory = DocumentBuilderFactory.newInstance()
        factory.isIgnoringComments = true
        factory.isIgnoringElementContentWhitespace = true
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(file)
        val moduleElement = document.childNodes.item(0).childNodesList.first { it.nodeType == Node.ELEMENT_NODE }
        val moduleName = moduleElement.attributes.getNamedItem("name").nodeValue
        val outputDir = moduleElement.attributes.getNamedItem("outputDir").nodeValue
        val qualifiedModuleName = outputDir.substringAfterLast("/")
        val javaSourceRoots = mutableListOf<File>()
        val classpath = mutableListOf<File>()
        val sources = mutableListOf<File>()

        for (index in 0 until moduleElement.childNodes.length) {
            val item = moduleElement.childNodes.item(index)

            if (item.nodeName == "classpath") {
                val path = item.attributes.getNamedItem("path").nodeValue
                if (path != outputDir) {
                    classpath += File(path)
                }
            }
            if (item.nodeName == "javaSourceRoots") {
                javaSourceRoots += File(item.attributes.getNamedItem("path").nodeValue)
            }
            if (item.nodeName == "sources") {
                sources += File(item.attributes.getNamedItem("path").nodeValue)
            }
        }

        return ModuleData(moduleName, qualifiedModuleName, classpath, sources, javaSourceRoots)
    }


    protected abstract fun beforePass()
    protected abstract fun afterPass()
    protected abstract fun processModule(moduleData: ModuleData): ProcessorAction

    protected fun runTestOnce(pass: Int) {
        println("Pass $pass")
        beforePass()
        val testDataPath = System.getProperty("fir.bench.jps.dir")?.toString() ?: "/Users/jetbrains/jps"
        val root = File(testDataPath)

        println("BASE PATH: ${root.absolutePath}")

        val modules =
            root.listFiles().sortedBy { it.lastModified() }.map { loadModule(it) }
                .filter { it.qualifiedName == "kotlin.idea.main" }


        for (module in modules.progress(step = 0.0) { "Analyzing ${it.qualifiedName}" }) {
            if (processModule(module).stop()) {
                break
            }
        }

        afterPass()
    }
}
