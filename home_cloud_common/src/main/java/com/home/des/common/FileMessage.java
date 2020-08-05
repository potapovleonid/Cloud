package com.home.des.common;

//будем кидаться классами внутри которых пакеты с данными, надобность в длинне файла отпадает
//т.к. будет np и tp из них можно будет высчитать когда останавливаться
public class FileMessage {
    private byte[] bytes = new byte[8192];
    private int numberPart;
    private int totalParts;

    public FileMessage(byte[] bytes, int numberPart, int totalParts) {
        this.bytes = bytes.clone();
        this.numberPart = numberPart;
        this.totalParts = totalParts;
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
}
