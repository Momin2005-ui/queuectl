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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkerRepository {

    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private static final long STALE_SECONDS = 30;
    public void start(int count) {
        try {
            List<Worker> workers = createWorkers(count);

            ExecutorService executorService =
                    Executors.newFixedThreadPool(workers.size());

            // Register once for this worker-start process
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

                System.err.println(
                        "Shutdown requested. Finishing current jobs..."
                );

                // Prevent workers from claiming another job
                shutdownRequested.set(true);

                // No new worker tasks will be submitted
                executorService.shutdown();

                try {
                    // Wait for currently running worker tasks to exit
                    executorService.awaitTermination(
                            Long.MAX_VALUE,
                            TimeUnit.SECONDS
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));

            for (Worker w : workers) {

                Runnable workerTask = () -> {

                    // Check BOTH Ctrl+C flag and DB worker state
                    while (!shutdownRequested.get()
                            && isWorkerRunning(w.getPid())) {

                        try {
                            Job job = claimNextJob(w.getPid());

                            if (job != null) {
                                System.out.println(
                                        w.getPid() + " claimed Job " + job.getId()
                                );

                                // Current job is allowed to finish
                                executeJob(job);

                            } else {
                                Thread.sleep(500);
                            }

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;

                        } catch (Exception e) {
                            System.err.println(
                                    "Worker " + w.getPid()
                                            + " failed: " + e.getMessage()
                            );
                        }
                    }

                    // Worker is now actually finished
                    markWorkerStopped(w.getPid());

                    System.out.println(
                            "Worker " + w.getPid() + " stopped"
                    );
                };

                executorService.submit(workerTask);
            }
            executorService.shutdown();

            executorService.awaitTermination(
                    Long.MAX_VALUE,
                    TimeUnit.SECONDS);

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
        String sql = "SELECT * FROM workers WHERE state = ?";
        try(Connection connection =DatabaseManager.getConnection();PreparedStatement ps=connection.prepareStatement(sql)){
            ps.setString(1,state);
            return ps.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Job claimNextJob(String workerId){

        String sql = "UPDATE jobs SET state='processing', workerId=?, updatedAt=?, lastHeartbeat=? " +
                "WHERE id = (SELECT id FROM jobs WHERE state='pending' " +
                "OR (state='failed' AND nextRetry<=?) " +
                "OR (state='processing' AND lastHeartbeat<=?) " +
                "ORDER BY createdAt LIMIT 1) " +
                "AND (state='pending' OR (state='failed' AND nextRetry<=?) OR (state='processing' AND lastHeartbeat<=?))";

        try( Connection connection=DatabaseManager.getConnection(); PreparedStatement ps=connection.prepareStatement(sql)) {

            String now = Instant.now().toString();
            String staleCutoff = Instant.now().minusSeconds(STALE_SECONDS).toString();

            ps.setString(1, workerId);   // workerId
            ps.setString(2, now);        // updatedAt
            ps.setString(3, now);        // lastHeartbeat (fresh claim time)
            ps.setString(4, now);        // failed.nextRetry cutoff (subquery)
            ps.setString(5, staleCutoff);// processing.lastHeartbeat cutoff (subquery)
            ps.setString(6, now);        // failed.nextRetry cutoff (outer WHERE)
            ps.setString(7, staleCutoff);// processing.lastHeartbeat cutoff (outer WHERE)

            int code = ps.executeUpdate();
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
        String sql = "SELECT stateWorker from workers WHERE Pid=?";
        try( Connection connection=DatabaseManager.getConnection();PreparedStatement ps=connection.prepareStatement(sql)) {
            
            ps.setString(1, workerId);

            try(ResultSet rs = ps.executeQuery();){
                if (rs.next()) {
                    String state = rs.getString("stateWorker");
                    return state.equalsIgnoreCase("running");
                }
                return false;
            }
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
        String sql = "UPDATE jobs SET state = 'completed', updatedAt = ? WHERE id = ? AND state = 'processing'";
        try(Connection connection = DatabaseManager.getConnection(); PreparedStatement  ps = connection.prepareStatement(sql)){
            ps.setString(1, Instant.now().toString());
            ps.setString(2, job.getId());
            ps.executeUpdate();
        }
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
        String sql = "SELECT * FROM jobs WHERE workerId=? AND state='processing'";
        try(Connection connection=DatabaseManager.getConnection();PreparedStatement preparedStatement= connection.prepareStatement(sql)){

            preparedStatement.setString(1,workerId);
            try(ResultSet rs = preparedStatement.executeQuery()){


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
                    String lastHeartbeat = rs.getString("lastHeartbeat");
                    if (lastHeartbeat != null) {
                        job.setLastHeartBeat(Instant.parse(lastHeartbeat));
                    }
                    String nextRetry = rs.getString("nextRetry");
                    if (nextRetry != null) {
                        job.setNextRetry(Instant.parse(nextRetry));
                    }
                    return job;
                }
            }

            return null;
        }
    }

    public Map<String, Integer> workerStatus() throws SQLException {

        String sql = """
            SELECT stateWorker, COUNT(*) AS count
            FROM workers
            GROUP BY stateWorker
            """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            Map<String, Integer> workerStatus = new HashMap<>();

            while (rs.next()) {
                workerStatus.put(
                        rs.getString("stateWorker"),
                        rs.getInt("count")
                );
            }

            return workerStatus;
        }
    }

    public void markWorkerStopAll() throws SQLException {
        String sql ="UPDATE workers SET stateWorker=? WHERE stateWorker=?";
        try(Connection connection=DatabaseManager.getConnection(); PreparedStatement ps =connection.prepareStatement(sql)){
            ps.setString(1,"stopped");
            ps.setString(2,"running");
            ps.executeUpdate();
        }
    }

    public void markWorkerStopped(String workerId){
        String sql ="UPDATE workers SET stateWorker=? WHERE Pid =? AND stateWorker=?";
        try(Connection connection=DatabaseManager.getConnection(); PreparedStatement ps =connection.prepareStatement(sql)){
            ps.setString(1,"stopped");
            ps.setString(2,workerId);
            ps.setString(3,"running");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
