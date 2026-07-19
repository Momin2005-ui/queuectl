package org.example.Repository;

import org.example.db.DatabaseManager;
import org.example.model.Job;
import org.example.model.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JobRepositoryTest {

    private JobRepository repository;

    @BeforeEach
    void setup() {
        DatabaseManager.setUrl("jdbc:sqlite:queuectl.dbTest");
        DatabaseManager.initialize();
        repository = new JobRepository();
    }

    @Test
    void shouldInsertJobSuccessfully() throws SQLException {

        Job job = new Job();

        job.setId("1");
        job.setCommand("echo Hello");
        job.setState(State.PENDING);
        job.setAttempts(0);
        job.setMaxRetries(3);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());

        repository.insert(job);

        Job savedJob = repository.findById("1");

        assertNotNull(savedJob);
        assertEquals("1", savedJob.getId());
        assertEquals("echo Hello", savedJob.getCommand());
        assertEquals(State.PENDING, savedJob.getState());
        assertEquals(0, savedJob.getAttempts());
        assertEquals(3, savedJob.getMaxRetries());
    }
}
