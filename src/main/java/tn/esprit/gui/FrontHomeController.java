package tn.esprit.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.web.WebView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.esprit.entities.Activite;
import tn.esprit.services.ActiviteServices;
import tn.esprit.services.AviationStackService;
import tn.esprit.services.OpenTripMapService;
import tn.esprit.services.WeatherService;
import tn.esprit.services.WeatherService.DayForecast;
import tn.esprit.services.WeatherService.WeatherForecast;

import java.net.URL;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class FrontHomeController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private VBox weatherContainer;
    @FXML private VBox mapSection;
    @FXML private HBox calendarSection;
    @FXML private VBox flightsSection;

    private static final String DEFAULT_LOCATION = "Tunis";
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private LocalDate calendarMonth = LocalDate.now();
    private Map<LocalDate, List<Activite>> activitiesByDate = new HashMap<>();
    private final ActiviteServices activiteService = new ActiviteServices();
    private VBox activityDetailsPanel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadWeather();
        loadFlights();
        loadActivitiesAndBuildCalendar();
        showMapPlaceholder();
    }

    private void showMapPlaceholder() {
        if (mapSection == null) return;
        mapSection.getChildren().clear();
        Label placeholder = new Label("Sélectionnez une activité (avec localisation) et cliquez sur « Voir sur la carte » pour afficher la carte et les attractions touristiques à proximité.");
        placeholder.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-wrap-text: true;");
        placeholder.setWrapText(true);
        mapSection.getChildren().add(placeholder);
    }

    private void loadWeather() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                WeatherService service = new WeatherService();
                WeatherForecast forecast = service.getForecast(DEFAULT_LOCATION, 5);
                Platform.runLater(() -> displayWeather(forecast));
            } catch (Exception e) {
                Platform.runLater(() -> showWeatherError());
            }
        });
    }

    private void displayWeather(WeatherForecast f) {
        if (weatherContainer == null) return;
        weatherContainer.getChildren().clear();
        if (f == null) {
            showWeatherError();
            return;
        }
        // Current weather card
        HBox currentBox = new HBox(20);
        currentBox.getStyleClass().add("front-card");
        currentBox.setPadding(new Insets(20));
        currentBox.setAlignment(Pos.CENTER_LEFT);

        try {
            ImageView iconView = new ImageView(new Image(f.iconUrl, true));
            iconView.setFitWidth(64);
            iconView.setFitHeight(64);
            iconView.setPreserveRatio(true);
            currentBox.getChildren().add(iconView);
        } catch (Exception ignored) { }

        VBox currentInfo = new VBox(4);
        Label locationL = new Label(f.city + ", " + f.country);
        locationL.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a2332;");
        Label tempL = new Label(String.format("%.0f °C", f.tempC));
        tempL.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
        Label condL = new Label(f.conditionText);
        condL.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        currentInfo.getChildren().addAll(locationL, tempL, condL);
        currentBox.getChildren().add(currentInfo);

        weatherContainer.getChildren().add(currentBox);

        // Forecast days
        if (!f.days.isEmpty()) {
            Label forecastTitle = new Label("Prévisions sur 5 jours");
            forecastTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a2332; -fx-padding: 16 0 8 0;");
            weatherContainer.getChildren().add(forecastTitle);

            HBox daysBox = new HBox(12);
            daysBox.setAlignment(Pos.CENTER_LEFT);
            for (DayForecast d : f.days) {
                VBox dayCard = new VBox(6);
                dayCard.getStyleClass().add("front-card");
                dayCard.setPadding(new Insets(16));
                dayCard.setMinWidth(120);
                dayCard.setAlignment(Pos.CENTER);

                String dayName = formatDayName(d.date);
                Label dateL = new Label(dayName);
                dateL.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #475569;");
                Label rangeL = new Label(String.format("%.0f° / %.0f°", d.maxTempC, d.minTempC));
                rangeL.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
                Label dayCondL = new Label(d.conditionText);
                dayCondL.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                dayCondL.setWrapText(true);
                dayCondL.setMaxWidth(100);

                dayCard.getChildren().addAll(dateL, rangeL, dayCondL);
                daysBox.getChildren().add(dayCard);
            }
            weatherContainer.getChildren().add(daysBox);
        }
    }

    private String formatDayName(String dateStr) {
        try {
            LocalDate d = LocalDate.parse(dateStr, API_DATE);
            LocalDate today = LocalDate.now();
            if (d.equals(today)) return "Aujourd'hui";
            if (d.equals(today.plusDays(1))) return "Demain";
            return d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void loadFlights() {
        loadFlightsFiltered(null, null);
    }

    private void loadFlightsFiltered(String depCountry, String arrCountry) {
        if (flightsSection == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AviationStackService service = new AviationStackService();
                List<String> countries = service.getCountries();
                String dep = (depCountry == null || depCountry.isEmpty() || "Tous".equals(depCountry)) ? null : depCountry;
                String arr = (arrCountry == null || arrCountry.isEmpty() || "Tous".equals(arrCountry)) ? null : arrCountry;
                List<AviationStackService.FlightInfo> flights = service.getFlightsFiltered(dep, arr, 100);
                Platform.runLater(() -> displayFlights(flights, countries, depCountry, arrCountry));
            } catch (Exception e) {
                Platform.runLater(() -> showFlightsError());
            }
        });
    }

    private void displayFlights(List<AviationStackService.FlightInfo> flights, List<String> countries,
                               String selectedDep, String selectedArr) {
        if (flightsSection == null) return;
        flightsSection.getChildren().clear();

        ComboBox<String> depCombo = new ComboBox<>();
        ComboBox<String> arrCombo = new ComboBox<>();
        List<String> depOpts = new ArrayList<>();
        depOpts.add("Tous");
        if (countries != null) depOpts.addAll(countries);
        List<String> arrOpts = new ArrayList<>();
        arrOpts.add("Tous");
        if (countries != null) arrOpts.addAll(countries);
        depCombo.setItems(FXCollections.observableArrayList(depOpts));
        arrCombo.setItems(FXCollections.observableArrayList(arrOpts));
        if (selectedDep != null && depOpts.contains(selectedDep)) depCombo.getSelectionModel().select(selectedDep);
        else depCombo.getSelectionModel().selectFirst();
        if (selectedArr != null && arrOpts.contains(selectedArr)) arrCombo.getSelectionModel().select(selectedArr);
        else arrCombo.getSelectionModel().selectFirst();
        depCombo.setPromptText("Pays de départ");
        arrCombo.setPromptText("Pays de destination");
        depCombo.setMinWidth(180);
        arrCombo.setMinWidth(180);
        DialogStyleHelper.styleCombo(depCombo);
        DialogStyleHelper.styleCombo(arrCombo);
        depCombo.setOnAction(e -> loadFlightsFiltered(depCombo.getSelectionModel().getSelectedItem(), arrCombo.getSelectionModel().getSelectedItem()));
        arrCombo.setOnAction(e -> loadFlightsFiltered(depCombo.getSelectionModel().getSelectedItem(), arrCombo.getSelectionModel().getSelectedItem()));

        HBox filterRow = new HBox(12);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.getChildren().addAll(
                new Label("Pays de départ:"),
                depCombo,
                new Label("Pays de destination:"),
                arrCombo
        );
        for (javafx.scene.Node n : filterRow.getChildren()) {
            if (n instanceof Label) n.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        }

        if (flights == null || flights.isEmpty()) {
            VBox card = new VBox(12);
            card.getStyleClass().add("front-card");
            card.setPadding(new Insets(20));
            Label title = new Label("Vols en temps réel (AviationStack)");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a2332;");
            Label empty = new Label("Aucun vol trouvé pour les filtres sélectionnés.");
            empty.setStyle("-fx-text-fill: #64748b;");
            card.getChildren().addAll(title, filterRow, empty);
            flightsSection.getChildren().add(card);
            return;
        }

        TableView<AviationStackService.FlightInfo> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMaxHeight(320);
        table.setMinHeight(200);

        TableColumn<AviationStackService.FlightInfo, String> colTransport = new TableColumn<>("Transport");
        colTransport.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().transport));
        colTransport.setPrefWidth(180);

        TableColumn<AviationStackService.FlightInfo, String> colDeparture = new TableColumn<>("Départ");
        colDeparture.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().departure));
        colDeparture.setPrefWidth(200);

        TableColumn<AviationStackService.FlightInfo, String> colArrival = new TableColumn<>("Arrivée");
        colArrival.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().arrival));
        colArrival.setPrefWidth(200);

        TableColumn<AviationStackService.FlightInfo, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        colStatus.setPrefWidth(100);

        table.getColumns().addAll(colTransport, colDeparture, colArrival, colStatus);
        table.getItems().addAll(flights);

        VBox card = new VBox(12);
        card.getStyleClass().add("front-card");
        card.setPadding(new Insets(20));
        Label title = new Label("Vols en temps réel (AviationStack)");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a2332;");
        card.getChildren().addAll(title, filterRow, table);
        flightsSection.getChildren().add(card);
    }

    private void showFlightsError() {
        if (flightsSection == null) return;
        flightsSection.getChildren().clear();
        Label err = new Label("Vols non disponibles. Vérifiez la clé API AviationStack dans config.properties.");
        err.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-wrap-text: true;");
        err.setWrapText(true);
        flightsSection.getChildren().add(err);
    }

    private void showWeatherError() {
        if (weatherContainer == null) return;
        weatherContainer.getChildren().clear();
        Label err = new Label("Météo non disponible.");
        err.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        weatherContainer.getChildren().add(err);
    }

    private void loadActivitiesAndBuildCalendar() {
        if (calendarSection == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Activite> activities = activiteService.afficher();
                Map<LocalDate, List<Activite>> map = new HashMap<>();
                for (Activite a : activities) {
                    if (a.getDate() != null) {
                        map.computeIfAbsent(a.getDate(), k -> new ArrayList<>()).add(a);
                    }
                }
                Platform.runLater(() -> {
                    activitiesByDate = map;
                    buildCalendar();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> buildCalendar());
            }
        });
    }

    private void buildCalendar() {
        if (calendarSection == null) return;
        calendarSection.getChildren().clear();

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER);
        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().add("calendar-nav-btn");
        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().add("calendar-nav-btn");
        Label monthLabel = new Label(calendarMonth.format(MONTH_YEAR));
        monthLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a2332; -fx-min-width: 200;");
        monthLabel.setAlignment(Pos.CENTER);
        prevBtn.setOnAction(e -> { calendarMonth = calendarMonth.minusMonths(1); buildCalendar(); });
        nextBtn.setOnAction(e -> { calendarMonth = calendarMonth.plusMonths(1); buildCalendar(); });
        header.getChildren().addAll(prevBtn, monthLabel, nextBtn);

        VBox calendarLeft = new VBox(8);
        calendarLeft.getChildren().add(header);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("activity-calendar-grid");
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(12));

        String[] weekdays = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int c = 0; c < 7; c++) {
            Label dayLabel = new Label(weekdays[c]);
            dayLabel.getStyleClass().add("calendar-weekday");
            grid.add(dayLabel, c, 0);
        }

        LocalDate first = calendarMonth.withDayOfMonth(1);
        int firstDayOfWeek = first.getDayOfWeek().getValue();
        int daysInMonth = calendarMonth.lengthOfMonth();
        LocalDate start = first.minusDays(firstDayOfWeek - 1);

        int row = 1;
        LocalDate d = start;
        for (int i = 0; i < 42; i++) {
            int col = i % 7;
            if (col == 0 && i > 0) row++;

            StackPane cell = new StackPane();
            cell.getStyleClass().add("calendar-day-cell");
            cell.setMinSize(44, 44);
            cell.setPrefSize(44, 44);

            boolean isCurrentMonth = d.getMonth() == calendarMonth.getMonth();
            boolean isToday = d.equals(LocalDate.now());
            if (!isCurrentMonth) cell.getStyleClass().add("calendar-day-other-month");
            if (isToday) cell.getStyleClass().add("calendar-day-today");

            Label dayNum = new Label(String.valueOf(d.getDayOfMonth()));
            dayNum.getStyleClass().add("calendar-day-num");
            if (!isCurrentMonth) dayNum.setStyle("-fx-text-fill: #94a3b8;");

            List<Activite> dayActivities = activitiesByDate.getOrDefault(d, Collections.emptyList());
            LocalDate clickDate = d;
            if (!dayActivities.isEmpty()) {
                cell.getStyleClass().add("calendar-day-has-activity");
                cell.setCursor(javafx.scene.Cursor.HAND);
                VBox cellContent = new VBox(2);
                cellContent.setAlignment(Pos.CENTER);
                cellContent.getChildren().addAll(dayNum, createActivityDot());
                cell.getChildren().add(cellContent);

                String tooltipText = dayActivities.stream()
                        .map(a -> {
                            String name = a.getNom() != null ? a.getNom() : "Activité";
                            if (a.getLatitude() != null && a.getLongitude() != null) {
                                name += " - " + String.format("%.4f, %.4f", a.getLatitude(), a.getLongitude());
                            }
                            return name;
                        })
                        .collect(Collectors.joining("\n"));
                Tooltip tip = new Tooltip(tooltipText + "\n(Cliquez pour les détails)");
                tip.setWrapText(true);
                tip.setMaxWidth(280);
                Tooltip.install(cell, tip);

                cell.setOnMouseClicked(e -> showActivityDetails(dayActivities, clickDate));
            } else {
                cell.getChildren().add(dayNum);
            }

            grid.add(cell, col, row);
            d = d.plusDays(1);
        }

        VBox calCard = new VBox(8);
        calCard.getStyleClass().add("front-card");
        calCard.setPadding(new Insets(20));
        calCard.getChildren().add(grid);

        VBox detailsPanel = new VBox(12);
        detailsPanel.getStyleClass().add("front-card");
        detailsPanel.setPadding(new Insets(20));
        detailsPanel.setMinWidth(320);
        detailsPanel.setPrefWidth(320);
        detailsPanel.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12;");
        Label detailsPlaceholder = new Label("Cliquez sur une date avec activité pour afficher les détails.");
        detailsPlaceholder.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-wrap-text: true;");
        detailsPlaceholder.setWrapText(true);
        detailsPanel.getChildren().add(detailsPlaceholder);
        activityDetailsPanel = detailsPanel;

        calendarLeft.getChildren().add(calCard);
        calendarSection.getChildren().addAll(calendarLeft, detailsPanel);
        calendarSection.setHgrow(detailsPanel, Priority.ALWAYS);
    }

    private void showActivityDetails(List<Activite> activities, LocalDate date) {
        if (activityDetailsPanel == null) return;
        VBox panel = activityDetailsPanel;
        panel.getChildren().clear();

        Label dateTitle = new Label("📅 " + date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
        dateTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a2332;");
        dateTitle.setWrapText(true);
        panel.getChildren().add(dateTitle);

        for (Activite a : activities) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(16));
            card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8;");
            if (a.getPhoto() != null && !a.getPhoto().isBlank()) {
                try {
                    String path = a.getPhoto();
                    if (!path.startsWith("file:") && !path.startsWith("http")) path = "file:" + path;
                    ImageView imgView = new ImageView(new Image(path, true));
                    imgView.setFitWidth(280);
                    imgView.setFitHeight(120);
                    imgView.setPreserveRatio(true);
                    card.getChildren().add(imgView);
                } catch (Exception ignored) { }
            }
            Label nameL = new Label(a.getNom() != null ? a.getNom() : "Activité");
            nameL.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2980b9;");
            nameL.setWrapText(true);
            Label descL = new Label(a.getDescription() != null ? a.getDescription() : "");
            descL.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
            descL.setWrapText(true);
            Label prixL = new Label("Prix: " + a.getPrix() + " DT");
            prixL.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a2332;");
            Label dureeL = new Label("Durée: " + a.getDuree() + " min");
            dureeL.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            String coords = (a.getLatitude() != null && a.getLongitude() != null)
                    ? String.format("%.4f, %.4f", a.getLatitude(), a.getLongitude()) : "—";
            Label coordsL = new Label("📍 " + coords);
            coordsL.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            coordsL.setWrapText(true);
            Button showMapBtn = null;
            if (a.getLatitude() != null && a.getLongitude() != null) {
                showMapBtn = new Button("Voir sur la carte");
                showMapBtn.getStyleClass().add("calendar-nav-btn");
                showMapBtn.setOnAction(ev -> loadMapForActivity(a));
            }
            if (showMapBtn != null) {
                card.getChildren().addAll(nameL, descL, prixL, dureeL, coordsL, showMapBtn);
            } else {
                card.getChildren().addAll(nameL, descL, prixL, dureeL, coordsL);
            }
            panel.getChildren().add(card);
        }
    }

    private void loadMapForActivity(Activite activity) {
        if (mapSection == null || activity.getLatitude() == null || activity.getLongitude() == null) return;
        mapSection.getChildren().clear();
        Label loadingL = new Label("Chargement de la carte et des attractions...");
        loadingL.setStyle("-fx-text-fill: #64748b;");
        mapSection.getChildren().add(loadingL);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                OpenTripMapService service = new OpenTripMapService();
                List<OpenTripMapService.TouristPlace> places = service.getPlacesNearby(
                        activity.getLatitude(), activity.getLongitude(), 5000, 15);
                String activityName = activity.getNom() != null ? activity.getNom() : "Activité";
                Platform.runLater(() -> displayMap(activity.getLatitude(), activity.getLongitude(), activityName, places));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    mapSection.getChildren().clear();
                    Label err = new Label("Impossible de charger la carte.");
                    err.setStyle("-fx-text-fill: #64748b;");
                    mapSection.getChildren().add(err);
                });
            }
        });
    }

    private void displayMap(double centerLat, double centerLon, String activityName, List<OpenTripMapService.TouristPlace> places) {
        if (mapSection == null) return;
        mapSection.getChildren().clear();

        StringBuilder markersJs = new StringBuilder();
        markersJs.append("var actMarker = L.marker([").append(centerLat).append(",").append(centerLon).append("]).addTo(map); actMarker.bindPopup(\"").append(escapeJs(activityName)).append("\");");
        for (OpenTripMapService.TouristPlace p : places) {
            String name = escapeJs(p.name);
            double dist = Math.round(p.distMeters);
            markersJs.append("L.marker([").append(p.lat).append(",").append(p.lon).append("]).addTo(map).bindPopup(\"").append(name).append(" (").append((int)dist).append(" m)\");");
        }

        String html = "<!DOCTYPE html><html><head><meta charset='utf-8'/><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script></head><body style='margin:0'><div id='map' style='width:100%;height:400px;'></div><script>var map=L.map('map').setView(["+centerLat+","+centerLon+"],15);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© OpenStreetMap'}).addTo(map);"+markersJs+"</script></body></html>";

        WebView webView = new WebView();
        webView.setMinHeight(400);
        webView.setPrefHeight(400);
        webView.getEngine().loadContent(html);

        VBox mapCard = new VBox(12);
        mapCard.getStyleClass().add("front-card");
        mapCard.setPadding(new Insets(20));
        Label mapTitle = new Label("📍 " + activityName + " — Attractions à proximité (5 km)");
        mapTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a2332;");
        mapCard.getChildren().addAll(mapTitle, webView);
        if (!places.isEmpty()) {
            Label listTitle = new Label("Liste des attractions :");
            listTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
            mapCard.getChildren().add(listTitle);
            VBox listBox = new VBox(4);
            for (OpenTripMapService.TouristPlace p : places) {
                Label pl = new Label("• " + p.name + " (" + (int)Math.round(p.distMeters) + " m)");
                pl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
                pl.setWrapText(true);
                listBox.getChildren().add(pl);
            }
            mapCard.getChildren().add(listBox);
        }
        mapSection.getChildren().add(mapCard);
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private Region createActivityDot() {
        Region dot = new Region();
        dot.setStyle("-fx-background-color: #2980b9; -fx-background-radius: 4; -fx-min-width: 8; -fx-min-height: 8; -fx-pref-width: 8; -fx-pref-height: 8;");
        return dot;
    }
}
