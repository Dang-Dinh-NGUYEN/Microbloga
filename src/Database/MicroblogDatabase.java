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

    public static void REPLY(String username, String header, String content, String reply_to_id) throws SQLException, ClassNotFoundException {
        String sql = "INSERT INTO messages (username, header, content, reply_to) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, username);
        pstmt.setString(2, header);
        pstmt.setString(3, content);
        pstmt.setInt(4, Integer.valueOf(reply_to_id));
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
