package com.home.des.server;

import java.sql.*;

public class JDBC_Connect {
    private static Connection connection = null;
    private static Statement statement = null;
    private static ResultSet resultSet = null;



    public static void connectJDBC() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            statement = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static boolean isRegisteredUser(String user_login, String user_password) throws SQLException {
        resultSet = statement
                .executeQuery(String.format("select * from users_tbl where user_login_fld = '%s' and user_password_fld = '%s'", user_login, user_password));
        if (resultSet.next()){
            return true;
        } else {
            return false;
        }
    }

    public synchronized static void registerNewUser(String user_login, String user_password) throws SQLException {
        statement.execute(String.format("insert into users_tbl (user_login_fld, user_password_fld) values ('%s','%s')", user_login, user_password));
    }
}
