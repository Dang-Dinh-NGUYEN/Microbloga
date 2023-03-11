import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public final static int PORT = 12345;
    public final static String SERVER = "localhost";

    public static void main(String[] args) throws Exception {
        ExecutorService executorService = Executors.newWorkStealingPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("WELCOME TO MICROBLOGAMU");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket));
            }
        }
    }
}