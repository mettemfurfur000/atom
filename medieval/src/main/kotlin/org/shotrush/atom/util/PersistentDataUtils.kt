package org.shotrush.atom.util

import org.bukkit.persistence.PersistentDataHolder
import org.shotrush.atom.core.data.PersistentData

operator fun PersistentDataHolder.contains(key: String): Boolean {
    return persistentDataContainer.has(PersistentData.key(key))
}