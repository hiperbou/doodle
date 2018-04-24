package com.nectar.doodle.controls.tree

import com.nectar.doodle.JvmName
import com.nectar.doodle.controls.SelectionModel
import com.nectar.doodle.controls.theme.TreeUI
import com.nectar.doodle.controls.theme.TreeUI.ItemPositioner
import com.nectar.doodle.controls.theme.TreeUI.ItemUIGenerator
import com.nectar.doodle.controls.tree.Tree.Direction.Down
import com.nectar.doodle.controls.tree.Tree.Direction.Up
import com.nectar.doodle.core.Gizmo
import com.nectar.doodle.drawing.Canvas
import com.nectar.doodle.event.DisplayRectEvent
import com.nectar.doodle.utils.SetPool
import kotlin.math.min


/**
 * Created by Nicholas Eddy on 3/23/18.
 */

private class Property<T>(var value: T)

typealias ExpansionObserver<T> = (source: Tree<T>, paths: Set<Path<Int>>) -> Unit

typealias ExpansionObservers<T> = SetPool<ExpansionObserver<T>>

private class ExpansionObserversImpl<T>(
        private val source: Tree<T>,
        mutableSet: MutableSet<ExpansionObserver<T>> = mutableSetOf()): SetPool<ExpansionObserver<T>>(mutableSet) {
    operator fun invoke(paths: Set<Path<Int>>) = delegate.forEach { it(source, paths) }
}

private object PathComparator: Comparator<Path<Int>> {
    override fun compare(a: Path<Int>, b: Path<Int>): Int {
        (0 until min(a.depth, b.depth)).forEach {
            (a[it] - b[it]).let {
                if (it != 0) {
                    return it
                }
            }
        }

        return 0
    }
}

private val DepthComparator = Comparator<Path<Int>> { a, b -> b.depth - a.depth }

class Tree<T>(private val model: Model<T>, private val selectionModel: SelectionModel<Path<Int>>? = null): Gizmo() {
    var rootVisible = false
        set(new) {
            if (field == new) { return }

            field = new

            // TODO: make more efficient?

            children.batch {
                clear()
                refreshAll()
            }
        }

    var numRows = 0
        private set

    public override var insets
        get(   ) = super.insets
        set(new) { super.insets = new }

    var renderer: TreeUI<T>? = null
        set(new) {
            if (new == renderer) { return }

            field = new?.also {
                itemPositioner  = it.positioner
                itemUIGenerator = it.uiGenerator

                children.batch {
                    clear()
                    refreshAll()
                }
            }
        }

    val expanded : ExpansionObservers<T> by lazy { ExpansionObserversImpl(this) }
    val collapsed: ExpansionObservers<T> by lazy { ExpansionObserversImpl(this) }

    private var itemPositioner : ItemPositioner<T>? = null
    private var itemUIGenerator: ItemUIGenerator<T>? = null

    private val expandedPaths = mutableSetOf<Path<Int>>()

    private val rowToPath = mutableMapOf<Int, Path<Int>>()
//    private val pathToRow = mutableMapOf<Path<Int>, Int>()

    private var firstVisibleRow =  0
    private var lastVisibleRow  = -1

    init {
        monitorsDisplayRect = true
        updateNumRows()
    }

    override fun render(canvas: Canvas) {
        renderer?.render(this, canvas)
    }

    override fun handleDisplayRectEvent(event: DisplayRectEvent) {
        val oldFirst = firstVisibleRow
        val oldLast  = lastVisibleRow

        event.apply {
            firstVisibleRow = new.y.let { when {
                it > old.y -> findRowAt(it, firstVisibleRow, Down)
                it < old.y -> findRowAt(it, firstVisibleRow, Up  )
                else       -> firstVisibleRow
            }}

            lastVisibleRow = (new.y + new.height).let { when {
                it > old.y + old.height -> findRowAt(it, lastVisibleRow, Down)
                it < old.y + old.height -> findRowAt(it, lastVisibleRow, Up  )
                else                    -> lastVisibleRow
            }}
        }

//        println("display rect changed: ${event.new}")

//        println("first: $firstVisibleRow, last: $lastVisibleRow")

        if (oldFirst > firstVisibleRow) {
            (firstVisibleRow until oldFirst).asSequence().mapNotNull { pathFromRow(it)?.run { it to this } }.forEach { (index, path) ->
                insert(children, path, index)
            }
        }

        if (oldLast < lastVisibleRow) {
            (oldLast + 1 .. lastVisibleRow).asSequence().mapNotNull { pathFromRow(it)?.run { it to this } }.forEach { (index, path) ->
                insert(children, path, index)
            }
        }
    }

