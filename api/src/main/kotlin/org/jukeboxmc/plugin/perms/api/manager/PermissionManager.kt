package org.jukeboxmc.plugin.perms.api.manager

import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Kaooot
 * @version 1.0
 */
interface PermissionManager {

    /**
     * Adds the given permission to the given user
     *
     * @param uuid       the unique id of the user
     * @param permission the permission to add
     *
     * @return whether the operation has been successfully completed
     */
    fun addPermission(uuid: UUID, permission: String): CompletableFuture<Boolean>

    /**
     * Proofs whether the given user has the given permission asynchronously
     *
     * @param uuid       the unique id of the user
     * @param permission the permission to check
     *
     * @return true, when the permission is in the set, otherwise false
     */
    fun hasPermission(uuid: UUID, permission: String): CompletableFuture<Boolean>

    /**
     * Proofs whether the given user has the given permission. The cached permission check should
     * only be executed on online users.
     *
     * @param uuid       the unique id of the user
     * @param permission the permission to check
     *
     * @return true, when the permission is in the set, otherwise false
     */
    fun hasCachedPermission(uuid: UUID, permission: String): Boolean

    /**
     * Removes the given permission from the given user
     *
     * @param uuid       the unique id of the user
     * @param permission the permission to remove
     *
     * @return whether the operation has been successfully completed
     */
    fun removePermission(uuid: UUID, permission: String): CompletableFuture<Boolean>
}