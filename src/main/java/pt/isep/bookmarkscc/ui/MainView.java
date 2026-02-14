package pt.isep.bookmarkscc.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pt.isep.bookmarkscc.AppResources;
import pt.isep.bookmarkscc.domain.Bookmark;
import pt.isep.bookmarkscc.persistence.BookmarkRepository;
import pt.isep.bookmarkscc.persistence.Db;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Optional;

public final class MainView {

    private final Navigator nav;

    private final VBox root = new VBox(10);

    private final ImageView photoView = new ImageView();
    private final Label who = new Label();

    private final ListView<String> bookmarksList = new ListView<>();

    public MainView(Navigator nav) {
        this.nav = nav;

        root.setPadding(new Insets(16));

        Label title = new Label("Área Principal");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        who.setWrapText(true);

        photoView.setFitWidth(96);
        photoView.setFitHeight(120);
        photoView.setPreserveRatio(true);

        HBox userBox = new HBox(12, photoView, who);
        userBox.setPadding(new Insets(6, 0, 6, 0));

        bookmarksList.setPrefHeight(120);
        bookmarksList.setMinHeight(100);
        bookmarksList.setMaxHeight(180);
        VBox.setVgrow(bookmarksList, Priority.ALWAYS);

        Button addBtn = new Button("Adicionar bookmark");
        addBtn.setOnAction(e -> addBookmark());

        Button refreshBtn = new Button("Recarregar bookmarks");
        refreshBtn.setOnAction(e -> loadBookmarks());

        HBox buttons = new HBox(10, addBtn, refreshBtn);

        Button logoutBtn = new Button("Logout (manual)");
        logoutBtn.setOnAction(e -> {
            Session.logout();
            this.nav.showLogin("Sessão terminada (logout manual).");
        });

        Label info = new Label(
                "Nota: se remover o Cartão, a sessão será bloqueada"
        );
        info.setWrapText(true);

        root.getChildren().addAll(
                title,
                userBox,
                new Label("Bookmarks:"),
                bookmarksList,
                buttons,
                logoutBtn,
                info
        );

        refresh();
        loadBookmarks();
    }

    public Parent root() { return root; }

    public void refresh() {
        if (Session.isLoggedIn()) {

            // ✅ TEXTO EM DUAS LINHAS
            who.setText(
                    "Sessão:" + "\n" +
                    "Nome: " + Session.displayName() + "\n" +
                    "Nº CC: " + Session.documentNumber()
            );

            byte[] photo = AppResources.getUserPhoto();
            if (photo != null && photo.length > 0) {
                try {
                    photoView.setImage(new Image(new ByteArrayInputStream(photo)));
                } catch (Exception ignored) {
                    photoView.setImage(null);
                }
            } else {
                photoView.setImage(null);
            }

        } else {
            who.setText("Sessão: (não autenticado)");
            photoView.setImage(null);
        }
    }

    private void addBookmark() {
        if (!Session.isLoggedIn()) return;

        TextInputDialog titleDlg = new TextInputDialog();
        titleDlg.setTitle("Novo Bookmark");
        titleDlg.setHeaderText("Título");
        Optional<String> title = titleDlg.showAndWait();
        if (title.isEmpty() || title.get().isBlank()) return;

        TextInputDialog urlDlg = new TextInputDialog("https://");
        urlDlg.setTitle("Novo Bookmark");
        urlDlg.setHeaderText("URL");
        Optional<String> url = urlDlg.showAndWait();
        if (url.isEmpty() || url.get().isBlank()) return;

        TextInputDialog tagsDlg = new TextInputDialog();
        tagsDlg.setTitle("Novo Bookmark");
        tagsDlg.setHeaderText("Tags (opcional)");
        Optional<String> tags = tagsDlg.showAndWait();

        try {
            var repo = new BookmarkRepository(Db.open());
            repo.saveEncrypted(
                    Bookmark.create(
                            title.get().trim(),
                            URI.create(url.get().trim()),
                            tags.orElse("")
                    )
            );
            loadBookmarks();
        } catch (Exception ex) {
            bookmarksList.getItems().add("Erro a gravar bookmark: " + ex.getMessage());
        }
    }

    private void loadBookmarks() {
        bookmarksList.getItems().clear();

        if (!Session.isLoggedIn()) {
            bookmarksList.getItems().add("(não autenticado)");
            return;
        }

        try {
            var repo = new BookmarkRepository(Db.open());
            for (Bookmark b : repo.listDecrypted()) {
                bookmarksList.getItems().add(b.title() + " — " + b.url());
            }
            if (bookmarksList.getItems().isEmpty()) {
                bookmarksList.getItems().add("(sem bookmarks)");
            }
        } catch (Exception ex) {
            bookmarksList.getItems().add("Erro a carregar bookmarks: " + ex.getMessage());
        }
    }
}
