package org.jukeboxmc.plugin.perms.util

/**
 * @author Kaooot
 * @version 1.0
 */
data class MySQLData(val host: String, val database: String, val credentials: Credentials) {

    data class Credentials(val username: String, val password: String)
}