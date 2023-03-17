package Handler;

import Clients.Publisher;
import Database.MicroblogDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public void PUBLISH_REPONSE() {
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
            if (message.equals("exit")) break;
            if (message.startsWith("#")) tag = message;

            body.append(message);
        }
        System.out.println(header);
        System.out.println(body.toString());
        String formattedDateTime = LocalDateTime.now().
                format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println(formattedDateTime);
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
            //throwable.printStackTrace();
            System.out.println("ERROR:PUBLISH FAILED");
        }
        out.println("OK");
    }

    public void MSG_ID(String opts) throws SQLException, ClassNotFoundException {
        String header = "MSG_IDS";
        out.println(header);

        int firstSpaceIndex = opts.indexOf(' ');
        String optionString = opts.substring(firstSpaceIndex + 1);
        String[] options = optionString.split(" ");
        int nbOpt = options.length;

        String user = null, tag = null, id = null, limit = "5";
        if (nbOpt > 0 && nbOpt <= 4) {
            for (int i = 0; i < nbOpt; i++) {
                if (options[i].startsWith("author:")) {
                    user = options[i].substring(options[i].indexOf(':') + 1);
                }
                if (options[i].startsWith("tag:")) {
                    tag = options[i].substring(options[i].indexOf(':') + 1);
                }
                if (options[i].startsWith("since_id:")) {
                    id = options[i].substring(options[i].indexOf(':') + 1);
                }
                if (options[i].startsWith("limit:")) {
                    limit = options[i].substring(options[i].indexOf(':') + 1);
                }
            }
        }

        String sql = "SELECT * FROM messages ";
        StringBuilder queryBuilder = new StringBuilder(sql);

        if (user != null || tag != null || id != null) queryBuilder.append("WHERE ");
        if (user != null) queryBuilder.append("username = ?");

        if (tag != null) {
            if (user != null) queryBuilder.append(" AND ");
            queryBuilder.append("id IN (SELECT message_id FROM message_tags WHERE tag_name = ?)");
        }

        if (id != null) {
            if (user != null || tag != null) queryBuilder.append(" AND ");
            queryBuilder.append("id > ?");
        }

        queryBuilder.append(" ORDER BY id DESC LIMIT ?");

        PreparedStatement stmt = MicroblogDatabase.conn.prepareStatement(queryBuilder.toString());
        int paramIndex = 1;
        if (queryBuilder.toString().contains("username = ?")) stmt.setString(paramIndex++, user);
        if (queryBuilder.toString().contains("id IN (SELECT message_id FROM message_tags WHERE tag_name = ?)")) stmt.setString(paramIndex++, tag);
        if (queryBuilder.toString().contains("id > ?")) stmt.setInt(paramIndex++, Integer.parseInt(id));
        stmt.setInt(paramIndex++, Integer.parseInt(limit));
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            // Get message ID
            int messageId = rs.getInt("id");

            // Check if author option was specified
            if (user != null) {
                String author = rs.getString("username");
                if (!author.equals(user))
                    // Skip message if it doesn't match author option
                    continue;
            }

            // Check if since_id option was specified
            if (id != null) {
                int messageSinceId = rs.getInt("id");
                if (messageSinceId <= Integer.parseInt(id))
                    // Skip message if it was published before since_id option
                    continue;
            }

            // Check if limit has been reached
            if (Integer.parseInt(limit) - 1 <= 0) break;

            out.println(">>" + messageId);
            PreparedStatement pstmt = MicroblogDatabase.conn.prepareStatement("SELECT * FROM messages WHERE id = ?");
            pstmt.setInt(1, messageId);
            ResultSet prs = pstmt.executeQuery();
            while (prs.next()) {
                out.println(">>" +prs.getString("header"));
                out.println(">>" +prs.getString("content"));
                out.println(">>" +prs.getString("timestamp"));
                out.println();
            }
            prs.close();
            pstmt.close();
        }
        rs.close();
        stmt.close();
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
                //while (clientSocket.isConnected()) {
                    while (true) {
                        //out.println("Select an action: PUBLISH | RCV_IDS [author:@user] [tag:#tag] [since_id:id] [limit:n] | RCV_MSG msg_id:id");
                        String cmd = in.readLine();
                        if (cmd.isBlank() || cmd.isEmpty() || cmd.equals("\r\n") || cmd.equals("\t") || cmd.equals("")) {
                            System.out.println("nullpoint");
                            break;
                        }
                        if (cmd.equals("PUBLISH")) {
                            PUBLISH_REPONSE();
                        } else if (cmd.startsWith("RCV_IDS")) {
                            System.out.println(cmd);
                            String header = "MSG_IDS";
                            out.println(header);

                            int firstSpaceIndex = cmd.indexOf(' ');
                            String optionString = cmd.substring(firstSpaceIndex + 1);
                            String[] options = optionString.split(" ");
                            int nbOpt = options.length;

                            String user = null, tag = null, id = null, limit = "5";
                            if (nbOpt > 0 && nbOpt <= 4) {
                                for (int i = 0; i < nbOpt; i++) {
                                    if (options[i].startsWith("author:")) {
                                        user = options[i].substring(options[i].indexOf(':') + 1);
                                    }
                                    if (options[i].startsWith("tag:")) {
                                        tag = options[i].substring(options[i].indexOf(':') + 1);
                                    }
                                    if (options[i].startsWith("since_id:")) {
                                        id = options[i].substring(options[i].indexOf(':') + 1);
                                    }
                                    if (options[i].startsWith("limit:")) {
                                        limit = options[i].substring(options[i].indexOf(':') + 1);
                                    }
                                }
                            }

                            String sql = "SELECT * FROM messages ";
                            StringBuilder queryBuilder = new StringBuilder(sql);

                            if (user != null || tag != null || id != null) queryBuilder.append("WHERE ");
                            if (user != null) queryBuilder.append("username = ?");

                            if (tag != null) {
                                if (user != null) queryBuilder.append(" AND ");
                                queryBuilder.append("id IN (SELECT message_id FROM message_tags WHERE tag_name = ?)");
                            }

                            if (id != null) {
                                if (user != null || tag != null) queryBuilder.append(" AND ");
                                queryBuilder.append("id > ?");
                            }

                            queryBuilder.append(" ORDER BY id DESC LIMIT ?");

                            PreparedStatement stmt = MicroblogDatabase.conn.prepareStatement(queryBuilder.toString());
                            int paramIndex = 1;
                            if (queryBuilder.toString().contains("username = ?")) stmt.setString(paramIndex++, user);
                            if (queryBuilder.toString().contains("id IN (SELECT message_id FROM message_tags WHERE tag_name = ?)")) stmt.setString(paramIndex++, tag);
                            if (queryBuilder.toString().contains("id > ?")) stmt.setInt(paramIndex++, Integer.parseInt(id));
                            stmt.setInt(paramIndex++, Integer.parseInt(limit));
                            ResultSet rs = stmt.executeQuery();

                            while (rs.next()) {
                                // Get message ID
                                int messageId = rs.getInt("id");

                                // Check if author option was specified
                                if (user != null) {
                                    String author = rs.getString("username");
                                    if (!author.equals(user))
                                        // Skip message if it doesn't match author option
                                        continue;
                                }

                                // Check if since_id option was specified
                                if (id != null) {
                                    int messageSinceId = rs.getInt("id");
                                    if (messageSinceId <= Integer.parseInt(id))
                                        // Skip message if it was published before since_id option
                                        continue;
                                }

                                // Check if limit has been reached
                                if (Integer.parseInt(limit) - 1 <= 0) break;

                                out.println(">>" + messageId);
                                PreparedStatement pstmt = MicroblogDatabase.conn.prepareStatement("SELECT * FROM messages WHERE id = ?");
                                pstmt.setInt(1, messageId);
                                ResultSet prs = pstmt.executeQuery();
                                while (prs.next()) {
                                    out.println(">>" +prs.getString("header"));
                                    out.println(">>" +prs.getString("content"));
                                    out.println(">>" +prs.getString("timestamp"));
                                    out.println();
                                }
                                prs.close();
                                pstmt.close();
                            }
                            rs.close();
                            stmt.close();

                        } else if (cmd.startsWith("RCV_MSG msg_id:")) {
                            String id = cmd.substring(cmd.indexOf(":") + 1);
                            String header = "MSG";
                            out.println(header);

                            String sql = "SELECT * FROM messages WHERE id = ?";

                            PreparedStatement stmt = MicroblogDatabase.conn.prepareStatement(sql);
                            stmt.setInt(1, Integer.parseInt(id));
                            ResultSet rs = stmt.executeQuery();
                            if(!rs.next()) {
                                out.println("ERREUR: MESSAGE DOES NOT EXIST");
                                rs.close();
                                stmt.close();
                                return;
                            }

                            while (rs.next()) {
                                int messageId = rs.getInt("id");
                                out.println(">>" + messageId);
                                out.println(">>" +rs.getString("header"));
                                out.println(">>" +rs.getString("content"));
                                out.println(">>" +rs.getString("timestamp"));
                                out.println();
                            }
                            rs.close();
                            stmt.close();

                        } else {
                            //out.println("ERREUR");
                            break;
                        }

                    }
                }
            //}
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
