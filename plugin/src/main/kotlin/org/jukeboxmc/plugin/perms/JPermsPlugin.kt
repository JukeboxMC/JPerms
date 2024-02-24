package org.jukeboxmc.plugin.perms

import org.jukeboxmc.api.config.Config
import org.jukeboxmc.api.config.ConfigType
import org.jukeboxmc.api.plugin.Plugin
import org.jukeboxmc.plugin.perms.api.JPerms
import org.jukeboxmc.plugin.perms.api.manager.GroupManager
import org.jukeboxmc.plugin.perms.api.manager.PermissionManager
import org.jukeboxmc.plugin.perms.command.JPermsCommand
import org.jukeboxmc.plugin.perms.listener.PlayerJoinListener
import org.jukeboxmc.plugin.perms.listener.PlayerQuitListener
import org.jukeboxmc.plugin.perms.manager.GroupManagerImpl
import org.jukeboxmc.plugin.perms.manager.PermissionManagerImpl
import org.jukeboxmc.plugin.perms.repository.GroupsRepository
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository
import org.jukeboxmc.plugin.perms.repository.UsersRepository
import org.jukeboxmc.plugin.perms.util.MySQL
import org.jukeboxmc.plugin.perms.util.MySQLData
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import java.io.File
import java.io.IOException

/**
 * @author Kaooot
 * @version 1.0
 */
class JPermsPlugin : Plugin(), JPerms {

    private lateinit var permissionManager: PermissionManager
    private lateinit var groupManager: GroupManager
    private lateinit var mySQL: MySQL
    private lateinit var redissonClient: RedissonClient

    override fun onEnable() {
        val mainConfig = Config(File(this.dataFolder, "config.yml"), ConfigType.YAML)
        mainConfig.addDefault("mysql.host", "127.0.0.1:3306")
        mainConfig.addDefault("mysql.user", "root")
        mainConfig.addDefault("mysql.password", "")
        mainConfig.addDefault("mysql.database", "jperms")
        mainConfig.addDefault("redis.host", "redis://127.0.0.1:6379")
        mainConfig.addDefault("redis.user", "default")
        mainConfig.addDefault("redis.password", "")
        mainConfig.addDefault("fallback_group", "default")

        try {
            mainConfig.save()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val redissonConfig = org.redisson.config.Config()
        redissonConfig.useSingleServer()
            .setAddress(mainConfig.getString("redis.host"))
            .setUsername(mainConfig.getString("redis.user"))
            .setPassword(mainConfig.getString("redis.password"))

        val host = mainConfig.getString("mysql.host")

        if (!host.contains(":")) {
            this.getLogger().error("Could not find the port in the mysql host string")

            return
        }

        val parts = host.split(":")

        val mySQLData = MySQLData(
            parts[0], mainConfig.getString("mysql.database"),
            MySQLData.Credentials(
                mainConfig.getString("mysql.user"),
                mainConfig.getString("mysql.password")
            )
        )

        this.redissonClient = Redisson.create(redissonConfig)

        val mySQL = MySQL(mySQLData)
        val permissionsRepository = PermissionsRepository(mySQL, redissonClient)
        val groupsRepository = GroupsRepository(mySQL, redissonClient)
        val usersRepository = UsersRepository(mySQL)

        this.permissionManager = PermissionManagerImpl(permissionsRepository)
        this.groupManager = GroupManagerImpl(groupsRepository, mainConfig)

        mySQL.connect().whenComplete { success, throwable ->
            if (throwable != null) {
                throwable.printStackTrace()

                return@whenComplete
            }

            if (success) {
                this.getLogger().info("The mysql connection has been established")

                permissionsRepository.setup()
                groupsRepository.setup()
                usersRepository.setup()
            } else {
                this.getLogger().error("Failed to build the mysql connection")
            }
        }

        this.getServer().getPluginManager().registerListener(PlayerJoinListener(permissionsRepository, usersRepository, groupsRepository, groupManager, mainConfig))
        this.getServer().getPluginManager().registerListener(PlayerQuitListener(permissionsRepository, groupsRepository))

        this.getServer().getCommandManager().registerCommand(JPermsCommand(this.permissionManager, this.groupManager, groupsRepository, usersRepository, permissionsRepository))
    }

    override fun onDisable() {
        this.getLogger().info("Shutting down redisson client")

        this.redissonClient.shutdown()

        this.getLogger().info("Closing mysql connection")

        mySQL.close().whenComplete { success, throwable ->
            if (throwable != null) {
                throwable.printStackTrace()

                return@whenComplete
            }

            if (success) {
                this.getLogger().info("The mysql connection has been closed")
            } else {
                this.getLogger().error("Failed to close the mysql connection")
            }
        }
    }

    override fun permissionManager(): PermissionManager {
        return this.permissionManager
    }

    override fun groupManager(): GroupManager {
        return this.groupManager
    }
}