package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import org.shotrush.atom.Atom
import org.shotrush.atom.systems.reinforce.ReinforcementSystem

object EventListeners {
    fun register(atom: Atom) {
        MoldListener.register(atom)
        PlayerDataTrackingListener.register(atom)
        PlayerMiningListener.register(atom)
        RecipeUnlockHandler.register(atom)
//        PlayerChatListener.register(this)
        atom.registerAtomListener(ReinforcementSystem)
    }

    fun Atom.registerAtomListener(listener: AtomListener) {
        server.pluginManager.registerSuspendingEvents(listener, this, listener.eventDefs)
    }
}