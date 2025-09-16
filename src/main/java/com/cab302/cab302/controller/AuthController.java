package com.cab302.cab302.controller;

import com.cab302.cab302.Database.Backend;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Optional;

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
        roleGroup = new ToggleGroup();
        studentRadio.setToggleGroup(roleGroup);
        teacherRadio.setToggleGroup(roleGroup);
        studentRadio.setSelected(true);
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
                clearFields();
            } else {
                statusLabel.setText("Invalid email or password.");
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        UserAccount found = userDAO.getByEmail(email);
        if (found == null) {
            setStatus("No account found for that email. Sign up first.", true);
            return;
        }
        if (!pass.equals(found.getPassword())) {
            setStatus("Incorrect password.", true);
            return;
        }

        // success
        setStatus("Welcome, " + found.getFirstName() + " " + found.getLastName() + "! (logged in)", false);

        //untested as i havent created the home controller page
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/cab302/cab302/base-layout.fxml"));
            Scene scene = new Scene(loader.load(), Main.WIDTH, Main.HEIGHT);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        emailField.clear();
        passwordField.clear();
        if (focusField != null) focusField.clear();
    }
}
