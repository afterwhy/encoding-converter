package com.afterwhy;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author d.karasev
 */
public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(new MainView(primaryStage), null));
        primaryStage.setWidth(400);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
