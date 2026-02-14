package pt.isep.bookmarkscc.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import pt.isep.bookmarkscc.AppResources;
import pt.isep.bookmarkscc.persistence.Db;
import pt.isep.bookmarkscc.pteid.AuthResult;
import pt.isep.bookmarkscc.pteid.PteidAuthService;
import pt.isep.bookmarkscc.pteid.PteidCardPoller;

public final class LoginView {

    private final Navigator nav;

    private final VBox root = new VBox(10);
    private final Label status = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();

    // Polling robusto: deteta inserção/remoção mesmo quando callbacks falham
    private final PteidCardPoller poller = new PteidCardPoller();

    // Evita múltiplas autenticações em paralelo
    private volatile boolean authRunning = false;

    public LoginView(Navigator nav) {
        this.nav = nav;

        // registar para shutdown global
        AppResources.setPoller(poller);

        root.setPadding(new Insets(16));

        Label title = new Label("Autenticação com Cartão de Cidadão");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label hint = new Label("Insira o cartão no leitor. A autenticação inicia automaticamente.");
        hint.setWrapText(true);

        progress.setVisible(false);
        progress.setPrefSize(28, 28);

        status.setWrapText(true);
        status.setStyle("-fx-font-size: 13px;");

        Button retryBtn = new Button("Tentar novamente");
        retryBtn.setOnAction(e -> retryDetection());

        root.getChildren().addAll(title, hint, progress, status, retryBtn);
        setStatus("Aguardando inserção do Cartão de Cidadão...");

        startPolling();
    }

    private void retryDetection() {
        Platform.runLater(() -> {
            progress.setVisible(false);
            authRunning = false;
            setStatus("A verificar leitor/cartão...");
        });

        poller.stop();
        startPolling();
    }

    public Parent root() {
        return root;
    }

    public void setStatus(String msg) {
        status.setText(msg);
    }

    /**
     * Opcional: se quiser chamar manualmente 
     * se já usar AppResources.shutdown()).
     */
    public void shutdown() {
        poller.stop();
    }

    private void startPolling() {
        poller.start(new PteidCardPoller.Listener() {
            @Override
            public void onInserted() {
                Platform.runLater(() -> setStatus("Cartão detetado. A autenticar..."));
                runAuthOnce();
            }

            @Override
            public void onRemoved() {
                Platform.runLater(() -> {
                    progress.setVisible(false);
                    authRunning = false;

                    // fechar DB e limpar chave/foto quando o cartão sai
                    Db.close();
                    AppResources.shutdown(); // limpa dbKey + userPhoto + pára poller/SDK

                    Session.logout();
                    setStatus("Cartão removido. Sessão bloqueada.");
                    nav.showLogin("Cartão removido. Sessão terminada.");
                });
            }

            @Override
            public void onError(String msg, Throwable t) {
                Platform.runLater(() -> {
                    progress.setVisible(false);
                    authRunning = false;
                    setStatus(msg + (t != null && t.getMessage() != null ? " " + t.getMessage() : ""));
                });
            }
        });
    }

    private void runAuthOnce() {
        if (authRunning) return;
        authRunning = true;

        Task<AuthResult> task = new Task<>() {
            @Override
            protected AuthResult call() {
                return PteidAuthService.authenticateWithPinChallenge();
            }
        };

        task.setOnRunning(e -> Platform.runLater(() -> progress.setVisible(true)));

        task.setOnSucceeded(e -> {
            progress.setVisible(false);
            authRunning = false;

            AuthResult r = task.getValue();
            if (r.ok()) {
                // derivar key32 do cartão e guardar em memória
                byte[] key32 = pt.isep.bookmarkscc.security.KeyDerivation
                        .deriveKey32(r.authCertDer(), r.documentNumber());
                AppResources.setDbKey(key32);

                // ✅ NOVO: guardar fotografia em memória
                AppResources.setUserPhoto(r.photoBytes());

                Session.login(r.displayName(), r.documentNumber());
                setStatus("Autenticado: " + r.displayName() + " (CC " + r.documentNumber() + ")");
                nav.showMain();
            } else {
                setStatus("Falha na autenticação: " + r.error());
            }
        });

        task.setOnFailed(e -> {
            progress.setVisible(false);
            authRunning = false;
            Throwable ex = task.getException();
            setStatus("Erro inesperado: " + (ex != null ? ex.getMessage() : "(sem detalhes)"));
        });

        Thread th = new Thread(task, "pteid-auth");
        th.setDaemon(true);
        th.start();
    }
}
