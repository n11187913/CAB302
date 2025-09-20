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
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuestionController {

    private ArrayList<JSONObject> questions;
    private int questionCount = 0;

    private int score = 0;
    private int highScore = 0;

    private Timeline timer;
    private final int totalSeconds = 60;
    private int secondsRemaining;

    private int currentStreak = 0;
    private int highestStreak = 0;

    private long questionStartTime = 0;
    private double fastestAnswerTime = 60;

    @FXML private Label scoreLabel;
    @FXML private Label highScoreLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressBar timerProgressBar;
    @FXML private Label streakLabel;
    @FXML private Label fastestAnswerLabel;

    @FXML private Label answerPlaceholder;
    @FXML private TextField answerField;
    @FXML private WebView questionWebView;
    @FXML private Button skipButton;

    @FXML private Button submit;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        questions = getQuestions(difficulty, 5);
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
            currentStreak++;
            if (score > highScore) highScore = score;
            scoreLabel.setText("Score: " + score);
            highScoreLabel.setText("High Score: " + highScore);

            if (currentStreak > highestStreak) highestStreak = currentStreak;
            streakLabel.setText("Streak: " + currentStreak);

            long answerTimeMillis = System.currentTimeMillis() - questionStartTime;
            double answerTimeSeconds = answerTimeMillis / 1000.0;
            double roundedTime = Math.round(answerTimeSeconds * 100.0) / 100.0;

            if (roundedTime < fastestAnswerTime) fastestAnswerTime = roundedTime;
            String formattedFastest = String.format("%.2f", fastestAnswerTime);
            fastestAnswerLabel.setText("Fastest: " + formattedFastest + " s");

            answerField.clear(); // clears the field after correct answer
            nextQuestion();
        } else {
            currentStreak = 0;
            answerField.clear(); // clear on incorrect too
            nextQuestion();
        }
    }

    private void renderLatexQuestion(String latex) {
        // Split into prefix and math content
        String html = """
    <html>
      <head>
        <script type="text/javascript" async
          src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js">
        </script>
      </head>
      <body style='overflow: hidden; color:#cccccc; background-color:#3c3c3c; height: 100%%; font-size:24px; margin: 0 auto; display: flex; justify-content: center; align-items: center;'>
        <div style='height: auto; margin: auto 0;'>
            <p style='width:100%%; text-align: center;' id="question">%s \\( %s \\)</p>
        </div>
      </body>
    </html>
    """.formatted(getPrefix(latex), getMathOnly(latex));

        questionWebView.getEngine().loadContent(html);
    }

    private String getPrefix(String latex) {
        int dollarIndex = latex.indexOf('$');
        return (dollarIndex > 0) ? latex.substring(0, dollarIndex).trim() : "";
    }

    private String getMathOnly(String latex) {
        return latex.replaceAll(".*\\$", "").replace("$", "").trim();
    }

    public void nextQuestion() {
        // If we're running low on questions, fetch more using current difficulty
        if (questionCount >= questions.size() - 2) {
            questions.addAll(getQuestions(difficulty, 5)); // 👈 pass difficulty and count
        }

        questionCount++;
        String latex = questions.get(questionCount).getString("problem");

        String newQuestionHtml = "\\( " + latex + " \\)";
        String escapedHtml = newQuestionHtml.replace("\\", "\\\\").replace("'", "\\'");

        String script = "document.getElementById('question').innerHTML = '" + escapedHtml + "';" +
                "MathJax.typeset();";

        questionWebView.getEngine().executeScript(script);
        questionStartTime = System.currentTimeMillis();
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

    private String difficulty = "easy"; // default

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    private ArrayList<JSONObject> getQuestions(String difficultyLabel, int count) {
        ArrayList<JSONObject> qs = new ArrayList<>();

        // Map difficulty label to numeric value expected by Flask API
        int difficultyLevel = switch (difficultyLabel.toLowerCase()) {
            case "easy" -> 1;
            case "medium" -> 2;
            case "hard" -> 3;
            default -> 1;
        };

        // Full pool of question types
        List<String> questionTypes = List.of(
                "algebra/basic",
//                "algebra/combine_like_terms",
//                "algebra/complex_quadratic",
                "algebra/factoring",
//                "algebra/system_of_equations",
                "calculus/power_rule_differentiation"
//                "calculus/power_rule_integration",
//                "statistics/combinations",
//                "statistics/permutations"
        );

        Random rand = new Random();

        try {
            HttpClient client = HttpClient.newHttpClient();

            for (int i = 0; i < count; i++) {
                String type = questionTypes.get(rand.nextInt(questionTypes.size()));
                String url = "https://math.saeidnia.com/" + type + "/" + difficultyLevel;

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject question = new JSONObject(response.body());
                qs.add(question);
            }

            Collections.shuffle(qs);
            return qs;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
