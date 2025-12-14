package org.shotrush.atom.listener

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

        RoomSystem.register()
        ReinforcementSystem.register()
        BlockBreakSystem.register()
    }
}