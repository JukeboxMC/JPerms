package org.jukeboxmc.plugin.perms.util;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;

/**
 * @author Kaooot
 * @version 1.0
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MySQL {

    private final MySQLData data;

    private Connection connection;

    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");

                this.connection = DriverManager.getConnection("jdbc:mysql://" + this.data.host()
                        + "/" + this.data.database() + "?useSSL=true&autoReconnect=true",
                    this.data.credentials().username(), this.data.credentials().password());

                return this.connection != null;
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();

                return false;
            }
        });
    }

    public PreparedStatement prepare(String query) {
        try {
            return this.connection.prepareStatement(query);
        } catch (SQLException e) {
            e.printStackTrace();

            return null;
        }
    }

    public CompletableFuture<PreparedStatement> prepareAsync(String query) {
        return CompletableFuture.supplyAsync(() -> this.prepare(query));
    }

    public CompletableFuture<Boolean> close() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!this.connection.isClosed()) {
                    this.connection.close();

                    return true;
                }

                return false;
            } catch (SQLException e) {
                e.printStackTrace();

                return false;
            }
        });
    }
}