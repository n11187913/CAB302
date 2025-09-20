package ui;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class ProfileController {

    @FXML private ImageView avatar;
    @FXML private Label nameLbl, emailLbl, bioLbl, levelLbl, status;
    @FXML private ComboBox<String> languageBox;
    @FXML private CheckBox accessibilityBox;
    @FXML private ProgressBar progressBar;

    private String username; // optional; set when login integrates

    // Simple email pattern (good enough for UI validation)
    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /** Optional entry point your teammate will call after login */
    public void initWithUser(String username) {
        this.username = username;
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

        // Language selection should do something visible
        languageBox.setOnAction(e -> {
            String sel = languageBox.getValue();
            status.setText("Language set to: " + sel);
            // TODO: persist language in your settings/profile table
        });

        // Accessibility toggle: apply/remove high contrast CSS on the Scene
        accessibilityBox.setOnAction(e -> {
            boolean on = accessibilityBox.isSelected();
            Scene scene = status.getScene();
            if (scene == null) {
                // In rare cases initialize runs before scene attaches
                status.sceneProperty().addListener((obs, o, n) -> {
                    if (n != null) toggleAccessibility(n, on);
                });
            } else {
                toggleAccessibility(scene, on);
            }
        });
    }

    private void toggleAccessibility(Scene scene, boolean on) {
        final String cssPath = "/ui/accessibility.css"; // put this file next to your FXMLs
        if (on) {
            if (getClass().getResource(cssPath) != null) {
                String uri = getClass().getResource(cssPath).toExternalForm();
                if (scene.getStylesheets().stream().noneMatch(s -> s.endsWith("accessibility.css"))) {
                    scene.getStylesheets().add(uri);
                }
                status.setText("Accessibility mode enabled");
            } else {
                status.setText("Accessibility stylesheet not found at " + cssPath);
            }
        } else {
            scene.getStylesheets().removeIf(s -> s.endsWith("accessibility.css"));
            status.setText("Accessibility mode disabled");
        }
    }

    @FXML
    private void onChangeName() {
        TextInputDialog d = new TextInputDialog(nameLbl.getText());
        d.setHeaderText("Change Name");
        d.setContentText("New name:");
        d.showAndWait().ifPresent(newName -> {
            newName = newName.trim();
            if (newName.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Name cannot be empty.").showAndWait();
                return;
            }
            nameLbl.setText(newName);
            status.setText("Name updated (persist later)");
            // TODO: persist name in backend
        });
    }

    @FXML
    private void onChangeEmail() {
        String current = (emailLbl.getText() == null || "—".equals(emailLbl.getText()))
                ? "" : emailLbl.getText();

        TextInputDialog d = new TextInputDialog(current);
        d.setHeaderText("Change Email");
        d.setContentText("New email:");

        d.showAndWait().ifPresent(input -> {
            final String email = input.trim(); // <- effectively final

            // (Optional) validate format
            // If you don't already have EMAIL_RE, add:
            // private static final Pattern EMAIL_RE = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
            if (!EMAIL_RE.matcher(email).matches()) {
                new Alert(Alert.AlertType.ERROR, "That doesn't look like a valid email.").showAndWait();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Update email to: " + email + " ?",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText("Confirm Email Change");

            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    emailLbl.setText(email);
                    status.setText("Email updated (persist later)");
                    new Alert(Alert.AlertType.INFORMATION, "Email updated to: " + email).showAndWait();
                    // TODO: persist to backend
                }
            });
        });
    }


    @FXML
    private void onChangePassword() {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText("Change Password");
        d.setContentText("New password (min 8 chars):");
        d.getEditor().setPromptText("********");
        d.showAndWait().ifPresent(newPw -> {
            if (newPw.length() < 8) {
                new Alert(Alert.AlertType.ERROR, "Password must be at least 8 characters.").showAndWait();
                return;
            }
            status.setText("Password changed (persist later)");
            new Alert(Alert.AlertType.INFORMATION, "Password changed.").showAndWait();
            // TODO: persist password (hash) in backend
        });
    }

    @FXML
    private void onDeleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete your account?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Delete Account");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                status.setText("Account deleted (backend delete TODO)");
                goToLogin();
            }
        });
    }



    private void goToLogin() {
        try {
            // Try the likely resource locations. Keep the ones you actually use.
            URL[] candidates = new URL[] {
                    getClass().getResource("/ui/login.fxml"),
                    getClass().getResource("/ui/Login-view.fxml"),
                    getClass().getResource("/com/cab302/cab302/login.fxml"),
                    getClass().getResource("/com/cab302/cab302/Login-view.fxml")
            };

            URL fxml = null;
            for (URL c : candidates) {
                if (c != null) { fxml = c; break; }
            }
            if (fxml == null) {
                throw new IllegalStateException("Login FXML not found on classpath. Check its path.");
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) status.getScene().getWindow();

            // EITHER reuse the existing Scene (keeps size and styles)…
            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            } else {
                // …or create a fresh Scene if none exists yet
                stage.setScene(new Scene(root, 960, 640));
            }

            stage.setTitle("Login");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open login: " + e.getMessage()).showAndWait();
        }
    }


    @FXML
    private void onUploadAvatar() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(avatar.getScene().getWindow());
        if (file != null) {
            avatar.setImage(new Image(file.toURI().toString()));
            status.setText("Profile picture updated");
            // TODO: persist avatar path if needed
        }
    }
}
