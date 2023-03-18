package Clients;

import Clients.Client;
import Server.Server;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Scanner;

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


    //mode request-response
    public static void main(String args[]) throws IOException, SQLException, ClassNotFoundException {
        Socket s = new Socket(Server.SERVER, Server.PORT);
        System.out.print("Enter username: ");
        Scanner scanner = new Scanner(System.in);
        String pseudo = scanner.nextLine();

        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        //sending request to server
        String header = pseudo;
        out.println(header);

        //Wait for a response from the server
        String reponse = in.readLine();
        System.out.println(">> " + reponse);

        if(reponse.equals("OK")){
            System.out.println("Enter PUBLISH to begin:");
            String cmd = br.readLine();
            if(cmd.equals("PUBLISH")) {
                out.println(cmd);
                header = "PUBLISH author: " + pseudo;
                out.println(header);
                System.out.println("Enter your messages (type '$' to stop) : ");
                String body = "", rep = "";
                do {
                    body = br.readLine();
                    out.println(body);
                    out.flush();
                } while (!body.equals("$"));
                rep = in.readLine();
                System.out.println(">>" + rep);
            }
        }

        //closing client socket
        in.close();
        out.close();
        br.close();
        s.close();
    }

}
