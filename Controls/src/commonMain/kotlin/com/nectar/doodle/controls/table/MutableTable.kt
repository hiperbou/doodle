package com.nectar.doodle.controls.table

import com.nectar.doodle.controls.EditOperation
import com.nectar.doodle.controls.ItemGenerator
import com.nectar.doodle.controls.ModelObserver
import com.nectar.doodle.controls.MutableListModel
import com.nectar.doodle.controls.SelectionModel
import com.nectar.doodle.core.View

/**
 * Created by Nicholas Eddy on 5/19/19.
 */

interface TableEditor<T> {
    fun <R> edit(list: MutableTable<T, *>, row: T, column: Column<R>, index: Int, current: View): EditOperation<T>
}

class MutableTable<T, M: MutableListModel<T>>(
        model         : M,
        selectionModel: SelectionModel<Int>? = null,
        block         : MutableColumnFactory<T>.() -> Unit): Table<T, M>(model, selectionModel, {}) {

    private val editors = mutableMapOf<Column<*>, ((T) -> T)?>()

    private inner class MutableColumnFactoryImpl: MutableColumnFactory<T> {
        override fun <R> column(header: View?, extractor: T.() -> R, cellGenerator: ItemGenerator<R>, editor: ((T) -> T)?, builder: ColumnBuilder.() -> Unit) = ColumnBuilderImpl().run {
            builder(this)

            InternalColumn(header, headerAlignment, cellGenerator, cellAlignment, width, minWidth, maxWidth, extractor).also {
                internalColumns += it

                editors[it] = editor
            }
        }
    }

    private val modelChanged: ModelObserver<T> = { _,removed,added,_ ->
        var trueRemoved = removed.filterKeys { it !in added   }
        var trueAdded   = added.filterKeys   { it !in removed }

        itemsRemoved(trueRemoved)
        itemsAdded  (trueAdded  )

        val oldHeight = height

//        if (trueRemoved.isNotEmpty() || trueAdded.isNotEmpty()) {
//            updateVisibleHeight()
//        }
//
//        trueAdded   = trueAdded.filterKeys   { it <= lastVisibleRow }
//        trueRemoved = trueRemoved.filterKeys { it <= lastVisibleRow }
//
//        if (trueRemoved.size > trueAdded.size && height < oldHeight) {
//            children.batch {
//                for (it in 0 until trueRemoved.size - trueAdded.size) {
//                    removeAt(0)
//                }
//            }
//        }
//
//        if (trueRemoved.isNotEmpty() || trueAdded.isNotEmpty()) {
//            // FIXME: Make this more efficient
//            (firstVisibleRow..lastVisibleRow).forEach { update(children, it) }
//        } else {
//            // These are the edited rows
//            added.keys.filter { it in removed }.forEach { update(children, it) }
//        }
    }

//    var <T: MutableTable<R,*>, R> T.behavior: TableBehavior<R>?
//        get(     ) = behavior_
//        set(value) { behavior_ = value }
//
//    private var behavior_ = null as MutableTableBehavior<T>?

    init {
        MutableColumnFactoryImpl().apply(block)

        model.changed += modelChanged
    }

    val editing get() = editingRow != null

    var editor = null as TableEditor<T>?

    private var editingRow    = null as Int?
    private var editOperation = null as EditOperation<T>?

    fun add      (value : T                         ) = model.add      (value        )
    fun add      (index : Int, values: T            ) = model.add      (index, values)
    fun remove   (value : T                         ) = model.remove   (value        )
    fun removeAt (index : Int                       ) = model.removeAt (index        )
    fun addAll   (values: Collection<T>             ) = model.addAll   (values       )
    fun addAll   (index : Int, values: Collection<T>) = model.addAll   (index, values)
    fun removeAll(values: Collection<T>             ) = model.removeAll(values       )
    fun retainAll(values: Collection<T>             ) = model.retainAll(values       )

    fun clear() = model.clear()

    override fun removedFromDisplay() {
        model.changed -= modelChanged

        super.removedFromDisplay()
    }

    fun <R> startEditing(index: Int, column: Column<R>) {
        editor?.let {
            model[index]?.let { row ->
                val i = index % children.size

                editingRow    = index
                editOperation = it.edit(this, row, column, index, children[i]).also {
                    it()?.let { children[i] = it }

//                    layout(children[i], row, index)
                }
            }
        }
    }

    fun completeEditing() {
        editOperation?.let { operation ->
            editingRow?.let { index ->
                val result = operation.complete() ?: return

                cleanupEditing()

                if (result == model.set(index, result)) {
                    // This is the case that the "new" value is the same as what was there
                    // so need to explicitly update since the model won't fire a change
//                    update(children, index)
                }
            }
        }
    }

    fun cancelEditing() {
//        cleanupEditing()?.let { update(children, it) }
    }

    private fun cleanupEditing(): Int? {
        editOperation?.cancel()
        val result    = editingRow
        editOperation = null
        editingRow    = null

        return result
    }

    private fun itemsAdded(values: Map<Int, T>) {
        if (selectionModel != null && values.isNotEmpty()) {
            val updatedSelection = mutableSetOf<Int>()

            for (selectionItem in selectionModel) {
                var delta = 0

                for (index in values.keys) {
                    if (selectionItem >= index) {
                        ++delta
                    }
                }

                updatedSelection.add(selectionItem + delta)
            }

            setSelection(updatedSelection)
        }
    }

    private fun itemsRemoved(values: Map<Int, T>) {
        if (selectionModel != null && values.isNotEmpty()) {

            val updatedSelection = mutableSetOf<Int>()

            for (selectionItem in selectionModel) {
                var delta = 0

                for (index in values.keys) {
                    if (selectionItem > index) {
                        delta--
                    }
                }

                if (delta != 0) {
                    updatedSelection.add(selectionItem + delta)
                }
            }

            removeSelection(values.keys)

            setSelection(updatedSelection)
        }
    }

    companion object {
//        operator fun invoke(
//                strand        : Strand,
//                progression   : IntProgression,
//                itemGenerator : ItemGenerator<Int>,
//                selectionModel: SelectionModel<Int>? = null,
//                fitContent    : Boolean              = true,
//                cacheLength   : Int                  = 10) =
//                MutableTable(strand, progression.toMutableList(), itemGenerator, selectionModel, fitContent, cacheLength)
//
//        operator fun <T> invoke(
//                strand        : Strand,
//                values        : kotlin.collections.List<T>,
//                itemGenerator : ItemGenerator<T>,
//                selectionModel: SelectionModel<Int>? = null,
//                fitContent    : Boolean              = true,
//                cacheLength   : Int                  = 10): MutableList<T, SimpleMutableListModel<T>> =
//                MutableTable(strand, SimpleMutableListModel(values.toMutableList()), itemGenerator, selectionModel, fitContent, cacheLength)
    }
}