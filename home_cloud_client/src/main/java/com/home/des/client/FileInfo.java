package com.home.des.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo {
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
