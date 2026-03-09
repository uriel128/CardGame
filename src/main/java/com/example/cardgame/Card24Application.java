package com.example.cardgame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Card24Application extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Card24Application.class.getResource("card24-view.fxml"));
        Scene scene = new Scene(loader.load(), 860, 540);
        scene.getStylesheets().add(Card24Application.class.getResource("card24.css").toExternalForm());

        stage.setTitle("Card 24 Game");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(500);
        stage.show();
    }
}
