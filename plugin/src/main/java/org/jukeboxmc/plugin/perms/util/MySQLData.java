package org.jukeboxmc.plugin.perms.util;

/**
 * @author Kaooot
 * @version 1.0
 */
public record MySQLData(String host, String database, Credentials credentials) {

    public record Credentials(String username, String password) {

    }
}