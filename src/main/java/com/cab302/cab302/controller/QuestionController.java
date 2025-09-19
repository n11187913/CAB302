package com.cab302.cab302.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class QuestionController {

    private ArrayList<JSONObject> questions;
    private int questionCount = 0;

    private int score = 0;
    private int highScore = 0;

    private Timeline timer;
    private final int totalSeconds = 60;
    private int secondsRemaining;

    @FXML private Label scoreLabel;
    @FXML private Label highScoreLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressBar timerProgressBar;

    @FXML private Label answerPlaceholder;
    @FXML private TextField answerField;
    @FXML private WebView questionWebView;
    @FXML private Button skipButton;

    @FXML private Button submit;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        questions = getQuestions();
        renderLatexQuestion(questions.get(questionCount).getString("problem"));
        answerField.setVisible(true);
        scoreLabel.setText("Score: 0");
        highScoreLabel.setText("High Score: 0");
        startGameTimer();

        answerField.setOnAction(e -> checkAnswer());
        skipButton.setOnAction(e -> nextQuestion());
    }

    private void startGameTimer() {
        secondsRemaining = totalSeconds;
        timerLabel.setText("Time: " + secondsRemaining);
        timerProgressBar.setProgress(1.0);

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsRemaining--;
            timerLabel.setText("Time: " + secondsRemaining);
            timerProgressBar.setProgress((double) secondsRemaining / totalSeconds);
            if (secondsRemaining <= 0) {
                timer.stop();
                endGame();
            }
        }));
        timer.setCycleCount(totalSeconds);
        timer.play();
    }

    private void endGame() {
        questionWebView.getEngine().loadContent("<html><body><h2 style='color:white;'>Time's up!</h2></body></html>");
        answerField.setDisable(true);
        answerPlaceholder.setVisible(false);
        skipButton.setDisable(true);
    }

    private void checkAnswer() {
        String userAnswer = answerField.getText().trim();
        String correctAnswer = questions.get(questionCount).getString("solution").trim();

        if (userAnswer.equalsIgnoreCase(correctAnswer)) {
            score++;
            if (score > highScore) highScore = score;
            scoreLabel.setText("Score: " + score);
            highScoreLabel.setText("High Score: " + highScore);

            answerField.clear(); // 👈 This clears the field after correct answer
            nextQuestion();
        } else {
            answerField.clear(); // 👈 Optional: clear on incorrect too
        }
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
