package com.nectar.doodle.controls.table

import com.nectar.doodle.controls.ItemGenerator
import com.nectar.doodle.controls.ListModel
import com.nectar.doodle.controls.ListSelectionManager
import com.nectar.doodle.controls.Selectable
import com.nectar.doodle.controls.SelectionModel
import com.nectar.doodle.controls.SimpleListModel
import com.nectar.doodle.controls.list.ListBehavior
import com.nectar.doodle.controls.panels.ScrollPanel
import com.nectar.doodle.core.Box
import com.nectar.doodle.core.Layout
import com.nectar.doodle.core.Positionable
import com.nectar.doodle.core.View
import com.nectar.doodle.drawing.AffineTransform.Companion.Identity
import com.nectar.doodle.drawing.Canvas
import com.nectar.doodle.geometry.Point
import com.nectar.doodle.geometry.Rectangle
import com.nectar.doodle.geometry.Size
import com.nectar.doodle.layout.constant
import com.nectar.doodle.layout.constrain
import com.nectar.doodle.scheduler.Strand
import com.nectar.doodle.utils.AdaptingObservableSet
import com.nectar.doodle.utils.Cancelable
import com.nectar.doodle.utils.ObservableSet
import com.nectar.doodle.utils.Pool
import com.nectar.doodle.utils.SetObserver
import com.nectar.doodle.utils.SetPool
import kotlin.math.max
import kotlin.math.min

