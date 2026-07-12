package org.example.Subcommands;

import org.example.Repository.WorkerRepository;
import picocli.CommandLine;

@CommandLine.Command(name = "start")
public class WorkerStart implements Runnable{

    @CommandLine.Option(names = {"--count"})
    int count;

    @Override
    public void run() {
        try{
            WorkerRepository workerRepository=new WorkerRepository();
            workerRepository.start(count);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
}
