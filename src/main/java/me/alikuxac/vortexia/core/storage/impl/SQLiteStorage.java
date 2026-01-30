// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage.impl;

import java.io.File;
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
import me.alikuxac.vortexia.core.storage.model.Identity;

public class SQLiteStorage implements IStorage {

  private static final String CREATE_TABLE = """
      CREATE TABLE IF NOT EXISTS vortexia_identities (
          uuid VARCHAR(36) PRIMARY KEY,
          premium_uuid VARCHAR(36),
          citizen_id VARCHAR(12),
          name VARCHAR(16) NOT NULL,
          pin VARCHAR(64),
          created_at BIGINT NOT NULL,
          updated_at BIGINT NOT NULL
      )
      """;

  private static final String CREATE_INDEX = """
      CREATE INDEX IF NOT EXISTS idx_name ON vortexia_identities(name);
      CREATE UNIQUE INDEX IF NOT EXISTS idx_citizen_id ON vortexia_identities(citizen_id);
      """;

  private static final String INSERT_IDENTITY = """
      INSERT OR REPLACE INTO vortexia_identities (uuid, premium_uuid, citizen_id, name, pin, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)
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

  private final VortexiaCore plugin;
  private final StorageConfig config;
  private HikariDataSource dataSource;

  public SQLiteStorage(VortexiaCore plugin, StorageConfig config) {
    this.plugin = plugin;
    this.config = config;
  }

  @Override
  public void initialize() throws StorageException {
    try {
      File dataFolder = plugin.getDataFolder();
      if (!dataFolder.exists()) {
        dataFolder.mkdirs();
      }

      File dbFile = new File(dataFolder, config.getSqliteFile());
      String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

      HikariConfig hikariConfig = new HikariConfig();
      hikariConfig.setJdbcUrl(jdbcUrl);
      hikariConfig.setMaximumPoolSize(1);
      hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
      hikariConfig.setPoolName("VortexiaSQLitePool");

      dataSource = new HikariDataSource(hikariConfig);

      createTables();

      plugin.getLoggerService().debug("SQLite storage initialized at: " + dbFile.getAbsolutePath());
    } catch (Exception e) {
      throw new StorageException("Failed to initialize SQLite storage", e);
    }
  }

  private void createTables() throws SQLException {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement createTable = conn.prepareStatement(CREATE_TABLE);
        PreparedStatement createIndex = conn.prepareStatement(CREATE_INDEX)) {

      createTable.executeUpdate();
      createIndex.executeUpdate();
    }
  }

  @Override
  public void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      plugin.getLoggerService().debug("SQLite storage connection pool closed");
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
