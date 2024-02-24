package org.jukeboxmc.plugin.perms.manager

import org.jukeboxmc.api.config.Config
import org.jukeboxmc.plugin.perms.api.manager.GroupManager
import org.jukeboxmc.plugin.perms.api.model.Group
import org.jukeboxmc.plugin.perms.repository.GroupsRepository
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Kaooot
 * @version 1.0
 */
class GroupManagerImpl(private val repository: GroupsRepository, private val config: Config) : GroupManager {

    override fun createGroup(group: Group): Boolean {
        return this.repository.insertGroup(group)
    }

    override fun groupById(identifier: String): Optional<Group> {
        return Optional.ofNullable(this.repository.selectGroup(identifier))
    }

    override fun groups(): Set<Group> {
        return this.repository.selectGroups()
    }

    override fun deleteGroup(identifier: String) {
        this.repository.deleteGroup(identifier)
    }

    override fun addGroup(uuid: UUID, group: String): CompletableFuture<Boolean> {
        return this.group(uuid).thenApply { g: Group? ->
            if (g == null || g.id().equals(group, ignoreCase = true)) {
                return@thenApply false
            }

            this.repository.addUserGroup(uuid, group)

            true
        }
    }

    override fun group(uuid: UUID): CompletableFuture<Group?> {
        return this.repository.selectUserGroup(uuid)
    }

    override fun activeGroups(uuid: UUID): CompletableFuture<Set<Group>> {
        return this.repository.activeUserGroups(uuid)
    }

    override fun cachedGroup(uuid: UUID): Group {
        return this.groupById(this.repository.userGroups()[uuid]!!).orElse(null)
    }

    override fun removeGroup(uuid: UUID, group: String): CompletableFuture<Boolean> {
        return this.activeGroups(uuid).thenApply { groups: Set<Group> ->
            if (group.equals(this.config.getString("fallback_group"), ignoreCase = true)) {
                return@thenApply false
            }

            for (g in groups) {
                if (!g.id().equals(group, ignoreCase = true)) {
                    continue
                }

                this.repository.removeUserGroup(uuid, g.id())

                return@thenApply true
            }

            false
        }
    }
}