package xyz.nardone.agenda_fps.applicazione.utility;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import xyz.nardone.agenda_fps.applicazione.BankerSharedIdAndDates;
import xyz.nardone.agenda_fps.applicazione.BankerStatistics;
import xyz.nardone.agenda_fps.applicazione.ConnectionRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Helper for building and populating a banker statistics pie chart.
 */
public class Piechart {
    private PieChart chart;
    private BankerStatistics bankerStatistics;
    private Integer bankerId;
    private String startDate;
    private String endDate;
    private ObservableList<BankerStatistics> bankerStatsList = FXCollections.observableArrayList();

    /**
     * Creates a pie chart helper bound to a chart and optional banker/date filters.
     */
    public Piechart(PieChart chart, Integer bankerId, String startDate, String endDate) {
        this.chart = chart;
        this.bankerId = bankerId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Loads statistics and updates the chart.
     */
    public void inizialize() throws IOException {
        loadStatistics();
    }

    /**
     * Returns the last loaded banker statistics.
     */
    public BankerStatistics getBankerStatistics() {
        return bankerStatistics;
    }

    private void loadStatistics() throws IOException {
        String query = buildQueryString();
        ConnectionRequest<Void> connection = new ConnectionRequest<>("POST", "/bankers/statistics" + query);
        bankerStatistics = connection.send(BankerStatistics.class);
        bankerStatsList.add(bankerStatistics);
        BankerStatistics stats = bankerStatsList.get(0);
        updatePieChart(stats);
    }

    private void updatePieChart(BankerStatistics stats) {
        int totale = bankerStatistics.appuntamenti == null ? 0 : bankerStatistics.appuntamenti;
        int sottoscrizioni = bankerStatistics.sottoscrizioni == null ? 0 : bankerStatistics.sottoscrizioni;
        int primiAppuntamenti = bankerStatistics.primiAppuntamenti == null ? 0 : bankerStatistics.primiAppuntamenti;
        int altro = totale - (sottoscrizioni + primiAppuntamenti);

        // Ensure no negative values
        altro = Math.max(altro, 0);

        // If all values are zero, display a placeholder message
        if (totale == 0) {
            chart.setData(FXCollections.observableArrayList());
            chart.setTitle("Nessun Appuntamento"); // Display a title indicating no data
            return;
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Sottoscrizioni", sottoscrizioni),
                new PieChart.Data("Primi Appuntamenti", primiAppuntamenti),
                new PieChart.Data("Altro", altro)
        );
        chart.setData(pieChartData);
        chart.setLegendVisible(false); // Hide legend

        // Add percentage labels
        for (PieChart.Data data : pieChartData) {
            double percentage = (data.getPieValue() / (double) totale) * 100;
            String label = String.format("%s (%.1f%%)", data.getName(), percentage);
            data.nameProperty().set(label);
        }

        // Apply Custom Colors
        applyPieChartColors();
    }

    private void applyPieChartColors() {
        for (PieChart.Data data : chart.getData()) {
            String sliceColor = "";
            if (data.getName().contains("Sottoscrizioni")) {
                sliceColor = "green"; // Green
            } else if (data.getName().contains("Primi Appuntamenti")) {
                sliceColor = "lightblue"; // Blue
            } else if (data.getName().contains("Altro")) {
                sliceColor = "lightgrey"; // Grey
            }
            data.getNode().setStyle("-fx-pie-color: " + sliceColor + ";");
        }
    }

    private String buildQueryString() {
        StringBuilder queryParams = new StringBuilder();
        if (!bankerId.equals(-1)) {
            queryParams.append("bankerId=").append(BankerSharedIdAndDates.getInstance().geId()).append("&");
        }
        if (startDate != null) {queryParams.append("startDate=").append(URLEncoder.encode(startDate, StandardCharsets.UTF_8)).append("&");}
        if (endDate != null) {queryParams.append("endDate=").append(URLEncoder.encode(endDate, StandardCharsets.UTF_8)).append("&");}
        return !queryParams.isEmpty() ? "?" + queryParams.substring(0, queryParams.length() - 1) : "";
    }
}
