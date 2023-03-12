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

        if(MicroblogDatabase.Authentification(pseudo)) {
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
            if(reponse.equals("OK")){
                System.out.println(in.readLine());

                // Prompt the user to choose an operation
                BufferedReader selector = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter the operation you want to perform:");
                String choice = selector.readLine();

                // Send the chosen operation to the server and wait for the result
                out.println(choice);
                if(choice.equals("PUBLISH")){
                    Publisher publisher = new Publisher();
                    publisher.execute();
                }else if(choice.startsWith("RCV_IDS")){
                    MicroblogDatabase.selectMessages(choice);
                }else if(choice.startsWith("RCV_MSG")){

                }

            }
        }
        s.close();
    }
}
