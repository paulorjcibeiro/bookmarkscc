package pt.isep.bookmarkscc.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class Navigator {

    private final Stage stage;
    private final LoginView loginView;

    private MainView mainView;   // lazy
    private final Scene loginScene;
    private Scene mainScene;     // lazy

    public Navigator(Stage stage) {
        this.stage = stage;
        this.loginView = new LoginView(this);

        this.loginScene = new Scene(loginView.root(), 520, 240);

        // NÃO criar MainView aqui (ainda não há dbKey)
        this.mainView = null;
        this.mainScene = null;
    }

    public void showLogin(String message) {
        Platform.runLater(() -> {
            stage.setScene(loginScene);
            stage.setTitle("BookmarksCC - Login (Cartão de Cidadão)");
            loginView.setStatus(message);
        });
    }

    public void showMain() {
        Platform.runLater(() -> {
            if (mainView == null) {
                mainView = new MainView(this);
                mainScene = new Scene(mainView.root(), 520, 500);
            }

            stage.setScene(mainScene);
            stage.setTitle("BookmarksCC - Main");
            mainView.refresh();
        });
    }

    public LoginView loginView() { return loginView; }
}
