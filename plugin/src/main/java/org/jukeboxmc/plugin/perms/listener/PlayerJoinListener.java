package org.jukeboxmc.plugin.perms.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jukeboxmc.api.JukeboxMC;
import org.jukeboxmc.api.config.Config;
import org.jukeboxmc.api.event.EventHandler;
import org.jukeboxmc.api.event.Listener;
import org.jukeboxmc.api.event.player.PlayerJoinEvent;
import org.jukeboxmc.api.logger.Logger;
import org.jukeboxmc.plugin.perms.api.manager.GroupManager;
import org.jukeboxmc.plugin.perms.repository.GroupsRepository;
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository;
import org.jukeboxmc.plugin.perms.repository.UsersRepository;

/**
 * @author Kaooot
 * @version 1.0
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerJoinListener implements Listener {

    private final PermissionsRepository permissionsRepository;
    private final UsersRepository usersRepository;
    private final GroupsRepository groupsRepository;
    private final GroupManager groupManager;
    private final Config config;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUUID();
        final String name = event.getPlayer().getName();

        this.permissionsRepository.insert(uuid);

        this.usersRepository.insertOrUpdate(uuid, name);

        final String fallbackGroup = this.config.getString("fallback_group");

        final Logger logger = JukeboxMC.Companion.getServer().getLogger();

        if (this.groupManager.groupById(fallbackGroup).isEmpty()) {
            logger.error("Could not try to assign the fallback group to " + name +
                " because the fallback group does not exist");

            return;
        }

        this.groupsRepository.insertUserGroup(uuid, fallbackGroup)
            .whenComplete((group, throwable) -> {
                if (group == null) {
                    this.groupsRepository.userGroups().fastPutAsync(uuid, fallbackGroup);

                    logger.info("The fallback group has been assigned to " + name +
                        " (" + uuid + ")");

                    this.groupManager.groupById(fallbackGroup)
                        .ifPresent(fallback -> event.getPlayer()
                            .addPermission(fallback.permissions()));
                } else {
                    this.groupsRepository.userGroups().fastPutAsync(uuid, group.id());

                    event.getPlayer().addPermission(group.permissions());

                    logger.info("Synced groups for " + name + " (" + uuid + "): " + group.id());
                }
            });
    }
}