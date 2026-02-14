package pt.isep.bookmarkscc;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class App extends Application {

    @Override
    public void start(Stage stage) {
        var view = new AppView();
        stage.setScene(new Scene(view.root, 520, 500));
        stage.setTitle("BookmarksCC");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

