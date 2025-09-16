package ui;

import com.cab302.cab302.Database.Backend;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.FileChooser;

import java.io.File;

public class ProfileController {
    @FXML private ImageView avatar;
    @FXML private Label nameLbl, emailLbl, phoneLbl, bioLbl, levelLbl, status;
    @FXML private ComboBox<String> languageBox;
    @FXML private CheckBox accessibilityBox;
    @FXML private ProgressBar progressBar;

    private String username;

    public void initWithUser(String username) {
        this.username = username;
        loadProfile();
    }

    private void loadProfile() {
        try (Backend db = new Backend()) {
            db.getUser(username).ifPresentOrElse(u -> {
                nameLbl.setText(u.username());
                emailLbl.setText(u.username() + "@gmail.com"); // placeholder
                phoneLbl.setText("+0412345678");                // placeholder
                bioLbl.setText("i love solving math problems!!");
                progressBar.setProgress(0.5);
                levelLbl.setText("⭐ Level 5");
            }, () -> status.setText("User not found"));
        } catch (Exception e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        languageBox.getItems().setAll("English", "Spanish", "French", "Chinese");
        languageBox.setValue("English");
    }

    @FXML
    private void onChangeEmail() {
        TextInputDialog d = new TextInputDialog(emailLbl.getText());
        d.setHeaderText("Change Email");
        d.setContentText("New email:");
        d.showAndWait().ifPresent(newEmail -> {
            emailLbl.setText(newEmail);
            status.setText("Email updated (backend save TODO)");
        });
    }

    @FXML
    private void onChangePassword() {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText("Change Password");
        d.setContentText("New password:");
        d.showAndWait().ifPresent(newPw -> {
            status.setText("Password updated (backend save TODO)");
        });
    }

    @FXML
    private void onDeleteAccount() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete your account?",
                ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                status.setText("Account deleted (backend delete TODO)");
            }
        });
    }

    @FXML
    private void onUploadAvatar() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
        File file = fc.showOpenDialog(avatar.getScene().getWindow());
        if (file != null) {
            avatar.setImage(new Image(file.toURI().toString()));
            status.setText("Profile picture updated");
        }
    }

    // ⬇⬇⬇ This was outside the class before — put it here
    @FXML
    private void onChangeNumber() {
        TextInputDialog d = new TextInputDialog(phoneLbl.getText());
        d.setHeaderText("Change Phone Number");
        d.setContentText("New number (+countrycode…):");

        d.showAndWait().ifPresent(newPhone -> {
            String cleaned = newPhone.trim();
            if (!cleaned.matches("[+\\d][\\d\\s-]{6,20}")) {
                status.setText("Invalid number format");
                return;
            }
            try (Backend db = new Backend()) {
                var user = db.getUser(username).orElseThrow();
                db.updatePhone(user.id(), cleaned);   // your Backend method
                phoneLbl.setText(cleaned);
                status.setText("Phone number updated");
            } catch (Exception e) {
                status.setText("Failed to update number: " + e.getMessage());
            }
        });
    }
}
