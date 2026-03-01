package tn.esprit.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.dto.CountryInfoDto;
import tn.esprit.entities.*;
import tn.esprit.services.*;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import nu.pattern.OpenCV;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class FrontVoyagesController implements Initializable {

    @FXML
    private FlowPane cardsContainer;

    @FXML
    private TextField searchField;

    @FXML
    private TextField minPriceField;

    @FXML
    private TextField maxPriceField;

    @FXML
    private ImageView imageDestination;

    @FXML
    private TextField rechercheField;

    @FXML
    private StackPane cameraPane;
    @FXML
    private ImageView cameraView;
    @FXML
    private Canvas drawingCanvas;
    @FXML
    private ToggleButton toggleCameraBtn;

    @FXML
    private ComboBox<String> currencyCombo;

    @FXML
    private VBox filterContainer;

    @FXML
    private TextArea aiChatArea;

    @FXML
    private TextField aiPromptField;

    @FXML
    private Button aiSendBtn;

    private final CurrencyService currencyService = new CurrencyService();
    private final GeminiService aiService = new GeminiService();
    private double currentExchangeRate = 1.0;
    private String currentCurrency = "TND";

    private VideoCapture capture;
    private java.util.concurrent.ScheduledExecutorService timer;
    private boolean cameraActive = false;
    private double lastX = -1;
    private double lastY = -1;

    private FrontController frontController;
    private final VoyageServices voyageService = new VoyageServices();
    private final UserServices userService = new UserServices();
    private final VoyageDestinationServices voyageDestinationService = new VoyageDestinationServices();
    private final ReservationVoyageServices reservationVoyageService = new ReservationVoyageServices();
    private final ReservationHotelServices reservationHotelService = new ReservationHotelServices();
    private final ReservationActiviteServices reservationActiviteService = new ReservationActiviteServices();
    private final ReservationTransportServices reservationTransportService = new ReservationTransportServices();
    private final HotelServices hotelService = new HotelServices();
    private final VoyageActiviteServices voyageActiviteService = new VoyageActiviteServices();
    private final TransportLocalServices transportLocalService = new TransportLocalServices();
    private final ReservationTransportTransportServices reservationTransportTransportService = new ReservationTransportTransportServices();
    private final CountryInfoService countryInfoService = new CountryInfoService();
    private final java.util.Set<String> searchTermsCache = new java.util.HashSet<>();

    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setFrontController(FrontController c) {
        this.frontController = c;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            OpenCV.loadShared();
        } catch (Throwable t) {
            System.err.println("Note: Could not load OpenCV locally: " + t.getMessage());
        }
        tn.esprit.utils.AnimationHelper.applyFadeIn(cardsContainer);
        setupAutoCompletion();
        if (currencyCombo != null) {
            currencyCombo.getItems().addAll("TND", "EUR", "USD");
            currencyCombo.setValue("TND");
            currencyCombo.setOnAction(e -> handleCurrencyChange());
        }

        loadCards();
    }

    private void handleCurrencyChange() {
        if (currencyCombo == null)
            return;
        String target = currencyCombo.getValue();
        if (target == null || target.equals(currentCurrency))
            return;

        if ("TND".equals(target)) {
            currentCurrency = "TND";
            currentExchangeRate = 1.0;
            loadCards(); // refresh UI with TND
            return;
        }

        currencyCombo.setDisable(true);
        Task<Double> rateTask = new Task<>() {
            @Override
            protected Double call() throws Exception {
                return currencyService.convertCurrency("TND", target, 1.0);
            }
        };

        rateTask.setOnSucceeded(e -> {
            currentCurrency = target;
            currentExchangeRate = rateTask.getValue();
            currencyCombo.setDisable(false);
            loadCards(); // refresh UI with new rate
        });

        rateTask.setOnFailed(e -> {
            currencyCombo.setDisable(false);
            currencyCombo.setValue(currentCurrency); // revert
            showError("Erreur de conversion", "Impossible de récupérer le taux de change pour " + target);
        });

        Thread th = new Thread(rateTask);
        th.setDaemon(true);
        th.start();
    }

    private void buildSearchTermsCache() {
        searchTermsCache.clear();
        try {
            List<Voyage> voyages = voyageService.afficher();
            for (Voyage v : voyages) {
                if (v.getNomVoyage() != null)
                    searchTermsCache.add(v.getNomVoyage());
                if (v.getTypeVoyage() != null)
                    searchTermsCache.add(v.getTypeVoyage());
                List<Destination> dests = voyageDestinationService.getDestinationsForVoyage(v.getIdVoyage());
                for (Destination d : dests) {
                    if (d.getPaysDepart() != null)
                        searchTermsCache.add(d.getPaysDepart());
                    if (d.getPaysArrivee() != null)
                        searchTermsCache.add(d.getPaysArrivee());
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not build search terms cache: " + e.getMessage());
        }
    }

    private void setupAutoCompletion() {
        if (searchField == null)
            return;

        buildSearchTermsCache();

        ContextMenu popup = new ContextMenu();
        popup.setStyle("-fx-max-height: 200px; -fx-font-size: 13px;");

        // Hide when clicking outside or focus loss
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                popup.hide();
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String text = newValue.trim().toLowerCase();
            if (text.isEmpty()) {
                popup.hide();
                return;
            }

            List<String> suggestions = searchTermsCache.stream()
                    .filter(term -> term.toLowerCase().contains(text))
                    .limit(10) // Limit suggestions so context menu doesn't grow huge
                    .collect(java.util.stream.Collectors.toList());

            if (suggestions.isEmpty()) {
                popup.hide();
            } else {
                popup.getItems().clear();
                for (String suggestion : suggestions) {
                    MenuItem item = new MenuItem(suggestion);
                    item.setOnAction(e -> {
                        searchField.setText(suggestion);
                        popup.hide();
                        filterVoyages(); // Trigger search action directly
                    });
                    popup.getItems().add(item);
                }

                if (!popup.isShowing()) {
                    javafx.geometry.Point2D p = searchField.localToScreen(0, searchField.getHeight());
                    if (p != null) {
                        popup.show(searchField, p.getX(), p.getY());
                    } else {
                        popup.show(searchField, javafx.geometry.Side.BOTTOM, 0, 0);
                    }
                }
            }
        });
    }

    @FXML
    public void filterVoyages() {
        loadCards();
    }

    @FXML
    public void resetFilter() {
        if (searchField != null)
            searchField.clear();
        if (minPriceField != null)
            minPriceField.clear();
        if (maxPriceField != null)
            maxPriceField.clear();
        loadCards();
    }

    public void loadCards() {
        cardsContainer.getChildren().clear();

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        try {
            if (minPriceField != null && !minPriceField.getText().trim().isEmpty()) {
                minPrice = new BigDecimal(minPriceField.getText().trim());
            }
            if (maxPriceField != null && !maxPriceField.getText().trim().isEmpty()) {
                maxPrice = new BigDecimal(maxPriceField.getText().trim());
            }
        } catch (NumberFormatException e) {
            showError("Erreur de saisie", "Veuillez entrer des montants valides.");
            return;
        }

        try {
            final BigDecimal finalMin = minPrice;
            final BigDecimal finalMax = maxPrice;

            List<Voyage> voyages = voyageService.afficher().stream()
                    .filter(v -> "visible".equalsIgnoreCase(v.getStatut()))
                    .filter(v -> {
                        if (v.getPrix() == null)
                            return true; // keep if price is unknown unless strict filter needed

                        BigDecimal convertedPrice = v.getPrix().multiply(BigDecimal.valueOf(currentExchangeRate));

                        if (finalMin != null && convertedPrice.compareTo(finalMin) < 0)
                            return false;
                        if (finalMax != null && convertedPrice.compareTo(finalMax) > 0)
                            return false;
                        return true;
                    })
                    .collect(java.util.stream.Collectors.toList());
            java.util.Map<Integer, String> guideNames = new java.util.HashMap<>();
            for (User u : userService.afficher())
                guideNames.put(u.getIdUser(), u.getNom() + " " + u.getPrenom());
            String searchText = (searchField != null && searchField.getText() != null)
                    ? searchField.getText().trim().toLowerCase()
                    : "";

            boolean isSingleChar = searchText.length() == 1;

            for (Voyage v : voyages) {
                List<Destination> dests = voyageDestinationService.getDestinationsForVoyage(v.getIdVoyage());

                if (!searchText.isEmpty()) {
                    boolean match = false;
                    if (v.getTypeVoyage() != null
                            && (isSingleChar ? v.getTypeVoyage().toLowerCase().startsWith(searchText)
                                    : v.getTypeVoyage().toLowerCase().contains(searchText)))
                        match = true;
                    if (v.getNomVoyage() != null
                            && (isSingleChar ? v.getNomVoyage().toLowerCase().startsWith(searchText)
                                    : v.getNomVoyage().toLowerCase().contains(searchText)))
                        match = true;
                    for (Destination d : dests) {
                        if (d.getPaysDepart() != null
                                && (isSingleChar ? d.getPaysDepart().toLowerCase().startsWith(searchText)
                                        : d.getPaysDepart().toLowerCase().contains(searchText)))
                            match = true;
                        if (d.getPaysArrivee() != null
                                && (isSingleChar ? d.getPaysArrivee().toLowerCase().startsWith(searchText)
                                        : d.getPaysArrivee().toLowerCase().contains(searchText)))
                            match = true;
                    }
                    if (!match)
                        continue;
                }

                List<Activite> activites = voyageActiviteService.getActivitesForVoyage(v.getIdVoyage());
                String guideName = guideNames.getOrDefault(v.getIdGuide(), "");
                VBox card = buildVoyageCard(v, guideName, dests, activites);
                cardsContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les voyages.");
        }
    }

    private VBox buildVoyageCard(Voyage v, String guideName, List<Destination> dests, List<Activite> activites) {
        VBox card = new VBox(12);
        card.getStyleClass().add("front-card");
        tn.esprit.utils.AnimationHelper.applyHoverScaleEffect(card);
        card.setPadding(new Insets(20));
        card.setPrefWidth(320);
        card.setMaxWidth(320);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(false);
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(280, 150);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imageView.setClip(clip);

        String imagePath = v.getImage();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            try {
                String imgUrl = imagePath.startsWith("http") ? imagePath
                        : new java.io.File(imagePath).toURI().toString();
                imageView.setImage(new Image(imgUrl, true));
            } catch (Exception e) {
                System.err.println("Could not load image: " + e.getMessage());
            }
        }

        Label typeL = new Label(v.getTypeVoyage() != null ? v.getTypeVoyage() : "Voyage");
        typeL.getStyleClass().add("card-title");
        String dates = (v.getDateDepart() != null ? v.getDateDepart().format(D_FMT) : "?") + " → "
                + (v.getDateRetour() != null ? v.getDateRetour().format(D_FMT) : "?");
        Label datesL = new Label(dates);
        datesL.getStyleClass().add("card-dates");

        double prixV = v.getPrix() != null ? v.getPrix().doubleValue() : 0.0;
        double prixFinal = prixV * currentExchangeRate;
        Label prixL = new Label(String.format(java.util.Locale.US, "Prix: %.2f %s", prixFinal, currentCurrency));
        prixL.getStyleClass().add("card-price");
        Label placesL = new Label("Places: " + v.getPlacesDisponibles());
        Label guideL = new Label("Guide: " + guideName);
        StringBuilder destStr = new StringBuilder();
        if (dests.isEmpty())
            destStr.append("—");
        else
            for (int i = 0; i < dests.size(); i++) {
                Destination d = dests.get(i);
                if (i > 0)
                    destStr.append(", ");
                destStr.append(d.getPaysDepart() != null ? d.getPaysDepart() : "?").append(" → ")
                        .append(d.getPaysArrivee() != null ? d.getPaysArrivee() : "?");
            }
        Label destL = new Label("Destination: " + destStr.toString());
        destL.setWrapText(true);
        destL.getStyleClass().add("card-dates");
        StringBuilder actStr = new StringBuilder("Activités: ");
        if (activites == null || activites.isEmpty())
            actStr.append("—");
        else
            for (int i = 0; i < activites.size(); i++) {
                if (i > 0)
                    actStr.append(", ");
                actStr.append(activites.get(i).getNom() != null ? activites.get(i).getNom() : "?");
            }
        Label activitesL = new Label(actStr.toString());
        activitesL.setWrapText(true);
        activitesL.getStyleClass().add("card-statut");
        if (v.getStatut() != null && !v.getStatut().isEmpty()) {
            Label statutL = new Label(v.getStatut());
            statutL.getStyleClass().add("card-statut");
            card.getChildren().addAll(imageView, typeL, datesL, prixL, placesL, guideL, destL, activitesL, statutL);
        } else {
            card.getChildren().addAll(imageView, typeL, datesL, prixL, placesL, guideL, destL, activitesL);
        }
        Button reserveBtn = new Button("Réserver");
        reserveBtn.getStyleClass().add("btn-reserve");
        reserveBtn.setOnAction(e -> onReserve(v));

        // ── Bouton Infos pays (uses first destination's paysArrivee) ──────────
        String paysArrivee = dests.isEmpty() ? null
                : (dests.get(0).getPaysArrivee());
        Button infoBtn = new Button("🌍 Infos pays");
        infoBtn.setStyle("-fx-background-color:#0d6efd; -fx-text-fill:white;"
                + " -fx-font-size:12px; -fx-padding:5 12; -fx-background-radius:6;"
                + " -fx-cursor:hand;");
        if (paysArrivee != null && !paysArrivee.isBlank()) {
            infoBtn.setOnAction(e -> onCountryInfo(paysArrivee));
        } else {
            infoBtn.setDisable(true);
        }

        HBox buttons = new HBox(10, reserveBtn, infoBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(buttons);
        return card;
    }

    /**
     * Queries RestCountries API in background and shows a Stage with country info.
     */
    private void onCountryInfo(String paysArrivee) {
        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setTitle("Chargement...");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);
        Label lbl = new Label("Informations pour \"" + paysArrivee + "\"...");
        lbl.setStyle("-fx-font-size:13px; -fx-text-fill:#333;");
        VBox loadingBox = new VBox(14, spinner, lbl);
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
     * Displays country info in a Stage with flag, timezone and Google Maps link.
     */
    private void showCountryStage(CountryInfoDto info) {
        Stage stage = new Stage();
        stage.setTitle("Infos : " + info.commonName());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

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

        Label title = new Label(info.commonName());
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#1a237e;");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 4, 0));

        String langText = (info.languages() == null || info.languages().isEmpty())
                ? "—"
                : String.join(", ", info.languages());
        String tzText = (info.timezones() == null || info.timezones().isEmpty())
                ? "—"
                : info.timezones().get(0)
                        + (info.timezones().size() > 1 ? " (+" + (info.timezones().size() - 1) + " autres)" : "");

        addInfoRowInline(grid, 0, "🌍  Nom", info.commonName());
        addInfoRowInline(grid, 1, "🏙️  Capitale", info.capital());
        addInfoRowInline(grid, 2, "💰  Devise", info.currencyName() + " (" + info.currencySymbol() + ")");
        addInfoRowInline(grid, 3, "🗣️  Langues", langText);
        addInfoRowInline(grid, 4, "🕐  Fuseau", tzText);

        Label mapsKey = new Label("🗺️  Google Maps");
        mapsKey.setStyle("-fx-font-weight:bold; -fx-text-fill:#555; -fx-font-size:13px;");
        if (info.googleMapsUrl() != null) {
            Hyperlink mapsLink = new Hyperlink("Ouvrir dans Google Maps");
            mapsLink.setStyle("-fx-text-fill:#1565c0; -fx-font-size:13px; -fx-padding:0;");
            mapsLink.setOnAction(e -> openMapInWebView(info.googleMapsUrl(), info.commonName()));
            grid.add(mapsKey, 0, 5);
            grid.add(mapsLink, 1, 5);
        } else {
            addInfoRowInline(grid, 5, "🗺️  Google Maps", "—");
        }

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color:#1a237e; -fx-text-fill:white;"
                + " -fx-font-size:13px; -fx-padding:6 24; -fx-background-radius:6;");
        closeBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(14, title, flagView, new Separator(), grid, closeBtn);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(24, 32, 20, 32));
        root.setStyle("-fx-background-color:#f8f9ff;");
        stage.setScene(new Scene(root, 440, 490));
        stage.showAndWait();
    }

    /** Opens Google Maps in an embedded WebView (not the external browser). */
    private void openMapInWebView(String url, String countryName) {
        Stage mapStage = new Stage();
        mapStage.setTitle("Google Maps — " + countryName);
        mapStage.initModality(Modality.APPLICATION_MODAL);
        WebView webView = new WebView();
        webView.getEngine().load(url);
        ProgressBar progress = new ProgressBar();
        progress.progressProperty().bind(webView.getEngine().getLoadWorker().progressProperty());
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setStyle("-fx-accent:#1a237e;");
        progress.visibleProperty().bind(webView.getEngine().getLoadWorker().runningProperty());
        Label urlLbl = new Label(url);
        urlLbl.setStyle("-fx-text-fill:#555; -fx-font-size:11px;");
        urlLbl.setMaxWidth(600);
        Button closeBtn = new Button("✕ Fermer");
        closeBtn.setStyle("-fx-background-color:#1a237e; -fx-text-fill:white;"
                + " -fx-padding:4 12; -fx-background-radius:4;");
        closeBtn.setOnAction(e -> mapStage.close());
        HBox toolbar = new HBox(10, urlLbl, closeBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));
        toolbar.setStyle("-fx-background-color:#e8eaf6; -fx-border-color:#c5cae9;"
                + " -fx-border-width:0 0 1 0;");
        HBox.setHgrow(urlLbl, Priority.ALWAYS);
        VBox root = new VBox(toolbar, progress, webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        mapStage.setScene(new Scene(root, 900, 650));
        mapStage.setResizable(true);
        mapStage.show();
    }

    private void addInfoRowInline(GridPane g, int row, String key, String value) {
        Label k = new Label(key);
        k.setStyle("-fx-font-weight:bold; -fx-text-fill:#555; -fx-font-size:13px;");
        Label v = new Label(value != null ? value : "—");
        v.setStyle("-fx-text-fill:#1a1a2e; -fx-font-size:13px;");
        v.setWrapText(true);
        v.setMaxWidth(220);
        g.add(k, 0, row);
        g.add(v, 1, row);
    }

    private void onReserve(Voyage voyage) {
        User user = SessionHolder.getCurrentUser();
        if (user == null) {
            showError("Connexion requise", "Veuillez vous connecter pour réserver.");
            return;
        }
        try {
            if (reservationVoyageService.existsByUserAndVoyage(user.getIdUser(), voyage.getIdVoyage())) {
                showError("Déjà réservé",
                        "Vous avez déjà réservé ce voyage. Vous ne pouvez pas le réserver à nouveau.");
                return;
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de vérifier vos réservations.");
            return;
        }
        if (!confirmVoyageReservation(voyage, user))
            return;
        try {
            ReservationVoyage rv = new ReservationVoyage();
            rv.setDateReservation(LocalDate.now());
            rv.setStatut(ReservationTransport.StatutReservation.EN_ATTENTE);
            rv.setMontantTotal(voyage.getPrix());
            rv.setIdUser(user.getIdUser());
            rv.setIdVoyage(voyage.getIdVoyage());
            reservationVoyageService.ajouter(rv);
            // Send confirmation email with confirm button asynchronously
            String userEmail = user.getEmail();
            String userName = user.getNom() + " " + user.getPrenom();
            String vType = voyage.getTypeVoyage() != null ? voyage.getTypeVoyage() : "Voyage";
            String dDepart = voyage.getDateDepart() != null ? voyage.getDateDepart().format(D_FMT) : "—";
            String dRetour = voyage.getDateRetour() != null ? voyage.getDateRetour().format(D_FMT) : "—";
            String montant = voyage.getPrix() != null ? voyage.getPrix().toString() : "0";
            EmailService.sendReservationConfirmation(userEmail, userName, vType, dDepart, dRetour, montant);
            showSuccess(
                    "Réservation voyage enregistrée (statut : EN ATTENTE).\nUn email de confirmation a été envoyé à "
                            + userEmail + ".");

        } catch (SQLException ex) {
            if ("DUPLICATE_VOYAGE".equals(ex.getMessage())) {
                showError("Déjà réservé", "Vous avez déjà réservé ce voyage.");
                return;
            }
            showError("Erreur", "Impossible d'enregistrer la réservation voyage.");
            return;
        }
        // Step 2: Hotel popup
        showHotelReservationPopup(user);
        // Step 3: Activity popup
        showActivityReservationPopup(voyage, user);
        // Step 4: Transport popup (optional)
        showTransportReservationPopup(voyage, user);
    }

    private boolean confirmVoyageReservation(Voyage v, User user) {
        StringBuilder content = new StringBuilder();
        content.append("Voyage: ").append(v.getTypeVoyage()).append("\n");
        content.append("Dates: ").append(v.getDateDepart() != null ? v.getDateDepart().format(D_FMT) : "").append(" - ")
                .append(v.getDateRetour() != null ? v.getDateRetour().format(D_FMT) : "").append("\n");
        content.append("Prix: ").append(v.getPrix()).append(" DT\n");
        try {
            List<Activite> activites = voyageActiviteService.getActivitesForVoyage(v.getIdVoyage());
            if (activites != null && !activites.isEmpty()) {
                content.append("Activités incluses: ");
                for (int i = 0; i < activites.size(); i++) {
                    if (i > 0)
                        content.append(", ");
                    content.append(activites.get(i).getNom()).append(" (")
                            .append(activites.get(i).getPrix() != null ? activites.get(i).getPrix() : "0")
                            .append(" DT)");
                }
                content.append("\n");
            }
        } catch (SQLException ignored) {
        }
        content.append("Client: ").append(user.getNom()).append(" ").append(user.getPrenom());
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmer réservation");
        a.setHeaderText("Réserver ce voyage ?");
        a.setContentText(content.toString());
        a.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showHotelReservationPopup(User user) {
        try {
            List<Hotel> hotels = hotelService.afficher();
            if (hotels.isEmpty())
                return;
            Dialog<ReservationHotel> d = new Dialog<>();
            d.setTitle("Réservation hôtel (optionnel)");
            d.setHeaderText("Souhaitez-vous réserver un hôtel ?");
            DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
            javafx.scene.control.DatePicker checkIn = new javafx.scene.control.DatePicker(LocalDate.now());
            javafx.scene.control.DatePicker checkOut = new javafx.scene.control.DatePicker(LocalDate.now().plusDays(1));
            DialogStyleHelper.styleDatePicker(checkIn);
            DialogStyleHelper.styleDatePicker(checkOut);
            ComboBox<Hotel> hotelCombo = new ComboBox<>();
            hotelCombo.getItems().addAll(hotels);
            hotelCombo.setConverter(new javafx.util.StringConverter<Hotel>() {
                @Override
                public String toString(Hotel h) {
                    return h == null ? "" : h.getNom() + " - " + h.getVille();
                }

                @Override
                public Hotel fromString(String s) {
                    return null;
                }
            });
            DialogStyleHelper.styleCombo(hotelCombo);
            if (!hotels.isEmpty())
                hotelCombo.getSelectionModel().selectFirst();
            GridPane g = DialogStyleHelper.buildGrid();
            DialogStyleHelper.addRow(g, 0, "Hôtel", hotelCombo);
            DialogStyleHelper.addRow(g, 1, "Check-in", checkIn);
            DialogStyleHelper.addRow(g, 2, "Check-out", checkOut);
            d.getDialogPane().setContent(g);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(btn -> {
                if (btn != ButtonType.OK)
                    return null;
                Hotel h = hotelCombo.getSelectionModel().getSelectedItem();
                if (h == null)
                    return null;
                LocalDate ci = checkIn.getValue();
                LocalDate co = checkOut.getValue();
                if (ci == null || co == null || !co.isAfter(ci))
                    return null;
                ReservationHotel rh = new ReservationHotel();
                rh.setDateCheckin(ci);
                rh.setDateCheckout(co);
                rh.setPrixTotal(h.getPrixNuit() != null
                        ? h.getPrixNuit()
                                .multiply(BigDecimal.valueOf(java.time.temporal.ChronoUnit.DAYS.between(ci, co)))
                        : BigDecimal.ZERO);
                rh.setIdUser(user.getIdUser());
                rh.setIdHotel(h.getIdHotel());
                return rh;
            });
            d.showAndWait().ifPresent(rh -> {
                try {
                    reservationHotelService.ajouter(rh);
                    showSuccess("Réservation hôtel enregistrée.");
                } catch (SQLException e) {
                    showError("Erreur", "Réservation hôtel impossible.");
                }
            });
        } catch (SQLException e) {
        }
    }

    private void showActivityReservationPopup(Voyage voyage, User user) {
        try {
            List<Activite> activites = voyageActiviteService.getActivitesForVoyage(voyage.getIdVoyage());
            if (activites.isEmpty())
                return;
            Dialog<List<Activite>> d = new Dialog<>();
            d.setTitle("Réservation activités (optionnel)");
            d.setHeaderText("Sélectionnez une ou plusieurs activités");
            DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
            ListView<Activite> listView = new ListView<>();
            listView.getItems().addAll(activites);
            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            listView.setCellFactory(lv -> new ListCell<Activite>() {
                @Override
                protected void updateItem(Activite a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(a == null || empty ? ""
                            : a.getNom() + " - " + (a.getPrix() != null ? a.getPrix() + " DT" : ""));
                }
            });
            d.getDialogPane().setContent(listView);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(
                    btn -> btn == ButtonType.OK ? new ArrayList<>(listView.getSelectionModel().getSelectedItems())
                            : null);
            d.showAndWait().ifPresent(selected -> {
                for (Activite a : selected) {
                    try {
                        ReservationActivite ra = new ReservationActivite();
                        ra.setDateReservation(LocalDate.now());
                        ra.setStatut(ReservationTransport.StatutReservation.CONFIRMEE);
                        ra.setMontantTotal(a.getPrix());
                        ra.setIdUser(user.getIdUser());
                        ra.setIdVoyage(voyage.getIdVoyage());
                        ra.setIdActivite(a.getIdActivite());
                        reservationActiviteService.ajouter(ra);
                    } catch (SQLException ex) {
                    }
                }
                if (!selected.isEmpty())
                    showSuccess("Réservation(s) activité(s) enregistrée(s).");
            });
        } catch (SQLException e) {
        }
    }

    private void showTransportReservationPopup(Voyage voyage, User user) {
        try {
            // Show ALL transports so user always has options (voyage-linked or unassigned)
            List<TransportLocal> allTransports = transportLocalService.getByVoyageOrUnassigned(voyage.getIdVoyage());
            if (allTransports.isEmpty()) {
                allTransports = transportLocalService.afficher();
            }
            if (allTransports.isEmpty())
                return;
            final List<TransportLocal> transportList = allTransports;
            javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(LocalDate.now());
            DialogStyleHelper.styleDatePicker(datePicker);
            ComboBox<TransportLocal> combo = new ComboBox<>();
            combo.setConverter(new javafx.util.StringConverter<TransportLocal>() {
                @Override
                public String toString(TransportLocal t) {
                    return t == null ? ""
                            : t.getCompagnie() + " - " + t.getTypeTransport() + " - " + t.getPrix() + " DT";
                }

                @Override
                public TransportLocal fromString(String s) {
                    return null;
                }
            });
            DialogStyleHelper.styleCombo(combo);
            java.util.Set<Integer> bookedTransportIds = new java.util.HashSet<>();
            java.util.function.Consumer<LocalDate> fillCombo = chosenDate -> {
                if (chosenDate == null)
                    return;
                try {
                    bookedTransportIds.clear();
                    bookedTransportIds
                            .addAll(reservationTransportTransportService.getTransportIdsBookedForDate(chosenDate));
                } catch (SQLException ignored) {
                }
                combo.getItems().clear();
                for (TransportLocal t : transportList) {
                    if (!bookedTransportIds.contains(t.getIdTransport()))
                        combo.getItems().add(t);
                }
                if (!combo.getItems().isEmpty())
                    combo.getSelectionModel().selectFirst();
            };
            datePicker.valueProperty().addListener((o, oldVal, newVal) -> fillCombo.accept(newVal));
            fillCombo.accept(LocalDate.now());
            Dialog<Pair<TransportLocal, LocalDate>> d = new Dialog<>();
            d.setTitle("Réservation transport (optionnel)");
            d.setHeaderText("Choisissez la date et un transport disponible");
            DialogStyleHelper.styleFrontDialogPane(d.getDialogPane());
            GridPane g = DialogStyleHelper.buildGrid();
            DialogStyleHelper.addRow(g, 0, "Date", datePicker);
            DialogStyleHelper.addRow(g, 1, "Transport", combo);
            d.getDialogPane().setContent(g);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.setResultConverter(btn -> {
                if (btn != ButtonType.OK)
                    return null;
                TransportLocal t = combo.getSelectionModel().getSelectedItem();
                if (t == null)
                    return null;
                LocalDate chosenDate = datePicker.getValue();
                if (chosenDate == null)
                    return null;
                return new Pair<>(t, chosenDate);
            });
            d.showAndWait().ifPresent(pair -> {
                TransportLocal t = pair.getKey();
                LocalDate chosenDate = pair.getValue();
                try {
                    ReservationTransport rt = new ReservationTransport();
                    rt.setDateReservation(chosenDate);
                    rt.setStatut(ReservationTransport.StatutReservation.CONFIRMEE);
                    rt.setPrixTotal(t.getPrix());
                    rt.setIdUser(user.getIdUser());
                    int idResa = reservationTransportService.ajouterAndReturnId(rt);
                    reservationTransportTransportService
                            .ajouter(new tn.esprit.entities.ReservationTransportTransport(idResa, t.getIdTransport()));
                    showSuccess("Réservation transport enregistrée.");
                } catch (SQLException e) {
                    showError("Erreur", "Réservation transport impossible.");
                }
            });
        } catch (SQLException e) {
        }
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    public void askAI() {
        String query = aiPromptField.getText();
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        // Add user query to chat
        aiChatArea.appendText("\nVous: " + query + "\n");
        aiPromptField.clear();

        // Disable input while generating
        aiPromptField.setDisable(true);
        aiSendBtn.setDisable(true);
        aiChatArea.appendText("Gemini: Réflexion en cours...\n");

        Task<String> geminiTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return aiService.getTravelResponse(query);
            }
        };

        geminiTask.setOnSucceeded(e -> {
            String aiResponse = geminiTask.getValue();
            // Replace the "Réflexion en cours..." placeholder safely
            String currentText = aiChatArea.getText();
            currentText = currentText.replace("Gemini: Réflexion en cours...\n", "");
            aiChatArea.setText(currentText);

            aiChatArea.appendText("Gemini: " + aiResponse + "\n");
            aiPromptField.setDisable(false);
            aiSendBtn.setDisable(false);
        });

        geminiTask.setOnFailed(e -> {
            String currentText = aiChatArea.getText();
            currentText = currentText.replace("Gemini: Réflexion en cours...\n", "");
            aiChatArea.setText(currentText);

            aiChatArea.appendText("Gemini: [Erreur de connexion à l'API]\n");
            aiPromptField.setDisable(false);
            aiSendBtn.setDisable(false);
            System.err.println("API AI Error: " + geminiTask.getException().getMessage());
        });

        Thread th = new Thread(geminiTask);
        th.setDaemon(true);
        th.start();
    }

    @FXML
    public void toggleFilters() {
        if (filterContainer == null)
            return;
        boolean isVisible = filterContainer.isVisible();

        if (isVisible) {
            // Collapse smoothly
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250),
                    filterContainer);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                filterContainer.setVisible(false);
                filterContainer.setManaged(false);
            });
            ft.play();
        } else {
            // Expand smoothly
            filterContainer.setVisible(true);
            filterContainer.setManaged(true);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250),
                    filterContainer);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    @FXML
    public void toggleCamera() {
        if (!cameraActive) {
            capture = new VideoCapture(0, org.opencv.videoio.Videoio.CAP_DSHOW); // Open default camera
            if (capture.isOpened()) {
                cameraActive = true;
                Runnable frameGrabber = () -> {
                    Mat frame = new Mat();
                    try {
                        if (capture.read(frame)) {
                            Mat hsv = new Mat();
                            Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);

                            // Detect Green color mask
                            Mat mask = new Mat();
                            // Strict HSV bounds for "Bright Green"
                            // hue: 40 to 80 avoids too yellow or blue.
                            // sat & val: high minimums to avoid shadows / background noise.
                            Core.inRange(hsv, new Scalar(45, 100, 100), new Scalar(75, 255, 255), mask);

                            // Morphological operations (Erode then Dilate) to remove background noise
                            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                                    new org.opencv.core.Size(5, 5));
                            Imgproc.erode(mask, mask, kernel);
                            Imgproc.dilate(mask, mask, kernel);

                            // Find contours instead of directly calculating moments of the whole mask
                            java.util.List<org.opencv.core.MatOfPoint> contours = new java.util.ArrayList<>();
                            Mat hierarchy = new Mat();
                            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL,
                                    Imgproc.CHAIN_APPROX_SIMPLE);

                            double maxArea = 800; // Strict Contour Area Filter
                            org.opencv.core.MatOfPoint largestContour = null;

                            for (org.opencv.core.MatOfPoint contour : contours) {
                                double contourArea = Imgproc.contourArea(contour);
                                if (contourArea > maxArea) {
                                    maxArea = contourArea;
                                    largestContour = contour;
                                }
                            }

                            double currentX = -1;
                            double currentY = -1;
                            if (largestContour != null) {
                                Moments moments = Imgproc.moments(largestContour);
                                double dArea = moments.get_m00();
                                if (dArea > 0) {
                                    currentX = moments.get_m10() / dArea;
                                    currentY = moments.get_m01() / dArea;
                                }
                            }

                            // Mirror image for a natural feel
                            Core.flip(frame, frame, 1);
                            if (currentX >= 0) {
                                currentX = frame.cols() - currentX;
                            }

                            double scaleX = drawingCanvas.getWidth() / frame.cols();
                            double scaleY = drawingCanvas.getHeight() / frame.rows();
                            double scaledX = currentX * scaleX;
                            double scaledY = currentY * scaleY;

                            Image img = mat2Image(frame);
                            final double x = currentX >= 0 ? scaledX : -1;
                            final double y = currentX >= 0 ? scaledY : -1;

                            Platform.runLater(() -> {
                                cameraView.setImage(img);
                                if (x >= 0 && y >= 0) {
                                    if (lastX >= 0 && lastY >= 0) {
                                        GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
                                        gc.setStroke(Color.GREEN);
                                        gc.setLineWidth(5);
                                        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                                        gc.strokeLine(lastX, lastY, x, y);
                                    }
                                    lastX = x;
                                    lastY = y;
                                } else {
                                    lastX = -1;
                                    lastY = -1;
                                }
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("Error frame: " + e.getMessage());
                    }
                };
                timer = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber, 0, 33, java.util.concurrent.TimeUnit.MILLISECONDS);
                toggleCameraBtn.setText("Arrêter Caméra");
                toggleCameraBtn.setStyle(
                        "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 6 15; -fx-background-radius: 5; -fx-cursor: hand;");
            } else {
                showError("Erreur Caméra", "Impossible d'ouvrir la webcam.");
                toggleCameraBtn.setSelected(false);
            }
        } else {
            cameraActive = false;
            toggleCameraBtn.setText("Démarrer Caméra");
            toggleCameraBtn.setStyle(
                    "-fx-background-color: #1a237e; -fx-text-fill: white; -fx-padding: 6 15; -fx-background-radius: 5; -fx-cursor: hand;");
            toggleCameraBtn.setSelected(false);
            try {
                if (timer != null)
                    timer.shutdown();
                if (capture != null)
                    capture.release();
            } catch (Exception e) {
            }
            cameraView.setImage(null);
        }
    }

    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new java.io.ByteArrayInputStream(buffer.toArray()));
    }

    @FXML
    public void clearCanvas() {
        GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        lastX = -1;
        lastY = -1;
    }

    @FXML
    public void stopAndSearch() {
        if (cameraActive)
            toggleCamera();

        WritableImage snapshot = new WritableImage((int) drawingCanvas.getWidth(), (int) drawingCanvas.getHeight());
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        // Use a solid white background for the snapshot so OCR can read black/colored
        // text easily
        params.setFill(Color.WHITE);
        drawingCanvas.snapshot(params, snapshot);
        java.awt.image.BufferedImage bImage = SwingFXUtils.fromFXImage(snapshot, null);

        showSuccess("Analyse OCR OCR du dessin en cours...");

        Task<String> ocrTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Preprocess for OCR: Ensure image is strictly black and white
                java.awt.image.BufferedImage processed = new java.awt.image.BufferedImage(bImage.getWidth(),
                        bImage.getHeight(), java.awt.image.BufferedImage.TYPE_BYTE_GRAY);

                // Convert non-white pixels (the drawn green line) to pure black, and everything
                // else to white
                for (int x = 0; x < processed.getWidth(); x++) {
                    for (int y = 0; y < processed.getHeight(); y++) {
                        int rgb = bImage.getRGB(x, y);
                        // Extract color components
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        // If it's not perfectly white (or very close), it's part of the drawing -> make
                        // it BLACK
                        if (r < 250 || g < 250 || b < 250) {
                            processed.setRGB(x, y, java.awt.Color.BLACK.getRGB());
                        } else {
                            processed.setRGB(x, y, java.awt.Color.WHITE.getRGB());
                        }
                    }
                }

                ITesseract instance = new Tesseract();
                instance.setDatapath("src/main/resources/tessdata");
                instance.setLanguage("fra");
                // Ask Tesseract to treat the image as a single character
                instance.setPageSegMode(10);

                return instance.doOCR(processed);
            }
        };

        ocrTask.setOnSucceeded(e -> {
            String result = ocrTask.getValue().replaceAll("[^a-zA-ZÀ-ÿ]", "").trim();
            if (!result.isEmpty()) {
                // Ensure we only use the first letter, even if OCR is noisy
                searchField.setText(result.substring(0, 1).toUpperCase());
                filterVoyages();
            } else {
                showError("Recherche par Dessin", "Aucun texte lisible détecté.");
            }
        });

        ocrTask.setOnFailed(e -> {
            showError("Erreur OCR", "Impossible de lire le dessin : " + ocrTask.getException().getMessage());
        });

        Thread th = new Thread(ocrTask);
        th.setDaemon(true);
        th.start();
    }

    @FXML
    public void importerImageEtChercher() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Sélectionner une Image de Destination");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        // Ensure we find the stage safely, you might need to adjust this depending on
        // how you get your stage
        java.io.File selectedFile = null;
        if (cardsContainer != null && cardsContainer.getScene() != null) {
            selectedFile = fileChooser.showOpenDialog(cardsContainer.getScene().getWindow());
        } else if (searchField != null && searchField.getScene() != null) {
            selectedFile = fileChooser.showOpenDialog(searchField.getScene().getWindow());
        } else {
            // Fallback if scenes are somehow unready (should not happen in JavaFX button
            // click)
            selectedFile = fileChooser.showOpenDialog(null);
        }

        if (selectedFile != null) {
            // Display Image
            Image img = new Image(selectedFile.toURI().toString());
            if (imageDestination != null) {
                imageDestination.setImage(img);
            }

            // Temporarily set text
            if (searchField != null) {
                searchField.setText("Analyse de l'image en cours...");
            }

            final java.io.File finalFile = selectedFile;

            // Background Task
            Task<String> visionTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return aiService.identifyDestinationFromImage(finalFile);
                }
            };

            visionTask.setOnSucceeded(e -> {
                String destination = visionTask.getValue();
                if (searchField != null) {
                    searchField.setText(destination);
                    // Automatically trigger the search if we get a result
                    filterVoyages();
                }
            });

            visionTask.setOnFailed(e -> {
                if (searchField != null) {
                    searchField.setText("Erreur d'analyse");
                }
                System.err.println("Gemini Vision API Error: " + visionTask.getException().getMessage());
            });

            Thread th = new Thread(visionTask);
            th.setDaemon(true);
            th.start();
        }
    }
}
