package org.civlabs.atom.core

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin

abstract class CoreAtom : SuspendingJavaPlugin() {
    companion object {
        lateinit var instance: CoreAtom
    }

    override fun onLoad() {
        instance = this
        super.onLoad()
    }
}