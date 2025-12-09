package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import org.shotrush.atom.Atom
import org.shotrush.atom.systems.blockbreak.BlockBreakSystem
import org.shotrush.atom.systems.reinforce.ReinforcementSystem
import org.shotrush.atom.systems.room.RoomSystem

object EventListeners {
    fun register(atom: Atom) {
        MoldListener.register(atom)
        PlayerDataTrackingListener.register(atom)
        PlayerMiningListener.register(atom)
        RecipeUnlockHandler.register(atom)
//        PlayerChatListener.register(this)
        atom.registerAtomListener(ReinforcementSystem)
        atom.registerAtomListener(RoomSystem)
        atom.registerAtomListener(BlockBreakSystem)
    }

    fun Atom.registerAtomListener(listener: AtomListener) {
        server.pluginManager.registerSuspendingEvents(listener, this, listener.eventDefs)
    }
}