package com.nectar.doodle.drawing

import com.nectar.doodle.geometry.Circle
import com.nectar.doodle.geometry.Ellipse
import com.nectar.doodle.geometry.Point
import com.nectar.doodle.geometry.Rectangle
import com.nectar.doodle.geometry.Size
import com.nectar.doodle.image.Image


interface Canvas: Renderer {

    var size        : Size
    var transform   : AffineTransform
    var optimization: Renderer.Optimization

    fun import(imageData: ImageData, at: Point)

    fun scale    (pin   : Point )
    fun rotate   (angle : Double)
    fun rotate   (around: Point, angle: Double)
    fun translate(by    : Point)

    fun flipVertically()
    fun flipVertically(around: Double)

    fun flipHorizontally()
    fun flipHorizontally(around: Double)

    fun rect(rectangle: Rectangle,           brush: Brush        )
    fun rect(rectangle: Rectangle, pen: Pen, brush: Brush? = null)

    fun rect(rectangle: Rectangle, radius: Double,           brush: Brush)
    fun rect(rectangle: Rectangle, radius: Double, pen: Pen, brush: Brush? = null)

    fun circle(circle: Circle,           brush: Brush        )
    fun circle(circle: Circle, pen: Pen, brush: Brush? = null)

    fun ellipse(ellipse: Ellipse,           brush: Brush        )
    fun ellipse(ellipse: Ellipse, pen: Pen, brush: Brush? = null)

    fun image(image: Image, destination: Rectangle, opacity: Float = 1f)

    interface ImageData
}
