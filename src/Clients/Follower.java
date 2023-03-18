package Clients;

import java.io.IOException;
import java.sql.SQLException;

public class Follower extends Client{
    public Follower() throws SQLException, ClassNotFoundException {}

    public void recevoir() throws IOException {
        while(true) {
            String reponse;
            reponse = in.readLine();
            if(reponse.equals("$")){
                break;
            }
            System.out.println(reponse);
        }
    }
}
