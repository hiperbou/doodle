package com.nectar.doodle.drawing.impl


import com.nectar.doodle.dom.Display
import com.nectar.doodle.dom.HtmlFactory
import com.nectar.doodle.dom.add
import com.nectar.doodle.dom.hasAutoOverflow
import com.nectar.doodle.dom.insert
import com.nectar.doodle.dom.numChildren
import com.nectar.doodle.dom.parent
import com.nectar.doodle.dom.remove
import com.nectar.doodle.dom.setDisplay
import com.nectar.doodle.dom.setHeightPercent
import com.nectar.doodle.dom.setWidthPercent
import com.nectar.doodle.drawing.Canvas
import com.nectar.doodle.drawing.CanvasFactory
import com.nectar.doodle.drawing.GraphicsSurface
import com.nectar.doodle.drawing.Renderer.Optimization.Quality
import com.nectar.doodle.geometry.Point
import com.nectar.doodle.geometry.Size
import com.nectar.doodle.utils.observable
import org.w3c.dom.HTMLElement
import kotlin.browser.document


class RealGraphicsSurface private constructor(
                    htmlFactory  : HtmlFactory,
                    canvasFactory: CanvasFactory,
        private var parent       : RealGraphicsSurface?,
        private val isContainer  : Boolean,
        private var canvasElement: HTMLElement,
        addToDocumentIfNoParent  : Boolean): GraphicsSurface, Iterable<RealGraphicsSurface> {

    constructor(htmlFactory: HtmlFactory,canvasFactory: CanvasFactory, element: HTMLElement): this(htmlFactory,canvasFactory, null, false, element, false)
    constructor(htmlFactory: HtmlFactory,canvasFactory: CanvasFactory, parent: RealGraphicsSurface? = null, isContainer: Boolean = false): this(htmlFactory, canvasFactory, parent, isContainer, htmlFactory.create("b"), true)

    override var visible = true
        set(new) {
            if (new) {
                rootElement.style.setDisplay()
            } else {
                rootElement.style.setDisplay(Display.None)
            }
        }

    override var zIndex = 0
        set(new) {
            parent?.setZIndex(this, new)
        }

    override val canvas: Canvas

    private val children    = mutableListOf<RealGraphicsSurface>()
    private val rootElement = canvasElement

    init {
        if (isContainer) {
            canvasElement = htmlFactory.create("b")

            canvasElement.style.setWidthPercent (100.0)
            canvasElement.style.setHeightPercent(100.0)

            rootElement.add(canvasElement)
        }

        canvas = canvasFactory(canvasElement)

        if (parent != null) {
            parent?.add(this)
        } else if (addToDocumentIfNoParent) {
            document.body?.add(rootElement)
        }
    }

    override fun endRender() {
        canvas.flush()

        if (isContainer && canvasElement.numChildren > 0) {
            rootElement.insert(canvasElement, 0)
        }
    }

    override fun beginRender() {
        canvas.clear()
        canvas.optimization = Quality

        if (isContainer && canvasElement.numChildren == 0 && canvasElement.parent != null) {
            canvasElement.parent!!.remove(canvasElement)
        }
    }

    override var position: Point by observable(Point.Origin) { _,_,new ->
        rootElement.parent?.let { it.takeIf { !it.hasAutoOverflow }?.let {
            rootElement.style.transform = "translate(${new.x}px, ${new.y}px)"
        } }
    }

    override var size: Size by observable(Size.Empty) { _,_,new ->
        rootElement.parent?.let { it.takeIf { !it.hasAutoOverflow }?.let {
            rootElement.style.width  = "${new.width }px"
            rootElement.style.height = "${new.height}px"
        } }
    }

    override fun iterator() = children.iterator()

    internal fun release() {
        if (parent != null) {
            parent!!.remove(this)
        } else {
            document.body?.remove(rootElement)
        }
    }

    private fun add(child: RealGraphicsSurface) {
        if (child.parent === this) {
            return
        }

        child.parent?.remove(child)
        children.add(child)
        rootElement.add(child.rootElement)

        child.parent = this
    }

    private fun remove(child: RealGraphicsSurface) {
        if (child.parent === this) {
            children.remove(child)
            rootElement.remove(child.rootElement)

            child.parent = null
        }
    }

    private fun setZIndex(child: RealGraphicsSurface, index: Int) {
        rootElement.remove(child.rootElement)
        rootElement.insert(child.rootElement, rootElement.numChildren - index)
    }
}
