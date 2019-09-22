package com.nectar.doodle.dom.impl

import com.nectar.doodle.Document
import com.nectar.doodle.HTMLButtonElement
import com.nectar.doodle.HTMLElement
import com.nectar.doodle.HTMLImageElement
import com.nectar.doodle.HTMLInputElement
import com.nectar.doodle.Node
import com.nectar.doodle.dom.HtmlFactory
import com.nectar.doodle.dom.clearBoundStyles
import com.nectar.doodle.dom.clearVisualStyles

internal class HtmlFactoryImpl(override val root: HTMLElement, private val document: Document): HtmlFactory {
    override fun <T: HTMLElement> create() = create("DIV") as T

    @Suppress("UNCHECKED_CAST")
    override fun <T: HTMLElement> create(tag: String) = prototypes.getOrPut(tag) {
        document.createElement(tag) as T
    }.cloneNode(false) as T

    override fun createText(text: String) = document.createTextNode(text)

    override fun createImage(source: String) = create<HTMLImageElement>("IMG").apply { src = source; draggable = false }

    override fun createOrUse(tag: String, possible: Node?): HTMLElement {
        var result = possible

        if (result == null || result !is HTMLElement || result.parentNode != null && !result.nodeName.equals(tag, ignoreCase = true)) {
            result = create(tag)
        } else {
            result.clearBoundStyles ()
            result.clearVisualStyles()
        }

        return result
    }

    override fun createInput(): HTMLInputElement = create("INPUT")

    override fun createButton(): HTMLButtonElement = create("BUTTON")

    private val prototypes = mutableMapOf<String, HTMLElement>()
}