package tn.esprit.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.dto.CountryInfoDto;
import tn.esprit.entities.Destination;
import tn.esprit.services.CountryInfoService;
import tn.esprit.services.CountryNotFoundException;
import tn.esprit.services.DestinationServices;

import java.sql.SQLException;

public class DestinationController {

    @FXML
    private TableView<Destination> table;
    @FXML
    private TableColumn<Destination, String> colPaysDepart;
    @FXML
    private TableColumn<Destination, String> colPaysArrivee;
    @FXML
    private TableColumn<Destination, String> colDescription;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnFilter;

    private final DestinationServices service = new DestinationServices();
    private final CountryInfoService countryInfoService = new CountryInfoService();
    private final ObservableList<Destination> list = FXCollections.observableArrayList();
    private final FilteredList<Destination> filteredList = new FilteredList<>(list, p -> true);
    private String filterPaysDepart;
    private String filterPaysArrivee;

    @FXML
    public void initialize() {
        colPaysDepart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaysDepart()));
        colPaysArrivee.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaysArrivee()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescription() != null ? c.getValue().getDescription() : ""));
        table.setItems(filteredList);
        if (searchField != null)
            searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        refresh();
    }

    private void applyFilter() {
        String q = searchField != null
                ? (searchField.getText() == null ? "" : searchField.getText()).trim().toLowerCase()
                : "";
        filteredList.setPredicate(d -> {
            if (filterPaysDepart != null && !filterPaysDepart.isEmpty() && (d.getPaysDepart() == null
                    || !d.getPaysDepart().toLowerCase().contains(filterPaysDepart.toLowerCase())))
                return false;
            if (filterPaysArrivee != null && !filterPaysArrivee.isEmpty() && (d.getPaysArrivee() == null
                    || !d.getPaysArrivee().toLowerCase().contains(filterPaysArrivee.toLowerCase())))
                return false;
            if (q.isEmpty())
                return true;
            return (d.getPaysDepart() != null && d.getPaysDepart().toLowerCase().contains(q))
                    || (d.getPaysArrivee() != null && d.getPaysArrivee().toLowerCase().contains(q))
                    || (d.getDescription() != null && d.getDescription().toLowerCase().contains(q));
        });
    }

    @FXML
    private void onFilter() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Filtrage");
        d.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(d.getDialogPane());
        TextField paysDep = new TextField(filterPaysDepart != null ? filterPaysDepart : "");
        paysDep.setPromptText("Pays départ (vide = tous)");
        TextField paysArr = new TextField(filterPaysArrivee != null ? filterPaysArrivee : "");
        paysArr.setPromptText("Pays arrivée (vide = tous)");
        DialogStyleHelper.styleField(paysDep);
        DialogStyleHelper.styleField(paysArr);
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Pays départ", paysDep);
        DialogStyleHelper.addRow(g, 1, "Pays arrivée", paysArr);
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(btn -> btn);
        if (d.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            filterPaysDepart = paysDep.getText() != null && !paysDep.getText().isBlank() ? paysDep.getText().trim()
                    : null;
            filterPaysArrivee = paysArr.getText() != null && !paysArr.getText().isBlank() ? paysArr.getText().trim()
                    : null;
            applyFilter();
        }
    }

    public void refresh() {
        try {
            list.clear();
            list.addAll(service.afficher());
        } catch (SQLException e) {
            showError("Erreur", "Chargement impossible.");
        }
    }

    @FXML
    private void onAdd() {
        Destination d = new Destination();
        if (showDialog(d, "Ajouter destination")) {
            try {
                service.ajouter(d);
                refresh();
            } catch (SQLException e) {
                showError("Erreur", "Ajout impossible.");
            }
        }
    }

    @FXML
    private void onEdit() {
        Destination d = table.getSelectionModel().getSelectedItem();
        if (d == null) {
            showError("Attention", "Selectionnez une ligne.");
            return;
        }
        if (showDialog(d, "Modifier destination")) {
            try {
                service.modifier(d);
                refresh();
            } catch (SQLException e) {
                showError("Erreur", "Modification impossible.");
            }
        }
    }

    @FXML
    private void onDelete() {
        Destination d = table.getSelectionModel().getSelectedItem();
        if (d == null) {
            showError("Attention", "Selectionnez une ligne.");
            return;
        }
        if (confirm("Supprimer cette destination ?")) {
            try {
                service.supprimer(d.getIdDestination());
                refresh();
            } catch (SQLException e) {
                showError("Erreur", "Suppression impossible.");
            }
        }
    }

    @FXML
    private void onCountryInfo() {
        Destination selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Attention", "Sélectionnez une destination dans le tableau d'abord.");
            return;
        }
        String paysArrivee = selected.getPaysArrivee();
        if (paysArrivee == null || paysArrivee.isBlank()) {
            showError("Données manquantes", "Cette destination n'a pas de pays d'arrivée défini.");
            return;
        }

        // Loading stage (Stage instead of Dialog to avoid JavaFX nested event loop
        // issues)
        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setTitle("Chargement...");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);
        Label loadingLabel = new Label("Informations pour \"" + paysArrivee + "\"...");
        loadingLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#333;");
        VBox loadingBox = new VBox(14, spinner, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(32));
        loadingBox.setStyle("-fx-background-color:#ffffff;");
        loadingStage.setScene(new Scene(loadingBox, 300, 130));
        loadingStage.setResizable(false);

        Task<CountryInfoDto> task = new Task<>() {
            @Override
            protected CountryInfoDto call() {
                return countryInfoService.getCountryInfo(paysArrivee);
            }
        };
        task.setOnSucceeded(ev -> {
            CountryInfoDto result = task.getValue();
            loadingStage.close();
            // Platform.runLater ensures loadingStage is fully closed before opening result
            // stage
            Platform.runLater(() -> showCountryStage(result));
        });
        task.setOnFailed(ev -> {
            loadingStage.close();
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                if (ex instanceof CountryNotFoundException) {
                    showError("Pays introuvable", ex.getMessage());
                } else {
                    showError("Erreur réseau", "Impossible de contacter RestCountries.\n" + ex.getMessage());
                }
            });
        });
        new Thread(task) {
            {
                setDaemon(true);
            }
        }.start();
        loadingStage.showAndWait();
    }

    /**
     * Shows country info in a Stage (not Dialog) to guarantee correct rendering.
     */
    private void showCountryStage(CountryInfoDto info) {
        Stage stage = new Stage();
        stage.setTitle("Infos : " + info.commonName());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        // Flag
        ImageView flagView = new ImageView();
        flagView.setFitWidth(160);
        flagView.setFitHeight(100);
        flagView.setPreserveRatio(true);
        if (info.flagUrl() != null && !info.flagUrl().equals("—")) {
            try {
                flagView.setImage(new Image(info.flagUrl(), true));
            } catch (Exception ignored) {
            }
        }

        // Title
        Label title = new Label(info.commonName());
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#1a237e;");

        // Info grid
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 4, 0));

        String langText = info.languages().isEmpty() ? "—" : String.join(", ", info.languages());
        String tzText = (info.timezones() == null || info.timezones().isEmpty())
                ? "—"
                : info.timezones().get(0)
                        + (info.timezones().size() > 1 ? " (+" + (info.timezones().size() - 1) + " autres)" : "");

        addInfoRowInline(grid, 0, "🌍  Nom", info.commonName());
        addInfoRowInline(grid, 1, "🏙️  Capitale", info.capital());
        addInfoRowInline(grid, 2, "💰  Devise", info.currencyName() + " (" + info.currencySymbol() + ")");
        addInfoRowInline(grid, 3, "🗣️  Langues", langText);
        addInfoRowInline(grid, 4, "🕐  Fuseau", tzText);

        // Google Maps link (clickable Hyperlink)
        Label mapsKeyLabel = new Label("🗺️  Google Maps");
        mapsKeyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555; -fx-font-size: 13px;");
        if (info.googleMapsUrl() != null) {
            Hyperlink mapsLink = new Hyperlink("Ouvrir dans Google Maps");
            mapsLink.setStyle("-fx-text-fill: #1565c0; -fx-font-size: 13px; -fx-padding: 0;");
            mapsLink.setOnAction(e -> openMapInWebView(info.googleMapsUrl(), info.commonName()));
            grid.add(mapsKeyLabel, 0, 5);
            grid.add(mapsLink, 1, 5);
        } else {
            addInfoRowInline(grid, 5, "🗺️  Google Maps", "—");
        }

        // Close button
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color:#1a237e; -fx-text-fill:white;"
                + " -fx-font-size:13px; -fx-padding:6 24; -fx-background-radius:6;");
        closeBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(14, title, flagView, new Separator(), grid, closeBtn);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(24, 32, 20, 32));
        root.setStyle("-fx-background-color:#f8f9ff;");

        stage.setScene(new Scene(root, 440, 470));
        stage.showAndWait();
    }

    /** Opens a WebView Stage inside the app to display Google Maps. */
    private void openMapInWebView(String url, String countryName) {
        Stage mapStage = new Stage();
        mapStage.setTitle("Google Maps — " + countryName);
        mapStage.initModality(Modality.APPLICATION_MODAL);

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.load(url);

        // Progress bar while page loads
        ProgressBar progress = new ProgressBar();
        progress.progressProperty().bind(engine.getLoadWorker().progressProperty());
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setStyle("-fx-accent: #1a237e;");
        progress.visibleProperty().bind(
                engine.getLoadWorker().runningProperty());

        // Toolbar : URL label + close button
        Label urlLabel = new Label(url);
        urlLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
        urlLabel.setWrapText(false);
        urlLabel.setMaxWidth(600);

        Button closeBtn = new Button("✕ Fermer");
        closeBtn.setStyle("-fx-background-color:#1a237e; -fx-text-fill:white;"
                + " -fx-font-size:12px; -fx-padding:4 12; -fx-background-radius:4;");
        closeBtn.setOnAction(e -> mapStage.close());

        javafx.scene.layout.HBox toolbar = new javafx.scene.layout.HBox(10, urlLabel, closeBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.setStyle("-fx-background-color: #e8eaf6; -fx-border-color: #c5cae9;"
                + " -fx-border-width: 0 0 1 0;");
        javafx.scene.layout.HBox.setHgrow(urlLabel, javafx.scene.layout.Priority.ALWAYS);

        VBox root = new VBox(toolbar, progress, webView);
        VBox.setVgrow(webView, javafx.scene.layout.Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 650);
        mapStage.setScene(scene);
        mapStage.setResizable(true);
        mapStage.show();
    }

    private void addInfoRowInline(GridPane g, int row, String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555; -fx-font-size: 13px;");
        Label valLabel = new Label(value != null ? value : "—");
        valLabel.setStyle("-fx-text-fill: #1a1a2e; -fx-font-size: 13px;");
        valLabel.setWrapText(true);
        valLabel.setMaxWidth(220);
        g.add(keyLabel, 0, row);
        g.add(valLabel, 1, row);
    }

    private boolean showDialog(Destination d, String title) {
        Dialog<Destination> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        DialogStyleHelper.styleDialogPane(dialog.getDialogPane());
        TextField paysD = new TextField(d.getPaysDepart());
        TextField paysA = new TextField(d.getPaysArrivee());
        TextField desc = new TextField(d.getDescription());
        GridPane g = DialogStyleHelper.buildGrid();
        DialogStyleHelper.addRow(g, 0, "Pays départ *", paysD);
        DialogStyleHelper.addRow(g, 1, "Pays arrivée *", paysA);
        DialogStyleHelper.addRow(g, 2, "Description", desc);
        DialogStyleHelper.styleField(paysD);
        DialogStyleHelper.styleField(paysA);
        DialogStyleHelper.styleField(desc);
        VBox content = new VBox(new Label(title), g);
        content.getStyleClass().add("crud-dialog-content");
        ((Label) content.getChildren().get(0)).getStyleClass().add("crud-dialog-title");
        content.setSpacing(16);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK)
                return null;
            if (paysD.getText() == null || paysD.getText().isBlank()) {
                showError("Validation", "Pays depart obligatoire.");
                return null;
            }
            if (paysA.getText() == null || paysA.getText().isBlank()) {
                showError("Validation", "Pays arrivee obligatoire.");
                return null;
            }
            d.setPaysDepart(paysD.getText().trim());
            d.setPaysArrivee(paysA.getText().trim());
            d.setDescription(desc.getText() != null && !desc.getText().isBlank() ? desc.getText().trim() : null);
            return d;
        });
        return dialog.showAndWait().orElse(null) != null;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setContentText(msg);
        a.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t);
        a.setContentText(m);
        a.showAndWait();
    }
}
