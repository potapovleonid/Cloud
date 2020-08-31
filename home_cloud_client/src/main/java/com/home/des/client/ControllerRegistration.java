package com.home.des.client;

import com.home.des.common.SQLMessage;
import com.home.des.common.ConnectionSettings;
import com.home.des.common.FileRequest;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class ControllerRegistration{
    private Socket socket;
    private ObjectEncoderOutputStream oos;
    private ObjectDecoderInputStream ois;
    private Stage registerStage;

    @FXML
    VBox registrationPanel;
    @FXML
    TextField passwordField;
    @FXML
    TextField loginField;

    private void connecting_server(){
        try {
            socket = new Socket(ConnectionSettings.HOST, ConnectionSettings.PORT);
            ois = new ObjectDecoderInputStream(socket.getInputStream());
            oos = new ObjectEncoderOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Не удалось установить подключение к серверу", ButtonType.OK).showAndWait();
            System.exit(0);
        }
    }

    public void bAuthorize(ActionEvent actionEvent) throws IOException, ClassNotFoundException, InterruptedException {
        try {
            connecting_server();
            if (!passwordField.getText().equals("") || !loginField.getText().equals("")) {
                oos.writeObject(new SQLMessage(loginField.getText(), passwordField.getText(), SQLMessage.Command.AUTHORIZE));
                Object answerServer = ois.readObject();
                if (answerServer instanceof FileRequest) {
                    if (((FileRequest) answerServer).getCommand() == FileRequest.Command.SUCCESS_AUTORIZE) {
                    registerStage = (Stage) registrationPanel.getScene().getWindow();
                    Stage cloudStage = new Stage();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/application.fxml"));
                    Parent root = loader.load();
                    cloudStage.setTitle("Home Cloud");
                    cloudStage.setScene(new Scene(root, 1200, 500));
                    cloudStage.show();
                    registerStage.hide();
                    Controller controller = loader.getController();
                    controller.setSocket(socket);
                    controller.setRegisterStage(registerStage);
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Введены не корректные данные", ButtonType.OK).showAndWait();
                    }
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Введи логин и пароль!", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void bRegister(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
        if (!passwordField.getText().equals("") || !loginField.getText().equals("")) {
            oos.writeObject(new SQLMessage(loginField.getText(), passwordField.getText(), SQLMessage.Command.REGISTER));
            Object answerServer = ois.readObject();
            FileRequest fileRequest = (FileRequest) answerServer;
            switch (fileRequest.getCommand()){
                case SUCCESS_REGISTER:
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Учётная запись создана", ButtonType.OK);
                    alert.setHeaderText(null);
                    alert.showAndWait();
                    break;
                case FALSE_REGISTER:
                    new Alert(Alert.AlertType.ERROR, "Учётная запись с таким логином уже существует", ButtonType.OK).showAndWait();
                    break;
            }
        } else {
            new Alert(Alert.AlertType.WARNING, "Введи логин и пароль!", ButtonType.OK).showAndWait();
        }
    }
}
