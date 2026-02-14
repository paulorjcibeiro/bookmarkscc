package pt.isep.bookmarkscc;

import javafx.application.Application;
import javafx.stage.Stage;
import pt.isep.bookmarkscc.persistence.Db;
import pt.isep.bookmarkscc.ui.Navigator;

public final class App extends Application {

    @Override
    public void start(Stage stage) {
        Db.open();

        Navigator nav = new Navigator(stage);
        nav.showLogin("Aguardando inserção do Cartão de Cidadão...");
        stage.show();
    }

    @Override
    public void stop() {
        Db.close();
        AppResources.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
