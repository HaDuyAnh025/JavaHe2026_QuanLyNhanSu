package database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Doc thong tin ket noi tu db.properties (cung thu muc voi class nay) va
 * mo Connection JDBC moi cho moi loi goi. Goi ket noi xong nho close()
 * (dung try-with-resources) de tra ve pool cua driver.
 */
public class DBConnection {

    private static final Properties CONFIG = loadConfig();

    private DBConnection() {
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("Khong tim thay database/db.properties trong classpath.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Khong doc duoc database/db.properties.", e);
        }
        return props;
    }

    public static Connection getConnection() throws SQLException {
        String url = CONFIG.getProperty("db.url");
        String username = CONFIG.getProperty("db.username");
        String password = CONFIG.getProperty("db.password");
        return DriverManager.getConnection(url, username, password);
    }
}
