package org.example.db;

import org.example.Helper.PropertyHelper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = PropertyHelper.get("DB_URL");

    public static void initialize(){
        try {
            Connection connection = getConnection();
            Statement statement =connection.createStatement();

            String createTableQuery = """
                    CREATE TABLE IF NOT EXISTS jobs (
                                            id TEXT PRIMARY KEY,
                                            command TEXT NOT NULL,
                                            state TEXT NOT NULL DEFAULT 'pending'
                                                CHECK(state IN ('pending','processing','completed','failed','dead')),
                                            attempts INTEGER NOT NULL DEFAULT 0,
                                            maxRetries INTEGER NOT NULL DEFAULT 3,
                                            createdAt TEXT NOT NULL,
                                            updatedAt TEXT NOT NULL
                                        );
                    
                    """;

            String createWorkerQuery= """
                    CREATE TABLE IF NOT EXISTS workers (
                                           pid INTEGER PRIMARY KEY,
                                           state TEXT NOT NULL DEFAULT 'running'
                                                CHECK(state IN ('stopped','running')),
                                           startedAt TEXT NOT NULL
                    
                    );
                    
                   
                    """;
            String createConfigQuery = """
                     CREATE TABLE IF NOT EXISTS config(
                                           key TEXT PRIMARY KEY,
                                           value TEXT NOT NULL
                    );
                    """;
            statement.execute(createTableQuery);
            statement.execute(createWorkerQuery);
            statement.execute(createConfigQuery);
            System.out.println("Database initialised");
            connection.close();
            statement.close();

        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to database");
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
