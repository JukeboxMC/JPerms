package org.jukeboxmc.plugin.perms;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.File;
import java.io.IOException;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jukeboxmc.api.config.Config;
import org.jukeboxmc.api.config.ConfigType;
import org.jukeboxmc.api.plugin.Plugin;
import org.jukeboxmc.plugin.perms.api.JPerms;
import org.jukeboxmc.plugin.perms.api.manager.GroupManager;
import org.jukeboxmc.plugin.perms.api.manager.PermissionManager;
import org.jukeboxmc.plugin.perms.command.JPermsCommand;
import org.jukeboxmc.plugin.perms.inject.GuiceModule;
import org.jukeboxmc.plugin.perms.listener.PlayerJoinListener;
import org.jukeboxmc.plugin.perms.listener.PlayerQuitListener;
import org.jukeboxmc.plugin.perms.repository.GroupsRepository;
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository;
import org.jukeboxmc.plugin.perms.repository.UsersRepository;
import org.jukeboxmc.plugin.perms.util.MySQL;
import org.jukeboxmc.plugin.perms.util.MySQLData;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

/**
 * @author Kaooot
 * @version 1.0
 */
@Accessors(fluent = true)
public class JPermsPlugin extends Plugin implements JPerms {

    private Injector injector;

    @Getter
    private RedissonClient redissonClient;

    @Getter
    private Config mainConfig;

    @Getter
    private MySQLData mySQLData;

    @Override
    public void onEnable() {
        this.mainConfig = new Config(new File(this.getDataFolder(), "config.yml"), ConfigType.YAML);
        this.mainConfig.addDefault("mysql.host", "127.0.0.1:3306");
        this.mainConfig.addDefault("mysql.user", "root");
        this.mainConfig.addDefault("mysql.password", "");
        this.mainConfig.addDefault("mysql.database", "jperms");
        this.mainConfig.addDefault("redis.host", "redis://127.0.0.1:6379");
        this.mainConfig.addDefault("redis.user", "default");
        this.mainConfig.addDefault("redis.password", "");
        this.mainConfig.addDefault("fallback_group", "default");

        try {
            this.mainConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final org.redisson.config.Config redissonConfig = new org.redisson.config.Config();
        redissonConfig.useSingleServer()
            .setAddress(this.mainConfig.getString("redis.host"))
            .setUsername(this.mainConfig.getString("redis.user"))
            .setPassword(this.mainConfig.getString("redis.password"));

        final String host = this.mainConfig.getString("mysql.host");

        if (!host.contains(":")) {
            this.getLogger().error("Could not find the port in the mysql host string");

            return;
        }

        final String[] parts = host.split(":");

        this.mySQLData = new MySQLData(parts[0], this.mainConfig.getString("mysql.database"),
            new MySQLData.Credentials(this.mainConfig.getString("mysql.user"),
                this.mainConfig.getString("mysql.password")));

        this.redissonClient = Redisson.create(redissonConfig);

        this.injector = Guice.createInjector(new GuiceModule(this));

        this.instance(MySQL.class).connect().whenComplete((success, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();

                return;
            }

            if (success) {
                JPermsPlugin.this.getLogger().info("The mysql connection has been established");

                this.instance(PermissionsRepository.class).setup();
                this.instance(GroupsRepository.class).setup();
                this.instance(UsersRepository.class).setup();
            } else {
                JPermsPlugin.this.getLogger().error("Failed to build the mysql connection");
            }
        });

        this.getServer().getPluginManager()
            .registerListener(this.instance(PlayerJoinListener.class));
        this.getServer().getPluginManager()
            .registerListener(this.instance(PlayerQuitListener.class));

        this.getServer().getCommandManager().registerCommand(this.instance(JPermsCommand.class));
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Shutting down redisson client");

        this.redissonClient.shutdown();

        this.getLogger().info("Closing mysql connection");

        this.instance(MySQL.class).close().whenComplete((success, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();

                return;
            }

            if (success) {
                JPermsPlugin.this.getLogger().info("The mysql connection has been closed");
            } else {
                JPermsPlugin.this.getLogger().error("Failed to close the mysql connection");
            }
        });
    }

    @Override
    public PermissionManager permissionManager() {
        return this.instance(PermissionManager.class);
    }

    @Override
    public GroupManager groupManager() {
        return this.instance(GroupManager.class);
    }

    public <T> T instance(Class<T> clazz) {
        return this.injector.getInstance(clazz);
    }
}