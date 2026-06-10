package com.voibiz.lanchat.desktop.controller;

import com.voibiz.lanchat.core.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private Button joinButton;

    @FXML
    public void initialize() {
        // Default behavior
    }

    @FXML
    public void onJoin(ActionEvent event) {
        String username = usernameField.getText();
        if (username != null && !username.trim().isEmpty()) {
            User user = new User(username.trim());
            
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.voibiz.lanchat.desktop.App.class);
            prefs.put("lanchat.username", user.getDisplayName());
            prefs.put("lanchat.userid", user.getUserId());
            
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
                Parent root = loader.load();
                
                MainController mainController = loader.getController();
                mainController.initUser(user);
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root, 800, 600);
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
