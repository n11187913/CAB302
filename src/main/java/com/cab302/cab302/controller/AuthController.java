package com.cab302.cab302.controller;

import com.cab302.cab302.Database.Backend;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Optional;

import static com.cab302.cab302.Main.changeScene;

public class AuthController {

    @FXML private TextField emailField;
    @FXML private TextField focusField; // optional: default to "Other"
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private RadioButton studentRadio;
    @FXML private RadioButton teacherRadio;
    private ToggleGroup roleGroup;

    @FXML
    public void initialize() {
        if (studentRadio != null && teacherRadio != null) {
            roleGroup = new ToggleGroup();
            studentRadio.setToggleGroup(roleGroup);
            teacherRadio.setToggleGroup(roleGroup);
            studentRadio.setSelected(true);
        }
    }

    @FXML
    private void onSignUp() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String focusArea = (focusField != null) ? focusField.getText().trim() : "";
        String role = studentRadio.isSelected() ? "student" : "teacher";

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all required fields.");
            return;
        }

        if (focusArea.isEmpty()) focusArea = "Other";

        try (Backend db = new Backend()) {
            long id = db.addUser(email, password, focusArea); // Using email as username
            statusLabel.setText("Sign-up successful! Your ID: " + id);
            clearFields();
        } catch (Exception e) {
            if (e.getMessage().contains("UNIQUE")) {
                statusLabel.setText("Email already exists.");
            } else {
                statusLabel.setText("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onLogIn() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter email and password.");
            return;
        }

        try (Backend db = new Backend()) {
            boolean ok = db.authenticate(email, password);
            if (ok) {
                Optional<Backend.User> user = db.getUser(email);
                String msg = "Login successful!";
                if (user.isPresent()) msg += " Welcome " + user.get().username();
                statusLabel.setText(msg);
                goToHome();
                clearFields();
            } else {
                statusLabel.setText("Invalid email or password.");
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearFields() {
        emailField.clear();
        passwordField.clear();
        if (focusField != null) focusField.clear();
    }

    @FXML
    private void goToLogin() {
        changeScene("/com/cab302/cab302/Auth/login-view.fxml");
    }

    @FXML
    private void goToSignUp() {
        changeScene("/com/cab302/cab302/Auth/signup-view.fxml");
    }

    @FXML
    private void goToHome() {
        changeScene("/com/cab302/cab302/home-view.fxml");
    }
}
