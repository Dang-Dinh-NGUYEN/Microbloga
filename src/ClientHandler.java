import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
            inputLine = in.readLine();
            if(MicroblogDatabase.authentification(inputLine)){
            System.out.println("CONNECT user: " + inputLine);

            //sending notification and a list of operations to client
            out.println("OK");
            out.println("Select an action: PUBLISH");

            while(true) {
                String choice = in.readLine();
                if (choice == null) {
                    break;
                }
                //System.out.println(choice);
            }
            in.close();
            out.close();
            clientSocket.close();

        }else{
                System.out.println("authentification failed");
            }
    }catch (Exception exception){
            exception.printStackTrace();
        }
    }
}
