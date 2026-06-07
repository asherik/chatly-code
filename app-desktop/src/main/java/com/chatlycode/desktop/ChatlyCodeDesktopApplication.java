package com.chatlycode.desktop;

import com.chatlycode.appserver.facade.ChatlyCodeFacade;
import com.chatlycode.desktop.controller.MainController;
import com.chatlycode.i18n.LocalizationService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;

public final class ChatlyCodeDesktopApplication extends Application {

    @Override
    public void start(Stage stage) {
        LocalizationService localization = new LocalizationService();
        MainController controller = new MainController(ChatlyCodeFacade.createDefault(), localization, Locale.getDefault());
        Scene scene = new Scene(controller.view(), 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/css/chatly-code.css").toExternalForm());
        stage.setTitle(localization.message(Locale.getDefault(), "app.title"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
