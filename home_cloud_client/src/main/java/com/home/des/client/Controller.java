package com.home.des.client;

import com.home.des.common.ConnectionSettings;
import com.home.des.common.FileInfo;
import com.home.des.common.FileMessage;
import com.home.des.common.FileRequest;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private ObjectDecoderInputStream ois;
    private ObjectEncoderOutputStream oos;
    private Socket socket;
    private Stage registerStage;
    private Stage cloudStage;
    @FXML
    Button bUpload;
    @FXML
    Button bDownload;
    @FXML
    Button bConnect;
    @FXML
    Button bUpdate;
    @FXML
    TableView<FileInfo> filesListComputer;
    @FXML
    TableView<FileInfo> filesListServer;
    @FXML
    ComboBox<String> disksBox;
    @FXML
    TextField pathField;
    @FXML
    VBox vBoxFileServer;
    @FXML
    HBox cloud_panel;

    public void settingTable(TableView<FileInfo> tableView) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileType().getName()));
        fileTypeColumn.setPrefWidth(30);

        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("File name");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        fileNameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("File size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(120);
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String str = String.format("%,d bytes", item);
                        if (item == -1L) {
                            str = "[DIR]";
                        }
                        setText(str);
                    }
                }
            };
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Date modified");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        tableView.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn);
        tableView.getSortOrder().add(fileTypeColumn);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        settingTable(filesListComputer);
        settingTable(filesListServer);

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);

        filesListComputer.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path selectPath = Paths.get(pathField.getText()).resolve(filesListComputer.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(selectPath)) {
                        updateListComputer(selectPath, filesListComputer);
                    }
                }
            }
        });
        updateListComputer(Paths.get(""), filesListComputer);
    }

    public void updateListComputer(Path path, TableView<FileInfo> tableView) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            tableView.getItems().clear();
            tableView.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            tableView.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void updateListServer() throws IOException, ClassNotFoundException {
        filesListServer.getItems().clear();
        try {
            oos.writeObject(new FileRequest(FileRequest.Command.INFO));
        } catch (IOException e) {
            alertMessage(Alert.AlertType.ERROR, "Подключение к серверу отсутствует", null, "Вы были отключены от сервера, пожалуйста переподключитесь");
            reConnect();
        }
        FileRequest fileRequest = (FileRequest) ois.readObject();
        if (fileRequest.getCommand() == FileRequest.Command.CONFIRMATE) {
            try {
                System.out.println("Сервер подтвердил передачу списка файлов");
                Object msg = ois.readObject();
                List<FileInfo> fileInfoList = ((FileMessage) msg).getFileInfoList();
                System.out.println("Получен список");
                filesListServer.getItems().addAll(fileInfoList);
            } catch (ClassNotFoundException | IOException e) {
                alertMessage(Alert.AlertType.ERROR, "Обновление списка", null, "Не удалось обновить список файлов сервера");
            }
        }
    }

    public void menuItemFileExit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void buttonUp(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateListComputer(upperPath, filesListComputer);
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateListComputer(Paths.get(element.getSelectionModel().getSelectedItem()), filesListComputer);
    }

    public void buttonDownload(ActionEvent actionEvent) throws IOException {
        if (filesListServer.getSelectionModel().getSelectedItem() != null) {
            Path pathToFile = Paths.get(pathField.getText() + "/" + filesListServer.getSelectionModel().getSelectedItem().getFilename());
            if (!Files.exists(pathToFile)) {
                Files.createFile(pathToFile);
            } else {
                Files.delete(pathToFile);
                Files.createFile(pathToFile);
            }
            try {
                oos.writeObject(new FileRequest(filesListServer.getSelectionModel().getSelectedItem().getFilename(), FileRequest.Command.DOWNLOAD));
            } catch (RuntimeException e) {
                alertMessage(Alert.AlertType.ERROR, "Подключение к серверу отсутствует", null, "Вы были отключены от сервера, пожалуйста переподключитесь");
                reConnect();
            }
            new Thread(() -> {
                try {
                    FileOutputStream fout = new FileOutputStream(pathToFile.toFile());
                    while (true) {
                        Object msg = ois.readObject();
                        FileMessage fileMessage = (FileMessage) msg;
                        fout.write(fileMessage.getBytes());
                        System.out.println("Подтверждён пакет: " + fileMessage.getNumberPart() + "/" + fileMessage.getTotalParts());
                        oos.writeObject(new FileRequest(FileRequest.Command.LOCK_OFF));
                        if (((FileMessage) msg).getNumberPart() == ((FileMessage) msg).getTotalParts()) {
                            break;
                        }
                    }
                    fout.close();
                    System.out.println("Файл: " + pathToFile.getFileName() + " успешно загружен с сервера");
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                updateListComputer(Paths.get(pathField.getText()), filesListComputer);
            }).start();
        } else {
            alertMessage(Alert.AlertType.INFORMATION, "Не выбран файл", null, "Не выбран файл для загрузки с сервера");
        }
    }

    public void buttonUpload(ActionEvent actionEvent) throws IOException, InterruptedException, ClassNotFoundException {
        if (filesListComputer.getSelectionModel().getSelectedItem() != null) {
            try {
                System.out.println("Загружаем файл: " + filesListComputer.getSelectionModel().getSelectedItem().getFilename());
                oos.writeObject(new FileRequest(filesListComputer.getSelectionModel().getSelectedItem().getFilename(), FileRequest.Command.UPLOAD));
            } catch (RuntimeException e) {
                e.printStackTrace();
                reConnect();
            }
            File fileUpload = new File(pathField.getText() + "/"
                    + filesListComputer.getSelectionModel().getSelectedItem().getFilename());
            int totalParts = new Long(fileUpload.length() / FileMessage.SIZE_BYTE_BUFFER).intValue();
            if (fileUpload.length() % FileMessage.SIZE_BYTE_BUFFER != 0) totalParts++;
            FileMessage fileMessageOut = new FileMessage(new byte[FileMessage.SIZE_BYTE_BUFFER], 0, totalParts);
            FileInputStream fis = new FileInputStream(fileUpload);
            FileRequest next_file;
            for (int i = 0; i < totalParts; i++) {
                if (!socket.isClosed()) {
                    int readBytes = fis.read(fileMessageOut.getBytes());
                    fileMessageOut.setNumberPart(i + 1);
                    if (readBytes < FileMessage.SIZE_BYTE_BUFFER) {
                        fileMessageOut.setBytes(Arrays.copyOfRange(fileMessageOut.getBytes(), 0, readBytes));
                    }
                    oos.writeObject(fileMessageOut);
                    System.out.println("Отправлен пакет: " + (i + 1) + "/" + totalParts);
                    if (i + 1 < totalParts) {
                        next_file = (FileRequest) ois.readObject();
                        if (next_file.getCommand() == FileRequest.Command.NEXT_FM) {
                            System.out.println("Отправляем следующий пакет");
                        }
                    }
                }
            }
            fis.close();
            updateListServer();
        } else {
            alertMessage(Alert.AlertType.INFORMATION, "Не выбран файл", null, "Не выбран файл для загрузки на сервера");
        }
    }

    public void alertMessage(Alert.AlertType alertType, String titleText, String headerText, String message) {
        Alert connect_server = new Alert(Alert.AlertType.INFORMATION);
        connect_server.setTitle(titleText);
        connect_server.setHeaderText(null);
        connect_server.setContentText(message);
        connect_server.showAndWait();
    }

    public void reConnect() {
        cloudStage.hide();
        registerStage.show();
    }

    public void setSocket(Socket socket) throws IOException, ClassNotFoundException {
        this.socket = socket;
        ois = new ObjectDecoderInputStream(socket.getInputStream(), (int) (FileMessage.SIZE_BYTE_BUFFER * 1.05));
        oos = new ObjectEncoderOutputStream(socket.getOutputStream(), (int) (FileMessage.SIZE_BYTE_BUFFER * 1.05));
        updateListServer();
    }

    public void setRegisterStage(Stage registerStage) throws InterruptedException {
        this.registerStage = registerStage;
        cloudStage = (Stage) cloud_panel.getScene().getWindow();
    }


}