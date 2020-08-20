package com.home.des.common;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileInfo implements Serializable {
    public enum FileType {
        File("F"), DIRECTORY("DIR");

        private String name;

        public String getName(){
            return name;
        }

        FileType(String name){
            this.name = name;
        }
    }

    public static String UP_PATH = "[..]";

    private String filename;
    private long size;
    private FileType fileType;
    private LocalDateTime lastModified;

    public FileInfo(Path path){
        try {
            this.filename = path.getFileName().toString();
            this.size = Files.size(path);
            this.fileType = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.File;
            if (this.fileType == FileType.DIRECTORY) size = -1;
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(0));
        } catch (IOException e) {
            throw new RuntimeException("Unable to create file info from path");
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

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
}
