package org.jukeboxmc.plugin.perms.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jukeboxmc.plugin.perms.api.model.Group;
import org.jukeboxmc.plugin.perms.model.GroupImpl;
import org.jukeboxmc.plugin.perms.util.MySQL;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

/**
 * @author Kaooot
 * @version 1.0
 */
@Singleton
@Accessors(fluent = true)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GroupsRepository {

    private final MySQL mySQL;
    private final RedissonClient redissonClient;

    private RMap<String, GroupImpl> groups;

    @Getter
    private RMap<UUID, String> userGroups;

    public void setup() {
        try (final PreparedStatement createGroupsTable = this.mySQL.prepare(
            "CREATE TABLE IF NOT EXISTS groups(id VARCHAR(100) NOT NULL, priority INT NOT NULL, " +
                "name_tag TEXT NOT NULL, chat_format TEXT NOT NULL, permissions " +
                "LONGTEXT NOT NULL, parent_id TEXT, PRIMARY KEY(id))")) {
            createGroupsTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (final PreparedStatement createUserGroupsTable = this.mySQL.prepare(
            "CREATE TABLE IF NOT EXISTS user_groups(player VARCHAR(36) NOT NULL, group_id " +
                "TEXT NOT NULL, assigned_on TIMESTAMP NOT NULL, active TINYINT NOT NULL)")) {
            createUserGroupsTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        this.groups = this.redissonClient.getMap("jperms:groups");
        this.userGroups = this.redissonClient.getMap("jperms:user-groups");

        this.mySQL.prepareAsync("SELECT * FROM groups").whenComplete((statement, throwable) -> {
            try (statement) {
                final ResultSet resultSet = statement.executeQuery();

                final Map<String, GroupImpl> groups = new HashMap<>();

                while (resultSet.next()) {
                    final String id = resultSet.getString("id");
                    final int priority = resultSet.getInt("priority");
                    final String nameTag = resultSet.getString("name_tag");
                    final String chatFormat = resultSet.getString("chat_format");

                    Set<String> permissions;
                    final String s = resultSet.getString("permissions");

                    if (s.contains(",")) {
                        permissions = new HashSet<>(Arrays.stream(s.split(", ")).toList());
                    } else {
                        permissions = new HashSet<>();

                        if (!s.isEmpty()) {
                            permissions.add(s);
                        }
                    }

                    final String parentId = resultSet.getString("parent_id");

                    final GroupImpl group = new GroupImpl(id, priority);
                    group.nameTag(nameTag);
                    group.chatFormat(chatFormat);
                    group.permissions(permissions);
                    group.parentId(parentId);

                    GroupsRepository.this.groups.fastPutAsync(id, group);

                    groups.put(id, group);
                }

                for (final GroupImpl group : groups.values()) {
                    GroupsRepository.this.inheritPerms(groups, group);
                }

                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public boolean insertGroup(Group group) {
        final boolean b = !this.groups.containsKey(group.id());

        if (b) {
            this.inheritPerms(this.groups, GroupImpl.ofApiModel(group));

            this.groups.fastPutAsync(group.id(), GroupImpl.ofApiModel(group));

            this.mySQL.prepareAsync("INSERT INTO groups(id, priority, name_tag, chat_format, " +
                    "permissions, parent_id) VALUES(?,?,?,?,?,?)")
                .whenComplete((statement, throwable) -> {
                    try (statement) {
                        statement.setString(1, group.id());
                        statement.setInt(2, group.priority());
                        statement.setString(3, group.nameTag());
                        statement.setString(4, group.chatFormat());
                        statement.setString(5, String.join(", ", group.permissions()));
                        statement.setString(6, group.parentId());
                        statement.execute();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
        }

        return b;
    }

    public GroupImpl selectGroup(String id) {
        return this.groups.getOrDefault(id, null);
    }

    public Set<Group> selectGroups() {
        return Set.copyOf(this.groups.values());
    }

    public void updateGroupNameTag(String id, String nameTag) {
        this.groups.fastPutAsync(id, this.groups.get(id).nameTag(nameTag));

        this.mySQL.prepareAsync("UPDATE groups SET name_tag = ? WHERE id = ?")
            .whenComplete((statement, throwable) -> {
                try (statement) {
                    statement.setString(1, nameTag);
                    statement.setString(2, id);
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
    }

    public void updateGroupChatFormat(String id, String chatFormat) {
        this.groups.fastPutAsync(id, this.groups.get(id).chatFormat(chatFormat));

        this.mySQL.prepareAsync("UPDATE groups SET chat_format = ? WHERE id = ?")
            .whenComplete((statement, throwable) -> {
                try (statement) {
                    statement.setString(1, chatFormat);
                    statement.setString(2, id);
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
    }

    public void updateGroupPerms(String id, Set<String> permissions) {
        final GroupImpl group = this.groups.get(id);
        group.permissions().clear();
        group.permissions().addAll(permissions);

        this.groups.fastPutAsync(id, group);

        this.mySQL.prepareAsync("UPDATE groups SET permissions = ? WHERE id = ?")
            .whenComplete((statement, throwable) -> {
                try (statement) {
                    statement.setString(1, String.join(", ", permissions));
                    statement.setString(2, id);
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
    }

    public void updateGroupParent(String id, String parentId) {
        this.groups.fastPutAsync(id, this.groups.get(id).parentId(parentId));

        this.mySQL.prepareAsync("UPDATE groups SET parent_id = ? WHERE id = ?")
            .whenComplete((statement, throwable) -> {
                try (statement) {
                    statement.setString(1, parentId);
                    statement.setString(2, id);
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
    }

    public void deleteGroup(String id) {
        this.groups.fastRemoveAsync(id);

        this.mySQL.prepareAsync("DELETE FROM groups WHERE id = ?")
            .whenComplete((statement, throwable) -> {
                try (statement) {
                    statement.setString(1, id);
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
    }

    public CompletableFuture<Group> insertUserGroup(UUID player, String id) {
        return this.selectUserGroup(player).thenApply(group -> {
            if (group != null && group.id().equalsIgnoreCase(id)) {
                return group;
            }

            this.addUserGroup(player, id);

            return null;
        });
    }

    public CompletableFuture<Group> selectUserGroup(UUID player) {
        return this.mySQL.prepareAsync("SELECT group_id FROM user_groups WHERE player = ? " +
            "AND active = ?").thenApply(statement -> {
            try (statement) {
                statement.setString(1, player.toString());
                statement.setInt(2, 1);

                final ResultSet resultSet = statement.executeQuery();

                final Map<String, GroupImpl> map = new HashMap<>();

                while (resultSet.next()) {
                    final String id = resultSet.getString("group_id");

                    map.put(id, this.groups.getOrDefault(id, null));
                }

                resultSet.close();

                if (map.isEmpty()) {
                    return null;
                }

                final String id = map.entrySet().stream()
                    .sorted((o1, o2) -> Integer.compare(o2.getValue().priority(),
                        o1.getValue().priority()))
                    .map(Map.Entry::getKey)
                    .toList()
                    .get(0);

                return this.groups.getOrDefault(id, null);
            } catch (SQLException e) {
                e.printStackTrace();

                return null;
            }
        });
    }

    public CompletableFuture<Set<Group>> activeUserGroups(UUID player) {
        return this.mySQL.prepareAsync("SELECT group_id FROM user_groups WHERE player = ? AND " +
            "active = ?").thenApply(statement -> {
            try (statement) {
                statement.setString(1, player.toString());
                statement.setInt(2, 1);

                final ResultSet resultSet = statement.executeQuery();
                final Set<Group> groups = new HashSet<>();

                while (resultSet.next()) {
                    final Group group = this.groups.getOrDefault(resultSet.getString("group_id"),
                        null);

                    if (group == null) {
                        continue;
                    }

                    groups.add(group);
                }

                resultSet.close();

                return groups;
            } catch (SQLException e) {
                e.printStackTrace();

                return null;
            }
        });
    }

    public void addUserGroup(UUID player, String group) {
        this.mySQL.prepareAsync("INSERT INTO user_groups(player, group_id, assigned_on, active) " +
            "VALUES(?,?,?,?)").whenComplete((statement, throwable) -> {
            try (statement) {
                statement.setString(1, player.toString());
                statement.setString(2, group);
                statement.setTimestamp(3, Timestamp.from(Instant.now()));
                statement.setInt(4, 1);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void removeUserGroup(UUID player, String group) {
        this.mySQL.prepareAsync("UPDATE user_groups SET active = ? WHERE player = ? " +
            "AND group_id = ? AND active = ?").whenComplete((statement, throwable) -> {
            try (statement) {
                statement.setInt(1, 0);
                statement.setString(2, player.toString());
                statement.setString(3, group);
                statement.setInt(4, 1);
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void inheritPerms(Map<String, GroupImpl> groups, GroupImpl group) {
        if (group.parentId() != null) {
            final GroupImpl parent = groups.get(group.parentId());

            if (parent == null) {
                return;
            }

            GroupImpl next = parent;

            while (next.parentId() != null) {
                final GroupImpl grandParent = groups.get(next.parentId());

                if (grandParent == null) {
                    break;
                }

                parent.inheritedPermissions().addAll(grandParent.permissions());

                next = grandParent;
            }

            group.inheritedPermissions().addAll(parent.permissions());
            group.inheritedPermissions().addAll(parent.inheritedPermissions());
        }
    }
}