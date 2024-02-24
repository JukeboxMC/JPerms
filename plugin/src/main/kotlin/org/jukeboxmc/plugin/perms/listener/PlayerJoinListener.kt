package org.jukeboxmc.plugin.perms.listener

import org.jukeboxmc.api.JukeboxMC
import org.jukeboxmc.api.config.Config
import org.jukeboxmc.api.event.EventHandler
import org.jukeboxmc.api.event.Listener
import org.jukeboxmc.api.event.player.PlayerJoinEvent
import org.jukeboxmc.plugin.perms.api.manager.GroupManager
import org.jukeboxmc.plugin.perms.api.model.Group
import org.jukeboxmc.plugin.perms.repository.GroupsRepository
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository
import org.jukeboxmc.plugin.perms.repository.UsersRepository

/**
 * @author Kaooot
 * @version 1.0
 */
class PlayerJoinListener(
    private val permissionsRepository: PermissionsRepository,
    private val usersRepository: UsersRepository,
    private val groupsRepository: GroupsRepository,
    private val groupManager: GroupManager,
    private val config: Config
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val uuid = event.getPlayer().getUUID()
        val name = event.getPlayer().getName()

        this.permissionsRepository.insert(uuid)
        this.usersRepository.insertOrUpdate(uuid, name)

        val fallbackGroup = config.getString("fallback_group")
        val logger = JukeboxMC.getServer().getLogger()

        if (this.groupManager.groupById(fallbackGroup).isEmpty) {
            logger.error("Could not try to assign the fallback group to $name because the fallback group does not exist")

            return
        }

        this.groupsRepository.insertUserGroup(uuid, fallbackGroup).whenComplete { group: Group?, _: Throwable? ->
            if (group == null) {
                this.groupsRepository.userGroups().fastPutAsync(uuid, fallbackGroup)

                logger.info("The fallback group has been assigned to $name ($uuid)")

                this.groupManager.groupById(fallbackGroup).ifPresent { fallback: Group -> event.getPlayer().addPermission(fallback.permissions()) }
            } else {
                this.groupsRepository.userGroups().fastPutAsync(uuid, group.id())

                event.getPlayer().addPermission(group.permissions())

                logger.info("Synced groups for $name ($uuid): ${group.id()}")
            }
        }
    }
}