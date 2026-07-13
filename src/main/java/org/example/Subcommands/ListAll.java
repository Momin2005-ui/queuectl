package org.example.Subcommands;

import org.example.Helper.TimeHelper;
import org.example.Repository.JobRepository;
import org.example.model.Job;
import picocli.CommandLine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@CommandLine.Command(name = "listAll")
public class ListAll implements Runnable {

    @Override
    public void run() {
        JobRepository jobRepository=new JobRepository();
        try {
            List<Job> jobs = jobRepository.listAll();

            for (Job job : jobs) {
                System.out.println(job.getId());
                System.out.println(job.getCommand());
                System.out.println(job.getState());
                System.out.println(job.getWorkerId());
                System.out.println(job.getMaxRetries());
                System.out.println(TimeHelper.format(job.getCreatedAt()));
                System.out.println(TimeHelper.format(job.getUpdatedAt()));
                System.out.println(TimeHelper.format(job.getNextRetry()));
                System.out.println("---------------------------------------------------------");

            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
