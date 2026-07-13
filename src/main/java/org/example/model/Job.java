package org.example.model;

import java.time.Instant;


public class Job {
    private String id;
    private String command;
    private State state;
    private int attempts;
    private int maxRetries;
    private String workerId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant nextRetry;
    private Instant lastHeartBeat;

    public Instant getLastHeartBeat() {
        return lastHeartBeat;
    }

    public void setLastHeartBeat(Instant lastHeartBeat) {
        this.lastHeartBeat = lastHeartBeat;
    }

    public Instant getNextRetry() {
        return nextRetry;
    }

    public void setNextRetry(Instant nextRetry) {
        this.nextRetry = nextRetry;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

}
