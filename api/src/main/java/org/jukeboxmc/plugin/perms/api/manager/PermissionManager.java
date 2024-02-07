package org.jukeboxmc.plugin.perms.api.manager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Kaooot
 * @version 1.0
 */
public interface PermissionManager {

    /**
     * Adds the given permission to the given user
     *
     * @param uuid       the unique id of the user
     * @param permission the permission to add
     *
     * @return whether the operation has been successfully completed
     */
    CompletableFuture<Boolean> addPermission(UUID uuid, String permission);

    /**
     * Proofs whether the given user has the given permission asynchronously
     *
     * @param uuid       the unique id of the user
     * @param permission the permission to check
     *
     * @return true, when the permission is in the set, otherwise false
     */
    CompletableFuture<Boolean> hasPermission(UUID uuid, String permission);

    /**
     * Proofs whether the given user has the given permission. The cached permission check should
     * only be executed on online users.
     *
     * @param uuid       the unique id of the user
     * @param permission the permission to check
     *
     * @return true, when the permission is in the set, otherwise false
     */
    boolean hasCachedPermission(UUID uuid, String permission);

    /**
     * Removes the given permission from the given user
     *
     * @param uuid       the unique id of the user
     * @param permission the permission to remove
     *
     * @return whether the operation has been successfully completed
     */
    CompletableFuture<Boolean> removePermission(UUID uuid, String permission);
}