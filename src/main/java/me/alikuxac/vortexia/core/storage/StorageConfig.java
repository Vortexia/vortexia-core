// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage;

public class StorageConfig {

  private final StorageType type;
  private final String sqliteFile;
  private final String mysqlHost;
  private final int mysqlPort;
  private final String mysqlDatabase;
  private final String mysqlUsername;
  private final String mysqlPassword;
  private final int maxPoolSize;
  private final int minIdle;
  private final long connectionTimeout;
  private final long idleTimeout;
  private final long maxLifetime;

  private StorageConfig(Builder builder) {
    this.type = builder.type;
    this.sqliteFile = builder.sqliteFile;
    this.mysqlHost = builder.mysqlHost;
    this.mysqlPort = builder.mysqlPort;
    this.mysqlDatabase = builder.mysqlDatabase;
    this.mysqlUsername = builder.mysqlUsername;
    this.mysqlPassword = builder.mysqlPassword;
    this.maxPoolSize = builder.maxPoolSize;
    this.minIdle = builder.minIdle;
    this.connectionTimeout = builder.connectionTimeout;
    this.idleTimeout = builder.idleTimeout;
    this.maxLifetime = builder.maxLifetime;
  }

  public StorageType getType() {
    return type;
  }

  public String getSqliteFile() {
    return sqliteFile;
  }

  public String getMysqlHost() {
    return mysqlHost;
  }

  public int getMysqlPort() {
    return mysqlPort;
  }

  public String getMysqlDatabase() {
    return mysqlDatabase;
  }

  public String getMysqlUsername() {
    return mysqlUsername;
  }

  public String getMysqlPassword() {
    return mysqlPassword;
  }

  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  public int getMinIdle() {
    return minIdle;
  }

  public long getConnectionTimeout() {
    return connectionTimeout;
  }

  public long getIdleTimeout() {
    return idleTimeout;
  }

  public long getMaxLifetime() {
    return maxLifetime;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private StorageType type = StorageType.SQLITE;
    private String sqliteFile = "vortexia.db";
    private String mysqlHost = "localhost";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "vortexia";
    private String mysqlUsername = "root";
    private String mysqlPassword = "";
    private int maxPoolSize = 10;
    private int minIdle = 2;
    private long connectionTimeout = 30000;
    private long idleTimeout = 600000;
    private long maxLifetime = 1800000;

    public Builder type(StorageType type) {
      this.type = type;
      return this;
    }

    public Builder sqliteFile(String sqliteFile) {
      this.sqliteFile = sqliteFile;
      return this;
    }

    public Builder mysqlHost(String mysqlHost) {
      this.mysqlHost = mysqlHost;
      return this;
    }

    public Builder mysqlPort(int mysqlPort) {
      this.mysqlPort = mysqlPort;
      return this;
    }

    public Builder mysqlDatabase(String mysqlDatabase) {
      this.mysqlDatabase = mysqlDatabase;
      return this;
    }

    public Builder mysqlUsername(String mysqlUsername) {
      this.mysqlUsername = mysqlUsername;
      return this;
    }

    public Builder mysqlPassword(String mysqlPassword) {
      this.mysqlPassword = mysqlPassword;
      return this;
    }

    public Builder maxPoolSize(int maxPoolSize) {
      this.maxPoolSize = maxPoolSize;
      return this;
    }

    public Builder minIdle(int minIdle) {
      this.minIdle = minIdle;
      return this;
    }

    public Builder connectionTimeout(long connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public Builder idleTimeout(long idleTimeout) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    public Builder maxLifetime(long maxLifetime) {
      this.maxLifetime = maxLifetime;
      return this;
    }

    public StorageConfig build() {
      return new StorageConfig(this);
    }
  }
}
