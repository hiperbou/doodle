package com.nectar.doodle.controls.menu

import com.nectar.doodle.controls.SingleItemSelectionModel
import com.nectar.doodle.controls.list.MutableList
import com.nectar.doodle.controls.list.MutableListModel
import com.nectar.doodle.core.Display
import com.nectar.doodle.core.Gizmo
import com.nectar.doodle.geometry.Point
import com.nectar.doodle.geometry.Size
import com.nectar.doodle.utils.PropertyObservers
import com.nectar.doodle.utils.PropertyObserversImpl


/**
 * Created by Nicholas Eddy on 4/30/18.
 */
//class PopupMenu(private val display: Display): Gizmo(), MenuItem {
//
//    override val subMenus     = mutableListOf<MenuItem>()
//    override var parentMenu   = null as MenuItem?
//    override var menuSelected = false
//        set(new) {
//            if (field != new) {
//                (selectedChanged as PropertyObserversImpl)(old = field, new = new)
//
//                field = new
//            }
//        }
//
//    override val selectedChanged: PropertyObservers<MenuItem, Boolean> by lazy { PropertyObserversImpl<MenuItem, Boolean>(this) }
//
//    init {
//        visible = false
//        layout  = ListLayout()
//    }
//
//    fun add(menu: Menu) {
//        if (menu.parentMenu !== this) {
//            subMenus += menu
//
//            menu.parentMenu = this
//
//            super.children += menu
//        }
//    }
//
//    fun show(owner: Gizmo, at: Point) {
//        visible = true
//
//        // FIXME: IMPLEMENT
//        position = owner.toAbsolute(at)
//
//        display.children += this
//    }
//}


class PopupMenu(private val display: Display): MutableList<MenuItem>(MutableListModel(), SingleItemSelectionModel()), MenuItem {

    override val subMenus get() = model.iterator()
    override var parentMenu     = null as MenuItem?
    override var menuSelected   = false
        set(new) {
            if (field != new) {
                (selectedChanged as PropertyObserversImpl)(old = field, new = new)

                field = new
            }
        }

    override val selectedChanged: PropertyObservers<MenuItem, Boolean> by lazy { PropertyObserversImpl<MenuItem, Boolean>(this) }

    init {
        visible = false
    }

    fun add(menu: Menu) {
        if (menu.parentMenu !== this) {
            size = Size(100.0, 4.0) // FIXME: remove

            model.add(menu)

            menu.parentMenu = this
        }
    }

    fun show(owner: Gizmo, at: Point) {
        visible = true

        // FIXME: IMPLEMENT
        position = owner.toAbsolute(at)

        display.children += this
    }
}