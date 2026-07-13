package org.example.Repository;

import org.example.db.DatabaseManager;
import org.example.model.Job;
import org.example.model.State;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobRepository {

    public void insert(Job job) throws SQLException {

        try {
            Connection connection = DatabaseManager.getConnection();
            String sql = "INSERT INTO jobs(id,command,state,attempts,workerId,maxRetries,createdAt,updatedAt,nextRetry) VALUES(?,?,?,?,?,?,?,?,?)";

            PreparedStatement ps =connection.prepareStatement(sql);

            ps.setString(1, job.getId());
            ps.setString(2,job.getCommand());
            ps.setString(3,job.getState().getValue());
            ps.setInt(4,job.getAttempts());
            ps.setInt(6,job.getMaxRetries());
            ps.setString(7,job.getCreatedAt().toString());
            ps.setString(8,job.getUpdatedAt().toString());


            int affectedRows = ps.executeUpdate();
            System.out.println(affectedRows + "rows inserted");
            ps.close();
            connection.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }



    }

    public List<Job> listAll() throws SQLException{

        String sql ="SELECT * FROM jobs";
        List<Job> jobs =new ArrayList<>();
        try(Connection connection = DatabaseManager.getConnection();PreparedStatement ps=connection.prepareStatement(sql);ResultSet rs =ps.executeQuery()) {

            while (rs.next()) {
                Job job = new Job();

                job.setId(rs.getString("id"));
                job.setCommand(rs.getString("command"));
                job.setState(State.fromValue(rs.getString("state")));
                job.setAttempts(rs.getInt("attempts"));
                job.setMaxRetries(rs.getInt("maxRetries"));
                job.setWorkerId(rs.getString("workerId"));
                job.setCreatedAt(Instant.parse(rs.getString("createdAt")));
                job.setUpdatedAt(Instant.parse(rs.getString("updatedAt")));
                String nextRetry = rs.getString("nextRetry");

                if (nextRetry != null) {
                    job.setNextRetry(Instant.parse(nextRetry));
                }

                jobs.add(job);
            }
        }
        return jobs;

    }

    public List<Job> listByState(String state) throws SQLException{
        String sql = "SELECT * FROM jobs WHERE state = ?";
        List< Job> jobs =new ArrayList<>();
        try(Connection connection=DatabaseManager.getConnection();PreparedStatement ps = connection.prepareStatement(sql)){
            ps.setString(1,state);
            try(ResultSet rs =ps.executeQuery()) {
                while(rs.next()){
                    Job job = new Job();

                    job.setId(rs.getString("id"));
                    job.setCommand(rs.getString("command"));
                    job.setState(State.fromValue(rs.getString("state")));
                    job.setAttempts(rs.getInt("attempts"));
                    job.setMaxRetries(rs.getInt("maxRetries"));
                    job.setWorkerId(rs.getString("workerId"));
                    job.setCreatedAt(Instant.parse(rs.getString("createdAt")));
                    job.setUpdatedAt(Instant.parse(rs.getString("updatedAt")));
                    String nextRetry = rs.getString("nextRetry");

                    if (nextRetry != null) {
                        job.setNextRetry(Instant.parse(nextRetry));
                    }

                    jobs.add(job);
                }
                return jobs;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Job> listDead() throws SQLException{
        String sql ="SELECT * FROM jobs WHERE state ='dead'";
        List<Job> deadJobs =new ArrayList<>();
        try(Connection connection=DatabaseManager.getConnection(); PreparedStatement ps =connection.prepareStatement(sql);ResultSet rs =ps.executeQuery()){
            while(rs.next()){
                Job job =new Job();
                job.setId(rs.getString("id"));
                job.setCommand(rs.getString("command"));
                job.setState(State.fromValue(rs.getString("state")));
                job.setAttempts(rs.getInt("attempts"));
                job.setMaxRetries(rs.getInt("maxRetries"));
                job.setWorkerId(rs.getString("workerId"));
                job.setCreatedAt(Instant.parse(rs.getString("createdAt")));
                job.setUpdatedAt(Instant.parse(rs.getString("updatedAt")));
                String nextRetry = rs.getString("nextRetry");

                if (nextRetry != null) {
                    job.setNextRetry(Instant.parse(nextRetry));
                }

                deadJobs.add(job);
            }
        }
        return deadJobs;
    }

    public void retryDeadJob(String jobId) throws  SQLException{
        String sql ="UPDATE jobs SET state=?,attempts=?,maxRetries=?,workerId=?,updatedAt=?,nextRetry=? WHERE id=? AND state='dead'";

        try(Connection connection =DatabaseManager.getConnection();PreparedStatement ps=connection.prepareStatement(sql)){
            ConfigRepository configRepository=new ConfigRepository();
            ps.setString(1,"pending");
            ps.setInt(2,0);
            ps.setInt(3,configRepository.getMaxRetries());
            ps.setNull(4, Types.VARCHAR);
            ps.setString(5,Instant.now().toString());
            ps.setNull(6,Types.VARCHAR);
            ps.setString(7,jobId);
            int executed= ps.executeUpdate();
            if(executed==1){
                System.out.println("Successfully updated to pending");
            } else if (executed==0) {
                System.out.println("provided job is not dead");

            } else{
                throw new SQLException("Something happened");
            }
        }
    }

    public Map<String,Integer> jobStatus(){
        String sql="SELECT state, COUNT(*) AS count FROM jobs GROUP BY state";
        try(Connection connection =DatabaseManager.getConnection(); PreparedStatement ps =connection.prepareStatement(sql);ResultSet rs =ps.executeQuery();) {
            Map<String, Integer> jobStatus = new HashMap<>();

            while (rs.next()) {
                jobStatus.put(rs.getString("state"), rs.getInt("count"));
            }
            return  jobStatus;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}


