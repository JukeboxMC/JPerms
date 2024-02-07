package org.jukeboxmc.plugin.perms.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.jukeboxmc.plugin.perms.util.MySQL;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

/**
 * @author Kaooot
 * @version 1.0
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PermissionsRepository {

    private final MySQL mySQL;
    private final RedissonClient redissonClient;

    private RMap<UUID, Set<String>> userPermissions;

    public void setup() {
        try (final PreparedStatement createTable = this.mySQL.prepare("CREATE TABLE IF NOT " +
            "EXISTS permissions(player VARCHAR(36) NOT NULL, permissions LONGTEXT NOT NULL, " +
            "PRIMARY KEY(player))")) {
            createTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        this.userPermissions = this.redissonClient.getMap("jperms:user-perms");
    }

    public void insert(UUID player) {
        this.select(player).whenComplete((permissions, throwable) -> {
            if (permissions == null) {
                this.mySQL.prepareAsync("INSERT INTO permissions(player, permissions) VALUES(?,?)")
                    .whenComplete((statement, t) -> {
                        try (statement) {
                            statement.setString(1, player.toString());
                            statement.setString(2, "");
                            statement.execute();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });

                this.userPermissions.fastPutAsync(player, new HashSet<>());
            } else {
                this.userPermissions.fastPutAsync(player, permissions);
            }
        });
    }

    public void update(UUID player, Set<String> permissions) {
        this.userPermissions.fastPutAsync(player, permissions);

        this.mySQL.prepareAsync("UPDATE permissions SET permissions = ? WHERE player = ?")
            .whenComplete((statement, throwable) -> {
                try (statement) {
                    statement.setString(1, String.join(", ", permissions));
                    statement.setString(2, player.toString());
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
    }

    public CompletableFuture<Set<String>> select(UUID player) {
        return this.mySQL.prepareAsync("SELECT permissions FROM permissions WHERE player = ?")
            .thenApply(statement -> {
                try (statement) {
                    statement.setString(1, player.toString());

                    final ResultSet resultSet = statement.executeQuery();
                    final Set<String> permissions;

                    if (resultSet.next()) {
                        final String s = resultSet.getString("permissions");

                        if (s.contains(",")) {
                            permissions = new HashSet<>(Arrays.stream(s.split(", ")).toList());
                        } else {
                            permissions = new HashSet<>();

                            if (!s.isEmpty()) {
                                permissions.add(s);
                            }
                        }
                    } else {
                        permissions = null;
                    }

                    resultSet.close();

                    return permissions;
                } catch (SQLException e) {
                    e.printStackTrace();

                    return null;
                }
            });
    }

    public void delete(UUID player) {
        this.clearCache(player);

        this.mySQL.prepareAsync("DELETE FROM permissions WHERE player = ?")
            .whenComplete((statement, throwable) -> {
                try (statement) {
                    statement.setString(1, player.toString());
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
    }

    public void clearCache(UUID player) {
        this.userPermissions.fastRemoveAsync(player);
    }

    public Set<String> cachedPermissions(UUID player){
        return this.userPermissions.getOrDefault(player, new HashSet<>());
    }

    public boolean hasCachedPerm(UUID player, String perm) {
        return this.userPermissions.getOrDefault(player, new HashSet<>()).contains(perm);
    }
}