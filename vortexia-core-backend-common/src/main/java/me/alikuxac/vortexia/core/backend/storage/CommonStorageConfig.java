// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.backend.storage;

public class CommonStorageConfig {

    private final String type;
    private final String sqliteFile;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;

    public CommonStorageConfig(String type, String sqliteFile, String mysqlHost, int mysqlPort,
                               String mysqlDatabase, String mysqlUsername, String mysqlPassword) {
        this.type = type != null ? type.toUpperCase() : "SQLITE";
        this.sqliteFile = sqliteFile != null ? sqliteFile : "vortexia.db";
        this.mysqlHost = mysqlHost != null ? mysqlHost : "localhost";
        this.mysqlPort = mysqlPort > 0 ? mysqlPort : 3306;
        this.mysqlDatabase = mysqlDatabase != null ? mysqlDatabase : "vortexia";
        this.mysqlUsername = mysqlUsername != null ? mysqlUsername : "root";
        this.mysqlPassword = mysqlPassword != null ? mysqlPassword : "";
    }

    public String getType() {
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
}
