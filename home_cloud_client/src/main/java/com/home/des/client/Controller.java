package com.home.des.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    @FXML
    ListView<FileInfo> filesListComputer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Path files = Paths.get(".idea");
        filesListComputer.getItems().addAll(scanFiles(files));

        filesListComputer.setCellFactory(new Callback<ListView<FileInfo>, ListCell<FileInfo>>() {
            @Override
            public ListCell<FileInfo> call(ListView<FileInfo> param) {
                return new ListCell<FileInfo>(){
                    @Override
                    protected void updateItem(FileInfo item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty){
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item.getFilename());
                        }
                    }
                };
            }
        });

        //необходимо инициализировать сервер, но сначала передача\скачивание файлов.
    }

    public List<FileInfo> scanFiles(Path filesPath){
        try {
            return Files.list(filesPath).map(FileInfo::new).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Неудачное сканирование файлов: " + filesPath.toAbsolutePath().toString());
        }
    }

    public void menuItemFileExit(ActionEvent actionEvent) {
        Platform.exit();
    }
}
