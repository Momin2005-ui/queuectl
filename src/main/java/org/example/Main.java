package org.example;

import org.example.cli.Queuecli;
import org.example.db.DatabaseManager;
import picocli.CommandLine;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
//        System.out.println("Hello cli");
        DatabaseManager.initialize();
        int exitCode = new CommandLine(new Queuecli()).execute(args);
//        System.out.println(exitCode);


    }


}