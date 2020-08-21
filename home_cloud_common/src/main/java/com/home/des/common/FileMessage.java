package com.home.des.common;

import java.io.Serializable;
import java.util.List;

public class FileMessage implements Serializable {
    public final static int SIZE_BYTE_BUFFER = 1024 * 1024 * 64;
//    public final static int SIZE_BYTE_BUFFER = 1024 * 8;
    private List<FileInfo> fileInfoList;
    private byte[] bytes;
    private int numberPart;
    private int totalParts;

    public FileMessage(byte[] bytes, int numberPart, int totalParts) {
        this.bytes = bytes;
        this.numberPart = numberPart;
        this.totalParts = totalParts;
    }

    public FileMessage(List<FileInfo> fileInfoList) {
        this.fileInfoList = fileInfoList;
    }

    public List<FileInfo> getFileInfoList() {
        return fileInfoList;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getNumberPart() {
        return numberPart;
    }

    public int getTotalParts() {
        return totalParts;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setNumberPart(int numberPart) {
        this.numberPart = numberPart;
    }

}
