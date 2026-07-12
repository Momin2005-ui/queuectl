package org.example.Repository;

import org.example.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigRepository {

    public int getMaxRetries() {
        try {
            Connection connection = DatabaseManager.getConnection();
            String sql = "SELECT value FROM config WHERE key = ?";

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, "maxRetries");

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("value");
            }
            ps.close();
            connection.close();

            throw new IllegalStateException(
                    "maxRetries configuration not found"
            );

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database error while getting maxRetries", e
            );
        }
    }

    public int getBackOffBase() throws SQLException{
        try{
            Connection connection= DatabaseManager.getConnection();
            String sql = "SELECT value FROM config WHERE key = ?";
            PreparedStatement ps= connection.prepareStatement(sql);
            ps.setString(1,"backOffBase");
            ResultSet rs= ps.executeQuery();

            if(rs.next()){
                return rs.getInt("value");
            }
            ps.close();
            connection.close();
            throw new IllegalStateException(
                    "backOffBase configuration not found"
            );
        } catch (Exception e) {
            throw new RuntimeException("connection to database"+e.getMessage());
        }

    }
}
