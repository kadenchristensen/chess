package dataaccess;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static final Properties properties = new Properties();

    static {
        loadPropertiesFromResources();
    }

    public static Connection getConnection() throws SQLException {
        String host = properties.getProperty("db.host");
        String port = properties.getProperty("db.port");
        String name = properties.getProperty("db.name");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        String url = String.format(
                "jdbc:mysql://%s:%s/%s?allowPublicKeyRetrieval=true&useSSL=false",
                host, port, name
        );

        return DriverManager.getConnection(url, user, password);
    }

    public static void createDatabase() throws SQLException {
        String host = properties.getProperty("db.host");
        String port = properties.getProperty("db.port");
        String name = properties.getProperty("db.name");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        String url = String.format(
                "jdbc:mysql://%s:%s?allowPublicKeyRetrieval=true&useSSL=false",
                host, port
        );

        try (Connection conn = DriverManager.getConnection(url, user, password);
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + name);
        }
    }

    static void loadProperties(Properties newProperties) {
        properties.clear();
        properties.putAll(newProperties);
    }

    static void loadPropertiesFromResources() {
        try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException("db.properties file not found");
            }
            properties.clear();
            properties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load db.properties", e);
        }
    }
}
