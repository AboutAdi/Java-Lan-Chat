package com.voibiz.lanchat.desktop;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
        String username = prefs.get("lanchat.username", null);
        String userId = prefs.get("lanchat.userid", null);
        
        if (username != null && userId != null && !username.isEmpty() && !userId.isEmpty()) {
            com.voibiz.lanchat.core.model.User user = new com.voibiz.lanchat.core.model.User(userId, username, "", 0);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            com.voibiz.lanchat.desktop.controller.MainController mainController = loader.getController();
            mainController.initUser(user);
            
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setTitle("LAN Chat - " + username);
            primaryStage.setScene(scene);
            primaryStage.show();
        } else {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 400, 400);
            primaryStage.setTitle("LAN Chat");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
