package org.jukeboxmc.plugin.perms.api.manager;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jukeboxmc.plugin.perms.api.model.Group;

/**
 * @author Kaooot
 * @version 1.0
 */
public interface GroupManager {

    /**
     * Creates a new {@link org.jukeboxmc.plugin.perms.api.model.Group}
     *
     * @param group which should be created
     *
     * @return whether the operation has been successfully completed
     */
    boolean createGroup(Group group);

    /**
     * Retrieves a group by its identifier
     *
     * @param identifier used to obtain the group
     *
     * @return an empty optional when the group could not be found, otherwise the optional is
     * holding the group object
     */
    Optional<Group> groupById(String identifier);

    /**
     * Retrieves all groups
     *
     * @return a set of all available groups
     */
    Set<Group> groups();

    /**
     * Deletes a {@link org.jukeboxmc.plugin.perms.api.model.Group} which is identified by its
     * identifier
     *
     * @param identifier used to identify the group that should be deleted
     */
    void deleteGroup(String identifier);

    /**
     * Adds a group to the given user
     *
     * @param uuid  the unique id of the user
     * @param group the group to add
     *
     * @return whether the operation has been successfully completed
     */
    CompletableFuture<Boolean> addGroup(UUID uuid, String group);

    /**
     * Retrieves the group of the given user asynchronously
     *
     * @param uuid the unique id of the user
     *
     * @return a fresh {@link org.jukeboxmc.plugin.perms.api.model.Group}
     */
    CompletableFuture<Group> group(UUID uuid);

    /**
     * Retrieves all active groups of the given user
     *
     * @param uuid the unique id of the user
     *
     * @return a fresh set of groups
     */
    CompletableFuture<Set<Group>> activeGroups(UUID uuid);

    /**
     * Retrieves the group of the given user. The cached group should only be used for online
     * users.
     *
     * @param uuid the unique id of the user
     *
     * @return a fresh {@link org.jukeboxmc.plugin.perms.api.model.Group}
     */
    Group cachedGroup(UUID uuid);

    /**
     * Removes a group from the given user
     *
     * @param uuid the unique id of the user
     * @param group the group to remove
     *
     * @return whether the operation has been successfully completed
     */
    CompletableFuture<Boolean> removeGroup(UUID uuid, String group);
}