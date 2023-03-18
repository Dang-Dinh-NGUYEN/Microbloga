package Clients;

import Database.MicroblogDatabase;
import Server.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class Client {
    protected static String pseudo;
    protected static BufferedReader in;
    protected static PrintWriter out;
    protected static BufferedReader br;
    private static Scanner scanner = new Scanner(System.in);

    public Client() throws SQLException, ClassNotFoundException {}

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        Socket s = new Socket(Server.SERVER, Server.PORT);
        System.out.print("Enter username: ");
        pseudo = scanner.nextLine();

        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
        br = new BufferedReader(new InputStreamReader(System.in));

        //sending request to server
        String header = pseudo;
        out.println(header);

        //Wait for a response from the server
        String reponse = in.readLine();
        System.out.println(">> " + reponse);

        // If the authentication was successful, display the available operations and prompt the user to choose one
        if(reponse.equals("OK")) {
            while (true) {
                //System.out.println(in.readLine());
                System.out.println("Enter the operation you want to perform:");
                // Prompt the user to choose an operation
                String choice = br.readLine();

                // Send the chosen operation to the server and wait for the result
                out.println(choice);

                if (choice.equals("PUBLISH")) {
                    Publisher publisher = new Publisher();
                    header = "PUBLISH author: " + pseudo;
                    out.println(header);
                    publisher.publish();
                }
                if (choice.startsWith("RCV_IDS") ||choice.startsWith("RCV_MSG")) {
                    new Follower().recevoir();
                }
                if (choice.startsWith("REPLY")) {
                    Repost repost = new Repost();
                    header = "REPLY author:" + pseudo + " reply_to_id:" + choice.substring(choice.indexOf(":") + 1) ;
                    out.println(header);
                    repost.reply();
                }
                if (choice.startsWith("REPUBLISH")) {
                    Repost repost = new Repost();
                    header = "REPUBLISH author:" + pseudo + " msg_id:" + choice.substring(choice.indexOf(":") + 1) ;
                    out.println(header);
                    repost.republish();
                }
            }
        }
        s.close();
    }
}
