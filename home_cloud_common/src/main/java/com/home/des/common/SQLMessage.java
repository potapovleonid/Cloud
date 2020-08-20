package com.home.des.common;

import java.io.Serializable;

public class SQLMessage implements Serializable {
    public enum Command {
        AUTHORIZE, REGISTER
    }

    private String loginName;
    private String password;
    private Command command;

    public SQLMessage(String loginName, String password, Command command) {
        this.loginName = loginName;
        this.password = password;
        this.command = command;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getPassword() {
        return password;
    }

    public Command getCommand() {
        return command;
    }
}
