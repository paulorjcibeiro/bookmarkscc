package pt.isep.bookmarkscc;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;

public final class AppView {

    public final VBox root = new VBox(12);
    private static Connection conn;
    private java.util.concurrent.ScheduledExecutorService monitor;


    public AppView() {
        root.setPadding(new Insets(16));
        showLogin();
    }

    /* ================= LOGIN ================= */

    private void showLogin() {
        root.getChildren().setAll(
                new Label("Autenticação com Cartão de Cidadão"),
                new ProgressIndicator(),
                new Label("A autenticar...")
        );

        Label status = (Label) root.getChildren().get(2);

        new Thread(() -> {
            AuthResult r = PteidAuthService.authenticateWithPinChallenge();

            Platform.runLater(() -> {
                if (!r.ok()) {
                    status.setText("Falha: " + r.error());
                    return;
                }

                Session.deriveAndSetKey(r.authCertDer(), r.documentNumber());
                Session.setUserPhoto(r.photoBytes());
                Session.login(r.displayName(), r.documentNumber());

                showMain();
            });
        }).start();
    }

    /* ================= MAIN ================= */

    private void showMain() {
        
        startCardMonitor();
        
        ImageView photo = new ImageView();
        photo.setFitWidth(90);
        photo.setPreserveRatio(true);

        byte[] ph = Session.getUserPhoto();
        if (ph != null)
            photo.setImage(new Image(new ByteArrayInputStream(ph)));

        Label who = new Label(
                "Nome: " + Session.displayName() +
                "\nNº CC: " + Session.documentNumber()
        );

        ListView<String> list = new ListView<>();
        loadBookmarks(list);

        Button add = new Button("Adicionar");
        add.setOnAction(e -> addBookmark(list));

        Button logout = new Button("Logout");
        logout.setOnAction(e -> secureShutdown());

        root.getChildren().setAll(photo, who, list, add, logout);
    }

    /* ================= BOOKMARKS ================= */

    private void addBookmark(ListView<String> list) {
        try {
            var t = new TextInputDialog();
            t.setHeaderText("Título");
            var title = t.showAndWait();
            if (title.isEmpty()) return;

            var u = new TextInputDialog("https://");
            u.setHeaderText("URL");
            var url = u.showAndWait();
            if (url.isEmpty()) return;

            byte[] key = Session.getDbKey();
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);

            var c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            c.init(javax.crypto.Cipher.ENCRYPT_MODE,
                    new javax.crypto.spec.SecretKeySpec(key, "AES"),
                    new javax.crypto.spec.GCMParameterSpec(128, nonce));

            byte[] cipher = c.doFinal(
                    (title.get() + "|" + url.get()).getBytes(StandardCharsets.UTF_8)
            );

            try (var ps = getConnection().prepareStatement(
                    "INSERT INTO bookmarks (nonce,data,created_at) VALUES(?,?,?)")) {
                ps.setBytes(1, nonce);
                ps.setBytes(2, cipher);
                ps.setString(3, Instant.now().toString());
                ps.executeUpdate();
            }

            loadBookmarks(list);

        } catch (Exception ignored) {}
    }

    private void loadBookmarks(ListView<String> list) {
        list.getItems().clear();

        try {
            byte[] key = Session.getDbKey();

            try (var rs = getConnection().createStatement()
                    .executeQuery("SELECT nonce,data FROM bookmarks ORDER BY id DESC")) {

                while (rs.next()) {
                    var c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
                    c.init(javax.crypto.Cipher.DECRYPT_MODE,
                            new javax.crypto.spec.SecretKeySpec(key, "AES"),
                            new javax.crypto.spec.GCMParameterSpec(128, rs.getBytes(1)));

                    byte[] plain = c.doFinal(rs.getBytes(2));
                    String[] p = new String(plain, StandardCharsets.UTF_8).split("\\|");
                    list.getItems().add(p[0] + " — " + p[1]);
                }
            }
        } catch (Exception ignored) {}
    }

    /* ================= DB ================= */

    private static Connection getConnection() throws Exception {
        if (conn != null) return conn;

        var dir = java.nio.file.Path.of(System.getProperty("user.home"), ".bookmarkscc");
        java.nio.file.Files.createDirectories(dir);

        conn = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("bookmarks.db"));

        try (var st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bookmarks(
                id INTEGER PRIMARY KEY,
                nonce BLOB NOT NULL,
                data BLOB NOT NULL,
                created_at TEXT NOT NULL)
            """);
        }

        return conn;
    }
    /* ================= CARD MONITOR ================= */

    private void startCardMonitor() {

        monitor = java.util.concurrent.Executors
                .newSingleThreadScheduledExecutor();

        monitor.scheduleAtFixedRate(() -> {
            try {
                if (!PteidSdk.isCardPresent()) {
                    javafx.application.Platform.runLater(this::secureShutdown);
                }
            } catch (Exception ignored) {}

        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void stopCardMonitor() {
        if (monitor != null) {
            monitor.shutdownNow();
            monitor = null;
        }
    }

    private void secureShutdown() {
        stopCardMonitor();
        Session.logout();
        javafx.application.Platform.exit();
    }

}

