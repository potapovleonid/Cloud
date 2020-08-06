package com.home.des.client;

import com.home.des.common.FileInfo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    Path root;

    @FXML
    ListView<FileInfo> filesListComputer;
    @FXML
    ListView<String> filesListServer;
    @FXML
    TextField pathField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Path files = Paths.get(".idea");
        filesListComputer.getItems().addAll(scanFiles(files));
        //тут надо ловить данные с сервака и обновлять список файлов
        filesListServer.getItems().addAll("File1", "File2", "File3", "File4", "File5", "File6");
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
        goToPath(Paths.get("./"));
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
}
