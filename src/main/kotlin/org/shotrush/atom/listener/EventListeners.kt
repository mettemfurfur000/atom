package org.shotrush.atom.listener

import org.shotrush.atom.Atom

object EventListeners {
    fun register(atom: Atom) {
        TestListener.register(atom)
        PlayerDataTrackingListener.register(atom)
        PlayerMiningListener.register(atom)
        RecipeUnlockHandler.register(atom)
//        PlayerChatListener.register(this)
    }
}