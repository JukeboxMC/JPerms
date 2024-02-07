package org.jukeboxmc.plugin.perms.api;

import org.jukeboxmc.plugin.perms.api.manager.GroupManager;
import org.jukeboxmc.plugin.perms.api.manager.PermissionManager;

/**
 * @author Kaooot
 * @version 1.0
 */
public interface JPerms {

    /**
     * Retrieves the permission manager which is responsible for managing the permissions of a
     * certain user
     *
     * @return a fresh {@link org.jukeboxmc.plugin.perms.api.manager.PermissionManager}
     */
    PermissionManager permissionManager();

    /**
     * Retrieves the group manager which is responsible for managing available groups and the groups
     * of a certain user
     *
     * @return a fresh {@link org.jukeboxmc.plugin.perms.api.manager.GroupManager}
     */
    GroupManager groupManager();
}