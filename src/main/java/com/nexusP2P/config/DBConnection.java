package com.nexusP2P.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {
    private static final Logger logger = Logger.getLogger(DBConnection.class.getName());

    private static final String URL =
            "jdbc:mysql://localhost:3306/nexus_p2p?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "12345";

    // ThreadLocal hər bir Thread üçün xüsusi və gizli bir Connection obyekti saxlayır.
    private static final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

    private DBConnection() {}

    /**
     * Hər bir Thread-ə yalnız özünə aid olan Singleton Connection obyektini verir.
     */
    public static Connection getConnection() {
        try {
            Connection connection = threadConnection.get();

            // Əgər cari Thread hələ bazaya qoşulmayıbsa və ya bağlantı qapanıbsa, yeni açırıq
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);

                // Yaradılan bağlantını bu thread-in şəxsi qutusuna yerləşdiririk
                threadConnection.set(connection);
                logger.log(Level.INFO, "NexusP2P : " + Thread.currentThread().getName() + " üçün MySQL baglandi");
            }
            return connection;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "DataBase connection fail", e);
            throw new RuntimeException("Connection fail ", e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "MySQL JDBC Driver tapilmadi", e);
            throw new RuntimeException("DataBase Driver catismir");
        }
    }

    /**
     * İşini bitirən Thread öz xüsusi bağlantısını təhlükəsiz şəkildə bağlayır
     */
    public static void closeConnection() {
        Connection connection = threadConnection.get();
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.log(Level.INFO, "Database connection close for " + Thread.currentThread().getName());
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Warning when connection was closed");
                throw new RuntimeException(e);
            } finally {
                // Yaddaş sızması (Memory Leak) olmaması üçün thread-in daxilini təmizləyirik
                threadConnection.remove();
            }
        }
    }
}