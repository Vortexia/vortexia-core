// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.storage.IStorage;
import me.alikuxac.vortexia.core.storage.StorageConfig;
import me.alikuxac.vortexia.core.storage.StorageException;
import me.alikuxac.vortexia.api.model.Identity;

public class MySQLStorage implements IStorage {

  private static final String CREATE_TABLE = """
      CREATE TABLE IF NOT EXISTS vortexia_identities (
          uuid VARCHAR(36) PRIMARY KEY,
          premium_uuid VARCHAR(36),
          citizen_id VARCHAR(12),
          name VARCHAR(16) NOT NULL,
          pin VARCHAR(64),
          created_at BIGINT NOT NULL,
          updated_at BIGINT NOT NULL,
          INDEX idx_name (name),
          UNIQUE INDEX idx_citizen_id (citizen_id)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
      """;

  private static final String INSERT_IDENTITY = """
      INSERT INTO vortexia_identities (uuid, premium_uuid, citizen_id, name, pin, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
          name = VALUES(name),
          premium_uuid = VALUES(premium_uuid),
          citizen_id = VALUES(citizen_id),
          pin = VALUES(pin),
          updated_at = VALUES(updated_at)
      """;

  private static final String SELECT_BY_UUID = """
      SELECT uuid, premium_uuid, citizen_id, name, pin, created_at, updated_at
      FROM vortexia_identities
      WHERE uuid = ?
      """;

  private static final String SELECT_BY_NAME = """
      SELECT uuid, premium_uuid, citizen_id, name, pin, created_at, updated_at
      FROM vortexia_identities
      WHERE name = ?
      """;

  private static final String SELECT_BY_PREMIUM_UUID = """
      SELECT uuid, premium_uuid, citizen_id, name, pin, created_at, updated_at
      FROM vortexia_identities
      WHERE premium_uuid = ?
      """;

  private static final String SELECT_BY_CITIZEN_ID = """
      SELECT uuid, premium_uuid, citizen_id, name, pin, created_at, updated_at
      FROM vortexia_identities
      WHERE citizen_id = ?
      """;

  private static final String UPDATE_PIN = """
      UPDATE vortexia_identities
      SET pin = ?, updated_at = ?
      WHERE uuid = ?
      """;

  private static final String UPDATE_PREMIUM_UUID = """
      UPDATE vortexia_identities
      SET premium_uuid = ?, updated_at = ?
      WHERE uuid = ?
      """;

  private static final String CREATE_METADATA_TABLE = """
      CREATE TABLE IF NOT EXISTS vortexia_metadata (
          uuid VARCHAR(36) NOT NULL,
          meta_key VARCHAR(64) NOT NULL,
          meta_value TEXT,
          PRIMARY KEY (uuid, meta_key),
          FOREIGN KEY (uuid) REFERENCES vortexia_identities(uuid) ON DELETE CASCADE
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
      """;

  private static final String SELECT_METADATA = """
      SELECT meta_value FROM vortexia_metadata WHERE uuid = ? AND meta_key = ?
      """;

  private static final String INSERT_METADATA = """
      INSERT INTO vortexia_metadata (uuid, meta_key, meta_value)
      VALUES (?, ?, ?)
      ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value)
      """;

  private static final String DELETE_METADATA = """
      DELETE FROM vortexia_metadata WHERE uuid = ? AND meta_key = ?
      """;

  private final VortexiaCore plugin;
  private final StorageConfig config;
  private HikariDataSource dataSource;

  public MySQLStorage(VortexiaCore plugin, StorageConfig config) {
    this.plugin = plugin;
    this.config = config;
  }

  @Override
  public void initialize() throws StorageException {
    try {
      String jdbcUrl = String.format(
          "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&autoReconnect=true",
          config.getMysqlHost(),
          config.getMysqlPort(),
          config.getMysqlDatabase());

      HikariConfig hikariConfig = new HikariConfig();
      hikariConfig.setJdbcUrl(jdbcUrl);
      hikariConfig.setUsername(config.getMysqlUsername());
      hikariConfig.setPassword(config.getMysqlPassword());
      hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
      hikariConfig.setMinimumIdle(config.getMinIdle());
      hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
      hikariConfig.setIdleTimeout(config.getIdleTimeout());
      hikariConfig.setMaxLifetime(config.getMaxLifetime());
      hikariConfig.setPoolName("VortexiaMySQLPool");

      hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
      hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
      hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

      dataSource = new HikariDataSource(hikariConfig);

      createTables();

      plugin.getLoggerService().debug(
          "MySQL storage initialized at: " + config.getMysqlHost() + ":" + config.getMysqlPort());
    } catch (Exception e) {
      throw new StorageException("Failed to initialize MySQL storage", e);
    }
  }

