package org.example.Subcommands;

import org.example.Helper.TimeHelper;
import org.example.Repository.JobRepository;
import org.example.model.Job;
import picocli.CommandLine;

import java.sql.SQLException;
import java.util.List;

@CommandLine.Command(name="list")
public class DlqList implements Runnable{
    JobRepository jobRepository=new JobRepository();
    @Override
    public void run() {
        try{
            List<Job> deadJobs = jobRepository.listDead();
            if(deadJobs.size()==0){
                System.out.println("There is no dead Jobs");
            }
            else {
                for(Job j : deadJobs){
                    System.out.println(j.getId());
                    System.out.println(j.getCommand());
                    System.out.println(j.getState());
                    System.out.println(j.getWorkerId());
                    System.out.println(j.getMaxRetries());
                    System.out.println(TimeHelper.format(j.getCreatedAt()));
                    System.out.println(TimeHelper.format(j.getUpdatedAt()));
                    System.out.println(TimeHelper.format(j.getNextRetry()));
                    System.out.println("---------------------------------------------------------");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