open class Table<T, M: ListModel<T>>(
        private   val strand        : Strand,
        protected val model         : M,
        protected val selectionModel: SelectionModel<Int>? = null,
                      block         : ColumnBuilder<T>.() -> Unit): View(), Selectable<Int> by ListSelectionManager(selectionModel, { model.size }) {

    private inner class ColumnBuilderImpl: ColumnBuilder<T> {
        override fun <R> column(
                header       : View?,
                width        : Double?,
                minWidth     : Double,
                maxWidth     : Double?,
                itemGenerator: ItemGenerator<R>,
                extractor    : (T) -> R
        ) = InternalColumn(header, itemGenerator, width, minWidth, maxWidth, extractor).also { internalColumns += it }
    }

    private inner class InternalColumn<R>(
            override val header        : View?,
                     val itemGenerator : ItemGenerator<R>,
                         preferredWidth: Double? = null,
            override val minWidth      : Double  = 0.0,
            override val maxWidth      : Double? = null,
                         extractor     : T.() -> R): Column<T>, ColumnSizePolicy.Column {

        private inner class FieldModel<A>(private val model: M, private val extractor: T.() -> A): ListModel<A> {
            override val size get() = model.size

            override fun get(index: Int) = model[index]?.let(extractor)

            override fun section(range: ClosedRange<Int>) = model.section(range).map(extractor)

            override fun contains(value: A) = value in model.map(extractor)

            override fun iterator() = model.map(extractor).iterator()
        }

        override var preferredWidth = preferredWidth
            set(new) {
                field = new

                field?.let {
                    resizingCol = index
                    columnSizePolicy.widthChanged(this@Table.width, internalColumns, index, it)
                    doLayout()
                    resizingCol = null
                }
            }

        override var width = preferredWidth ?: minWidth
            set(new) {
                field = max(minWidth, new).let {
                    if (maxWidth != null) {
                        min(maxWidth, it)
                    } else {
                        it
                    }
                }
            }

        private val x get() = view.x

        private val index get() = columns.indexOf(this)

        private var transform get() = view.transform
            set(new) {
                this@Table.header.children.getOrNull(index)?.transform = new
                view.transform = new
            }

        private var animation: Cancelable? = null
            set(new) {
                field?.cancel()

                field = new
            }

        override fun moveBy(x: Double) {
            val translateX = transform.translateX
            val delta      = min(max(x, 0 - (view.x + translateX)), this@Table.width - width - (view.x + translateX))

            transform *= Identity.translate(delta)

            internalColumns.dropLast(1).forEachIndexed { index, column ->
                if (column != this) {
                    val targetBounds = this@Table.header.children[index].bounds
                    val targetMiddle = targetBounds.x + column.transform.translateX + targetBounds.width / 2

                    val value = when (targetMiddle) {
                        in view.x + translateX + delta            .. view.x + translateX                    ->  width
                        in view.x + translateX                    .. view.x + translateX + delta            -> -width
                        in view.bounds.right + translateX         .. view.bounds.right + translateX + delta -> -width
                        in view.bounds.right + translateX + delta .. view.bounds.right + translateX         ->  width
                        else                                                                                ->  null
                    }

                    value?.let {
                        val oldTransform = column.transform
                        val minViewX     = if (index > this.index) column.x - width else column.x
                        val maxViewX     = minViewX + width
                        val offset       = column.x + column.transform.translateX
                        val translate    = min(max(value, minViewX - offset), maxViewX - offset)

//                        println("index: $index, min: $minViewX, max: $maxViewX, offset: $offset, translate: $translate")

                        column.animation = behavior?.moveColumn {
                            column.transform = oldTransform.translate(translate * it)
                        }
                    }
                }
            }
        }

        override fun resetPosition() {
            var moved      = false
            val myOffset   = view.x + transform.translateX
            var myNewIndex = if (myOffset >= internalColumns.last().view.x ) internalColumns.size - 2 else index

            internalColumns.forEachIndexed { index, column ->
                if (!moved && myOffset < column.view.x + column.transform.translateX) {
                    myNewIndex = index - if (this.index < index) 1 else 0
                    moved      = true
                }

                column.animation?.cancel()
                column.transform = Identity
            }

            if (index == myNewIndex) {
                return
            }

            this@Table.header.children.batch {
                if (myNewIndex < size) {
                    add(myNewIndex, removeAt(index))
                } else {
                    add(removeAt(index))
                }
            }

            (panel.content as Box).children.batch {
                if (myNewIndex < size) {
                    add(myNewIndex, removeAt(index))
                } else {
                    add(removeAt(index))
                }
            }

            internalColumns.add(myNewIndex, internalColumns.removeAt(index))

            doLayout()
        }

        val view = com.nectar.doodle.controls.list.List(strand, FieldModel(model, extractor), itemGenerator).apply {
            acceptsThemes = false
        }

        fun behavior(behavior: TableBehavior<T>?) {
            behavior?.let {
                view.behavior = object : ListBehavior<R> {
                    override val generator: ListBehavior.RowGenerator<R>
                        get() = object : ListBehavior.RowGenerator<R> {
                            override fun invoke(list: com.nectar.doodle.controls.list.List<R, *>, row: R, index: Int, current: View?) = behavior.cellGenerator.invoke(this@Table, row, index, itemGenerator, current)
                        }

                    override val positioner: ListBehavior.RowPositioner<R>
                        get() = object : ListBehavior.RowPositioner<R> {
                            override fun invoke(list: com.nectar.doodle.controls.list.List<R, *>, row: R, index: Int) = behavior.rowPositioner.invoke(this@Table, model[index]!!, index).run { Rectangle(0.0, y, list.width, height) }

                            override fun rowFor(list: com.nectar.doodle.controls.list.List<R, *>, y: Double) = behavior.rowPositioner.rowFor(this@Table, y)
                        }

                    override fun render(view: com.nectar.doodle.controls.list.List<R, *>, canvas: Canvas) {
                        if (this@InternalColumn != internalColumns.last()) {
                            behavior.renderColumnBody(this@Table, this@InternalColumn, canvas)
                        }
                    }
                }
            }
        }
    }

    val numRows get() = model.size
    val isEmpty get() = model.isEmpty

    var columnSizePolicy: ColumnSizePolicy<T> = ConstrainedSizePolicy()
        set(new) {
            field = new

            doLayout()
        }

    var behavior = null as TableBehavior<T>?
        set(new) {
            if (new == behavior) { return }

            field?.let {
                it.bodyDirty   = null
                it.headerDirty = null

                it.uninstall(this)
            }

            field = new?.also { behavior ->
                behavior.bodyDirty   = bodyDirty
                behavior.headerDirty = headerDirty

                internalColumns.forEach {
                    it.behavior(behavior)
                }

                behavior.install(this)

                header.children.batch {
                    clear()

                    headerItemsToColumns.clear()

                    addAll(internalColumns.dropLast(1).map { column ->
                        behavior.headerCellGenerator(this@Table, column).also {
                            headerItemsToColumns[it] = column
                        }
                    })
                }

                behavior.headerPositioner.invoke(this@Table).apply {
                    header.height = height
                }

                layout = constrain(header, panel) { header, panel ->
                    behavior.headerPositioner.invoke(this@Table).apply {
                        header.top    = header.parent.top + y
                        header.height = constant(height)
                    }

                    panel.top    = header.bottom
                    panel.left   = panel.parent.left
                    panel.right  = panel.parent.right
                    panel.bottom = panel.parent.bottom
                }
            }
        }

    val columns: List<Column<T>> get() = internalColumns.dropLast(1)

    val selectionChanged: Pool<SetObserver<Table<T, *>, Int>> = SetPool()

    fun contains(value: T) = value in model

    private val internalColumns = mutableListOf<InternalColumn<*>>()

    init {
        ColumnBuilderImpl().apply(block)

        internalColumns += InternalColumn(null, itemGenerator = object : ItemGenerator<String> {
            override fun invoke(item: String, previous: View?) = object : View() {}
        }) { "" } // FIXME: Use a more robust method to avoid any rendering of the cell contents
    }

    private val headerItemsToColumns = mutableMapOf<View, InternalColumn<*>>()

    private val header = object: Box() {
        init {
            layout = object : Layout() {
                override fun layout(positionable: Positionable) {
                    var x = 0.0
                    var totalWidth = 0.0

                    positionable.children.forEachIndexed { index, view ->
                        view.bounds = Rectangle(Point(x, 0.0), Size(internalColumns[index].width, positionable.height))

                        x += view.width
                        totalWidth += view.width
                    }

                    positionable.width = totalWidth + internalColumns[internalColumns.size - 1].width
                }
            }
        }

        override fun render(canvas: Canvas) {
            behavior?.renderHeader(this@Table, canvas)
        }
    }

    private val panel = ScrollPanel(object: Box() {
        init {
            children += internalColumns.map { it.view }

            layout = object : Layout() {
                override fun layout(positionable: Positionable) {
                    var x          = 0.0
                    var height     = 0.0
                    var totalWidth = 0.0

                    positionable.children.forEachIndexed { index, view ->
                        view.bounds = Rectangle(Point(x, 0.0), Size(internalColumns[index].width, view.minimumSize.height))

                        x          += view.width
                        height      = max(height, view.height)
                        totalWidth += view.width
                    }

                    positionable.size = Size(max(positionable.parent!!.width, totalWidth), max(positionable.parent!!.height, height))

                    positionable.children.forEach {
                        it.height = positionable.height
                    }
                }
            }
        }

        override fun render(canvas: Canvas) {
            behavior?.renderBody(this@Table, canvas)
        }
    }.apply {
        // FIXME: Use two scroll-panels instead since async scrolling makes this look bad
        boundsChanged += { _,old,new ->
            if (old.x != new.x) {
                header.x = new.x
            }
        }
    })
    @Suppress("PrivatePropertyName")
    protected open val selectionChanged_: SetObserver<SelectionModel<Int>, Int> = { set,removed,added ->
        val adaptingSet: ObservableSet<Table<T, *>, Int> = AdaptingObservableSet(this, set)

        (selectionChanged as SetPool).forEach {
            it(adaptingSet, removed, added)
        }
    }

    init {
        children += listOf(header, panel)

        selectionModel?.let { it.changed += selectionChanged_ }
    }

    private val bodyDirty  : () -> Unit = { panel.content?.rerender() }
    private val headerDirty: () -> Unit = { header.rerender        () }

    operator fun get(index: Int) = model[index]

    override fun removedFromDisplay() {
        selectionModel?.let { it.changed -= selectionChanged_ }

        super.removedFromDisplay()
    }

    public override var insets
        get(   ) = super.insets
        set(new) { super.insets = new }

    private var resizingCol: Int? = null

    override fun doLayout() {
        resizingCol = resizingCol ?: 0
        width = columnSizePolicy.layout(this.width, this.internalColumns, resizingCol ?: 0)
        resizingCol = null

        super.doLayout()

        header.doLayout()
        (panel.content as? Box)?.doLayout()
    }

    companion object {
        operator fun <T> invoke(
                       strand        : Strand,
                       values        : List<T>,
                       selectionModel: SelectionModel<Int>? = null,
                       block         : ColumnBuilder<T>.() -> Unit): Table<T, ListModel<T>> = Table(strand, SimpleListModel(values), selectionModel, block)
    }
}