  private void createTables() throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      try (PreparedStatement stmt = conn.prepareStatement(CREATE_TABLE)) {
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt = conn.prepareStatement(CREATE_METADATA_TABLE)) {
        stmt.executeUpdate();
      }
    }
  }

  @Override
  public void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      plugin.getLoggerService().debug("MySQL storage connection pool closed");
    }
  }

  @Override
  public CompletableFuture<Void> saveIdentity(UUID uuid, UUID premiumUuid, String citizenId, String name, String pin) {
    return CompletableFuture.runAsync(() -> {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(INSERT_IDENTITY)) {

        long now = System.currentTimeMillis();
        stmt.setString(1, uuid.toString());
        stmt.setString(2, premiumUuid != null ? premiumUuid.toString() : null);
        stmt.setString(3, citizenId);
        stmt.setString(4, name);
        stmt.setString(5, pin);
        stmt.setLong(6, now);
        stmt.setLong(7, now);

        stmt.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to save identity", e);
      }
    });
  }

  @Override
  public CompletableFuture<Optional<Identity>> getIdentity(UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(SELECT_BY_UUID)) {

        stmt.setString(1, uuid.toString());

        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return Optional.of(mapResultSetToIdentity(rs));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to get identity by UUID", e);
      }
      return Optional.empty();
    });
  }

  @Override
  public CompletableFuture<Optional<Identity>> getIdentityByName(String name) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(SELECT_BY_NAME)) {

        stmt.setString(1, name);

        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return Optional.of(mapResultSetToIdentity(rs));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to get identity by name", e);
      }
      return Optional.empty();
    });
  }

  @Override
  public CompletableFuture<Boolean> updatePin(UUID uuid, String newPin) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(UPDATE_PIN)) {

        stmt.setString(1, newPin);
        stmt.setLong(2, System.currentTimeMillis());
        stmt.setString(3, uuid.toString());

        int rowsAffected = stmt.executeUpdate();
        return rowsAffected > 0;
      } catch (SQLException e) {
        throw new RuntimeException("Failed to update PIN", e);
      }
    });
  }

  @Override
  public CompletableFuture<Optional<Identity>> getIdentityByPremiumUuid(UUID premiumUuid) {
    return CompletableFuture.supplyAsync(() -> {
      if (premiumUuid == null) {
        return Optional.empty();
      }
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(SELECT_BY_PREMIUM_UUID)) {

        stmt.setString(1, premiumUuid.toString());

        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return Optional.of(mapResultSetToIdentity(rs));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to get identity by premium UUID", e);
      }
      return Optional.empty();
    });
  }

  @Override
  public CompletableFuture<Optional<Identity>> getIdentityByCitizenId(String citizenId) {
    return CompletableFuture.supplyAsync(() -> {
      if (citizenId == null) {
        return Optional.empty();
      }
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(SELECT_BY_CITIZEN_ID)) {

        stmt.setString(1, citizenId);

        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return Optional.of(mapResultSetToIdentity(rs));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to get identity by citizen ID", e);
      }
      return Optional.empty();
    });
  }

  @Override
  public CompletableFuture<Boolean> updatePremiumUuid(UUID uuid, UUID premiumUuid) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(UPDATE_PREMIUM_UUID)) {

        stmt.setString(1, premiumUuid != null ? premiumUuid.toString() : null);
        stmt.setLong(2, System.currentTimeMillis());
        stmt.setString(3, uuid.toString());

        int rowsAffected = stmt.executeUpdate();
        return rowsAffected > 0;
      } catch (SQLException e) {
        throw new RuntimeException("Failed to update premium UUID", e);
      }
    });
  }

  @Override
  public CompletableFuture<Optional<String>> getMetadata(UUID uuid, String key) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(SELECT_METADATA)) {

        stmt.setString(1, uuid.toString());
        stmt.setString(2, key);

        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return Optional.ofNullable(rs.getString("meta_value"));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to get metadata", e);
      }
      return Optional.empty();
    });
  }

  @Override
  public CompletableFuture<Void> saveMetadata(UUID uuid, String key, String value) {
    return CompletableFuture.runAsync(() -> {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(INSERT_METADATA)) {

        stmt.setString(1, uuid.toString());
        stmt.setString(2, key);
        stmt.setString(3, value);

        stmt.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to save metadata", e);
      }
    });
  }

  @Override
  public CompletableFuture<Void> deleteMetadata(UUID uuid, String key) {
    return CompletableFuture.runAsync(() -> {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement stmt = conn.prepareStatement(DELETE_METADATA)) {

        stmt.setString(1, uuid.toString());
        stmt.setString(2, key);

        stmt.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to delete metadata", e);
      }
    });
  }

  @Override
  public boolean isConnected() {
    return dataSource != null && !dataSource.isClosed();
  }

  private Identity mapResultSetToIdentity(ResultSet rs) throws SQLException {
    String premiumUuidStr = rs.getString("premium_uuid");
    return Identity.builder()
        .uuid(UUID.fromString(rs.getString("uuid")))
        .premiumUuid(premiumUuidStr != null ? UUID.fromString(premiumUuidStr) : null)
        .citizenId(rs.getString("citizen_id"))
        .name(rs.getString("name"))
        .pin(rs.getString("pin"))
        .createdAt(rs.getLong("created_at"))
        .updatedAt(rs.getLong("updated_at"))
        .build();
  }
}
