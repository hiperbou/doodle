package com.nectar.doodle.controls.theme.basic.tree

import com.nectar.doodle.controls.text.Label
import com.nectar.doodle.controls.text.LabelFactory
import com.nectar.doodle.controls.text.TextField
import com.nectar.doodle.controls.theme.TreeRenderer
import com.nectar.doodle.controls.theme.TreeRenderer.RowGenerator
import com.nectar.doodle.controls.theme.TreeRenderer.RowPositioner
import com.nectar.doodle.controls.tree.EditOperation
import com.nectar.doodle.controls.tree.MutableTree
import com.nectar.doodle.controls.tree.Tree
import com.nectar.doodle.controls.tree.TreeEditor
import com.nectar.doodle.core.Display
import com.nectar.doodle.core.View
import com.nectar.doodle.drawing.Canvas
import com.nectar.doodle.drawing.Color.Companion.green
import com.nectar.doodle.drawing.Color.Companion.lightgray
import com.nectar.doodle.drawing.ColorBrush
import com.nectar.doodle.event.KeyEvent
import com.nectar.doodle.event.KeyEvent.Companion.VK_BACKSPACE
import com.nectar.doodle.event.KeyEvent.Companion.VK_DELETE
import com.nectar.doodle.event.KeyEvent.Companion.VK_DOWN
import com.nectar.doodle.event.KeyEvent.Companion.VK_ESCAPE
import com.nectar.doodle.event.KeyEvent.Companion.VK_LEFT
import com.nectar.doodle.event.KeyEvent.Companion.VK_RETURN
import com.nectar.doodle.event.KeyEvent.Companion.VK_RIGHT
import com.nectar.doodle.event.KeyEvent.Companion.VK_UP
import com.nectar.doodle.event.KeyListener
import com.nectar.doodle.event.MouseEvent
import com.nectar.doodle.event.MouseListener
import com.nectar.doodle.focus.FocusManager
import com.nectar.doodle.geometry.Point
import com.nectar.doodle.geometry.Rectangle
import com.nectar.doodle.layout.ConstraintLayout
import com.nectar.doodle.layout.constrain
import com.nectar.doodle.system.SystemInputEvent.Modifier.Ctrl
import com.nectar.doodle.system.SystemInputEvent.Modifier.Meta
import com.nectar.doodle.utils.Encoder
import com.nectar.doodle.utils.HorizontalAlignment.Left
import com.nectar.doodle.utils.Path
import com.nectar.doodle.utils.RelativePositionMonitor
import com.nectar.doodle.utils.isEven
import kotlin.math.max

private class BasicTreeRowPositioner<T>(private val height: Double): RowPositioner<T> {
    override fun rowBounds(tree: Tree<T, *>, node: T, path: Path<Int>, index: Int): Rectangle {
        return Rectangle(tree.insets.left, tree.insets.top + index * height, tree.width - tree.insets.run { left + right }, height)
    }

    override fun contentBounds(tree: Tree<T, *>, node: T, path: Path<Int>, index: Int): Rectangle {
        // FIXME: Centralize
        val depth  = (path.depth - if (!tree.rootVisible) 1 else 0)
        val indent = 20.0 * (1 + depth)

        return Rectangle(tree.insets.left + indent, tree.insets.top + index * height, tree.width - tree.insets.run { left + right } - indent, height)
    }

    override fun row(of: Tree<T, *>, atY: Double): Int {
        return max(0, ((atY - of.insets.top) / height).toInt())
    }
}

interface ContentGenerator<T> {
    operator fun invoke(tree: Tree<T, *>, node: T, path: Path<Int>, index: Int, previous: View? = null): View
}

private class LabelContentGenerator<T>(private val labelFactory: LabelFactory): ContentGenerator<T> {
    override fun invoke(tree: Tree<T, *>, node: T, path: Path<Int>, index: Int, previous: View?): View {
        return when (previous) {
            is Label -> { previous.text = node.toString(); previous }
            else     -> labelFactory(node.toString()).apply {
                fitText             = false
                horizontalAlignment = Left
            }
        }
    }
}

