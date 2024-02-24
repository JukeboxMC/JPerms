package org.jukeboxmc.plugin.perms.api.manager

import org.jukeboxmc.plugin.perms.api.model.Group
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Kaooot
 * @version 1.0
 */
interface GroupManager {

    /**
     * Creates a new [org.jukeboxmc.plugin.perms.api.model.Group]
     *
     * @param group which should be created
     *
     * @return whether the operation has been successfully completed
     */
    fun createGroup(group: Group): Boolean

    /**
     * Retrieves a group by its identifier
     *
     * @param identifier used to obtain the group
     *
     * @return an empty optional when the group could not be found, otherwise the optional is
     * holding the group object
     */
    fun groupById(identifier: String): Optional<Group>

    /**
     * Retrieves all groups
     *
     * @return a set of all available groups
     */
    fun groups(): Set<Group>

    /**
     * Deletes a [org.jukeboxmc.plugin.perms.api.model.Group] which is identified by its
     * identifier
     *
     * @param identifier used to identify the group that should be deleted
     */
    fun deleteGroup(identifier: String)

    /**
     * Adds a group to the given user
     *
     * @param uuid  the unique id of the user
     * @param group the group to add
     *
     * @return whether the operation has been successfully completed
     */
    fun addGroup(uuid: UUID, group: String): CompletableFuture<Boolean>

    /**
     * Retrieves the group of the given user asynchronously
     *
     * @param uuid the unique id of the user
     *
     * @return a fresh [org.jukeboxmc.plugin.perms.api.model.Group]
     */
    fun group(uuid: UUID): CompletableFuture<Group?>

    /**
     * Retrieves all active groups of the given user
     *
     * @param uuid the unique id of the user
     *
     * @return a fresh set of groups
     */
    fun activeGroups(uuid: UUID): CompletableFuture<Set<Group>>

    /**
     * Retrieves the group of the given user. The cached group should only be used for online
     * users.
     *
     * @param uuid the unique id of the user
     *
     * @return a fresh [org.jukeboxmc.plugin.perms.api.model.Group]
     */
    fun cachedGroup(uuid: UUID): Group

    /**
     * Removes a group from the given user
     *
     * @param uuid the unique id of the user
     * @param group the group to remove
     *
     * @return whether the operation has been successfully completed
     */
    fun removeGroup(uuid: UUID, group: String): CompletableFuture<Boolean>
}