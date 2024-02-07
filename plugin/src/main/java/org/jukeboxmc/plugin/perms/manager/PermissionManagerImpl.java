package org.jukeboxmc.plugin.perms.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.jukeboxmc.plugin.perms.api.manager.PermissionManager;
import org.jukeboxmc.plugin.perms.repository.PermissionsRepository;

/**
 * @author Kaooot
 * @version 1.0
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PermissionManagerImpl implements PermissionManager {

    private final PermissionsRepository repository;

    @Override
    public CompletableFuture<Boolean> addPermission(UUID uuid, String permission) {
        return this.addOrRemovePerm(uuid, permission, true);
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(UUID uuid, String permission) {
        return this.repository.select(uuid)
            .thenApply(permissions -> permissions.contains(permission));
    }

    @Override
    public boolean hasCachedPermission(UUID uuid, String permission) {
        return this.repository.hasCachedPerm(uuid, permission);
    }

    @Override
    public CompletableFuture<Boolean> removePermission(UUID uuid, String permission) {
        return this.addOrRemovePerm(uuid, permission, false);
    }

    private CompletableFuture<Boolean> addOrRemovePerm(UUID uuid, String permission, boolean add) {
        return this.repository.select(uuid).thenApply(permissions -> {
            final boolean success;
            if (add) {
                success = permissions.add(permission);
            } else {
                success = permissions.remove(permission);
            }

            this.repository.update(uuid, permissions);

            return success;
        });
    }
}