    operator fun get(path: Path<Int>): T? = model[path]

    operator fun get(row: Int): T? = rowToPath[row]?.let { model[it] }

    fun isLeaf(path: Path<Int>) = model.isLeaf(path)

    fun expanded(path: Path<Int>) = path in expandedPaths

    @JvmName("expandRows") fun expand(row : Int     ) = expand(setOf(row))
    @JvmName("expandRows") fun expand(rows: Set<Int>) = expand(rows.asSequence().map { rowToPath[it] }.filterNotNull().toSet())

    fun expand(path: Path<Int>) = expand(setOf(path))

    fun expand(paths: Set<Path<Int>>) {
        val pathList = paths.asSequence().filter { it.depth > 0 && !expanded(it) }.sortedWith(PathComparator.then(DepthComparator))

        var empty         = true
        val pathsToUpdate = mutableSetOf<Path<Int>>()

        children.batch {

            pathList.forEach {
                empty = false

                expandedPaths += it

                updateNumRows()

                if (visible(it)) {
                    pathsToUpdate -= it

                    this@Tree.height += heightBelow(it)

                    update        (this, it)
                    insertChildren(this, it)

                    pathsToUpdate += ancestralSiblingsAfter(it)
                }
            }

            pathsToUpdate.forEach {
                updateRecursively(this, it)
            }
        }

        expandedPaths.addAll(paths)

        if (!empty) {
            (expanded as ExpansionObserversImpl)(pathList.toSet())
        }
    }

    fun expandAll() {
        val pathsToExpand = HashSet<Path<Int>>()

        expandAllBelowPath(Path(), pathsToExpand)

        expand(pathsToExpand)
    }

    @JvmName("collapseRows") fun collapse(row : Int     ) = collapse(setOf(row))
    @JvmName("collapseRows") fun collapse(rows: Set<Int>) = collapse(rows.asSequence().map { rowToPath[it] }.filterNotNull().toSet())

    fun collapse(path : Path<Int>     ) = collapse(setOf(path))
    fun collapse(paths: Set<Path<Int>>) {
        val pathList = paths.asSequence().filter { it.depth > 0 && expanded(it) }.sortedWith(PathComparator.thenDescending(DepthComparator))
        var empty    = true

        children.batch {
            pathList.firstOrNull { visible(it) }?.let {
                expandedPaths -= pathList
                empty          = false

                updateNumRows()

                update(this, it)

                ancestralSiblingsAfter(it).forEach {
                    updateRecursively(this, it)
                }

                // FIXME: This should be handled better
                this@Tree.height = heightBelow(Path()) + insets.run { top + bottom }

                // Remove old children
                (numRows until size).forEach {
                    removeAt(numRows)
                    rowToPath.remove(it) //?.let { pathToRow.remove(it) }
                }
            }
        }

        if (!empty) {
            (collapsed as ExpansionObserversImpl)(pathList.toSet())
        }
    }

    fun collapseAll() = collapse(expandedPaths)

    fun selected(row : Int      ) = rowToPath[row]?.let { selected(it) } ?: false
    fun selected(path: Path<Int>) = selectionModel?.contains(path) ?: false

    @JvmName("addSelectionRows")
    fun addSelection(rows : Set<Int>      ) = addSelection(rows.asSequence().map { rowToPath[it] }.filterNotNull().toSet())
    fun addSelection(paths: Set<Path<Int>>) {
        selectionModel?.addAll(paths)
    }

    @JvmName("setSelectionRows")
    fun setSelection(rows : Set<Int>      ) = setSelection(rows.asSequence().map { rowToPath[it] }.filterNotNull().toSet())
    fun setSelection(paths: Set<Path<Int>>) {
        selectionModel?.replaceAll(paths)
    }

