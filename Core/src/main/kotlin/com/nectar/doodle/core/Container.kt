package com.nectar.doodle.core

import com.nectar.doodle.geometry.Point


open class Container: Gizmo(), Iterable<Gizmo> {
    init {
        focusable = false
    }

    override fun iterator() = children.iterator()

    public override var insets
        get(   ) = super.insets
        set(new) { super.insets = new }

    public override var layout
        get(   ) = super.layout
        set(new) { super.layout = new }

    public override var isFocusCycleRoot
        get(   ) = super.isFocusCycleRoot
        set(new) { super.isFocusCycleRoot = new }

    public override val children = super.children

    public override fun setZIndex(of: Gizmo, to: Int) = super.setZIndex(of, to)

    public override fun isAncestor(of: Gizmo) = super.isAncestor(of)

    public override fun child(at: Point) = super.child(at)
}