private class BasicTreeRow<T>(private val contentFactory: ContentGenerator<T>, private val labelFactory: LabelFactory, private val focusManager: FocusManager?, tree: Tree<T, *>, node: T, var path: Path<Int>, index: Int): View() {
    private var depth     = -1
    private val iconWidth = 20.0

    private var icon = null as Label?

    private var content: View = contentFactory(tree, node, path, index)

    private var background = lightgray

    private lateinit var constraintLayout: ConstraintLayout

    init {
        children += content

        styleChanged += { rerender() }

        mouseChanged += object: MouseListener {
            private var pressed   = false
            private var mouseOver = false

            override fun mouseEntered(event: MouseEvent) {
                mouseOver       = true
                backgroundColor = backgroundColor?.lighter(0.25f)
            }

            override fun mouseExited(event: MouseEvent) {
                mouseOver       = false
                backgroundColor = background
            }

            override fun mousePressed(event: MouseEvent) {
                pressed = true
            }

            override fun mouseReleased(event: MouseEvent) {
                if (mouseOver && pressed) {
                    setOf(path).also {
                        tree.apply {
                            when {
                                Ctrl in event.modifiers ||
                                        Meta in event.modifiers -> if (selected(path)) removeSelection(it) else addSelection(it)
                                else                    -> setSelection(it)
                            }
                        }
                    }

                    focusManager?.requestFocus(tree)
                }
                pressed = false
            }
        }

        update(tree, node, path, index)
    }

    fun update(tree: Tree<T, *>, node: T, path: Path<Int>, index: Int) {
        this.path = path

        content = contentFactory(tree, node, path, index, content).also {
            if (it != content) {
                children -= content
                children += it
                depth     = -1 // force layout
            }
        }

        val newDepth = (path.depth - if (!tree.rootVisible) 1 else 0)

        if (newDepth != depth) {
            constraintLayout = constrain(content) { label ->
                label.top    = label.parent.top
                label.left   = label.parent.left + { iconWidth * (1 + newDepth) }
                label.right  = label.parent.right
                label.bottom = label.parent.bottom
            }

            constrainIcon(icon)

            layout = constraintLayout
            depth  = newDepth
        }

        when (!tree.isLeaf(this.path)) {
            true -> {
                val text = if (tree.expanded(path)) "-" else "+"

                icon = icon?.apply {
                    this.text = text
                } ?: labelFactory(text).apply {
                    fitText = false
                    width   = iconWidth

                    this@BasicTreeRow.children += this

                    mouseChanged += object: MouseListener {
                        private var pressed   = false
                        private var mouseOver = false

                        override fun mouseEntered(event: MouseEvent) {
                            mouseOver = true
                        }

                        override fun mouseExited(event: MouseEvent) {
                            mouseOver = false
                        }

                        override fun mousePressed(event: MouseEvent) {
                            pressed   = true
                            mouseOver = true
                        }

                        override fun mouseReleased(event: MouseEvent) {
                            if (mouseOver && pressed) {
                                when (tree.expanded(this@BasicTreeRow.path)) {
                                    true -> tree.collapse(this@BasicTreeRow.path)
                                    else -> tree.expand  (this@BasicTreeRow.path)
                                }
                            }
                            pressed = false
                        }
                    }

                    constrainIcon(this)
                }
            }

            false -> {
                icon?.let {
                    this.children -= it
                    constraintLayout.unconstrain(it)
                }
                icon = null
            }
        }

        background = if (tree.selected(path)) green else lightgray

        background = when {
            index.isEven -> background.lighter()
            else         -> background
        }

        backgroundColor = background
    }

    override fun render(canvas: Canvas) {
        backgroundColor?.let { canvas.rect(bounds.atOrigin, ColorBrush(it)) }
    }

