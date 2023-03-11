import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            //reading username and verify user's profile on database
            String inputLine;
            String user;
            user = in.readLine();
            if(MicroblogDatabase.authentification(user)){
            System.out.println("CONNECT user: " + user);

            //sending notification and a list of operations to client
            out.println("OK");
            out.println("Select an action: PUBLISH");

            while(true) {
                String choice = in.readLine();
                if (choice == null) {
                    break;
                }
                switch (choice){
                    case "PUBLISH":
                        String header = in.readLine();
                        StringBuilder body = new StringBuilder();
                        while (true) {
                            String message = in.readLine();
                            if(message.equals("\\r\\n")) break;
                            body.append(message);
                        }
                        System.out.println(header);
                        System.out.println(body.toString());
                        LocalDateTime now = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String formattedDateTime = now.format(formatter);
                        System.out.println(formattedDateTime);
                        MicroblogDatabase.PUBLISH(user,header,body.toString());
                        out.println("OK");
                        break;

                    default:
                        out.println("ERREUR");
                        break;

                }
            }
            in.close();
            out.close();
            clientSocket.close();

        }else{
                System.out.println("ERREUR"); //authentification failed
            }
    }catch (Exception exception){
            System.out.println("ERREUR");
        }
    }
}
