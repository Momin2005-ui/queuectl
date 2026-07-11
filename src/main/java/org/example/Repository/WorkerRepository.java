package org.example.Repository;

import org.example.db.DatabaseManager;
import org.example.model.StateWorker;
import org.example.model.Worker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkerRepository {

    public void start(int count){
        // 1. Start/register the requested workers

        // 2. Keep workers running continuously
        //    They must NOT exit just because there are currently no jobs

        // 3. Atomically claim a pending/retry-ready job
        //    Prevent two workers/processes from claiming the same job

        // 4. Mark job as processing and record enough information
        //    to recover it if the worker crashes

        // 5. Execute the command through the shell

        // 6. Exit code 0 -> completed
        //    Non-zero -> failed -> backoff -> retry or dead

        // 7. If no eligible job exists -> wait briefly, then poll again

        // 8. Graceful stop / SIGINT / SIGTERM:
        //    finish current job, then worker exits

        // 9. SIGKILL/crash:
        //    no cleanup code runs; another process must detect the stale
        //    processing job and recover it within 60 seconds
        try {
            List<Worker> workers = createWorkers(count);

            

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public List<Worker> createWorkers(int count){
        Connection connection = null;
        try{
            List<Worker> workers= new ArrayList<>();
            connection= DatabaseManager.getConnection();
            String sql = "INSERT INTO workers(pid,state,startedAt) VALUES(?,?,?)";
            PreparedStatement ps =connection.prepareStatement(sql);
            connection.setAutoCommit(false);
            for(int i=0;i<count;i++){
                String id = UUID.randomUUID().toString();
                Instant startedAt = Instant.now();

                Worker worker =new Worker();
                worker.setPid(id);
                worker.setStateWorker(StateWorker.RUNNING);
                worker.setStartedAt(startedAt);
                ps.setString(1,id);
                ps.setString(2,"running");
                ps.setString(3, startedAt.toString());
                ps.executeUpdate();
                workers.add(worker);
            }
            connection.commit();
            connection.close();
            ps.close();
            return workers;
        } catch (Exception e) {
            if(connection!=null){
                try{
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    throw new RuntimeException(rollbackException);
                }
            }
            throw new RuntimeException(e);
        }finally {
            if(connection!=null){
                try{
                    connection.close();
                }catch (SQLException e){
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public ResultSet listByState(String state) throws SQLException {
        Connection connection =DatabaseManager.getConnection();
        String sql = "SELECT * FROM workers WHERE state = ?";
        PreparedStatement ps=connection.prepareStatement(sql);
        return ps.executeQuery();

    }

    public int claimNextJob(String jobId){

        try {
            Connection connection=DatabaseManager.getConnection();
            String sql ="UPDATE jobs SET state='processing',jobId=?,updatedAt=? WHERE id=(SELECT id FROM jobs WHERE state='pending' ORDER BY createdAt LIMIT 1) AND state='pending'";
            PreparedStatement ps=connection.prepareStatement(sql);
            ps.setString(1,jobId);
            ps.setString(2,Instant.now().toString());
            int code =ps.executeUpdate(); // rows Affected
            return code;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
