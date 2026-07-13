package org.example.Subcommands;

import org.example.Repository.WorkerRepository;
import picocli.CommandLine;

import java.sql.SQLException;

@CommandLine.Command(name = "stop")
public class WorkerStop implements Runnable{
    @Override
    public void run() {
        WorkerRepository workerRepository= new WorkerRepository();
        try {
            workerRepository.markWorkerStopAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
