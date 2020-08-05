package com.home.des.common;

//будем кидать в обе стороны, нужно подумать как реализовать запрос на INFO
public class FileRequest {
    enum Command {
        DOWNLOAD, UPLOAD, INFO
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
