package org.jukeboxmc.plugin.perms.api.model;

import java.util.Set;

/**
 * @author Kaooot
 * @version 1.0
 */
public interface Group {

    /**
     * The identifier of this group
     *
     * @return the group id
     */
    String id();

    /**
     * The internal priority of this group
     *
     * @return the group's priority
     */
    int priority();

    /**
     * The permissions of this group (also contains inherited permissions)
     *
     * @return this group's permissions
     */
    Set<String> permissions();

    /**
     * The name tag of this group
     *
     * @return name tag
     */
    String nameTag();

    /**
     * The chat format of this group
     *
     * @return chat format
     */
    String chatFormat();

    /**
     * The identifier of the parent of this group
     *
     * @return parent if available, otherwise null
     */
    String parentId();
}