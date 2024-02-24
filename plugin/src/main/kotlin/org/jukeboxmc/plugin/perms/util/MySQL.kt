package org.jukeboxmc.plugin.perms.util

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.concurrent.CompletableFuture

/**
 * @author Kaooot
 * @version 1.0
 */
class MySQL(private val data: MySQLData) {

    private var connection: Connection? = null

    fun connect(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver")

                this.connection = DriverManager.getConnection(
                    "jdbc:mysql://" + this.data.host + "/" + this.data.database +
                            "?useSSL=true&autoReconnect=true", this.data.credentials.username, this.data.credentials.password
                )

                return@supplyAsync this.connection != null
            } catch (e: SQLException) {
                e.printStackTrace()

                return@supplyAsync false
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()

                return@supplyAsync false
            }
        }
    }

    fun prepare(query: String): PreparedStatement? {
        return try {
            this.connection?.prepareStatement(query)
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }

    fun prepareAsync(query: String): CompletableFuture<PreparedStatement> {
        return CompletableFuture.supplyAsync { this.prepare(query) }
    }

    fun close(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                if (!this.connection?.isClosed!!) {
                    this.connection?.close()

                    return@supplyAsync true
                }

                return@supplyAsync false
            } catch (e: SQLException) {
                e.printStackTrace()

                return@supplyAsync false
            }
        }
    }
}