package org.example.cli;

import org.example.Subcommands.WorkerStart;
import org.example.Subcommands.WorkerStop;
import picocli.CommandLine;

@CommandLine.Command(name = "worker",subcommands = {WorkerStart.class, WorkerStop.class})
public class Worker implements Runnable{
    @Override
    public void run() {

    }
}
