package com.nectar.doodle.utils

/**
 * Created by Nicholas Eddy on 10/21/17.
 */

typealias PropertyObserver<S, T> = (source: S, old: T, new: T) -> Unit

interface PropertyObservers<out S, out T> {
    operator fun plusAssign (observer: PropertyObserver<S, T>)
    operator fun minusAssign(observer: PropertyObserver<S, T>)
}

class PropertyObserversImpl<S, T>(private val mutableSet: MutableSet<PropertyObserver<S, T>>): MutableSet<PropertyObserver<S, T>> by mutableSet, PropertyObservers<S, T> {
    override fun plusAssign(observer: PropertyObserver<S, T>) {
        mutableSet += observer
    }

    override fun minusAssign(observer: PropertyObserver<S, T>) {
        mutableSet -= observer
    }
}


typealias ListObserver<S, T> = (source: ObservableList<S, T>, removed: List<Int>, added: Map<Int, T>) -> Unit

interface ListObservers<S, T> {
    operator fun plusAssign (observer: ListObserver<S, T>)
    operator fun minusAssign(observer: ListObserver<S, T>)
}

class ListObserversImpl<S, T>(private val mutableSet: MutableSet<ListObserver<S, T>>): MutableSet<ListObserver<S, T>> by mutableSet, ListObservers<S, T> {
    override fun plusAssign(observer: ListObserver<S, T>) {
        mutableSet += observer
    }

    override fun minusAssign(observer: ListObserver<S, T>) {
        mutableSet -= observer
    }
}

// TODO: Change so only deltas are reported
class ObservableList<S, E>(val source: S, private val list: MutableList<E>): MutableList<E> by list {

    private val onChange_ = ListObserversImpl<S, E>(mutableSetOf())
    val onChange: ListObservers<S, E> = onChange_

    override fun add(element: E) = list.add(element).ifTrue {
        onChange_.forEach {
            it(this, listOf(), mapOf(Pair(list.size - 1, element)))
        }
    }

    override fun remove(element: E): Boolean {
        val index = list.indexOf(element)

        return when {
            index < 0 -> false
            else      -> list.remove(element).ifTrue { onChange_.forEach { it(this, listOf(index), mapOf()) } }
        }
    }

    override fun addAll(elements: Collection<E>) = batch { list.addAll(elements) }

    override fun addAll(index: Int, elements: Collection<E>) = batch { list.addAll(index, elements) }

    override fun removeAll(elements: Collection<E>) = batch { list.removeAll(elements) }
    override fun retainAll(elements: Collection<E>) = batch { list.retainAll(elements) }

    override fun clear() {
        val size = list.size

        list.clear()

        onChange_.forEach {
            it(this, (0 until size).mapTo(mutableListOf()) { it }, mapOf())
        }
    }

    override operator fun set(index: Int, element: E) = list.set(index, element).also {
        if (it !== element) {
            onChange_.forEach {
                it(this, listOf(index), mapOf(Pair(index, element)))
            }
        }
    }

    override fun add(index: Int, element: E) {
        list.add(index, element)

        onChange_.forEach {
            it(this, listOf(), mapOf(Pair(index, element)))
        }
    }

    override fun removeAt(index: Int) = list.removeAt(index).also {
        onChange_.forEach {
            it(this, listOf(index), mapOf())
        }
    }

    private fun <T> batch(block: () -> T): T {
        if (onChange_.isEmpty()) {
            return block()
        } else {
            // TODO: Can this be optimized?
            val old = ArrayList(list)

            return block().also {
                if (old != this) {
                    val removed = mutableListOf<Int   >()
                    val added   = mutableMapOf <Int, E>()

                    old.forEachIndexed { index, item ->
                        if (index >= this.size || this[index] != item) {
                            removed += index
                        }
                    }

                    this.forEachIndexed { index, item ->
                        if (index >= old.size || old[index] != item) {
                            added[index] = item
                        }
                    }

                    onChange_.forEach {
                        it(this, removed, added)
                    }
                }
            }
        }
    }
}