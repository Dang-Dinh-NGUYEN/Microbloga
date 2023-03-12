package Handler;

import Database.MicroblogDatabase;

import java.io.IOException;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class PublisherHandler extends ClientHandler implements Runnable {

    public PublisherHandler(Socket clientSocket) throws IOException {
        super(clientSocket);
    }

    @Override
    public void run() {
        String header = null;
        try {
            header = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder body = new StringBuilder();
        String tag = null;
        while (true) {
            String message = null;
            try {
                message = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (message.equals("\\r\\n")) break;
            if (message.startsWith("#")) tag = message;

            body.append(message);
        }
        System.out.println(header);
        System.out.println(body.toString());
        String formattedDateTime = LocalDateTime.now().
                format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try {
            MicroblogDatabase.PUBLISH(user, header, body.toString());

            //if the message includes a tag (suppose that each message has only one tag) then add to database
            if(tag != null) {
                int id = MicroblogDatabase.GET_MSG_ID(user,formattedDateTime);
                if ( id != -1) {
                    MicroblogDatabase.ADD_TAG(id,tag);
                }
            }
        } catch (SQLException | ClassNotFoundException throwable) {
            throwable.printStackTrace();
        }
        out.println("OK");
    }
}
