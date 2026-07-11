package org.example.model;

import java.time.Instant;

public class Worker {
    String Pid;
    StateWorker stateWorker;
    Instant startedAt;

    public String getPid() {
        return Pid;
    }

    public void setPid(String pid) {
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
