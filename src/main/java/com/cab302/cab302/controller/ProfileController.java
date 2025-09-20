
package com.cab302.cab302.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;


public class ProfileController {

    @FXML private ImageView avatar;
    @FXML private Label nameLbl, emailLbl, bioLbl, levelLbl, status;
    @FXML private ComboBox<String> languageBox;

    private String username;

    @FXML
    private void initialize() {

        try {
            Image defaultImg = new Image(
                    getClass().getResourceAsStream("/com/cab302/cab302/Default-Profile.jpg")
            );
            avatar.setImage(defaultImg);
        } catch (Exception e) {
            System.err.println("Could not load default avatar: " + e.getMessage());
        }
        languageBox.getItems().setAll("English", "Spanish", "French", "Chinese");
        languageBox.setValue("English");
        if (username == null) {
            nameLbl.setText("Guest");
            emailLbl.setText("—");
            status.setText("No user loaded (waiting for login)");
        }
    }

    @FXML
    private void onChangeEmail() {
        TextInputDialog d = new TextInputDialog(
                emailLbl.getText() == null || emailLbl.getText().equals("—") ? "" : emailLbl.getText());
        d.setHeaderText("Change Email");
        d.setContentText("New email:");
        d.showAndWait().ifPresent(newEmail -> {
            emailLbl.setText(newEmail.trim());
            status.setText("Email updated (persist later)");
        });
    }

    @FXML
    private void onChangePassword() {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText("Change Password");
        d.setContentText("New password:");
        d.showAndWait().ifPresent(newPw -> status.setText("Password changed (persist later)"));
    }

    @FXML
    private void onDeleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete your account?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // TODO: call backend delete here
                status.setText("Account deleted (backend delete TODO)");

                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/cab302/cab302/auth/signup-view.fxml")
                    );
                    Parent authroot = loader.load();
                    Stage stage = (Stage) status.getScene().getWindow();
                    stage.getScene().setRoot(authroot);
                    stage.setTitle("Sign up");
                    stage.centerOnScreen();

                } catch (IOException e) {
                    e.printStackTrace();
                    status.setText("Failed to go back to login: " + e.getMessage());
                }
            }
        });
    }
    @FXML
    private void onUploadAvatar() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(avatar.getScene().getWindow());
        if (file != null) {
            avatar.setImage(new Image(file.toURI().toString()));
            status.setText("Profile picture updated");
        }
    }

    public void onChangeName(ActionEvent actionEvent) {
    }
}