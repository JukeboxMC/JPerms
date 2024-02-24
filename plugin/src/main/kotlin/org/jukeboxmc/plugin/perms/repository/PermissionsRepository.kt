package org.jukeboxmc.plugin.perms.repository

import org.jukeboxmc.plugin.perms.util.MySQL
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Kaooot
 * @version 1.0
 */
class PermissionsRepository(private val mySQL: MySQL, private val redissonClient: RedissonClient) {

    private lateinit var userPermissions: RMap<UUID, Set<String>>

    fun setup() {
        try {
            this.mySQL.prepare("CREATE TABLE IF NOT EXISTS permissions(player VARCHAR(36) NOT NULL, permissions LONGTEXT NOT NULL, PRIMARY KEY(player))").use { createTable -> createTable!!.execute() }
        } catch (e: SQLException) {
            e.printStackTrace()
        }

        this.userPermissions = this.redissonClient.getMap("jperms:user-perms")
    }

    fun insert(player: UUID) {
        this.select(player).whenComplete { permissions: Set<String>?, _: Throwable? ->
            if (permissions == null) {
                this.mySQL.prepareAsync("INSERT INTO permissions(player, permissions) VALUES(?,?)").whenComplete { statement: PreparedStatement, _: Throwable? ->
                    try {
                        statement.use {
                            statement.setString(1, player.toString())
                            statement.setString(2, "")
                            statement.execute()
                        }
                    } catch (e: SQLException) {
                        e.printStackTrace()
                    }
                }

                this.userPermissions.fastPutAsync(player, HashSet())
            } else {

                this.userPermissions.fastPutAsync(player, permissions)
            }
        }
    }

    fun update(player: UUID, permissions: MutableSet<String>) {
        this.userPermissions.fastPutAsync(player, permissions)

        this.mySQL.prepareAsync("UPDATE permissions SET permissions = ? WHERE player = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, java.lang.String.join(", ", permissions))
                    statement.setString(2, player.toString())
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun select(player: UUID): CompletableFuture<MutableSet<String>?> {
        return this.mySQL.prepareAsync("SELECT permissions FROM permissions WHERE player = ?").thenApply { statement: PreparedStatement ->
            try {
                statement.use {
                    statement.setString(1, player.toString())

                    val resultSet = statement.executeQuery()
                    val permissions: MutableSet<String>?

                    if (resultSet.next()) {
                        val s = resultSet.getString("permissions")

                        if (s.contains(",")) {
                            permissions = HashSet(s.split(", "))
                        } else {
                            permissions = HashSet()

                            if (s.isNotEmpty()) {
                                permissions.add(s)
                            }
                        }

                    } else {
                        permissions = null
                    }

                    resultSet.close()

                    return@thenApply permissions
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                return@thenApply null
            }
        }
    }

    fun delete(player: UUID) {
        this.clearCache(player)

        this.mySQL.prepareAsync("DELETE FROM permissions WHERE player = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, player.toString())
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun clearCache(player: UUID) {
        this.userPermissions.fastRemoveAsync(player)
    }

    fun cachedPermissions(player: UUID): Set<String>? {
        return this.userPermissions.getOrDefault(player, HashSet())
    }

    fun hasCachedPerm(player: UUID, perm: String): Boolean {
        return this.userPermissions.getOrDefault(player, HashSet()).contains(perm)
    }
}