    private fun constrainIcon(icon: Label?) {
        icon?.let {
            constraintLayout.constrain(it, content) { icon, label ->
                icon.top    = label.top
                icon.right  = label.left
                icon.bottom = label.bottom
            }
        }
    }
}

open class TreeLabelItemUIGenerator<T>(private val labelFactory: LabelFactory, private val focusManager: FocusManager?, private val contentGenerator: ContentGenerator<T> = LabelContentGenerator(labelFactory)): RowGenerator<T> {
    override fun invoke(tree: Tree<T, *>, node: T, path: Path<Int>, index: Int, current: View?): View = when (current) {
        is BasicTreeRow<*> -> (current as BasicTreeRow<T>).apply { update(tree, node, path, index) }
        else               -> BasicTreeRow(contentGenerator, labelFactory, focusManager, tree, node, path, index)
    }
}

class BasicTreeUI<T>(override val generator: RowGenerator<T>): TreeRenderer<T>, KeyListener {
    constructor(labelFactory: LabelFactory, focusManager: FocusManager?): this(TreeLabelItemUIGenerator(labelFactory, focusManager))

//    override val generator : RowGenerator<T>  = TreeLabelItemUIGenerator(labelFactory, focusManager)
    override val positioner: RowPositioner<T> = BasicTreeRowPositioner(20.0)

    override fun render(view: Tree<T, *>, canvas: Canvas) {}

    override fun install(view: Tree<T, *>) {
        view.keyChanged += this
    }

    override fun uninstall(view: Tree<T, *>) {
        view.keyChanged -= this
    }

    override fun keyPressed(event: KeyEvent) {
        (event.source as Tree<*, *>).let { tree ->
            when (event.code) {
                // TODO: Centralize
                VK_UP    -> tree.selection.firstOrNull()?.also { tree.rowFromPath(it)?.also { if (it > 0               ) tree.setSelection(setOf(it - 1)) } }?.let { Unit } ?: Unit
                VK_DOWN  -> tree.selection.firstOrNull()?.also { tree.rowFromPath(it)?.also { if (it < tree.numRows - 1) tree.setSelection(setOf(it + 1)) } }?.let { Unit } ?: Unit
                VK_RIGHT -> tree.selection.firstOrNull()?.also { tree.expand(it) }?.let { Unit } ?: Unit
                VK_LEFT  -> tree.selection.firstOrNull()?.also { if (tree.expanded(it)) { tree.collapse(it) } else it.parent?.let { tree.setSelection(setOf(it)) } }?.let { Unit } ?: Unit
            }
        }
    }
}

private class MutableLabelItemUIGenerator<T>(private val focusManager: FocusManager?, labelFactory: LabelFactory): TreeLabelItemUIGenerator<T>(labelFactory, focusManager) {
    override fun invoke(tree: Tree<T, *>, node: T, path: Path<Int>, index: Int, current: View?) = super.invoke(tree, node, path, index, current).also {
        if (current !is BasicTreeRow<*>) {
            val result = it as BasicTreeRow<*>

            it.mouseChanged += object: MouseListener {
                override fun mouseReleased(event: MouseEvent) {
                    if (event.clickCount == 2) {
                        (tree as MutableTree).startEditing(result.path)
                    }
                }
            }
        }
    }
}


class BasicMutableTreeUI<T>(focusManager: FocusManager?, labelFactory: LabelFactory): TreeRenderer<T>, KeyListener {
    override val generator : TreeRenderer.RowGenerator<T>  = MutableLabelItemUIGenerator(focusManager, labelFactory)
    override val positioner: TreeRenderer.RowPositioner<T> = BasicTreeRowPositioner(20.0)

    override fun render(view: Tree<T, *>, canvas: Canvas) {}

    override fun install(view: Tree<T, *>) {
        view.keyChanged += this
    }

