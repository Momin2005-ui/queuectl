package org.example.Subcommands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.Helper.TimeHelper;
import org.example.Repository.JobRepository;
import org.example.model.Job;
import org.example.model.State;
import picocli.CommandLine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@CommandLine.Command(name = "list")
public class ListByState implements Runnable {

    @CommandLine.Option(names = "--state",required = true)
    String state;

    @CommandLine.Option(names = "--json")
    boolean json;

    JobRepository jobRepository =new JobRepository();
    @Override
    public void run() {
        try {
            List<Job> jobsByState =jobRepository.listByState(state);

            if(json){
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                System.out.println(mapper.writeValueAsString(jobsByState));
                return;
            }
            if(jobsByState.size()==0){
                System.out.println("There are no jobs for the "+state+" state");
                return;
            }
            for (Job job : jobsByState) {
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
