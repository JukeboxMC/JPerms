package org.jukeboxmc.plugin.perms.listener

import org.jukeboxmc.api.event.EventHandler
import org.jukeboxmc.api.event.Listener
import org.jukeboxmc.api.event.player.PlayerQuitEvent
import org.jukeboxmc.plugin.perms.repository.GroupsRepository
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository

/**
 * @author Kaooot
 * @version 1.0
 */
class PlayerQuitListener(private val permissionsRepository: PermissionsRepository, private val groupsRepository: GroupsRepository) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val uuid = event.getPlayer().getUUID()

        this.permissionsRepository.clearCache(uuid)
        this.groupsRepository.userGroups().fastRemoveAsync(uuid)
    }
}