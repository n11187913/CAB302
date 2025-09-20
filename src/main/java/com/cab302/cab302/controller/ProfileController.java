package com.cab302.cab302.controller;

import com.cab302.cab302.Database.Backend;
import com.cab302.cab302.model.UserAccount;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class ProfileController {

    @FXML private ImageView avatar;
    @FXML private Label nameLbl, emailLbl, bioLbl, levelLbl, status;
    @FXML private ComboBox<String> languageBox;

    private Long profileId;      // from AuthController.getCurrentUser()
    private String email;        // current email for convenience

    @FXML
    private void initialize() {
        try {
            Image defaultImg = new Image(
                    getClass().getResourceAsStream("/com/cab302/cab302/Default-Profile.jpg")
            );
            avatar.setImage(defaultImg);
        } catch (Exception ignored) { }

        languageBox.getItems().setAll("English", "Spanish", "French", "Chinese");
        languageBox.setValue("English");

        UserAccount ua = AuthController.getCurrentUser();
        if (ua == null) {
            nameLbl.setText("Guest");
            emailLbl.setText("—");
            status.setText("No user loaded (waiting for login)");
            return;
        }

        profileId = ua.getId();
        email = ua.getEmail();
        nameLbl.setText((ua.getFirstName() + " " + ua.getLastName()).trim());
        emailLbl.setText(email);
        status.setText("Welcome, " + nameLbl.getText());
    }

    @FXML
    private void onChangeEmail() {
        if (!ensureLoggedIn()) return;

        TextInputDialog d = new TextInputDialog(emailLbl.getText());
        d.setTitle("Change Email");
        d.setHeaderText(null);
        d.setContentText("New email:");
        d.showAndWait().ifPresent(newEmail -> {
            String e = newEmail.trim();
            if (e.isEmpty()) { status.setText("Email not changed: empty value."); return; }
            try (Backend db = new Backend()) {
                db.updateEmail(profileId, e);
                email = e;
                emailLbl.setText(e);
                // also reflect in AuthController’s cached user
                UserAccount ua = AuthController.getCurrentUser();
                if (ua != null) ua.setEmail(e);
                status.setText("Email updated.");
            } catch (Exception ex) {
                status.setText("Failed to update email: " + ex.getMessage());
            }
        });
    }

    @FXML
    private void onChangePassword() {
        if (!ensureLoggedIn()) return;

        TextInputDialog d = new TextInputDialog();
        d.setTitle("Change Password");
        d.setHeaderText(null);
        d.setContentText("New password:");
        d.showAndWait().ifPresent(pw -> {
            String npw = pw.trim();
            if (npw.length() < 8) { status.setText("Password too short (min 8)."); return; }
            try (Backend db = new Backend()) {
                db.updatePassword(profileId, npw);
                status.setText("Password changed.");
            } catch (Exception ex) {
                status.setText("Failed to change password: " + ex.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteAccount() {
        if (!ensureLoggedIn()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete your account?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try (Backend db = new Backend()) {
                    db.deleteUser(profileId);
                } catch (Exception ex) {
                    status.setText("Failed to delete account: " + ex.getMessage());
                    return;
                }
                status.setText("Account deleted.");
                // Return to login screen
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/cab302/cab302/auth/Login-view.fxml")
                    );
                    Parent root = loader.load();
                    Stage stage = (Stage) status.getScene().getWindow();
                    stage.getScene().setRoot(root);
                    stage.setTitle("Sign In / Log In");
                    stage.centerOnScreen();
                } catch (IOException e) {
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

    @FXML
    public void onChangeName(ActionEvent actionEvent) {
        if (!ensureLoggedIn()) return;

        TextInputDialog d = new TextInputDialog(nameLbl.getText());
        d.setTitle("Change Name");
        d.setHeaderText(null);
        d.setContentText("New name:");
        Optional<String> res = d.showAndWait();
        if (res.isEmpty()) return;

        String newName = res.get().trim();
        if (newName.isEmpty()) { status.setText("Name not changed: empty value."); return; }

        try (Backend db = new Backend()) {
            db.updateName(profileId, newName);
            nameLbl.setText(newName);
            // reflect in AuthController cache
            UserAccount ua = AuthController.getCurrentUser();
            if (ua != null) {
                String[] parts = newName.split("\\s+", 2);
                ua.setFirstName(parts.length > 0 ? parts[0] : "");
                ua.setLastName(parts.length > 1 ? parts[1] : "");
            }
            status.setText("Name updated.");
        } catch (Exception e) {
            status.setText("Failed to update name: " + e.getMessage());
        }
    }

    private boolean ensureLoggedIn() {
        if (profileId == null) {
            status.setText("No user session.");
            return false;
        }
        return true;
    }
}
