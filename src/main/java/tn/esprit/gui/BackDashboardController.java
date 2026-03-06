package tn.esprit.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.TransportLocal;
import tn.esprit.entities.User;
import tn.esprit.services.DashboardStatsService;
import tn.esprit.services.UserServices;
import tn.esprit.services.VoyageSentimentService;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class BackDashboardController implements Initializable {

    @FXML private Label statUsers;
    @FXML private Label statClients;
    @FXML private Label statGuides;
    @FXML private Label statVoyages;
    @FXML private Label statHotels;
    @FXML private Label statTransport;
    @FXML private Label statActivites;
    @FXML private Label statReservations;
    @FXML private StackPane transportPieContainer;
    @FXML private StackPane transportBarContainer;
    @FXML private StackPane moduleBarContainer;
    @FXML private StackPane reservationPieContainer;
    @FXML private FlowPane sentimentCardsContainer;

    private final UserServices userServices = new UserServices();
    private final VoyageSentimentService sentimentService = new VoyageSentimentService();
    private final DashboardStatsService statsService = new DashboardStatsService();

    @Override
    public void initialize(URL location, ResourceBundle resources) { }

    public void refresh() {
        try {
            List<User> users = userServices.afficher();
            if (statUsers != null) statUsers.setText(String.valueOf(users.size()));
            if (statClients != null) statClients.setText(String.valueOf(users.stream().filter(u -> u.getRole() == User.Role.CLIENT).count()));
            if (statGuides != null) statGuides.setText(String.valueOf(users.stream().filter(u -> u.getRole() == User.Role.GUIDE_TOURISTIQUE).count()));
        } catch (SQLException e) {
            if (statUsers != null) statUsers.setText("—");
            if (statClients != null) statClients.setText("—");
            if (statGuides != null) statGuides.setText("—");
        }

        // Module counts
        Map<String, Integer> modules = statsService.getModuleCounts();
        if (statVoyages != null) statVoyages.setText(String.valueOf(modules.getOrDefault("voyages", 0)));
        if (statHotels != null) statHotels.setText(String.valueOf(modules.getOrDefault("hotels", 0)));
        if (statTransport != null) statTransport.setText(String.valueOf(modules.getOrDefault("transport", 0)));
        if (statActivites != null) statActivites.setText(String.valueOf(modules.getOrDefault("activites", 0)));
        int totalRes = statsService.getReservationCounts().values().stream().mapToInt(Integer::intValue).sum();
        if (statReservations != null) statReservations.setText(String.valueOf(totalRes));

        // Charts
        refreshTransportPieChart();
        refreshTransportBarChart();
        refreshModuleBarChart();
        refreshReservationPieChart();

        if (sentimentCardsContainer != null) {
            sentimentCardsContainer.getChildren().clear();
            for (VoyageSentimentService.VoyageSentimentStats s : sentimentService.computeAllVoyageStats()) {
                sentimentCardsContainer.getChildren().add(buildSentimentCard(s));
            }
        }
    }

    private void refreshTransportPieChart() {
        if (transportPieContainer == null) return;
        transportPieContainer.getChildren().clear();
        PieChart chart = new PieChart();
        chart.getStyleClass().add("dashboard-pie-chart");
        Map<TransportLocal.TypeTransport, Long> data = statsService.getTransportCountByType();
        for (Map.Entry<TransportLocal.TypeTransport, Long> e : data.entrySet()) {
            if (e.getValue() > 0) {
                chart.getData().add(new PieChart.Data(e.getKey().name(), e.getValue()));
            }
        }
        if (chart.getData().isEmpty()) {
            chart.getData().add(new PieChart.Data("Aucun transport", 1));
        }
        transportPieContainer.getChildren().add(chart);
    }

    private void refreshTransportBarChart() {
        if (transportBarContainer == null) return;
        transportBarContainer.getChildren().clear();
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.getStyleClass().add("dashboard-bar-chart");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> e : statsService.getTransportReservationsByCompagnie(8)) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        if (series.getData().isEmpty()) {
            series.getData().add(new XYChart.Data<>("—", 0));
        }
        chart.getData().add(series);
        transportBarContainer.getChildren().add(chart);
    }

    private void refreshModuleBarChart() {
        if (moduleBarContainer == null) return;
        moduleBarContainer.getChildren().clear();
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.getStyleClass().add("dashboard-bar-chart");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Number> e : statsService.getModuleBarData()) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        chart.getData().add(series);
        moduleBarContainer.getChildren().add(chart);
    }

    private void refreshReservationPieChart() {
        if (reservationPieContainer == null) return;
        reservationPieContainer.getChildren().clear();
        PieChart chart = new PieChart();
        chart.getStyleClass().add("dashboard-pie-chart");
        for (Map.Entry<String, Number> e : statsService.getReservationPieData()) {
            chart.getData().add(new PieChart.Data(e.getKey(), e.getValue().doubleValue()));
        }
        if (chart.getData().isEmpty()) {
            chart.getData().add(new PieChart.Data("Aucune réservation", 1));
        }
        reservationPieContainer.getChildren().add(chart);
    }

    private VBox buildSentimentCard(VoyageSentimentService.VoyageSentimentStats s) {
        double score = round(s.combinedScore, 2);
        String sentimentTheme = score > 0.2 ? "positive" : (score < -0.2 ? "negative" : "neutral");

        VBox card = new VBox(12);
        card.getStyleClass().addAll("sentiment-card", "sentiment-" + sentimentTheme);
        card.setPrefWidth(280);
        card.setMinWidth(260);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.TOP_LEFT);

        Label titleL = new Label(s.voyageName);
        titleL.getStyleClass().add("sentiment-card-title");
        titleL.setWrapText(true);
        titleL.setMaxWidth(Double.MAX_VALUE);

        HBox scoreRow = new HBox(8);
        scoreRow.setAlignment(Pos.CENTER_LEFT);
        Label scoreLabel = new Label("Score");
        scoreLabel.getStyleClass().add("sentiment-card-label");
        Label scoreVal = new Label(String.format("%.2f", score));
        scoreVal.getStyleClass().add("sentiment-card-score");
        scoreRow.getChildren().addAll(scoreLabel, scoreVal);

        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        VBox posBox = new VBox(2);
        posBox.setAlignment(Pos.CENTER);
        Label posVal = new Label(String.valueOf(s.positiveCount));
        posVal.getStyleClass().add("sentiment-stat-value");
        posVal.setStyle("-fx-text-fill: #059669;");
        Label posLbl = new Label("Positif");
        posLbl.getStyleClass().add("sentiment-stat-label");
        posBox.getChildren().addAll(posVal, posLbl);

        VBox negBox = new VBox(2);
        negBox.setAlignment(Pos.CENTER);
        Label negVal = new Label(String.valueOf(s.negativeCount));
        negVal.getStyleClass().add("sentiment-stat-value");
        negVal.setStyle("-fx-text-fill: #dc2626;");
        Label negLbl = new Label("Négatif");
        negLbl.getStyleClass().add("sentiment-stat-label");
        negBox.getChildren().addAll(negVal, negLbl);

        VBox neuBox = new VBox(2);
        neuBox.setAlignment(Pos.CENTER);
        Label neuVal = new Label(String.valueOf(s.neutralCount));
        neuVal.getStyleClass().add("sentiment-stat-value");
        neuVal.setStyle("-fx-text-fill: #64748b;");
        Label neuLbl = new Label("Neutre");
        neuLbl.getStyleClass().add("sentiment-stat-label");
        neuBox.getChildren().addAll(neuVal, neuLbl);

        statsRow.getChildren().addAll(posBox, negBox, neuBox);

        HBox reactRow = new HBox(8);
        reactRow.setAlignment(Pos.CENTER_LEFT);
        Label reactL = new Label("👍 " + s.totalLikes + "  ·  👎 " + s.totalDislikes);
        reactL.getStyleClass().add("sentiment-card-reactions");
        reactRow.getChildren().add(reactL);

        card.getChildren().addAll(titleL, scoreRow, statsRow, reactRow);
        return card;
    }

    private static double round(double v, int decimals) {
        double m = Math.pow(10, decimals);
        return Math.round(v * m) / m;
    }
}
