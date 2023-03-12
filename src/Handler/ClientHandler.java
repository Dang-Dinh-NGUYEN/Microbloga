package Handler;

import Database.MicroblogDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    protected static BufferedReader in;
    protected static PrintWriter out;
    protected static String user;

    public ClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    @Override
    public void run() {
        //reading username and verify user's profile on database
        try {
            user = in.readLine();
            if (MicroblogDatabase.Authentification(user)) {
                System.out.println("CONNECT user: " + user);
                PublisherHandler publisherHandler = new PublisherHandler(clientSocket);

                //sending notification and a list of operations to client
                out.println("OK");
                while (clientSocket.isConnected()) {
                    while (true) {
                        out.println("Select an action: PUBLISH | RCV_IDS [author:@user] [tag:#tag] [since_id:id] [limit:n] | RCV_MSG msg_id:id");
                        String choice = in.readLine();
                        if (choice == null) break;
                        if (choice.equals("PUBLISH")) {
                            publisherHandler.run();
                        } else if (choice.startsWith("RCV_IDS")) {
                            publisherHandler.MSG_ID(choice);
                        } else if (choice.startsWith("RCV_MSG msg_id:")) {
                            String id = choice.substring(choice.indexOf(':') + 1);
                            publisherHandler.MSG(id);
                        } else {
                            out.println("ERREUR");
                            break;
                        }
                    }
                }
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.close();
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
