package com.home.des.client;

import com.home.des.common.ConnectionSettings;
import com.home.des.common.FileInfo;
import com.home.des.common.FileMessage;
import com.home.des.common.FileRequest;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.apache.logging.log4j.core.util.Loader;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    Path root;
    private ObjectDecoderInputStream ois;
    private ObjectEncoderOutputStream oos;
    private Socket socket;

    @FXML
    ListView<FileInfo> filesListComputer;
    @FXML
    ListView<FileInfo> filesListServer;
    @FXML
    TextField pathField;
    @FXML
    Button bUpload;

    public Controller() throws IOException {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Path files = Paths.get(".idea");
        filesListComputer.getItems().addAll(scanFiles(files));
        //тут надо ловить данные с сервака и обновлять список файлов
//        filesListServer.getItems().addAll("File1", "File2", "File3", "File4", "File5", "File6");
        filesListComputer.setCellFactory(new Callback<ListView<FileInfo>, ListCell<FileInfo>>() {
            @Override
            public ListCell<FileInfo> call(ListView<FileInfo> param) {
                return new ListCell<FileInfo>() {
                    @Override
                    protected void updateItem(FileInfo item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            String formattedFilename = String.format("%-30s", item.getFilename());
                            String formattedFileSize = String.format("%,d bytes", item.getSize());
                            if (item.getSize() == -1L) {
                                formattedFileSize = String.format("%s", "[DIR]");
                            }
                            if (item.getSize() == -2L) {
                                formattedFileSize = "";
                            }
                            String text = String.format("%s %-20s", formattedFilename, formattedFileSize);
                            setText(text);
                        }
                    }
                };
            }
        });
        goToPath(Paths.get(""));
        ;
        //необходимо инициализировать сервер, но сначала передача\скачивание файлов.
    }

    public void goToPath(Path path) {
        this.root = path;
        pathField.setText(root.toAbsolutePath().toString());
        filesListComputer.getItems().clear();
        filesListComputer.getItems().add(new FileInfo(FileInfo.UP_PATH, -2L));
        filesListComputer.getItems().addAll(scanFiles(root));
        filesListComputer.getItems().sort(new Comparator<FileInfo>() {
            @Override
            public int compare(FileInfo o1, FileInfo o2) {
                if (o1.getFilename().equals(FileInfo.UP_PATH)) {
                    return -1;
                }
                if ((int) Math.signum(o1.getSize()) == (int) Math.signum(o2.getSize())) {
                    return o1.getFilename().compareTo(o2.getFilename());
                }
                return new Long(o1.getSize() - o2.getSize()).intValue();
            }
        });
    }

    public List<FileInfo> scanFiles(Path filesPath) {
        try {
            return Files.list(filesPath).map(FileInfo::new).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Неудачное сканирование файлов: " + filesPath.toAbsolutePath().toString());
        }
    }

    public void menuItemFileExit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void filesListComputerClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            FileInfo fileInfo = filesListComputer.getSelectionModel().getSelectedItem();
            if (fileInfo.isDirectory()) {
                goToPath(root.resolve(fileInfo.getFilename()));
            }
            if (fileInfo.getSize() == -2L) {
                goToPath(root.toAbsolutePath().getParent());
            }
        }
    }

    //надо разнести отправку листов в отдельный класс
    public void buttonUpdateFileList(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
        filesListServer.getItems().clear();
        oos.writeObject(new FileRequest(FileRequest.Command.INFO));
        FileRequest fileRequest = (FileRequest) ois.readObject();
        if (fileRequest.getCommand() == FileRequest.Command.CONFIRMATE) {
            System.out.println("Сервер подтвердил передачу списка файлов");
            Object msg = ois.readObject();
            List<FileInfo> fileInfoList = ((FileMessage) msg).getFileInfoList();
            System.out.println("Получен список");
            filesListServer.getItems().addAll(fileInfoList);
            filesListServer.setCellFactory(new Callback<ListView<FileInfo>, ListCell<FileInfo>>() {
                @Override
                public ListCell<FileInfo> call(ListView<FileInfo> param) {
                    return new ListCell<FileInfo>() {
                        @Override
                        protected void updateItem(FileInfo item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                                setStyle("");
                            } else {
                                String formattedFilename = String.format("%-30s", item.getFilename());
                                String formattedFileSize = String.format("%,d bytes", item.getSize());
                                if (item.getSize() == -1L) {
                                    formattedFileSize = String.format("%s", "[DIR]");
                                }
                                if (item.getSize() == -2L) {
                                    formattedFileSize = "";
                                }
                                String text = String.format("%s %-20s", formattedFilename, formattedFileSize);
                                setText(text);
                            }
                        }
                    };
                }
            });
        }
    }

    public void connect_server(ActionEvent actionEvent) throws IOException {
        socket = new Socket(ConnectionSettings.HOST, ConnectionSettings.PORT);
        this.ois = new ObjectDecoderInputStream(socket.getInputStream(), (int) (FileMessage.SIZE_BYTE_BUFFER * 1.05));
        this.oos = new ObjectEncoderOutputStream(socket.getOutputStream());
        System.out.println("success connection");
    }

    public void buttonDownload(ActionEvent actionEvent) throws IOException, InterruptedException {
        Path pathToFile = Paths.get(pathField.getText() + "/" + filesListServer.getSelectionModel().getSelectedItem().getFilename());
        if (!Files.exists(pathToFile)) {
            Files.createFile(pathToFile);
        } else {
            Files.delete(pathToFile);
            Files.createFile(pathToFile);
        }
        oos.writeObject(new FileRequest(filesListServer.getSelectionModel().getSelectedItem().getFilename(), FileRequest.Command.DOWNLOAD));
        new Thread(() -> {
            try {
                FileOutputStream fout = new FileOutputStream(pathToFile.toFile());
                while (true) {
                    Object msg = ois.readObject();
                    FileMessage fileMessage = (FileMessage) msg;
                    fout.write(fileMessage.getBytes());

                    System.out.println("Confrimate part: " + fileMessage.getNumberPart() + "/" + fileMessage.getTotalParts());
                    oos.writeObject(new FileRequest(FileRequest.Command.CONFIRMATE));
                    if (((FileMessage) msg).getNumberPart() == ((FileMessage) msg).getTotalParts()) {
                        break;
                    }
                }
                fout.close();
                System.out.println("Success");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void buttonUpload(ActionEvent actionEvent) throws IOException, InterruptedException {
        if (filesListComputer.getSelectionModel().getSelectedItem() != null) {
//            System.out.println("Загружаем файл: " + filesListComputer.getSelectionModel().getSelectedItem().getFilename());
//            File fileUpload = new File(pathField.getText() + "/"
//                    + filesListComputer.getSelectionModel().getSelectedItem().getFilename());
////            System.out.println(fileUpload.getAbsoluteFile());
//            int totalParts = new Long(fileUpload.length() / FileMessage.SIZE_BYTE_BUFFER).intValue();
//            if (fileUpload.length() % FileMessage.SIZE_BYTE_BUFFER != 0) totalParts++;
//            FileMessage fileMessageOut = new FileMessage(new byte[FileMessage.SIZE_BYTE_BUFFER], 0, totalParts);
//            FileInputStream fis = new FileInputStream(fileUpload);
//            for (int i = 0; i < totalParts; i++) {
//                if (!socket.isClosed()){
//                    int readBytes = fis.read(fileMessageOut.getBytes());
//                    fileMessageOut.setNumberPart(i+1);
//                    if (readBytes < FileMessage.SIZE_BYTE_BUFFER){
//                        fileMessageOut.setBytes(Arrays.copyOfRange(fileMessageOut.getBytes(), 0, readBytes));
//                    }
//
//                }
//            }

            //Вот здесь ошика
            bUpload.setVisible(false);
            Thread.sleep(10000);
            bUpload.setVisible(true);
        } else System.out.println("Файл не выбран");
    }
}