    @JvmName("removeSelectionRows")
    fun removeSelection(rows : Set<Int>      ) = removeSelection(rows.asSequence().map { rowToPath[it] }.filterNotNull().toSet())
    fun removeSelection(paths: Set<Path<Int>>) {
        selectionModel?.removeAll(paths)
    }

    fun clearSelection() = selectionModel?.clear()

    fun visible(row: Int) = rowToPath[row]?.let { visible(it) } ?: false

    tailrec fun visible(path: Path<Int>): Boolean {
        return when {
            path.depth == 0 -> rootVisible
            path.depth == 1 -> true
            else            -> {
                val parent = path.parent

                when {
                    parent == null || !expanded(parent) -> false
                    else                                -> visible(parent)
                }
            }
        }
    }

    fun makeVisible(path: Path<Int>) {
        var parent = path.parent

        while (parent != null) {
            expand(parent)

            parent = parent.parent
        }
    }

    private enum class Direction {
        Up, Down
    }

    private fun updateNumRows() {
        numRows = rowsBelow(Path()) + if(rootVisible) 1 else 0
    }

    private fun findRowAt(y: Double, nearbyRow: Int, direction: Direction): Int {
        return min(numRows - 1, itemPositioner?.rowFor(y) ?: nearbyRow)

//        var index = nearbyRow
//
//        itemPositioner?.let { positioner ->
//            while (true) {
//                pathFromRow(index)?.let { path ->
//                    val bounds = positioner(this, this[path]!!, path, index)
//
//                    when (direction) {
//                        Up   -> if (index <= 0           || y >  bounds.y                ) return index else --index
//                        else -> if (index >= numRows - 1 || y <= bounds.y + bounds.height) return index else ++index
//                    }
//                }
//            }
//        }
//
//        return index
    }

    private fun siblingsAfter(path: Path<Int>, parent: Path<Int>) = path.bottom?.let {
        (it + 1 until model.numChildren(parent)).map { parent + it }
    } ?: emptyList()

    private fun ancestralSiblingsAfter(path: Path<Int>): Set<Path<Int>> {
        var parent = path.parent
        var child  = path
        val result = mutableSetOf<Path<Int>>()

        while (parent != null) {
            result += siblingsAfter(child, parent)
            child  = parent
            parent = parent.parent
        }

        result += siblingsAfter(child, parent ?: Path())

        return result
    }

    private fun refreshAll() {
        val root = Path<Int>()

        // FIXME: Move to better location; handle rootVisible case
        height = heightBelow(root) + insets.run { top + bottom }
    }

    private fun insertChildren(children: MutableList<Gizmo>, parent: Path<Int>, parentIndex: Int = rowFromPath(parent)): Int {
        var index = parentIndex + 1

        (0 until model.numChildren(parent)).forEach { index = insert(children, parent + it, index) }

        return index
    }

    private fun insert(children: MutableList<Gizmo>, path: Path<Int>, index: Int = rowFromPath(path)): Int {
        var result = index

        // Path index not found (could be invisible)
        if (index >= 0) {
            itemUIGenerator?.let {
                model[path]?.let { value ->
                    rowToPath[index] = path
//                    pathToRow[path ] = index

                    val expanded = path in expandedPaths

                    if (children.size <= lastVisibleRow - firstVisibleRow) {
                        it(this, value, path, index).also {
                            when {
                                index > children.lastIndex -> children.add(it)
                                else                       -> children.add(index, it)
                            }

                            layout(it, value, path, index)
                        }
                    } else {
                        update(children, path, index)
                    }

                    ++result

                    if (path.depth == 0 || expanded) {
                        result = insertChildren(children, path, index)
                    }
                }
            }
        }

        return result
    }

    private fun updateChildren(children: MutableList<Gizmo>, parent: Path<Int>, parentIndex: Int = rowFromPath(parent)): Int {
        var index = parentIndex + 1

        (0 until model.numChildren(parent)).forEach { index = updateRecursively(children, parent + it, index) }

        return index
    }

