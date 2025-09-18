package ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;

public class ProfileController {

    @FXML private ImageView avatar;
    @FXML private Label nameLbl, emailLbl, bioLbl, levelLbl, status;
    @FXML private ComboBox<String> languageBox;
    @FXML private CheckBox accessibilityBox;
    @FXML private ProgressBar progressBar;

    private String username; // optional; set when login integrates

    /** Optional entry point your teammate will call after login */
    public void initWithUser(String username) {
        this.username = username;
        // Populate UI with user-specific values (simple placeholders here)
        nameLbl.setText(username);
        emailLbl.setText(username + "@gmail.com");
        status.setText("Loaded profile for " + username);
    }

    /** Runs automatically after FXML loads */
    @FXML
    private void initialize() {
        // Static UI defaults so the page works even without login
        languageBox.getItems().setAll("English", "Spanish", "French", "Chinese");
        languageBox.setValue("English");
        progressBar.setProgress(0.5); // demo progress
        levelLbl.setText("⭐ Level 5");
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
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete your account?",
                ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Delete Account");
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                status.setText("Account deleted (wire backend later)");
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
}
