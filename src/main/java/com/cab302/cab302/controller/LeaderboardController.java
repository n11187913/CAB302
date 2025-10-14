package com.cab302.cab302.controller;

import com.cab302.cab302.Database.Backend;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.Comparator;

public class LeaderboardController {

    @FXML
    private TableView<Backend.LeaderboardEntry> leaderboardTable;
    @FXML
    private TableColumn<Backend.LeaderboardEntry, String> nameColumn;
    @FXML
    private TableColumn<Backend.LeaderboardEntry, Integer> scoreColumn;
    @FXML
    private TableColumn<Backend.LeaderboardEntry, Integer> correctColumn;
    @FXML
    private TableColumn<Backend.LeaderboardEntry, Double> accuracyColumn;

    @FXML
    public void initialize() {

        leaderboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        nameColumn.setMaxWidth(1f * Integer.MAX_VALUE * 40); // ~40%
        scoreColumn.setMaxWidth(1f * Integer.MAX_VALUE * 20); // ~20%
        correctColumn.setMaxWidth(1f * Integer.MAX_VALUE * 20); // ~20%
        accuracyColumn.setMaxWidth(1f * Integer.MAX_VALUE * 20); // ~20%

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("highscore"));
        correctColumn.setCellValueFactory(new PropertyValueFactory<>("correctAnswers"));
        accuracyColumn.setCellValueFactory(new PropertyValueFactory<>("accuracy"));

        accuracyColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Double accuracy, boolean empty) {
                super.updateItem(accuracy, empty);
                if (empty || accuracy == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f%%", accuracy * 100));
                }
            }
        });

        try (Backend db = new Backend()) {
            ArrayList<Backend.LeaderboardEntry> leaderboard = db.getLeaderboard();

            leaderboard.sort(Comparator.comparingDouble(Backend.LeaderboardEntry::getAccuracy).reversed());

            ObservableList<Backend.LeaderboardEntry> data = FXCollections.observableArrayList(leaderboard);
            leaderboardTable.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
