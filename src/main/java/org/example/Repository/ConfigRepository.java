package org.example.Repository;

import org.example.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigRepository {

        public int getMaxRetries() {
            String sql = "SELECT value FROM config WHERE key = ?";
            try(Connection connection = DatabaseManager.getConnection();PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, "maxRetries");

               try( ResultSet rs = ps.executeQuery();){

                   if (rs.next()) {
                       return rs.getInt("value");
                   }
               }


                throw new IllegalStateException(
                        "maxRetries configuration not found"
                );

            } catch (SQLException e) {
                throw new RuntimeException(
                        "Database error while getting maxRetries", e
                );
            }
        }

       public int getBackOffBase() {
        String sql = "SELECT value FROM config WHERE key = ?";
        try(Connection connection= DatabaseManager.getConnection();PreparedStatement ps= connection.prepareStatement(sql)){

            ps.setString(1,"backOffBase");
            try( ResultSet rs= ps.executeQuery();){

                if(rs.next()){
                    return rs.getInt("value");
                }
            }

            throw new IllegalStateException(
                    "backOffBase configuration not found"
            );
        } catch (SQLException e) {
            throw new RuntimeException("connection to database"+e.getMessage());
        }

    }

       public void setMaxRetries(int value){

            String sql = "UPDATE config SET value=? WHERE key=?";

            try(Connection connection=DatabaseManager.getConnection(); PreparedStatement ps =connection.prepareStatement(sql)) {
                ps.setInt(1,value);
                ps.setString(2,"maxRetries");
                int executed=ps.executeUpdate();
                if(executed==1){
                    System.out.println("Successfully updated");
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
       }

       public void setBackOffBase(int value){
           String sql = "UPDATE config SET value=? WHERE key=?";

           try(Connection connection=DatabaseManager.getConnection(); PreparedStatement ps =connection.prepareStatement(sql)) {
               ps.setInt(1,value);
               ps.setString(2,"backOffBase");
               int executed=ps.executeUpdate();
               if(executed==1){
                   System.out.println("Successfully updated");
               }

           } catch (SQLException e) {
               throw new RuntimeException(e);
           }
       }
}
