package com.nectar.doodle.theme

import com.nectar.doodle.core.View
import com.nectar.doodle.theme.Modules.BehaviorResult.Matched
import com.nectar.doodle.theme.Modules.BehaviorResult.NotMatched
import com.nectar.doodle.theme.adhoc.AdhocTheme
import org.kodein.di.DKodein
import org.kodein.di.Kodein.Builder
import org.kodein.di.Kodein.Module
import org.kodein.di.erased.bind
import org.kodein.di.erased.inSet
import org.kodein.di.erased.instance
import org.kodein.di.erased.setBinding
import org.kodein.di.erased.singleton
import org.kodein.di.erasedSet
import kotlin.reflect.KClass

/**
 * Created by Nicholas Eddy on 4/15/20.
 */

class Modules {
    enum class BehaviorResult { Matched, NotMatched }

    interface BehaviorResolver {
        val theme: KClass<out Theme>? get() = null

        operator fun invoke(view: View): BehaviorResult
    }

    companion object {
        val ThemeModule = Module(allowSilentOverride = true, name = "Theme") {
            bind<ThemeManager>        () with singleton { instance<InternalThemeManager>() }
            bind<InternalThemeManager>() with singleton { ThemeManagerImpl(instance()) }
        }

        val AdhocThemeModule = Module(name = "AdhocTheme") {
            importOnce(ThemeModule, allowOverride = true)

            bind() from setBinding<BehaviorResolver>()

            bind<AdhocTheme>() with singleton { object: AdhocTheme(Instance(erasedSet())) {} }
        }

        inline fun <reified T: View> Builder.bindBehavior(theme: KClass<out Theme>? = null, crossinline block: DKodein.(T) -> Unit) = bindConditionalBehavior<T>(theme) {
            block(this, it)
            Matched
        }

        // TODO: Can this be renamed to bindBehavior in 1.4?
        inline fun <reified T: View> Builder.bindConditionalBehavior(theme: KClass<out Theme>? = null, crossinline block: DKodein.(T) -> BehaviorResult) {
            importOnce(AdhocThemeModule, allowOverride = true)

            bind<BehaviorResolver>().inSet() with singleton {
                object: BehaviorResolver {
                    override val theme = theme

                    override fun invoke(view: View) = when (view) {
                        is T -> block(this@singleton, view)
                        else -> NotMatched
                    }
                }
            }
        }
    }
}