import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Scanner;

public class Client {
    private static String pseudo;
    private static Scanner scanner = new Scanner(System.in);

    public Client() throws SQLException, ClassNotFoundException {
        System.out.print("Enter username: ");
        this.pseudo = scanner.nextLine();
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        Socket s = new Socket(Server.SERVER, Server.PORT);
        Client client = new Client();

        if(MicroblogDatabase.authentification(pseudo)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            //sending request to server
            String header = client.pseudo;
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
            }
        }
        s.close();
    }
}
