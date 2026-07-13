package org.example.cli;

import org.example.Repository.JobRepository;
import org.example.Repository.WorkerRepository;
import picocli.CommandLine;

import java.sql.SQLException;
import java.util.Map;

@CommandLine.Command(name="status")
public class Status implements Runnable{
    JobRepository jobRepository=new JobRepository();
    WorkerRepository workerRepository =new WorkerRepository();
    @Override
    public void run() {
        try{
            Map<String,Integer> jobStatus = jobRepository.jobStatus();
            Map<String,Integer> workerStatus =workerRepository.workerStatus();
            System.out.println("Job Status ---------------------------------------------------");
            System.out.println("Pending: " + jobStatus.getOrDefault("pending", 0));
            System.out.println("Processing: " + jobStatus.getOrDefault("processing", 0));
            System.out.println("Completed: " + jobStatus.getOrDefault("completed", 0));
            System.out.println("Failed: " + jobStatus.getOrDefault("failed", 0));
            System.out.println("Dead: " + jobStatus.getOrDefault("dead", 0));
            System.out.println("Worker Status -------------------------------------");
            System.out.println("Running" +workerStatus.getOrDefault("running",0));
            System.out.println("Stopped"+workerStatus.getOrDefault("stopped",0));



        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
