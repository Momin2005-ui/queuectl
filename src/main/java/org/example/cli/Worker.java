package org.example.cli;

import org.example.Subcommands.WorkerStart;
import picocli.CommandLine;

@CommandLine.Command(name = "worker",subcommands = {WorkerStart.class})
public class Worker {
}
