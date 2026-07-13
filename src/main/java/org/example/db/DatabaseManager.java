package org.example.db;

import org.example.Helper.PropertyHelper;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = PropertyHelper.get("DB_URL");

    public static void initialize(){
        String sql ="INSERT OR IGNORE INTO config(key,value) VALUES(?,?)";
        try(Connection connection = getConnection();Statement statement =connection.createStatement()) {

            String createTableQuery = """
                    CREATE TABLE IF NOT EXISTS jobs (
                                            id TEXT PRIMARY KEY,
                                            command TEXT NOT NULL,
                                            state TEXT NOT NULL DEFAULT 'pending'
                                                CHECK(state IN ('pending','processing','completed','failed','dead')),
                                            attempts INTEGER NOT NULL,
                                            maxRetries INTEGER NOT NULL,
                                            workerId  TEXT,
                                            createdAt TEXT NOT NULL,
                                            updatedAt TEXT NOT NULL,
                                            nextRetry TEXT,
                                            lastHeartbeat TEXT
                                        );
                    
                    """;

            String createWorkerQuery= """
                    CREATE TABLE IF NOT EXISTS workers (
                                           Pid TEXT PRIMARY KEY,
                                           stateWorker TEXT NOT NULL DEFAULT 'running'
                                                CHECK(stateWorker IN ('stopped','running')),
                                           startedAt TEXT NOT NULL
                    
                    );
                    
                   
                    """;
            String createConfigQuery = """
                     CREATE TABLE IF NOT EXISTS config(
                                           key TEXT PRIMARY KEY,
                                           value INTEGER NOT NULL
                    );
                    """;
            statement.execute(createTableQuery);
            statement.execute(createWorkerQuery);
            statement.execute(createConfigQuery);

            try(PreparedStatement ps =connection.prepareStatement(sql); PreparedStatement ps1 =connection.prepareStatement(sql)){
                ps.setString(1,"backOffBase");
                ps.setInt(2,2);

                ps1.setString(1,"maxRetries");
                ps1.setInt(2,3);

                ps.executeUpdate();
                ps1.executeUpdate();
            }



//            System.out.println("Database initialised");



        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Unable to initialize database: " + e.getMessage(), e
            );
        }
    }

    public static Connection getConnection() {

        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to SQLite database.", e);
        }
    }

}
