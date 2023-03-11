import java.sql.*;
import java.util.Scanner;

public class MicroblogDatabase {
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3307/db_microblog";
    private static final String USER = "root";
    private static final String PASS = "";

    private static Connection conn;

    public MicroblogDatabase() throws SQLException, ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void PUBLISH(String username, String header, String content) throws SQLException {
        String sql = "INSERT INTO messages (username, content) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, username);
        pstmt.setString(2, content);
        pstmt.executeUpdate();
        System.out.println("Message inserted: " + content);
    }

    public void selectAllMessages() throws SQLException {
        String sql = "SELECT * FROM users";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            int id = rs.getInt("id");
            String username = rs.getString("username");
            //String content = rs.getString("content");
            //Timestamp timestamp = rs.getTimestamp("timestamp");
            System.out.println(id + "\t" + username + "\t" /*+ content + "\t" + timestamp*/);
        }
    }

    public void close() throws SQLException {
        conn.close();
    }

    public static boolean authentification(String username) throws SQLException, ClassNotFoundException {
        boolean exists = false;
        MicroblogDatabase db = new MicroblogDatabase();
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
            conn.close();
        } catch (SQLException ex) {
            System.out.println("User doesn't exist");
        }
        db.close();
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
            System.out.println(authentification("@dean"));
            System.out.println(authentification("@james"));
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
