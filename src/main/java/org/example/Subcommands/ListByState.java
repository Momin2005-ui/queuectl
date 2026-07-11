package org.example.Subcommands;

import org.example.Repository.JobRepository;
import org.example.model.State;
import picocli.CommandLine;

import java.sql.ResultSet;
import java.sql.SQLException;

@CommandLine.Command(name = "list")
public class ListByState implements Runnable {

    @CommandLine.Option(names = "--state",required = true)
    String state;

    JobRepository jobRepository =new JobRepository();
    @Override
    public void run() {
        try {
            ResultSet rs =jobRepository.listByState(state);
            while (rs.next()) {

                System.out.println(rs.getString("id"));
                System.out.println(rs.getString("command"));
                System.out.println(rs.getString("state"));
                System.out.println(rs.getInt("attempts"));
                System.out.println(rs.getInt("maxRetries"));
                System.out.println(rs.getString("createdAt"));
                System.out.println(rs.getString("updatedAt"));

                System.out.println("----------------");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
