package com.home.des.server;

import com.home.des.common.ConnectionSettings;
import com.home.des.common.FileInfo;
import com.home.des.common.FileMessage;
import com.home.des.common.FileRequest;
import io.netty.channel.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class MyHandler extends ChannelInboundHandlerAdapter {
    private ExecutorService executorService;
    private boolean block = false;
    private int uploadPart = 0;
    private FileOutputStream uploadFileStream;
    private Path pathUploadFile;


    public MyHandler() {
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconected");
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {

        if (msg instanceof FileRequest) {
            if (((FileRequest) msg).getCommand() == FileRequest.Command.DOWNLOAD) {
                System.out.println("Получено сообщение на скачивание: " + ((FileRequest) msg).getFileName());
                new Thread(() -> {
                    try {
                        File fileDownload = new File(ConnectionSettings.destination_server_files.toString() + "/" + ((FileRequest) msg).getFileName());
                        int totalParts = new Long(fileDownload.length() / FileMessage.SIZE_BYTE_BUFFER).intValue();
                        if (fileDownload.length() % FileMessage.SIZE_BYTE_BUFFER != 0) {
                            totalParts++;
                        }
                        FileMessage fileMessageOut = new FileMessage(new byte[FileMessage.SIZE_BYTE_BUFFER], 0, totalParts);
                        FileInputStream fis = new FileInputStream(fileDownload);
                        for (int i = 0; i < totalParts; i++) {
                            if (!ctx.isRemoved()) {
                                //тестовая строка || теряется много оперативы
//                                FileMessage fileMessageOut = new FileMessage(new byte[FileMessage.SIZE_BYTE_BUFFER], -1, totalParts);
                                int readByte = fis.read(fileMessageOut.getBytes());
                                fileMessageOut.setNumberPart(i + 1);
                                if (readByte < FileMessage.SIZE_BYTE_BUFFER) {
                                    fileMessageOut.setBytes(Arrays.copyOfRange(fileMessageOut.getBytes(), 0, readByte));
                                }
                                ctx.writeAndFlush(fileMessageOut);
                                block = true;
                                while (block) {
                                    Thread.sleep(50);
                                }
                                System.out.println("Sended part: " + fileMessageOut.getNumberPart());
                            }
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("complete transfer");
                    // передача
                }).start();
            }
            if (((FileRequest) msg).getCommand() == FileRequest.Command.UPLOAD) {
                pathUploadFile = Paths.get(ConnectionSettings.destination_server_files.toString() + "/" + ((FileRequest) msg).getFileName());
                if (!Files.exists(pathUploadFile)) {
                    Files.createFile(pathUploadFile);
                } else {
                    Files.delete(pathUploadFile);
                    Files.createFile(pathUploadFile);
                }
                uploadFileStream = new FileOutputStream(pathUploadFile.toFile());
                uploadPart++;
            }
            if (((FileRequest) msg).getCommand() == FileRequest.Command.INFO) {
                System.out.println("Получен запрос на список файлов");
                executorService.execute(() -> {
                    ctx.writeAndFlush(new FileRequest(FileRequest.Command.CONFIRMATE));
                    try {
                        List<FileInfo> infoList = Files.list(ConnectionSettings.destination_server_files).map(FileInfo::new).collect(Collectors.toList());
                        ctx.writeAndFlush(new FileMessage(infoList));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            if (((FileRequest) msg).getCommand() == FileRequest.Command.LOCK_OFF) {
                block = false;
            }
        }
        if (msg instanceof FileMessage) {
            if (uploadFileStream != null && uploadPart == ((FileMessage) msg).getNumberPart()) {
                uploadFileStream.write(((FileMessage) msg).getBytes());

                System.out.println("Принят пакет: " + ((FileMessage) msg).getNumberPart() + "/" + ((FileMessage) msg).getTotalParts());
                if (((FileMessage) msg).getNumberPart() == ((FileMessage) msg).getTotalParts()) {
                    System.out.println("На сервер загружен файл: " + pathUploadFile.toFile().getName());
                    pathUploadFile = null;
                    uploadFileStream.close();
                    uploadPart = 0;
                }
                if (((FileMessage) msg).getNumberPart() != ((FileMessage) msg).getTotalParts()) {
                    ctx.writeAndFlush(new FileRequest(FileRequest.Command.NEXT_FM));
                    uploadPart++;
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
