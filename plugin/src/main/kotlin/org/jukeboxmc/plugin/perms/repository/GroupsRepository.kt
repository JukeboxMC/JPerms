package org.jukeboxmc.plugin.perms.repository

import org.jukeboxmc.plugin.perms.api.model.Group
import org.jukeboxmc.plugin.perms.model.GroupImpl
import org.jukeboxmc.plugin.perms.util.MySQL
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Kaooot
 * @version 1.0
 */
class GroupsRepository(private val mySQL: MySQL, private val redissonClient: RedissonClient) {

    private lateinit var groups: RMap<String, GroupImpl>
    private lateinit var userGroups: RMap<UUID, String>

    fun setup() {
        try {
            this.mySQL.prepare("CREATE TABLE IF NOT EXISTS groups(id VARCHAR(100) NOT NULL, priority INT NOT NULL, name_tag TEXT NOT NULL, chat_format TEXT NOT NULL, permissions LONGTEXT NOT NULL, parent_id TEXT, PRIMARY KEY(id))")
                .use { createGroupsTable -> createGroupsTable!!.execute() }
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        try {
            this.mySQL.prepare("CREATE TABLE IF NOT EXISTS user_groups(player VARCHAR(36) NOT NULL, group_id TEXT NOT NULL, assigned_on TIMESTAMP NOT NULL, active TINYINT NOT NULL)")
                .use { createUserGroupsTable -> createUserGroupsTable!!.execute() }
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        this.groups = this.redissonClient.getMap("jperms:groups")
        this.userGroups = this.redissonClient.getMap("jperms:user-groups")

        this.mySQL.prepareAsync("SELECT * FROM groups").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    val resultSet = statement.executeQuery()
                    val groups: MutableMap<String, GroupImpl> = HashMap()

                    while (resultSet.next()) {
                        val id = resultSet.getString("id")
                        val priority = resultSet.getInt("priority")
                        val nameTag = resultSet.getString("name_tag")
                        val chatFormat = resultSet.getString("chat_format")
                        var permissions: MutableSet<String>
                        val s = resultSet.getString("permissions")

                        if (s.contains(",")) {
                            permissions = HashSet(s.split(", "))
                        } else {
                            permissions = HashSet()
                            if (s.isNotEmpty()) {
                                permissions.add(s)
                            }
                        }

                        val parentId = resultSet.getString("parent_id")

                        val group = GroupImpl(id, priority)
                        group.nameTag(nameTag)
                        group.chatFormat(chatFormat)
                        group.permissions(permissions)
                        group.parentId(parentId)

                        this@GroupsRepository.groups.fastPutAsync(id, group)

                        groups[id] = group
                    }

                    for (group in groups.values) {
                        this.inheritPerms(groups, group)
                    }

                    resultSet.close()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun insertGroup(group: Group): Boolean {
        val b = !this.groups.containsKey(group.id())

        if (b) {
            this.inheritPerms(groups, GroupImpl.ofApiModel(group))

            this.groups.fastPutAsync(group.id(), GroupImpl.ofApiModel(group))

            this.mySQL.prepareAsync("INSERT INTO groups(id, priority, name_tag, chat_format, permissions, parent_id) VALUES(?,?,?,?,?,?)").whenComplete { statement: PreparedStatement, _: Throwable? ->
                try {
                    statement.use {
                        statement.setString(1, group.id())
                        statement.setInt(2, group.priority())
                        statement.setString(3, group.nameTag())
                        statement.setString(4, group.chatFormat())
                        statement.setString(5, java.lang.String.join(", ", group.permissions()))
                        statement.setString(6, group.parentId())
                        statement.execute()
                    }
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        }

        return b
    }

    fun selectGroup(id: String): GroupImpl? {
        return this.groups.getOrDefault(id, null)
    }

    fun selectGroups(): MutableSet<GroupImpl> {
        return java.util.Set.copyOf(groups.values)
    }

    fun updateGroupNameTag(id: String, nameTag: String) {
        val group: GroupImpl = this.groups[id]!!
        group.nameTag(nameTag)

        this.groups.fastPutAsync(id, group)

        this.mySQL.prepareAsync("UPDATE groups SET name_tag = ? WHERE id = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, nameTag)
                    statement.setString(2, id)
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun updateGroupChatFormat(id: String, chatFormat: String) {
        val group: GroupImpl = this.groups[id]!!
        group.chatFormat(chatFormat)

        this.groups.fastPutAsync(id, group)

        this.mySQL.prepareAsync("UPDATE groups SET chat_format = ? WHERE id = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, chatFormat)
                    statement.setString(2, id)
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun updateGroupPerms(id: String, permissions: Set<String>) {
        val group = groups[id]!!
        group.permissions().clear()
        group.permissions().addAll(permissions)

        this.groups.fastPutAsync(id, group)

        this.mySQL.prepareAsync("UPDATE groups SET permissions = ? WHERE id = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, java.lang.String.join(", ", permissions))
                    statement.setString(2, id)
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun updateGroupParent(id: String, parentId: String) {
        val group: GroupImpl = this.groups[id]!!
        group.parentId(parentId)

        this.groups.fastPutAsync(id, group)

        this.mySQL.prepareAsync("UPDATE groups SET parent_id = ? WHERE id = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, parentId)
                    statement.setString(2, id)
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun deleteGroup(id: String) {
        this.groups.fastRemoveAsync(id)

        this.mySQL.prepareAsync("DELETE FROM groups WHERE id = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, id)
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun insertUserGroup(player: UUID, id: String): CompletableFuture<Group?> {
        return this.selectUserGroup(player).thenApply { group: Group? ->
            if (group != null && group.id().equals(id, ignoreCase = true)) {
                return@thenApply group
            }

            this.addUserGroup(player, id)

            null
        }
    }

    fun selectUserGroup(player: UUID): CompletableFuture<Group?> {
        return this.mySQL.prepareAsync("SELECT group_id FROM user_groups WHERE player = ? AND active = ?").thenApply { statement: PreparedStatement ->
            try {
                statement.use {
                    statement.setString(1, player.toString())
                    statement.setInt(2, 1)

                    val resultSet = statement.executeQuery()
                    val map: MutableMap<String, GroupImpl?> = HashMap()

                    while (resultSet.next()) {
                        val id = resultSet.getString("group_id")

                        map[id] = this.groups.getOrDefault(id, null)
                    }


                    resultSet.close()
                    if (map.isEmpty()) {
                        return@thenApply null
                    }

                    val id = map.entries.stream()
                        .sorted { o1, o2 -> (o1.value?.priority() ?: -1).compareTo(o2.value?.priority() ?: -1) }
                        .map { it.key }
                        .toList()[0]

                    return@thenApply this.groups.getOrDefault(id, null)
                }
            } catch (e: SQLException) {
                e.printStackTrace()

                return@thenApply null
            }
        }
    }

    fun activeUserGroups(player: UUID): CompletableFuture<Set<Group>> {
        return this.mySQL.prepareAsync("SELECT group_id FROM user_groups WHERE player = ? AND active = ?").thenApply { statement: PreparedStatement ->
            try {
                statement.use {
                    statement.setString(1, player.toString())
                    statement.setInt(2, 1)

                    val resultSet = statement.executeQuery()
                    val groups: MutableSet<Group> = HashSet()

                    while (resultSet.next()) {
                        val group = this.groups.getOrDefault(resultSet.getString("group_id"), null) ?: continue

                        groups.add(group)
                    }

                    resultSet.close()

                    return@thenApply groups
                }
            } catch (e: SQLException) {
                e.printStackTrace()

                return@thenApply null
            }
        }
    }

    fun addUserGroup(player: UUID, group: String) {
        this.mySQL.prepareAsync("INSERT INTO user_groups(player, group_id, assigned_on, active) VALUES(?,?,?,?)").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, player.toString())
                    statement.setString(2, group)
                    statement.setTimestamp(3, Timestamp.from(Instant.now()))
                    statement.setInt(4, 1)
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun removeUserGroup(player: UUID, group: String?) {
        this.mySQL.prepareAsync("UPDATE user_groups SET active = ? WHERE player = ? AND group_id = ? AND active = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setInt(1, 0)
                    statement.setString(2, player.toString())
                    statement.setString(3, group)
                    statement.setInt(4, 1)
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun userGroups() = this.userGroups

    private fun inheritPerms(groups: Map<String, GroupImpl>, group: GroupImpl) {
        val parent = groups[group.parentId()] ?: return
        var next = parent

        while (next.parentId().isNotEmpty()) {
            val grandParent = groups[next.parentId()] ?: break

            parent.inheritedPermissions().addAll(grandParent.permissions())

            next = grandParent
        }

        group.inheritedPermissions().addAll(parent.permissions())
        group.inheritedPermissions().addAll(parent.inheritedPermissions())
    }
}