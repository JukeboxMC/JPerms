package org.jukeboxmc.plugin.perms.repository

import org.jukeboxmc.plugin.perms.util.MySQL
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Kaooot
 * @version 1.0
 */
class UsersRepository(private val mySQL: MySQL) {

    fun setup() {
        try {
            this.mySQL.prepare("CREATE TABLE IF NOT EXISTS permission_users(player VARCHAR(36) NOT NULL, name VARCHAR(16) NOT NULL, PRIMARY KEY(player))").use { createTable -> createTable!!.execute() }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun insertOrUpdate(player: UUID, name: String) {
        this.name(player).whenComplete { s: String?, _: Throwable? ->
            if (s == null) {
                this.mySQL.prepareAsync("INSERT INTO permission_users(player, name) VALUES(?,?)").whenComplete { statement: PreparedStatement, _: Throwable? ->
                    try {
                        statement.use {
                            statement.setString(1, player.toString())
                            statement.setString(2, name)
                            statement.execute()
                        }
                    } catch (e: SQLException) {
                        e.printStackTrace()
                    }
                }
            } else if (!s.equals(name, ignoreCase = true)) {
                this.updateName(player, name)
            }
        }
    }

    fun name(player: UUID): CompletableFuture<String?> {
        return this.mySQL.prepareAsync("SELECT name FROM permission_users WHERE player = ?").thenApply { statement: PreparedStatement ->
            try {
                statement.use {
                    statement.setString(1, player.toString())

                    val resultSet = statement.executeQuery()
                    val name: String? = if (resultSet.next()) {
                        resultSet.getString("name")
                    } else {
                        null
                    }

                    resultSet.close()

                    return@thenApply name
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                return@thenApply null
            }
        }
    }

    fun uuid(name: String?): CompletableFuture<UUID?> {
        return this.mySQL.prepareAsync("SELECT player FROM permission_users WHERE name = ?").thenApply { statement: PreparedStatement ->
            try {
                statement.use {
                    statement.setString(1, name)

                    val resultSet = statement.executeQuery()
                    val uuid: UUID? = if (resultSet.next()) {
                        UUID.fromString(resultSet.getString("player"))
                    } else {
                        null
                    }

                    resultSet.close()

                    return@thenApply uuid
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                return@thenApply null
            }
        }
    }

    private fun updateName(player: UUID, name: String?) {
        this.mySQL.prepareAsync("UPDATE permission_users SET name = ? WHERE player = ?").whenComplete { statement: PreparedStatement, _: Throwable? ->
            try {
                statement.use {
                    statement.setString(1, name)
                    statement.setString(2, player.toString())
                    statement.execute()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }
}