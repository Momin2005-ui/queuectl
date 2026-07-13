package org.example.cli;

import org.example.Subcommands.Enqueue;
import org.example.Subcommands.ListAll;
import org.example.Subcommands.ListByState;
import picocli.CommandLine;

@CommandLine.Command(name = "queuecli" , subcommands = {Enqueue.class, ListAll.class, ListByState.class, Worker.class, Config.class,DeadLetterQueue.class, Status.class},mixinStandardHelpOptions = true)
public class Queuecli implements Runnable {


    @Override
    public void run() {
        System.out.println("Write the subcommand");

    }
}
