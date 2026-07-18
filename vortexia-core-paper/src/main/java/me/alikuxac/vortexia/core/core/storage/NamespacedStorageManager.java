// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.storage;

import me.alikuxac.vortexia.api.storage.StorageManager;
import me.alikuxac.vortexia.api.storage.SQLFunction;
import me.alikuxac.vortexia.api.storage.SafeConnection;
import me.alikuxac.vortexia.api.storage.SQLValidator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NamespacedStorageManager implements StorageManager {

    private final StorageManager parent;
    private final String namespace;

    public NamespacedStorageManager(StorageManager parent, String namespace) {
        this.parent = parent;
        this.namespace = namespace;
    }

    @Override
    public CompletableFuture<Optional<String>> getMetadata(UUID uuid, String key) {
        // Tự động phân tách namespace cho metadata của từng addon bằng tiền tố "namespace:"
        return parent.getMetadata(uuid, namespace + ":" + key);
    }

    @Override
    public CompletableFuture<Void> setMetadata(UUID uuid, String key, String value) {
        return parent.setMetadata(uuid, namespace + ":" + key, value);
    }

    @Override
    public CompletableFuture<Void> removeMetadata(UUID uuid, String key) {
        return parent.removeMetadata(uuid, namespace + ":" + key);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection rawConnection = parent.getConnection();
        return new SafeConnection(rawConnection, namespace);
    }

    @Override
    public CompletableFuture<Void> executeUpdate(String sql, Object... params) {
        try {
            SQLValidator.validate(sql, namespace);
            return parent.executeUpdate(sql, params);
        } catch (SQLException e) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public <T> CompletableFuture<T> executeQuery(String sql, SQLFunction<ResultSet, T> mapper, Object... params) {
        try {
            SQLValidator.validate(sql, namespace);
            return parent.executeQuery(sql, mapper, params);
        } catch (SQLException e) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public String getDatabaseType() {
        return parent.getDatabaseType();
    }
}
