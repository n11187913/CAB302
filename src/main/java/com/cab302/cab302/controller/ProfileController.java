package com.cab302.cab302.controller;

import com.cab302.cab302.Database.Backend;
import com.cab302.cab302.model.UserAccount;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Optional;

import static com.cab302.cab302.Main.changeScene;

public class ProfileController {
    @FXML private void goHome()        { changeScene("home-view.fxml"); }
    @FXML private void goLeaderboard() { changeScene("leaderboard-view.fxml"); }
    @FXML private void goAbout()       { changeScene("about-view.fxml"); }
    @FXML private void goProfile()     { /* already here */ }

    @FXML private ImageView avatar;
    @FXML private Label nameLbl, emailLbl, bioLbl, levelLbl, status;
    @FXML private ComboBox<String> languageBox;

    private Long profileId;
    private String email;

    @FXML
    private void initialize() {
        setDefaultAvatar();

        if (languageBox != null) {
            languageBox.getItems().setAll("English", "Spanish", "French", "Chinese");
            languageBox.setValue("English");
        }

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

        //  to load persisted avatar
        try (Backend db = new Backend()) {
            String path = db.getAvatarPath(profileId);
            if (path != null && !path.isBlank()) {
                File f = new File(path);
                if (f.exists()) {
                    avatar.setImage(new Image(f.toURI().toString()));
                } else {
                    // File missing on disk – fall back and clear the stale path
                    setDefaultAvatar();
                    db.updateAvatarPath(profileId, null);
                }
            }
        } catch (Exception ignore) {
            // to Keep default avatar on any failure
        }
    }

    private void setDefaultAvatar() {
        try {
            Image defaultImg = new Image(
                    getClass().getResourceAsStream("/com/cab302/cab302/Default-Profile.jpg")
            );
            avatar.setImage(defaultImg);
        } catch (Exception ignored) { }
    }

    @FXML
    private void onSignOut() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to sign out?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;

            // Clear the session without modifying AuthController class API
            clearAuthSession();

            status.setText("Signed out.");
            com.cab302.cab302.Main.changeScene("Auth/login-view.fxml");
        });
    }
    private void clearAuthSession() {
        try {
            java.lang.reflect.Field f =
                    com.cab302.cab302.controller.AuthController.class.getDeclaredField("currentUser");
            f.setAccessible(true);
            f.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) { }
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
                UserAccount ua = AuthController.getCurrentUser();
                if (ua != null) ua.setEmail(e);
                status.setText("Email updated.");
            } catch (Exception ex) {
                status.setText("Failed to update email: " + ex.getMessage());
            }
        });
    }
    @FXML
    private void goBackHome(ActionEvent event) {
        changeScene("home-view.fxml");
    }

    @FXML
    private void onChangePassword() {
        if (!ensureLoggedIn()) return;

        TextInputDialog d = new TextInputDialog();
        d.setTitle("Change Password");
        d.setHeaderText(null);
        d.setContentText("New password:");

        d.showAndWait().ifPresent(newPw -> {
            String npw = newPw.trim();
            if (npw.isEmpty()) {
                status.setText("Password unchanged.");
                return;
            }
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
            if (btn != ButtonType.YES) return;

            try (Backend db = new Backend()) {
                db.deleteUser(profileId);
            } catch (Exception ex) {
                status.setText("Failed to delete account: " + ex.getMessage());
                return;
            }

            status.setText("Account deleted.");
            changeScene("Auth/login-view.fxml");
        });
    }

    @FXML
    private void onUploadAvatar() {
        if (!ensureLoggedIn()) { status.setText("No user session."); return; }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(avatar.getScene().getWindow());
        if (file != null) {
            // Update UI
            avatar.setImage(new Image(file.toURI().toString()));
            status.setText("Profile picture updated");

            // Persist path
            try (Backend db = new Backend()) {
                db.updateAvatarPath(profileId, file.getAbsolutePath());
            } catch (Exception ex) {
                status.setText("Saved locally, but failed to store avatar path: " + ex.getMessage());
            }
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
