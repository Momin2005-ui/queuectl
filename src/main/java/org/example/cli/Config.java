package org.example.cli;

import org.example.Subcommands.Set;
import picocli.CommandLine;

@CommandLine.Command(name = "config" ,subcommands = {Set.class})
public class Config implements Runnable {
    @Override
    public void run() {

    }
}
