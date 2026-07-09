package org.example.Repository;

import org.example.db.DatabaseManager;
import org.example.model.Job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JobRepository {

    public void insert(Job job) throws SQLException {
        Connection connection = DatabaseManager.getConnection();
        String sql = "INSERT INTO jobs(id,command,state,attempts,max_retries,created_at,updated_at) VALUES(?,?,?,?,?,?,?)";

        PreparedStatement ps =connection.prepareStatement(sql);

        ps.setString(1, job.getId());
        ps.setString(2,job.getCommand());
        ps.setString(3,job.getState().getValue());
        ps.setInt(4,job.getAttempts());

    }
}
