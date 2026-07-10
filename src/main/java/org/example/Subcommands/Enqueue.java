package org.example.Subcommands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Repository.JobRepository;
import org.example.model.Job;
import org.example.model.State;
import picocli.CommandLine;

import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "enqueue")
public class Enqueue implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "Job JSON")
    private String jobJson;

    @Override
    public Integer call() {

        ObjectMapper objectMapper = new ObjectMapper();
        JobRepository jobRepository = new JobRepository();

        try {
            Job job = objectMapper.readValue(jobJson, Job.class);

            job.setAttempts(0);
            job.setState(State.PENDING);
            job.setMaxRetries(3);
            job.setCreatedAt(Instant.now());
            job.setUpdatedAt(Instant.now());

            jobRepository.insert(job);

            System.out.println("Job enqueued successfully.");

            return 0; // Success

        } catch (JsonProcessingException e) {

            System.err.println("Invalid JSON: " + e.getMessage());
            return 1;

        } catch (SQLException e) {

            System.err.println("Database error: " + e.getMessage());
            return 2;

        } catch (Exception e) {

            System.err.println("Unexpected error: " + e.getMessage());
            return 3;
        }
    }
}