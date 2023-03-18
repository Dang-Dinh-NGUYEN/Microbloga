package Clients;

import java.io.IOException;
import java.sql.SQLException;

public class Repost extends Client{
    public Repost() throws SQLException, ClassNotFoundException {}

    public void reply() throws IOException, SQLException, ClassNotFoundException {
        new Publisher().publish();
    }

    public void republish() throws SQLException, ClassNotFoundException, IOException {
        new Follower().recevoir();
    }
}
