package org.example.Repository;

import org.example.db.DatabaseManager;
import org.example.model.Job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JobRepository {

    public void insert(Job job) throws SQLException {

        try {
            Connection connection = DatabaseManager.getConnection();
            String sql = "INSERT INTO jobs(id,command,state,attempts,max_retries,created_at,updated_at) VALUES(?,?,?,?,?,?,?)";

            PreparedStatement ps =connection.prepareStatement(sql);

            ps.setString(1, job.getId());
            ps.setString(2,job.getCommand());
            ps.setString(3,job.getState().getValue());
            ps.setInt(4,job.getAttempts());
            ps.setInt(5,job.getMaxRetries());
            ps.setString(6,job.getCreatedAt().toString());
            ps.setString(7,job.getUpdatedAt().toString());

            int affectedRows = ps.executeUpdate();
            System.out.println(affectedRows + "rows inserted");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }



    }

    public ResultSet listAll() throws SQLException{
        try{
            Connection connection = DatabaseManager.getConnection();
            String sql ="SELECT * FROM jobs";
            PreparedStatement ps=connection.prepareStatement(sql);

            java.sql.ResultSet resultSet =ps.executeQuery();

            return  resultSet;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


