package com.home.des.common;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo implements Serializable {
    public static String UP_PATH = "[..]";

    private String filename;
    private long size;

    public FileInfo(Path path){
        try {
            this.filename = path.getFileName().toString();
            if (Files.isDirectory(path)){
                this.size = -1;
            } else {
                this.size = Files.size(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка с файлом: " + path.toAbsolutePath().toString());
        }
    }

    public FileInfo(String filename, long size){
        this.filename = filename;
        this.size = size;
    }

    public boolean isDirectory(){
        return size == -1L;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
