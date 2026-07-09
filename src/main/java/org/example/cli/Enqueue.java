package org.example.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Job;
import org.example.model.State;
import picocli.CommandLine;

import java.time.Instant;
import java.util.Date;

@CommandLine.Command(name = "enqueue ")
public class Enqueue implements Runnable  {

    @CommandLine.Parameters(description = "Job JSON",index = "0")
    private String jobJson;

    @Override
    public void run() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Job job = objectMapper.readValue(jobJson, Job.class);
            job.setAttempts(3);
            job.setState(State.PENDING);
            job.setMax_retries(3);
            job.setCreatedAt(Instant.now());
            job.setUpdatedAt(Instant.now());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
