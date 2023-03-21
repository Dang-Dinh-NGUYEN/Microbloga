package Server;
import Database.MicroblogDatabase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MicroblogCentral {
    protected static Map<String, Set<PrintWriter>> subscriptions = new ConcurrentHashMap<>();
    protected static Map<String, Queue<Integer>> messageQueues = new ConcurrentHashMap<>();

    // A mapping of usernames to their corresponding client handlers
    protected static Map<String, ClientHandlerMF> clients = new HashMap<>();

    public final static int PORT = 12345;
    public final static String SERVER = "localhost";

    public static void main(String[] args) throws Exception {
        ExecutorService executorService = Executors.newWorkStealingPool();
        ServerSocket serverSocket = new ServerSocket(PORT);
        try {
            System.out.println("WELCOME TO MICROBLOGAMU - SERVER CENTRAL");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandlerMF(clientSocket));
            }
        } finally {
            serverSocket.close();
            executorService.shutdown();
        }
    }

    private static class ClientHandlerMF implements Runnable{
        private final Socket clientSocket;
        protected static BufferedReader in;
        protected static PrintWriter out;
        protected static String user;

        protected Set<String> tag_followers = new HashSet<>();
        protected Set<String> followers;

        public ClientHandlerMF(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                //reading username and verify user's profile on database
                user = in.readLine();
                if (MicroblogDatabase.Authentification(user)) {
                    // Add the client to the map of connected clients
                    synchronized(clients) {
                        clients.put(user, this);
                    }

                    System.out.println("CONNECT user: " + user);
                    //sending admission to client
                    out.println("OK");
                    String cmd;
                    while ((cmd = in.readLine()) != null) {
                        //list of followers of this user
                        followers = MicroblogDatabase.GET_USER_FOLLOWERS(user);

                        if (cmd.equals("PUBLISH")) {
                            PUBLISH_MF();
                        }
                        /*
                        else if (cmd.startsWith("RCV_IDS")) {
                            MSG_ID(cmd);
                        } else if (cmd.startsWith("RCV_MSG")) {
                            RCV_MSG(cmd);
                        } else if (cmd.startsWith("REPLY")) {
                            PUBLISH_REPLY();
                        } else if (cmd.startsWith("REPUBLISH")) {
                            REPUBLISH(cmd);
                        } else if (cmd.startsWith("SUBSCRIBE") || cmd.startsWith("UNSUBSCRIBE")) {
                            SUBSCRIPTION(cmd);
                        }
                         */
                    }
                } else {
                    out.println("Can't find your account/ Account doesn't exist.");
                }
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException | SQLException | ClassNotFoundException ioException) {
                ioException.printStackTrace();
            }finally {
                // Remove the client from the map of connected clients
                synchronized (clients) {
                    clients.remove(user);
                }
            }
        }

        private void PUBLISH_MF() throws IOException, SQLException, ClassNotFoundException {
            //reading message header
            String header;
            header = in.readLine();

            //reading message content from client's IO
            StringBuilder body = new StringBuilder();
            while (true) {
                String message;
                message = in.readLine();
                if (message.equals("$")) break;
                body.append(message);
            }

            //adding message into database
            MicroblogDatabase.PUBLISH(user, header, body.toString());

            out.println("OK");

            //printing message on server
            System.out.println(header);
            System.out.println(body.toString());
            String formattedDateTime = LocalDateTime.now().
                    format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println(formattedDateTime);

            int msg_id = MicroblogDatabase.GET_LAST_MSG_ID();

            // Send notifications to online followers
            for (String follower : followers) {
                Socket followerSocket = followers.get(follower);
                if (followerSocket != null) {
                    PrintWriter followerOut = new PrintWriter(followerSocket.getOutputStream(), true);
                    followerOut.println(user + " just posted: " + header);
                }
            }


/*
            //placer dans les files concernées les nouveaux messages publiés
            for(String user : followers){
                Queue<Integer> messageQueue = messageQueues.get(user);
                if (messageQueue == null) {
                    messageQueue = new ConcurrentLinkedQueue<>();
                    messageQueues.put(user, messageQueue);
                }
                messageQueue.offer(msg_id);
            }

            //sending notifications to followers of tags included in the message
            List<String> tags = MicroblogDatabase.GET_MSG_TAGS(msg_id);
            for(String tag : tags) {
                Set<String> followers = MicroblogDatabase.GET_TAG_FOLLOWERS(tag);
                List<String> followerNames = new ArrayList<>(followers);
                tag_followers.addAll(followerNames);
            }
            System.out.println(tag_followers.toString());

            for (String follower : tag_followers) {
                Set<PrintWriter> subscribers = subscriptions.get(follower);
                if (subscribers != null) {
                    for (PrintWriter subscriber : subscribers) {
                        subscriber.println("new message from: " + user);
                    }
                }
            }

 */
        }

        public void REPLY_MF() throws IOException, SQLException, ClassNotFoundException {
            //reading message header
            String header = null;
            header = in.readLine();

            //reading message content
            StringBuilder body = new StringBuilder();
            while (true) {
                String message = null;
                message = in.readLine();
                if (message.equals("$")) break;
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
            }
            System.out.println(header);
            System.out.println(body.toString());
            String formattedDateTime = LocalDateTime.now().
                    format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println(formattedDateTime);

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
            String sql = "SELECT * FROM messages WHERE id = ?";

            PreparedStatement stmt = MicroblogDatabase.conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(id));
            ResultSet rs = stmt.executeQuery();

            String author = null, msg_id = null,
                    content = null, header = null, date = null;
            Integer reply_to_id = null;

            while (rs.next()) {
                msg_id = String.valueOf(rs.getInt("id"));
                author = rs.getString("username");
                header = rs.getString("header");
                content = rs.getString("content");
                date = rs.getString("timestamp");
                reply_to_id = rs.getInt("reply_to");
                if (rs.wasNull()) {
                    reply_to_id = null;
                }
            }
            String response = ">> MSG author:" + author + " msg_id:" + msg_id;
            if(reply_to_id != null) response += " reply_to_id:" + reply_to_id;
            if(header.contains("REPUBLISH")) response += " republished:true";

            out.println(response);
            out.println(content);
            out.println(date);
            out.println();

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
            }
        }

        public void SUBSCRIPTION(String cmd) throws IOException, SQLException, ClassNotFoundException {
            Pattern pattern = Pattern.compile("(author|tag):([#@]\\w+)");
            Matcher matcher = pattern.matcher(cmd);

            if (matcher.find()) {
                String type = matcher.group(1);
                String value = matcher.group(2);

                if(cmd.startsWith("SUBSCRIBE")){
                    if (type.equals("author")) {
                        MicroblogDatabase.SUBSCRIBE_USER(MicroblogDatabase.GET_USER_ID(user), MicroblogDatabase.GET_USER_ID(value));
                        out.println(">> OK");
                    } else if(type.equals("tag")){
                        MicroblogDatabase.SUBSCRIBE_TAG(MicroblogDatabase.GET_USER_ID(user),value);
                        out.println(">> OK");
                    } else {
                        out.println("ERREUR");
                        return;
                    }
                }
                else if(cmd.startsWith("UNSUBSCRIBE")){
                    if (type.equals("author")) {
                        MicroblogDatabase.UNSUBSCRIBE_USER(MicroblogDatabase.GET_USER_ID(user), MicroblogDatabase.GET_USER_ID(value));
                        out.println(">> OK");
                    } else if(type.equals("tag")){
                        MicroblogDatabase.UNSUBSCRIBE_TAG(MicroblogDatabase.GET_USER_ID(user),value);
                        out.println(">> OK");
                    } else {
                        out.println("ERREUR");
                    }
                }

            } else {
                // Handle invalid subscription command
                out.println("Invalid subscription command: " + cmd);
            }

        }

    }
}
