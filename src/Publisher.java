import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Scanner;

public class Publisher {
    private String pseudo;
    private Scanner scanner = new Scanner(System.in);

    public Publisher() throws SQLException, ClassNotFoundException {
        System.out.print("Enter username: ");
        this.pseudo = scanner.nextLine();
        //System.out.println(MicroblogDatabase.authentification(pseudo));
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        String server = "localhost";
        int port = 12345;

        Socket s = new Socket(server, port);
        Publisher publisher = new Publisher();

        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String header = "PUBLISH author: " + publisher.pseudo;
        out.println(header);

        System.out.print("Enter your messages : ");
        String body = "", reponse = "";
        while (true) {
            body = br.readLine();
            out.println(body);
            out.flush();
            reponse = in.readLine();
            System.out.println(">>" + reponse);
        }
    }
}
