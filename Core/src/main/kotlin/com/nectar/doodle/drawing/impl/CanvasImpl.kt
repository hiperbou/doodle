package com.nectar.doodle.drawing.impl

import com.nectar.doodle.dom.HtmlFactory
import com.nectar.doodle.dom.add
import com.nectar.doodle.dom.childAt
import com.nectar.doodle.dom.index
import com.nectar.doodle.dom.left
import com.nectar.doodle.dom.numChildren
import com.nectar.doodle.dom.parent
import com.nectar.doodle.dom.remove
import com.nectar.doodle.dom.removeTransform
import com.nectar.doodle.dom.setHeight
import com.nectar.doodle.dom.setHeightPercent
import com.nectar.doodle.dom.setWidth
import com.nectar.doodle.dom.setWidthPercent
import com.nectar.doodle.dom.top
import com.nectar.doodle.drawing.AffineTransform.Companion.Identity
import com.nectar.doodle.drawing.Brush
import com.nectar.doodle.drawing.Canvas
import com.nectar.doodle.drawing.Canvas.ImageData
import com.nectar.doodle.drawing.Font
import com.nectar.doodle.drawing.Pen
import com.nectar.doodle.drawing.Renderer
import com.nectar.doodle.drawing.SolidBrush
import com.nectar.doodle.geometry.Circle
import com.nectar.doodle.geometry.Ellipse
import com.nectar.doodle.geometry.Point
import com.nectar.doodle.geometry.Polygon
import com.nectar.doodle.geometry.Rectangle
import com.nectar.doodle.geometry.Size
import com.nectar.doodle.image.Image
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import kotlin.dom.clear


