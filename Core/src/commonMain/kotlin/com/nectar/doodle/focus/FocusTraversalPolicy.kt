package com.nectar.doodle.focus

import com.nectar.doodle.core.View


/**
 * This interface defines the way focus moves over the children of a [View].
 */

interface FocusTraversalPolicy {
    enum class TraversalType {
        Forward,
        Backward,
        Upward,
        Downward
    }

    /**
     * Returns the next item in a [View] based on the given [View].
     *
     * @param  within The View
     * @param  from  The current View
     * @return       The next item to gain focus
     */
    fun next(within: View, from: View?): View?

    /**
     * Returns the previous item in a [View] based on the given [View].
     *
     * @param  within The View
     * @param  from  The current View
     * @return       The previous item to gain focus
     */
    fun previous(within: View, from: View?): View?

    /**
     * Returns the last item in a [View] that should receive focus.
     *
     * @param  within The View
     * @return        The last item to gain focus
     */
    fun last(within: View): View?

    /**
     * Returns the first item in a [View] that should receive focus.
     *
     * @param  within The View
     * @return        The first item to gain focus
     */
    fun first(within: View): View?

    /**
     * Returns the item in a [View] that should receive focus by default.
     *
     * @param  within The View
     * @return        The item to gain focus by default
     */
    fun default(within: View): View?
}