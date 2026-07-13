package org.example.cli;

import org.example.Subcommands.DlqList;
import org.example.Subcommands.Retry;
import picocli.CommandLine;

@CommandLine.Command(name="dlq",subcommands = {DlqList.class, Retry.class})
public class DeadLetterQueue {
}
