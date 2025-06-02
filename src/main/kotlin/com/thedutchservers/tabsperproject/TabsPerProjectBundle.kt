package com.thedutchservers.tabsperproject

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.TabsPerProjectBundle"

object TabsPerProjectBundle : DynamicBundle(BUNDLE) {
    @Suppress("unused")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = 
        getMessage(key, *params)
}