internal class CanvasImpl(
        override val renderRegion: Node,
        private val htmlFactory: HtmlFactory,
        vectorRendererFactory: VectorRendererFactory): Canvas, Renderer, CanvasContext {

    override var size           = Size.Empty
    override var transform      = Identity
    override var optimization   = Renderer.Optimization.Quality
    override var renderPosition = null as Node?

    override fun rect(rectangle: Rectangle,           brush: Brush ) = present(null, brush) { getRect(rectangle) }
    override fun rect(rectangle: Rectangle, pen: Pen, brush: Brush?) = vectorRenderer.rect(rectangle, pen, brush) //= present(pen,  brush) { getRect(rectangle) }

    override fun rect(rectangle: Rectangle, radius: Double,           brush: Brush ) = present(null, brush) { roundedRect(rectangle, radius) }
    override fun rect(rectangle: Rectangle, radius: Double, pen: Pen, brush: Brush?) = vectorRenderer.rect(rectangle, radius, pen, brush) //present(pen,  brush) { roundedRect(rectangle, radius) }

    override fun circle(circle: Circle,           brush: Brush ) = present(null, brush) { roundedRect(circle.boundingRectangle, circle.radius) }
    override fun circle(circle: Circle, pen: Pen, brush: Brush?) = vectorRenderer.circle(circle, pen, brush) //= present(pen,  brush) { roundedRect(circle.boundingRectangle, circle.radius) }

    override fun ellipse(ellipse: Ellipse,           brush: Brush ) = present(null, brush) { roundedRect(ellipse.boundingRectangle, ellipse.xRadius, ellipse.yRadius) }
    override fun ellipse(ellipse: Ellipse, pen: Pen, brush: Brush?) = vectorRenderer.ellipse(ellipse, pen, brush) //= present(pen,  brush) { roundedRect(ellipse.boundingRectangle, ellipse.xRadius, ellipse.yRadius) }

    // =============== Complex =============== //

    override fun line(point1: Point, point2: Point, pen: Pen) = vectorRenderer.line(point1, point2, pen)

    override fun path(points: List<Point>, pen: Pen) = vectorRenderer.path(points, pen)

    override fun poly(polygon: Polygon,           brush: Brush ) = vectorRenderer.poly(polygon,      brush)
    override fun poly(polygon: Polygon, pen: Pen, brush: Brush?) = vectorRenderer.poly(polygon, pen, brush)

    override fun arc(center: Point, radius: Double, sweep: Double, rotation: Double,           brush: Brush ) = vectorRenderer.arc(center, radius, sweep, rotation,      brush)
    override fun arc(center: Point, radius: Double, sweep: Double, rotation: Double, pen: Pen, brush: Brush?) = vectorRenderer.arc(center, radius, sweep, rotation, pen, brush)


    private val vectorRenderer by lazy { vectorRendererFactory(this) }

    private val isTransformed get() = !transform.isIdentity

//    override val imageData: ImageData
//        get () {
//            val elements = (0 until region.numChildren).mapTo(mutableListOf()) { region.childAt(it)!! }
//
//            return ImageDataImpl(elements)
//        }

    override fun import(imageData: ImageData, at: Point) {
//        if (imageData is ImageDataImpl) {
//            val elements = (imageData as ImageDataImpl).getElements()
//            val clones   = mutableListOf<HTMLElement>(elements.size)
//
//            for (aElement in elements) {
//                clones.add(aElement.cloneNode())
//            }
//
//            addData(clones, at)
//        }
    }

    private fun addData(elements: List<HTMLElement>, at: Point) {
        elements.forEach { element ->
            element.style.top  = "${element.top  + at.y}"
            element.style.left = "${element.left + at.x}"

            if (renderPosition != null) {
                renderPosition?.let {
                    val nextSibling = it.nextSibling as HTMLElement?

                    renderRegion.replaceChild(element, it)

                    renderPosition = nextSibling
                }
            } else {
                renderRegion.add(element)
            }
        }
    }

    override fun text(text: String, font: Font, at: Point, brush: Brush) {
//        if (text.isEmpty() || !brush.visible) {
//            return
//        }
//
//        if (!isTransformed && brush is SolidBrush && !isComplexFont(font)) {
//            val textGlyph = createTextGlyph(brush, font, text, point)
//
//            completeOperation(textGlyph.getData())
//        } else {
//            vectorRenderer.text(text, font, point, brush)
//        }
    }

    override fun clippedText(text: String, font: Font, point: Point, clipRect: Rectangle, brush: Brush) {
//        if (text.isEmpty() || clipRect.empty || !brush.visible) {
//            return
//        }
//
//        if (!isTransformed && brush is SolidBrush && !isComplexFont(font)) {
//            val aGlyph = createClipRect(clipRect)
//
//            val aOldRenderPosition = renderPosition
//
//            renderPosition = aGlyph.getData().getChildAt(0)
//
//            val aTextGlyph = createTextGlyph(brush, font, text, Point.create(point.x - clipRect.x, point.y - clipRect.y))
//
//            val aData = aGlyph.getData()
//
//            if (renderPosition !== aTextGlyph.getData()) {
//                aData.add(aTextGlyph.getData())
//            }
//
//            renderPosition = aOldRenderPosition
//
//            completeOperation(aData)
//        } else {
//            vectorRenderer.clippedText(text, font, point, clipRect, brush)
//        }
    }

    override fun wrappedText(text: String, font: Font, point: Point, minBounds: Double, maxBounds: Double, brush: Brush) {
//        if (text.isEmpty() || !brush.visible) {
//            return
//        }
//
//        if (!isTransformed && brush is SolidBrush && !isComplexFont(font)) {
//            val aTextGlyph = createWrappedTextGlyph(brush,
//                    text,
//                    font,
//                    point,
//                    minBounds,
//                    maxBounds)
//
//            completeOperation(aTextGlyph.getData())
//        } else {
//            // TODO: IMPLEMENT
//        }
    }

    override fun image(image: Image, destination: Rectangle, opacity: Float) {
//        val rect = Rectangle.create(image.size.width, image.size.width)
//
//        if (shouldDrawImage(image, rect, destination, opacity)) {
//            if (canTransformFilledRect) {
//                val aImageGlyph = createImageGlyph(image, transform(destination), opacity)
//
//                completeOperation(aImageGlyph.getData())
//            } else {
//                vectorRenderer.image(image, rect, destination, opacity)
//            }
//        }
    }

    override fun image(image: Image, source: Rectangle, destination: Rectangle, opacity: Float) {
//        if (shouldDrawImage(image, source, destination, opacity)) {
//            if (canTransformFilledRect) {
//                val transformedRect = transform(destination)
//
//                val aGlyph = createClipRect(transformedRect)
//
//                val aOldRenderPosition = renderPosition
//
//                renderPosition = aGlyph.getData().getChildAt(0)
//
//                val aXRatio = transformedRect.width / source.width
//                val aYRatio = transformedRect.height / source.height
//
//                val aImageGlyph = createImageGlyph(image,
//                        Rectangle.create(0 - aXRatio * source.x,
//                                0 - aYRatio * source.y,
//                                aXRatio * image.size.width,
//                                aYRatio * image.size.height),
//                        opacity)
//
//                val aData = aGlyph.getData()
//
//                if (renderPosition !== aImageGlyph.getData()) {
//                    aData.add(aImageGlyph.getData())
//                }
//
//                renderPosition = aOldRenderPosition
//
//                completeOperation(aData)
//            } else {
//                vectorRenderer.image(image, source, destination, opacity)
//            }
//        }
    }

    override fun translate(by: Point) {
        transform = transform.translate(by)
    }

    override fun scale(pin: Point) {
        transform = transform.scale(pin)
    }

    override fun rotate(angle: Double) {
        transform = transform.rotate(angle)
    }

    override fun rotate(around: Point, angle: Double) {
        translate(around)
        rotate(angle)
        translate(-around)
    }

    override fun flipVertically() {
        scale(Point(1.0, -1.0))
    }

    override fun flipVertically(around: Double) {
        translate(Point(0.0, around))
        flipVertically()
        translate(Point(0.0, -around))
    }

    override fun flipHorizontally() {
        scale(Point(-1.0, 1.0))
    }

    override fun flipHorizontally(around: Double) {
        translate(Point(around, 0.0))
        flipHorizontally()
        translate(Point(-around, 0.0))
    }

    override fun clear() {
        renderPosition = renderRegion.childAt(0)

        vectorRenderer.clear()
    }

    override fun flush() {
        renderPosition?.let {
            val index = renderRegion.index(it)

            if (index >= 0) {
                while (index < renderRegion.numChildren) {
                    renderRegion.remove(renderRegion.childAt(index)!!)
                }
            }
        }

        vectorRenderer.flush()

        transform = Identity
    }

    private val canTransformFilledRect get() =
        transform.scaleX >= 0   &&
        transform.scaleY >= 0   &&
        transform.shearX == 0.0 &&
        transform.shearY == 0.0

    private fun visible(pen: Pen?, brush: Brush?) = (pen?.visible ?: false) || (brush?.visible ?: false)

    private fun present(pen: Pen?, brush: Brush?, block: () -> HTMLElement?) {
        if (visible(pen, brush)) {
            block()?.let {
                if (brush is SolidBrush) {
                    it.style.backgroundColor = "#${brush.color.hexString}"
                }
                if (pen != null) {
                    it.style.borderWidth = "${pen.thickness}px"
                    it.style.borderStyle = "solid" // TODO: Handle dashes
                    it.style.borderColor = "#${pen.color.hexString}"
                }

                completeOperation(it)
            }
        }
    }

    private fun getRect(rectangle: Rectangle): HTMLElement {
        val rect = renderPosition?.let {
            if (it is HTMLElement && it.nodeName.toLowerCase() == "b") {
                it.clear()
                it.style.border = ""
                it.removeTransform()
                return@let it as HTMLElement
            } else null
        } ?: htmlFactory.create("b").also {
            it.style.setWidthPercent (100.0)
            it.style.setHeightPercent(100.0)
        }

        rect.style.transform = "translate(${rectangle.x}px, ${rectangle.y}px)"
        rect.style.setWidth (rectangle.width )
        rect.style.setHeight(rectangle.height)

        return rect
    }

    private fun roundedRect(rectangle: Rectangle,                   radius: Double) = getRect(rectangle).also { it.style.borderRadius = "${radius}px" }
    private fun roundedRect(rectangle: Rectangle, xRadius: Double, yRadius: Double) = getRect(rectangle).also { it.style.borderRadius = "${xRadius}px / ${yRadius}px" }

    private fun transform(rectangle: Rectangle): Rectangle {
        if (isTransformed) {
            val points = transform.transform(*arrayOf(rectangle.position,
                    Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height)))

            return Rectangle(
                    points[0].x,
                    points[0].y,
                    points[1].x - points[0].x,
                    points[1].y - points[0].y)
        }

        return rectangle
    }

    private fun shouldDrawImage(image: Image, source: Rectangle, destination: Rectangle, opacity: Float) = opacity > 0 && !(source.empty || destination.empty)

    private fun isComplexFont(font: Font) = font.isRotated || font.layout !== Font.Layout.LEFT_RIGHT

    private fun completeOperation(element: HTMLElement) {
        if (renderPosition == null) {
            renderRegion.add(element)
        } else {
            if (element !== renderPosition) {
                renderPosition?.parent?.replaceChild(element, renderPosition!!)
            }

            renderPosition = element.nextSibling as HTMLElement?
        }
    }

