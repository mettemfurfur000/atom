package org.shotrush.atom.systems.physics

import org.civlabs.atom.core.listener.AtomListener
import org.civlabs.atom.core.listener.EventClass
import org.civlabs.atom.core.listener.EventRunner

object GravitySystem : AtomListener {
    override val eventDefs = mapOf<EventClass, EventRunner>()
}