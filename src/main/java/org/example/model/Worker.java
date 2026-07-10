package org.example.model;

import java.time.Instant;

public class Worker {
    long Pid;
    StateWorker stateWorker;
    Instant startedAt;

    public long getPid() {
        return Pid;
    }

    public void setPid(long pid) {
        Pid = pid;
    }

    public StateWorker getStateWorker() {
        return stateWorker;
    }

    public void setStateWorker(StateWorker stateWorker) {
        this.stateWorker = stateWorker;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
}