    override fun uninstall(view: Tree<T, *>) {
        view.keyChanged -= this
    }

    override fun keyPressed(event: KeyEvent) {
        (event.source as MutableTree<*, *>).let { tree ->
            when (event.code) {
                VK_DELETE, VK_BACKSPACE -> tree.selection./*sortedByDescending { it }.*/forEach { tree.removeAt(it) }
                VK_UP                   -> tree.selection.firstOrNull()?.also { tree.rowFromPath(it)?.also { if (it > 0               ) tree.setSelection(setOf(it - 1)) } }?.let { Unit } ?: Unit
                VK_DOWN                 -> tree.selection.firstOrNull()?.also { tree.rowFromPath(it)?.also { if (it < tree.numRows - 1) tree.setSelection(setOf(it + 1)) } }?.let { Unit } ?: Unit
                VK_RIGHT                -> tree.selection.firstOrNull()?.also { tree.expand(it) }?.let { Unit } ?: Unit
                VK_LEFT                 -> tree.selection.firstOrNull()?.also { if (tree.expanded(it)) { tree.collapse(it) } else it.parent?.let { tree.setSelection(setOf(it)) } }?.let { Unit } ?: Unit
            }
        }
    }
}

@Suppress("PrivatePropertyName", "unused")
open class TextEditOperation<T>(
        private val focusManager   : FocusManager?,
        private val encoder        : Encoder<T, String>,
        private val display        : Display,
        private val positionMonitor: RelativePositionMonitor,
        private val tree           : MutableTree<T, *>,
                    node           : T,
        private val path           : Path<Int>,
                    contentBounds  : Rectangle,
        private val current        : View): TextField(), EditOperation<T> {

    private val positionChanged = { _:View, old: Rectangle, new: Rectangle ->
        bounds = Rectangle(bounds.position + new.position - old.position, bounds.size)
    }

    init {
        backgroundColor = current.backgroundColor

        bounds = contentBounds.at(contentBounds.position + tree.toAbsolute(Point.Origin))

        positionMonitor[current] += positionChanged

        text = encoder.encode(node) ?: ""

        horizontalAlignment = Left

        styleChanged += { rerender() }

        focusChanged += { _,_,_ ->
            if (!hasFocus) {
                tree.cancelEditing()
            }
        }

        this.keyChanged += object: KeyListener {
            override fun keyReleased(event: KeyEvent) {
                when (event.code) {
                    VK_RETURN -> { tree.completeEditing(); focusManager?.requestFocus(tree) }
                    VK_ESCAPE -> { tree.cancelEditing  (); focusManager?.requestFocus(tree) }
                }
            }
        }
    }

    override fun addedToDisplay() {
        focusManager?.requestFocus(this)
        selectAll()
    }

    override fun invoke(): View? {
        display.children += this
        return null
    }

    override fun complete() = encoder.decode(text).also {
        display.children -= this
        positionMonitor[current] -= positionChanged
    }

    override fun cancel() {
        display.children -= this
        positionMonitor[current] -= positionChanged
    }
}

class TreeTextEditor<T>(
        private val focusManager   : FocusManager?,
        private val encoder        : Encoder<T, String>,
        private val display        : Display,
        private val positionMonitor: RelativePositionMonitor): TreeEditor<T> {
    override fun edit(tree: MutableTree<T, *>, node: T, path: Path<Int>, contentBounds: Rectangle, current: View): EditOperation<T> = TextEditOperation(focusManager, encoder, display, positionMonitor, tree, node, path, contentBounds, current)

    companion object {
        operator fun invoke(focusManager: FocusManager?, display: Display, positionMonitor: RelativePositionMonitor): TreeTextEditor<String> {
            return TreeTextEditor(focusManager, object: Encoder<String, String> {
                override fun decode(b: String) = b
                override fun encode(a: String) = a
            }, display, positionMonitor)
        }
    }
}