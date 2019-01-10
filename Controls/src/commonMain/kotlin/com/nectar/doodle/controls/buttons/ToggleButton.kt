package com.nectar.doodle.controls.buttons

import com.nectar.doodle.core.Icon
import com.nectar.doodle.utils.PropertyObservers
import com.nectar.doodle.utils.PropertyObserversImpl


private class ToggleButtonModel: ButtonModelImpl() {

    override var selected
        get(   ) = super.selected
        set(new) {
            if (super.selected != new) {
                buttonGroup?.let {
                    callCount++
                    it.setSelected(this, new)
                    callCount--

//                    TODO: make more efficient, at this point the ButtonGroup has reset the selection, so no need to tell listeners
//                    if (callCount > 0) {
//                        super.selected = new
//                        return
//                    }
                }

                super.selected = new
            }
        }

    private var callCount = 0

    override var pressed
        get(   ) = super.pressed
        set(new) {
            if (super.pressed != new) {
                super.pressed = new

                if (!pressed && armed) {
                    selected = !selected
                }
            }
        }
}

// FIXME: It is possible to assign a non-ToggleButtonModel to these types.  Is that desirable?
@Suppress("PrivatePropertyName")
open class ToggleButton(text: String = "", icon: Icon<Button>? = null): PushButton(text, icon, ToggleButtonModel()) {
    constructor(icon: Icon<Button>): this("", icon)

    init {
        super.model.apply {
            selectedChanged += selectedChanged_
        }
    }

    override var model = super.model
        set(new) {
            field.apply {
                selectedChanged -= selectedChanged_
            }

            super.model = new

            field.apply {
                selectedChanged += selectedChanged_
            }
        }

    val selectedChanged: PropertyObservers<ToggleButton, Boolean> by lazy { PropertyObserversImpl<ToggleButton, Boolean>(this) }

    private val selectedChanged_ = ::selectedChangedFun
    private fun selectedChangedFun(@Suppress("UNUSED_PARAMETER") model: ButtonModel, old: Boolean, new: Boolean) = (selectedChanged as PropertyObserversImpl)(old, new)
}