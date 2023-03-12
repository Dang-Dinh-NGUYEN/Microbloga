package Database;

import java.sql.*;
import java.util.Scanner;

public class MicroblogDatabase {
    public static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String DB_URL = "jdbc:mysql://localhost:3307/db_microblog";
    public static final String USER = "root";
    public static final String PASS = "";

    public static Connection conn;

    static {
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
        }
    }


    public MicroblogDatabase() throws SQLException, ClassNotFoundException {
        //Class.forName(JDBC_DRIVER);
        //conn = DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void PUBLISH(String username, String header, String content) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO messages (username, header, content) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, username);
        pstmt.setString(2, header);
        pstmt.setString(3, content);
        pstmt.executeUpdate();
    }

    public static int GET_MSG_ID(String user, String formattedDateTime) throws SQLException {
        String sql = "SELECT id FROM messages WHERE username = ? AND timestamp = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, user);
        pstmt.setString(2, formattedDateTime);
        ResultSet rs = pstmt.executeQuery();
        int id = -1;
        if (rs.next()) {
            id = rs.getInt("id");
        }
        rs.close();
        pstmt.close();
        return id;
    }

    private static boolean HAS_TAG(String tag) throws SQLException, ClassNotFoundException {
        boolean exist = false;

        String sql = "SELECT COUNT(*) FROM tags WHERE (tag = ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, tag);
        // Execute the query and check if the client's name exists in the database
        ResultSet rs = pstmt.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        if (count > 0) exist = true;

        // Close the database connection and release resources
        rs.close();
        pstmt.close();

        return exist;
    }

    private static void NEW_TAG(String tag) throws SQLException, ClassNotFoundException {
        if(HAS_TAG(tag)) return;
        String sql = "INSERT INTO tags (tag) VALUES (?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, tag);
        pstmt.executeUpdate();
    }

    public static void ADD_TAG(int id, String tag) throws SQLException, ClassNotFoundException {
        MicroblogDatabase db = new MicroblogDatabase();
        NEW_TAG(tag);
        String sql = "INSERT INTO message_tags (message_id, tag_name) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, String.valueOf(id));
        pstmt.setString(2, tag);
        pstmt.executeUpdate();
        db.close();
    }

    //public static void  selectMessagesById()

    /*
    public static void selectMessages(String opts) throws SQLException, ClassNotFoundException {
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

            PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString());
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

                System.out.println(messageId);
                PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM messages WHERE id = ?");
                pstmt.setInt(1, messageId);
                ResultSet prs = pstmt.executeQuery();
                while (prs.next()) {
                    System.out.println(prs.getString("header"));
                    System.out.println(prs.getString("content"));
                    System.out.println(prs.getString("timestamp"));
                    System.out.println();
                }
                prs.close();
                pstmt.close();
            }
            rs.close();
            stmt.close();
        }
    }

     */

    public void close() throws SQLException {
        conn.close();
    }

    public static boolean Authentification(String username) throws SQLException, ClassNotFoundException {
        boolean exists = false;
        try {
            // Create a statement to execute a SQL query
            String query = "SELECT COUNT(*) FROM users WHERE (username = ?)";
            PreparedStatement stmt = conn.prepareStatement(query);

            // Set the client's name as a parameter in the query
            stmt.setString(1, username);

            // Execute the query and check if the client's name exists in the database
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            if (count > 0) {
                exists = true;
            } else {
                SignUp();
            }

            // Close the database connection and release resources
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("User doesn't exist");
        }
        return exists;
    }

    public static void SignUp() throws SQLException {
        System.out.println("Can't find your account/ Account doesn't exist.");
        System.out.println("Do you want to sign up? yes/no");
        System.out.print("select: ");
        Scanner scanner = new Scanner(System.in);

        if(scanner.nextLine().equals("yes")) {
            System.out.print("Create your username (begin with '@'): ");
            String username = scanner.nextLine();
            assert(username.charAt(0) == '@');

            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, "");
            pstmt.executeUpdate();
            System.out.println("User inserted: " + username);
        }else{
            throw new SQLException("Cannot connect to server");
        }
    }

    public static void main(String[] args) {
        try {
            //db.SignUp("james");
            //db.insertMessage("john", "Hello World!");
            System.out.println(Authentification("@dean"));
            System.out.println(Authentification("@james"));
            System.out.println(HAS_TAG("#algo"));
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
