package org.example.Repository;

import org.example.db.DatabaseManager;
import org.example.model.Job;
import org.example.model.State;
import org.example.model.StateWorker;
import org.example.model.Worker;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

            ExecutorService executorService = Executors.newFixedThreadPool(workers.size());

            for(Worker w : workers){
                  Runnable workerTask = new Runnable() {
                      @Override
                      public void run() {
                          while(isWorkerRunning(w.getPid())){
                                 try{
                                     Job job =claimNextJob(w.getPid());

                                     if(job!=null){
                                         System.out.println(w.getPid()+"claimed Job"+job.getId());
                                         executeJob(job);
                                     }
                                     else{
                                         Thread.sleep(500);
                                     }
                                 }catch (InterruptedException e){
                                     Thread.currentThread().interrupt();
                                     break;
                                 } catch (Exception e) {
                                     System.err.println("Worker"+w.getPid()+"failed"+e.getMessage());
                                 }
                          }
                          System.out.println("worker"+w.getPid()+"stopped");

                      }
                  };
                  executorService.submit(workerTask);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public List<Worker> createWorkers(int count){
        Connection connection = null;
        try{
            List<Worker> workers= new ArrayList<>();
            connection= DatabaseManager.getConnection();
            String sql = "INSERT INTO workers(pid,stateWorker,startedAt) VALUES(?,?,?)";
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
        ps.setString(1,state);
        return ps.executeQuery();

    }

    public Job claimNextJob(String workerId){

        try {
            Connection connection=DatabaseManager.getConnection();
            String sql ="UPDATE jobs SET state='processing',workerId=?,updatedAt=? WHERE id=(SELECT id FROM jobs WHERE state='pending' OR (state='failed' AND nextRetry<=?) ORDER BY createdAt LIMIT 1) AND (state='pending' OR (state='failed' AND nextRetryAt <=?))";
            PreparedStatement ps=connection.prepareStatement(sql);
            String now =Instant.now().toString();
            ps.setString(1,workerId);
            ps.setString(2,now);
            ps.setString(3,now);
            ps.setString(4,now);
            int code =ps.executeUpdate(); // rows Affected
            if(code==1){
                return fetchJob(workerId);
            }
            else{
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isWorkerRunning(String workerId){
        try {
            Connection connection=DatabaseManager.getConnection();
            String sql = "SELECT stateWorker from worker WHERE Pid=?";
            PreparedStatement ps=connection.prepareStatement(sql);
            ps.setString(1, workerId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String state = rs.getString("stateWorker");
                return state.equalsIgnoreCase("running");
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void executeJob(Job job) throws IOException, InterruptedException, SQLException {

        ProcessBuilder processBuilder;

        if(System.getProperty("os.name").toLowerCase().contains("win")){
            processBuilder=new ProcessBuilder("cmd.exe","/c",job.getCommand());
        }
        else{
            processBuilder=new ProcessBuilder("bash","-c", job.getCommand());
        }

        processBuilder.inheritIO();

        Process process =processBuilder.start();

        int exitCode= process.waitFor();

        if(exitCode==0){
            markCompleted(job);
        }
        else{
            exponentialBackoff(job);
        }

    }

    public void markCompleted(Job job) throws SQLException {
        Connection connection = DatabaseManager.getConnection();
        String sql = "UPDATE jobs SET state = 'completed', updatedAt = ? WHERE id = ? AND state = 'processing'";
        PreparedStatement  ps = connection.prepareStatement(sql);
        ps.setString(1, Instant.now().toString());
        ps.setString(2, job.getId());
        ps.executeUpdate();
    }

    public void exponentialBackoff(Job job) throws SQLException {
        ConfigRepository configRepository = new ConfigRepository();

        int attempts = job.getAttempts() + 1;
        Instant now = Instant.now();

        try (Connection connection = DatabaseManager.getConnection()) {

            if (attempts >= configRepository.getMaxRetries()) {

                String sql = """
                UPDATE jobs
                SET state = 'dead',
                    attempts = ?,
                    updatedAt = ?,
                    nextRetry = NULL
                WHERE id = ?
                  AND state = 'processing'
                """;

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, attempts);
                    ps.setString(2, now.toString());
                    ps.setString(3, job.getId());
                    ps.executeUpdate();
                }

            } else {
                int base = configRepository.getBackOffBase();
                long delay = (long) Math.pow(base, attempts);
                Instant nextRetry = now.plusSeconds(delay);

                String sql = """
                UPDATE jobs
                SET state = 'failed',
                    attempts = ?,
                    updatedAt = ?,
                    nextRetry = ?
                WHERE id = ?
                  AND state = 'processing'
                """;

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, attempts);
                    ps.setString(2, now.toString());
                    ps.setString(3, nextRetry.toString());
                    ps.setString(4, job.getId());
                    ps.executeUpdate();
                }
            }
        }
    }

    public Job fetchJob(String workerId) throws SQLException {
        Connection connection=DatabaseManager.getConnection();
        String sql = "SELECT * FROM jobs WHERE workerId=? AND state='processing'";
        PreparedStatement preparedStatement= connection.prepareStatement(sql);
        preparedStatement.setString(1,workerId);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
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

            return job;
        }

        return null;
    }

}
