package org.jukeboxmc.plugin.perms.manager

import org.jukeboxmc.plugin.perms.api.manager.PermissionManager
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Kaooot
 * @version 1.0
 */
class PermissionManagerImpl(private val repository: PermissionsRepository) : PermissionManager{

    override fun addPermission(uuid: UUID, permission: String): CompletableFuture<Boolean> {
        return this.addOrRemovePerm(uuid, permission, true)
    }

    override fun hasPermission(uuid: UUID, permission: String): CompletableFuture<Boolean> {
        return this.repository.select(uuid).thenApply { permissions: Set<String>? -> permissions?.contains(permission) ?: false }
    }

    override fun hasCachedPermission(uuid: UUID, permission: String): Boolean {
        return this.repository.hasCachedPerm(uuid, permission)
    }

    override fun removePermission(uuid: UUID, permission: String): CompletableFuture<Boolean> {
        return this.addOrRemovePerm(uuid, permission, false)
    }

    private fun addOrRemovePerm(uuid: UUID, permission: String, add: Boolean): CompletableFuture<Boolean> {
        return this.repository.select(uuid).thenApply { permissions: MutableSet<String>? ->
            val success: Boolean = if (add) {
                permissions?.add(permission) ?: false
            } else {
                permissions?.remove(permission) ?: false
            }

            if (permissions != null) {
                this.repository.update(uuid, permissions)
            }

            success
        }
    }
}