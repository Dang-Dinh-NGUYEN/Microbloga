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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void PUBLISH_REPLY() throws IOException, SQLException, ClassNotFoundException {
        //reading message header
        String header = null;
        header = in.readLine();

        //reading message content
        StringBuilder body = new StringBuilder();
        String tag = null;
        while (true) {
            String message = null;
            message = in.readLine();
            if (message.equals("$")) break;
            if (message.startsWith("#")) tag = message;
            body.append(message);
        }

        if(header.startsWith("REPLY")) {
            String replyToId;
            Pattern pattern = Pattern.compile("reply_to_id:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(header);
            if (matcher.find()) {
                replyToId = matcher.group(1);
            } else {
                out.println("ERREUR");
                return;
            }
            MicroblogDatabase.REPLY(user, header, body.toString(), replyToId);
        } else {
            MicroblogDatabase.PUBLISH(user, header, body.toString());
        }

        System.out.println(header);
        System.out.println(body.toString());
        String formattedDateTime = LocalDateTime.now().
                format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println(formattedDateTime);

        //if the message includes a tag (suppose that each message has only one tag) then add to database
        if(tag != null) {
            int id = MicroblogDatabase.GET_MSG_ID(user,formattedDateTime);
            if ( id != -1) {
                MicroblogDatabase.ADD_TAG(id,tag);
            }
        }

        out.flush();
        out.println("OK");
    }

    public void MSG_ID(String opts) throws SQLException, ClassNotFoundException {
        String header = ">> MSG_IDS";
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

        String query = queryBuilder.toString();
        PreparedStatement stmt = MicroblogDatabase.conn.prepareStatement(query);

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
                    continue; // Skip message if it doesn't match author option
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

            out.println(messageId);
            PreparedStatement pstmt = MicroblogDatabase.conn.prepareStatement("SELECT * FROM messages WHERE id = ?");
            pstmt.setInt(1, messageId);
            ResultSet prs = pstmt.executeQuery();
            while (prs.next()) {
                out.println(prs.getString("header"));
                out.println(prs.getString("content"));
                out.println(prs.getString("timestamp"));
                out.println();
            }
            prs.close();
            pstmt.close();
        }
        out.flush();
        rs.close();
        stmt.close();
        out.println("$"); // send a null or empty line to terminate the stream
    }

    public void RCV_MSG(String cmd) throws SQLException {
        String id = cmd.substring(cmd.indexOf(":") + 1);
        String header = ">> MSG";
        out.println(header);

        String sql = "SELECT * FROM messages WHERE id = ?";

        PreparedStatement stmt = MicroblogDatabase.conn.prepareStatement(sql);
        stmt.setInt(1, Integer.parseInt(id));
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            out.println(rs.getInt("id"));
            out.println(rs.getString("header"));
            out.println(rs.getString("content"));
            out.println(rs.getString("timestamp"));
            out.println();
        }
        out.flush();
        rs.close();
        stmt.close();
        out.println("$"); // send a null or empty line to terminate the stream
    }

    public void REPUBLISH(String cmd) throws SQLException, ClassNotFoundException, IOException {
        String header = null;
        header = in.readLine();

        String republishId;
        Pattern pattern = Pattern.compile("msg_id:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(cmd);
        if (matcher.find()) {
            republishId = matcher.group(1);

            String sql = "SELECT * FROM messages WHERE id = ?";
            PreparedStatement stmt = MicroblogDatabase.conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(republishId));
            ResultSet rs = stmt.executeQuery();

            String body = null;
            while (rs.next()) {
                header = header + " - " + rs.getString("header");
                body = rs.getString("content");

                out.println(rs.getInt("id"));
                out.println(rs.getString("header"));
                out.println(rs.getString("content"));
                out.println(rs.getString("timestamp"));
                out.println();
            }
            MicroblogDatabase.PUBLISH(user, header, body);

            out.flush();
            out.println(">> OK");
        } else {
            out.println("ERREUR");
            return;
        }
    }

    @Override
    public void run() {
        //reading username and verify user's profile on database
        try {
            user = in.readLine();
            if (MicroblogDatabase.Authentification(user)) {
                System.out.println("CONNECT user: " + user);
                //sending notification and a list of operations to client
                out.println("OK");
                while (clientSocket.isConnected()) {
                    //while (true) {
                        String cmd = in.readLine();
                        /*
                        if (cmd.isBlank() || cmd.isEmpty() || cmd.equals("\r\n") || cmd.equals("\t") || cmd.equals("")) {
                            System.out.println("nullpoint");
                            break;
                        }
                         */
                        if (cmd.equals("PUBLISH")) {
                            PUBLISH_REPLY();
                        } else if (cmd.startsWith("RCV_IDS")) {
                            MSG_ID(cmd);
                        } else if (cmd.startsWith("RCV_MSG")) {
                            RCV_MSG(cmd);
                        } else if (cmd.startsWith("REPLY")) {
                            PUBLISH_REPLY();
                        } else if (cmd.startsWith("REPUBLISH")) {
                            REPUBLISH(cmd);
                        }
                    }
                }
            //}
            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
