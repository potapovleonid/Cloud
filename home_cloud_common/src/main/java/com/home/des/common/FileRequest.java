package com.home.des.common;

import java.io.Serializable;

//будем кидать в обе стороны, нужно подумать как реализовать запрос на INFO
public class FileRequest implements Serializable {
    public enum Command {
        DOWNLOAD, UPLOAD, INFO, CONFIRMATE, NEXT_FM, LOCK_OFF,
        SUCCESS_AUTORIZE, FALSE_AUTORIZE, SUCCESS_REGISTER, FALSE_REGISTER
    }

    private String fileName;
    private Command command;

    public FileRequest(String fileName, Command command) {
        this.fileName = fileName;
        this.command = command;
    }

    //для запроса списка файлов методом INFO
    public FileRequest(Command cmd) {
        this.command = cmd;
    }

    public String getFileName() {
        return fileName;
    }

    public Command getCommand() {
        return command;
    }


}
