package org.jukeboxmc.plugin.perms.api.model

/**
 * @author Kaooot
 * @version 1.0
 */
interface Group {

    /**
     * The identifier of this group
     *
     * @return the group id
     */
    fun id(): String

    /**
     * The internal priority of this group
     *
     * @return the group's priority
     */
    fun priority(): Int

    /**
     * The permissions of this group (also contains inherited permissions)
     *
     * @return this group's permissions
     */
    fun permissions(): MutableSet<String>

    /**
     * The name tag of this group
     *
     * @return name tag
     */
    fun nameTag(): String

    /**
     * The chat format of this group
     *
     * @return chat format
     */
    fun chatFormat(): String

    /**
     * The identifier of the parent of this group
     *
     * @return parent if available, otherwise null
     */
    fun parentId(): String
}