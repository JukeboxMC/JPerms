package org.jukeboxmc.plugin.perms.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.jukeboxmc.plugin.perms.util.MySQL;

/**
 * @author Kaooot
 * @version 1.0
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UsersRepository {

    private final MySQL mySQL;

    public void setup() {
        try (final PreparedStatement createTable = this.mySQL.prepare("CREATE TABLE IF NOT " +
            "EXISTS permission_users(player VARCHAR(36) NOT NULL, name VARCHAR(16) NOT NULL, " +
            "PRIMARY KEY(player))")) {
            createTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertOrUpdate(UUID player, String name) {
        this.name(player).whenComplete((s, throwable) -> {
            if (s == null) {
                this.mySQL.prepareAsync("INSERT INTO permission_users(player, name) VALUES(?,?)")
                    .whenComplete((statement, t) -> {
                        try (statement) {
                            statement.setString(1, player.toString());
                            statement.setString(2, name);
                            statement.execute();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
            } else if (!s.equalsIgnoreCase(name)) {
                this.updateName(player, name);
            }
        });
    }

    public CompletableFuture<String> name(UUID player) {
        return this.mySQL.prepareAsync("SELECT name FROM permission_users WHERE player = ?")
            .thenApply(statement -> {
                try (statement) {
                    statement.setString(1, player.toString());

                    final ResultSet resultset = statement.executeQuery();
                    final String name;

                    if (resultset.next()) {
                        name = resultset.getString("name");
                    } else {
                        name = null;
                    }

                    resultset.close();

                    return name;
                } catch (SQLException e) {
                    e.printStackTrace();

                    return null;
                }
            });
    }

    public CompletableFuture<UUID> uuid(String name) {
        return this.mySQL.prepareAsync("SELECT player FROM permission_users WHERE name = ?")
            .thenApply(statement -> {
                try (statement) {
                    statement.setString(1, name);

                    final ResultSet resultSet = statement.executeQuery();
                    final UUID uuid;

                    if (resultSet.next()) {
                        uuid = UUID.fromString(resultSet.getString("player"));
                    } else {
                        uuid = null;
                    }

                    resultSet.close();

                    return uuid;
                } catch (SQLException e) {
                    e.printStackTrace();

                    return null;
                }
            });
    }

    private void updateName(UUID player, String name) {
        this.mySQL.prepareAsync("UPDATE permission_users SET name = ? WHERE player = ?")
            .whenComplete((statement, throwable) -> {
                try (statement) {
                    statement.setString(1, name);
                    statement.setString(2, player.toString());
                    statement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
    }
}