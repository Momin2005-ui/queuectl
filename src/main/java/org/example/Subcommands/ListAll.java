package org.example.Subcommands;

import org.example.Repository.JobRepository;
import picocli.CommandLine;

import java.sql.ResultSet;
import java.sql.SQLException;

@CommandLine.Command(name = "listAll")
public class ListAll implements Runnable {

    @Override
    public void run() {
        JobRepository jobRepository=new JobRepository();
        try {
            ResultSet rs =jobRepository.listAll();

            while (rs.next()) {

                System.out.println(rs.getString("id"));
                System.out.println(rs.getString("command"));
                System.out.println(rs.getString("state"));
                System.out.println(rs.getInt("attempts"));
                System.out.println(rs.getInt("maxRetries"));
                System.out.println(rs.getString("workerId"));
                System.out.println(rs.getString("createdAt"));
                System.out.println(rs.getString("updatedAt"));

                System.out.println("----------------");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
