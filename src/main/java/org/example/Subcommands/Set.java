package org.example.Subcommands;

import org.example.Repository.ConfigRepository;
import picocli.CommandLine;

@CommandLine.Command(name = "set")
public class Set implements Runnable{

    @CommandLine.Parameters(index = "0")
    String key;

    @CommandLine.Parameters(index = "1")
    int value;

    ConfigRepository configRepository=new ConfigRepository();

    @Override
    public void run() {
       switch (key){
           case "max-retries":
               configRepository.setMaxRetries(value);
               break;
           case "backoff-base":
               configRepository.setBackOffBase(value);
               break;
           default:
               throw new IllegalArgumentException("check your config key"+key);
       }
    }
}
