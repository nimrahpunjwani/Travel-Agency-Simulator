package db;

import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/FREE";
    private static final String USER = "SYSTEM";
    private static final String PASSWORD = "Oracle123";

    static {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Oracle JDBC Driver not found.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}