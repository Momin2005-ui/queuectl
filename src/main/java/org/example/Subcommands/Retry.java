package org.example.Subcommands;

import org.example.Repository.JobRepository;
import picocli.CommandLine;

import java.sql.SQLException;

@CommandLine.Command(name = "retry")
public class Retry implements Runnable{

    @CommandLine.Parameters(index = "0")
    String jobId;

    JobRepository jobRepository =new JobRepository();

    @Override
    public void run() {
        try {
            jobRepository.retryDeadJob(jobId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
