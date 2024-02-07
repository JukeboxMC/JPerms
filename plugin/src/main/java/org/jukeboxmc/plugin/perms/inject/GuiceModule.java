package org.jukeboxmc.plugin.perms.inject;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;
import org.jukeboxmc.plugin.perms.JPermsPlugin;
import org.jukeboxmc.plugin.perms.api.manager.GroupManager;
import org.jukeboxmc.plugin.perms.api.manager.PermissionManager;
import org.jukeboxmc.plugin.perms.command.JPermsCommand;
import org.jukeboxmc.plugin.perms.listener.PlayerJoinListener;
import org.jukeboxmc.plugin.perms.listener.PlayerQuitListener;
import org.jukeboxmc.plugin.perms.manager.GroupManagerImpl;
import org.jukeboxmc.plugin.perms.manager.PermissionManagerImpl;
import org.jukeboxmc.plugin.perms.repository.GroupsRepository;
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository;
import org.jukeboxmc.plugin.perms.repository.UsersRepository;
import org.jukeboxmc.plugin.perms.util.MySQL;

/**
 * @author Kaooot
 * @version 1.0
 */
@RequiredArgsConstructor
public class GuiceModule extends AbstractModule {

    private final JPermsPlugin plugin;

    @Override
    protected void configure() {
        final MySQL mySQL = new MySQL(this.plugin.mySQLData());
        final PermissionsRepository permissionsRepository =
            new PermissionsRepository(mySQL, this.plugin.redissonClient());
        final GroupsRepository groupsRepository =
            new GroupsRepository(mySQL, this.plugin.redissonClient());
        final PermissionManager permissionManager =
            new PermissionManagerImpl(permissionsRepository);
        final GroupManager groupManager =
            new GroupManagerImpl(groupsRepository, this.plugin.mainConfig());
        final UsersRepository usersRepository = new UsersRepository(mySQL);

        this.bind(MySQL.class).toInstance(mySQL);
        this.bind(PermissionsRepository.class).toInstance(permissionsRepository);
        this.bind(GroupsRepository.class).toInstance(groupsRepository);
        this.bind(UsersRepository.class).toInstance(new UsersRepository(mySQL));
        this.bind(PermissionManager.class).toInstance(permissionManager);
        this.bind(GroupManager.class).toInstance(groupManager);
        this.bind(PlayerJoinListener.class)
            .toInstance(new PlayerJoinListener(permissionsRepository, usersRepository,
                groupsRepository, groupManager, this.plugin.mainConfig()));
        this.bind(PlayerQuitListener.class)
            .toInstance(new PlayerQuitListener(permissionsRepository, groupsRepository));
        this.bind(JPermsCommand.class).toInstance(new JPermsCommand(permissionManager,
            groupManager, groupsRepository, usersRepository, permissionsRepository));
    }
}