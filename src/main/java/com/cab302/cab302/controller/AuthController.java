package com.cab302.cab302.controller;

import com.cab302.cab302.Main;
import com.cab302.cab302.model.UserAccount;
import com.cab302.cab302.TestUserList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class AuthController {
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final UserList userDAO = new TestUserList();

    @FXML
    private void onSignUp() {
        String first = trim(firstNameField.getText());
        String last  = trim(lastNameField.getText());
        String email = trim(emailField.getText());
        String pass  = passwordField.getText();

        // added some signup valdification messages
        if (first.isEmpty() || last.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            setStatus("All fields are required.", true);
            return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            setStatus("Please enter a valid email address.", true);
            return;
        }
        if (userDAO.existsByEmail(email)) {
            setStatus("An account with that email already exists. Try logging in.", true);
            return;
        }

        userDAO.add(new UserAccount(first, last, email, pass));
        setStatus("Account created for " + first + " " + last + ". You can log in now.", false);
    }

    @FXML
    private void onLogIn() {
        String email = trim(emailField.getText());
        String pass  = passwordField.getText();

        // added some login valdification messages
        if (email.isEmpty() || pass.isEmpty()) {
            setStatus("Email and password are required.", true);
            return;
        }

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
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/cab302/cab302/home-view.fxml"));
            Scene homeScene = new Scene(loader.load(), Main.WIDTH, Main.HEIGHT);

            //ADD PAGE HERE instead of HomeController <-------------------
            //HomeController homeController = loader.getController();
           // homeController.setWelcomeMessage("Welcome, " + found.getFirstName() + "!");

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(homeScene);
        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Error loading home screen.", true);
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

//this changes label to display valdation error message below buttons
    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill: #b00020;" : "-fx-text-fill: #2e7d32;");
    }
}