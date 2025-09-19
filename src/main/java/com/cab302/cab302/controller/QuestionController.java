package com.cab302.cab302.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import javafx.scene.control.ToggleGroup;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;

import static com.cab302.cab302.Main.changeScene;

public class QuestionController {

    private ArrayList<JSONObject> questions;
    private int questionCount = 0;

    @FXML
    public Label statusLabel;

    @FXML
    private Label answerPlaceholder;

    @FXML
    private TextField answerField;

    @FXML
    private WebView questionWebView;

    @FXML
    private Button submit;

    @FXML
    public void on_user_input() {
        answerField.setVisible(false); // ensure it's hidden initially

        answerPlaceholder.setOnMouseClicked(e -> {
            answerPlaceholder.setVisible(false);
            answerField.setVisible(true);
            answerField.requestFocus();
        });

        answerField.setOnKeyTyped(e -> {
            if (!answerField.isVisible()) {
                answerPlaceholder.setVisible(false);
                answerField.setVisible(true);
            }
        });
    }

    private void renderLatexQuestion(String latex) {
        String html = """
    <html>
      <head>
        <script type="text/javascript" async
          src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js">
        </script>
      </head>
      <body style='overflow: hidden; color:#cccccc; background-color:#3c3c3c; height: 100%%; font-size:24px; margin: 0 auto; display: flex; justify-content: center; align-items: center;'>
        <div height: auto; margin: auto 0;>
            <p style='width=100%%; text-align: center;' id="question">\\( %s \\)</p>
        </div>
      </body>
    </html>
    """.formatted(latex);

        questionWebView.getEngine().loadContent(html);
    }

    public void nextQuestion() {
        if (questionCount >= questions.size() - 2) {
            questions.addAll(getQuestions());
        }
        questionCount++;
        String latex = questions.get(questionCount).getString("problem");

        String newQuestionHtml = "\\( " + latex + " \\)";

        String escapedHtml = newQuestionHtml.replace("\\", "\\\\")
                .replace("'", "\\'");

        String script = "document.getElementById('question').innerHTML = '" + escapedHtml + "';" +
                "MathJax.typeset();";

        questionWebView.getEngine().executeScript(script);
    }

    @FXML
    public void initialize() {
        questions = getQuestions();
        String latex = questions.get(questionCount).getString("problem");

        answerField.setVisible(true);

        renderLatexQuestion(latex);
    }

    @FXML
    public void onSkip() {
        nextQuestion();
    }

    @FXML
    public void onSubmit() {
        if (answerField.getText().isEmpty()) {
            statusLabel.setText("Please enter your answer");
        }
    }

    private ArrayList<JSONObject> getQuestions() {
        ArrayList<JSONObject> qs = new ArrayList<JSONObject>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://math.saeidnia.com/algebra/basic/1")).build();

            for (int i = 0; i < 3; i++) {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject question = new JSONObject(response.body());
                qs.add(question);
            }
            return qs;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
