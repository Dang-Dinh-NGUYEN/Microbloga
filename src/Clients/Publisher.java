package Clients;

import Clients.Client;

import java.io.*;
import java.sql.SQLException;

public class Publisher extends Client {

    public Publisher() throws SQLException, ClassNotFoundException {}

    public void publish() throws IOException, SQLException, ClassNotFoundException {
        System.out.println("Enter your messages (type '$' to stop) : ");
        String body = "", reponse = "";
        do {
            body = br.readLine();
            out.println(body);
            out.flush();
        } while (!body.equals("$"));
        reponse = in.readLine();
        System.out.println(">>" + reponse);
    }
}
