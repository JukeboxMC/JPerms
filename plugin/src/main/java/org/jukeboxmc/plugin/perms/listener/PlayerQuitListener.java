package org.jukeboxmc.plugin.perms.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jukeboxmc.api.event.EventHandler;
import org.jukeboxmc.api.event.Listener;
import org.jukeboxmc.api.event.player.PlayerQuitEvent;
import org.jukeboxmc.plugin.perms.JPermsPlugin;
import org.jukeboxmc.plugin.perms.repository.GroupsRepository;
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository;

/**
 * @author Kaooot
 * @version 1.0
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerQuitListener implements Listener {

    private final PermissionsRepository permissionsRepository;
    private final GroupsRepository groupsRepository;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUUID();

        this.permissionsRepository.clearCache(uuid);
        this.groupsRepository.userGroups().fastRemoveAsync(uuid);
    }
}