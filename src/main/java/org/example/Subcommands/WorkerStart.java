package org.example.Subcommands;

import picocli.CommandLine;

@CommandLine.Command(name = "start")
public class WorkerStart implements Runnable{

    @CommandLine.Option(names = {"--count"})
    int count;

    @Override
    public void run() {

    }
}
