package Clients;

import Clients.Client;

import java.io.*;
import java.sql.SQLException;

public class Publisher extends Client {

    public Publisher() throws SQLException, ClassNotFoundException {}

    public void execute() throws IOException, SQLException, ClassNotFoundException {
        String header = "PUBLISH author: " + pseudo;
        out.println(header);

        System.out.println("Enter your messages (type '\\r\\n' to stop) : ");
        String body = "", reponse = "";
        while (true) {
            body = br.readLine();
            out.println(body);
            out.flush();
            if(body.equals("\\r\\n")) break;
        }
        reponse = in.readLine();
        System.out.println(">>" + reponse);
    }
}