//    private fun createTextGlyph(brush: SolidBrush, font: Font, text: String, position: Point): TextGlyph {
//        val glyph = TextGlyph(renderPosition, text, font)
//
//        glyph.setColor   (brush.color)
//        glyph.setOpacity (brush.color.opacity)
//        glyph.setPosition(position)
//
//        return glyph
//    }
//
//    private fun createWrappedTextGlyph(brush: SolidBrush, text: String, font: Font, position: Point, minBounds: Double, maxBounds: Double): WrappedTextGlyph {
//        val indent = position.x - minBounds
//        val glyph  = WrappedTextGlyph(renderPosition, text, font, indent)
//
//        glyph.setColor   (brush.color)
//        glyph.setWidth   (maxBounds - minBounds)
//        glyph.setOpacity (brush.color.opacity)
//        glyph.setPosition(Point.create(minBounds, position.y))
//
//        return glyph
//    }
//
//    private fun createImageGlyph(image: Image, rectangle: Rectangle, opacity: Float): ImageGlyph {
//        val glyph = ImageGlyph(image, renderPosition)
//
//        glyph.setBounds (rectangle)
//        glyph.setOpacity(opacity)
//
//        return glyph
//    }
//
//    private fun createClipRect(rectangle: Rectangle): Glyph {
//        val glyph = RectGlyph(renderPosition)
//
//        glyph.setBounds(rectangle)
//
//        glyph.getData().removeAll()
//
//        return glyph
//    }
}