package org.jukeboxmc.plugin.perms.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.jukeboxmc.api.config.Config;
import org.jukeboxmc.plugin.perms.api.manager.GroupManager;
import org.jukeboxmc.plugin.perms.api.model.Group;
import org.jukeboxmc.plugin.perms.repository.GroupsRepository;

/**
 * @author Kaooot
 * @version 1.0
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GroupManagerImpl implements GroupManager {

    private final GroupsRepository repository;
    private final Config config;

    @Override
    public boolean createGroup(Group group) {
        return this.repository.insertGroup(group);
    }

    @Override
    public Optional<Group> groupById(String identifier) {
        return Optional.ofNullable(this.repository.selectGroup(identifier));
    }

    @Override
    public Set<Group> groups() {
        return this.repository.selectGroups();
    }

    @Override
    public void deleteGroup(String identifier) {
        this.repository.deleteGroup(identifier);
    }

    @Override
    public CompletableFuture<Boolean> addGroup(UUID uuid, String groupId) {
        return this.group(uuid).thenApply(group -> {
            if (group == null || group.id().equalsIgnoreCase(groupId)) {
                return false;
            }

            this.repository.addUserGroup(uuid, groupId);

            return true;
        });
    }

    @Override
    public CompletableFuture<Group> group(UUID uuid) {
        return this.repository.selectUserGroup(uuid);
    }

    @Override
    public CompletableFuture<Set<Group>> activeGroups(UUID uuid) {
        return this.repository.activeUserGroups(uuid);
    }

    @Override
    public @Nullable Group cachedGroup(UUID uuid) {
        return this.groupById(this.repository.userGroups().get(uuid)).orElse(null);
    }

    @Override
    public CompletableFuture<Boolean> removeGroup(UUID uuid, String groupId) {
        return this.activeGroups(uuid).thenApply(groups -> {
            if (groupId.equalsIgnoreCase(this.config.getString("fallback_group"))) {
                return false;
            }

            for (final Group group : groups) {
                if (!group.id().equalsIgnoreCase(groupId)) {
                    continue;
                }

                this.repository.removeUserGroup(uuid, group.id());

                return true;
            }

            return false;
        });
    }
}