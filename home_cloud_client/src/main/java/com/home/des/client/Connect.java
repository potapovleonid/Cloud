package com.home.des.client;

import com.home.des.common.ConnectionSettings;
import com.home.des.common.FileInfo;
import com.home.des.common.FileMessage;
import com.home.des.common.FileRequest;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class Connect {
    private ObjectDecoderInputStream ois;
    private ObjectEncoderOutputStream oos;
    private Socket socket;

    public Connect() throws IOException {
        socket = new Socket(ConnectionSettings.HOST, ConnectionSettings.PORT);
        this.ois = new ObjectDecoderInputStream(socket.getInputStream(), 100*1024*1024);
        this.oos = new ObjectEncoderOutputStream(socket.getOutputStream());
    }

    private void requestInfo() throws IOException, ClassNotFoundException {
        oos.writeObject(new FileRequest(FileRequest.Command.INFO));
        FileRequest fileRequest = (FileRequest) ois.readObject();
        if (fileRequest.getCommand() == FileRequest.Command.CONFIRMATE){
            System.out.println("Сервер подтвердил передачу списка файлов");
            Object msg = ois.readObject();
            List<FileInfo> fileInfoList = ((FileMessage) msg).getFileInfoList();
            System.out.println("Получен список");
            for (int i = 0; i < fileInfoList.size(); i++) {
                System.out.println(fileInfoList.get(i).getFilename());
            }
        }
//        FileMessage fileMessage = (FileMessage) ois.readObject();
//        List<FileInfo> fileInfoList = fileMessage.getFileInfoList();
//        for (int i = 0; i < fileInfoList.size(); i++) {
//            System.out.println(fileInfoList.get(i).getFilename());
//        }
    }

    private void downloadFile(String filename) throws IOException {
//        CountDownLatch countDownLatch = new CountDownLatch(1);
        Path pathToFile = Paths.get("./user_files/" + filename);
        if (!Files.exists(pathToFile)) {
            Files.createFile(pathToFile);
        } else {
            Files.delete(pathToFile);
            Files.createFile(pathToFile);
        }
        oos.writeObject(new FileRequest(filename, FileRequest.Command.DOWNLOAD));
        new Thread(() -> {
        try {
            FileRequest fileRequest = (FileRequest) ois.readObject();
            if (fileRequest.getCommand() == FileRequest.Command.CONFIRMATE) {
                System.out.println("succes confirmate");
                FileOutputStream fout = new FileOutputStream(pathToFile.toFile());
                while (true) {
                    Object msg = ois.readObject();
                    FileMessage fileMessage = (FileMessage) msg;
                    fout.write(fileMessage.getBytes());

                    System.out.println("Confrimate part: " + fileMessage.getNumberPart() + "/" + fileMessage.getTotalParts());
                    if (((FileMessage) msg).getNumberPart() == ((FileMessage) msg).getTotalParts()){
//                        countDownLatch.countDown();
                        break;
                    }

                }
                fout.close();
            } else System.out.println("Error download");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Success");
        }).start();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Connect connect = new Connect();
        connect.requestInfo();
//        connect.downloadFile("vi1.mp4");
    }
}