    private fun update(children: MutableList<Gizmo>, path: Path<Int>, index: Int = rowFromPath(path)): Int {
        var result = index

        // Path index not found (could be invisible)
        if (index in firstVisibleRow .. lastVisibleRow) {
            itemUIGenerator?.let {
                model[path]?.let { value ->
                    rowToPath[index] = path
//                    pathToRow[path ] = index

                    val i = index % children.size

                    it(this, value, path, index, children.getOrNull(i)).also {
                        children[i] = it

                        layout(it, value, path, index)
                    }

                    ++result
                }
            }
        }

        return result
    }

    private fun layout(gizmo: Gizmo, node: T, path: Path<Int>, index: Int) {
        itemPositioner?.let {
            gizmo.bounds = it(this, node, path, index)
        }
    }

    private fun updateRecursively(children: MutableList<Gizmo>, path: Path<Int>, index: Int = rowFromPath(path)): Int {
        var result = update(children, path, index)

        if (result >= 0 && expanded(path)) {
            result = updateChildren(children, path, index)
        }

        return result
    }

    private fun rowExpanded(index: Int) = pathFromRow(index)?.let { expanded(it) } ?: false

    private fun pathFromRow(index: Int): Path<Int>? {
        if (model.isEmpty()) {
            return null
        }

//        return rowToPath.getOrPut(index) {
//            addRowsToPath(Path(), Property(index + if (!rootVisible) 1 else 0)).also {
//                pathToRow[it] = index
//            }
//        }

        return addRowsToPath(Path(), index + if (!rootVisible) 1 else 0).first
    }

    // TODO: Have this return an Int?
    private fun rowFromPath(path: Path<Int>): Int /*= pathToRow.getOrPut(path)*/ {
        var row = if (rootVisible) 0 else -1
        var pathIndex = 0
        var currentPath = Path<Int>()
        var numChildren = model.numChildren(currentPath)

        if (pathIndex < path.depth) {
            var i = 0
            while (i < numChildren) {
                ++row

                if (i == path[pathIndex]) {
                    pathIndex++

                    if (pathIndex >= path.depth) {
                        return row
                    }

                    if (!rowExpanded(row)) {
                        break
                    }

                    currentPath += i
                    numChildren = model.numChildren(currentPath)
                    i = -1
                } else {
                    row += rowsBelow(currentPath + i)
                }
                ++i
            }

            row = -1
        }

        return row
    }

    private fun rowsBelow(path: Path<Int>): Int {
        var numRows = 0

        if (path.depth == 0 || (path in expandedPaths && visible(path))) {
            val numChildren = model.numChildren(path)

            (0 until numChildren).asSequence().map { path + it }.forEach { numRows += rowsBelow(it) + 1 }
        }

        return numRows
    }

    private fun heightBelow(path: Path<Int>): Double {
        // TODO: move this logic into ItemPositioner
        return rowsBelow(path) * (model[path]?.let { itemPositioner?.invoke(this, it, path, 0)?.height } ?: 0.0)
    }

    private fun expandAllBelowPath(path: Path<Int>, expandedPath: MutableSet<Path<Int>> = mutableSetOf()) {
        if (model.isLeaf(path)) {
            return
        }

        val numChildren = model.numChildren(path)

        (0 until numChildren).forEach {
            (path + it).let { child ->
                expandedPath += child

                if (!model.isLeaf(child)) {
                    if (child !in expandedPaths) {
                        expandedPath.add(child)
                    }

                    expandAllBelowPath(child, expandedPath)
                }
            }
        }
    }

    private fun addRowsToPath(path: Path<Int>, index: Int): Pair<Path<Int>, Int> {
        if (index <= 0) {
            return path to index
        }

        var newIndex = index

        var newPath     = path
        val numChildren = model.numChildren(path)

        for(i in 0 until numChildren) {
            newPath = path + i

            --newIndex

            if (newIndex == 0) {
                break
            }

            if (expanded(newPath)) {
                addRowsToPath(newPath, newIndex).also {
                    newPath  = it.first
                    newIndex = it.second
                }

                if (newIndex == 0) {
                    break
                }
            }
        }

        return newPath to newIndex
    }
}
