package tn.esprit.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public final String URL="jdbc:mysql://localhost:3306/carthagevoyage";
    public final String USER="root";
    public final String PWD ="";
    private Connection cnx;
    private static DBConnection instance;

    private DBConnection(){
        try {
            cnx = DriverManager.getConnection(URL,USER,PWD);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
    public static DBConnection getInstance(){
        if(instance==null)
            instance= new DBConnection();
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}
