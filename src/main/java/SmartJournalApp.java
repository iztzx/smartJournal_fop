import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SmartJournalApp extends Application {

    private SmartJournal smartJournal;
    private User currentUser;
    private BorderPane rootLayout;
    private Scene mainScene;

    // UI Components
    private ListView<SmartJournal.JournalEntry> timelineList;
    private VBox timelineContainer;
    private Button addEntryBtn;
    private Label greetingLabel, quoteLabel;
    private String currentSummaryText;

    @Override
    public void start(Stage primaryStage) {
        smartJournal = new SmartJournal();

        // Initialize DB Schema
        DbManager.initializeDatabase();

        // 1. Show Login.
        if (!showLoginDialog()) {
            return;
        }

        // 2. Load Data
        smartJournal.setCurrentUser(currentUser);
        smartJournal.setOnLevelUp(() -> showLevelUpAlert());
        smartJournal.loadUserData();
        smartJournal.loadHistory();

        // 3. Build UI
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root-pane");

        HBox topBar = createTopBar(primaryStage);
        rootLayout.setTop(topBar);

        SplitPane mainContent = createMainContent();
        rootLayout.setCenter(mainContent);

        // Responsive sizing
        mainScene = new Scene(rootLayout, 1100, 750);
        loadCSS();

        if (currentUser == null) {
            Platform.exit();
            return;
        }

        primaryStage.setTitle("SmartJournal - " + currentUser.getDisplayName());
        primaryStage.setScene(mainScene);

        // --- ENTRANCE ANIMATION ---
        rootLayout.setOpacity(0);
        primaryStage.show();

        FadeTransition ft = new FadeTransition(Duration.millis(800), rootLayout);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        updateDynamicUI();
    }

    private void showLevelUpAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🎉 LEVEL UP!");
        alert.setHeaderText("Congratulations!");
        alert.setContentText("You reached Level " + smartJournal.getLevel() + "!");
        alert.show(); // Non-blocking if possible, but standard alert is fine
    }

    private void loadCSS() {
        String cssPath = "/journal_styles.css";
        if (getClass().getResource(cssPath) != null) {
            mainScene.getStylesheets().setAll(getClass().getResource(cssPath).toExternalForm());
        } else {
            System.err.println("CSS NOT FOUND in " + cssPath);
        }
    }

    private void updateDynamicUI() {
        Platform.runLater(() -> {
            greetingLabel.setText(
                    LanguageManager.get(smartJournal.getGreeting()) + ", " + currentUser.getDisplayName() + "!");

            String mood = "Neutral";
            if (!smartJournal.getEntries().isEmpty()) {
                mood = smartJournal.getEntries().get(0).getAiMood();
            }

            String rawQuote = smartJournal.getDailyQuote(mood);
            quoteLabel.setText("\"" + LanguageManager.get(rawQuote) + "\"");

            SmartJournal.JournalEntry today = smartJournal.getTodayEntry();
            if (today != null) {
                addEntryBtn.setText(LanguageManager.get("btn.editentry"));
                addEntryBtn.setOnAction(e -> openJournalEditor(today));
            } else {
                addEntryBtn.setText(LanguageManager.get("btn.newentry"));
                addEntryBtn.setOnAction(e -> openJournalEditor(null));
            }
        });
    }

    // --- NON-BLOCKING LOGIN DIALOG ---
    private boolean showLoginDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("login.title"));
        dialog.setHeaderText(null);

        // Remove default header implementation constraints for cleaner look
        dialog.setGraphic(null);

        StackPane container = new StackPane();
        container.setPadding(new Insets(30));
        container.setPrefWidth(400);

        // LOGIN VIEW
        VBox loginView = new VBox(20);
        loginView.setAlignment(Pos.CENTER);

        Label title = new Label(LanguageManager.get("login.signin"));
        title.getStyleClass().add("header-text");

        TextField emailLogin = new TextField();
        emailLogin.setPromptText(LanguageManager.get("prompt.email"));
        emailLogin.setPrefHeight(40);

        PasswordField passLogin = new PasswordField();
        passLogin.setPromptText(LanguageManager.get("prompt.password"));
        passLogin.setPrefHeight(40);

        Label loginError = new Label();
        loginError.setStyle("-fx-text-fill: -color-error-fg; -fx-font-weight: bold;");
        loginError.setWrapText(true);

        Button btnLogin = new Button(LanguageManager.get("btn.signin"));
        btnLogin.getStyleClass().add("primary-button");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(45);

        Hyperlink switchToReg = new Hyperlink(LanguageManager.get("login.create"));
        loginView.getChildren().addAll(title, emailLogin, passLogin, loginError, btnLogin, switchToReg);

        // REGISTER VIEW
        VBox regView = new VBox(20);
        regView.setAlignment(Pos.CENTER);
        regView.setVisible(false);

        Label regTitle = new Label(LanguageManager.get("login.create"));
        regTitle.getStyleClass().add("header-text");

        TextField emailReg = new TextField();
        emailReg.setPromptText(LanguageManager.get("prompt.email"));
        emailReg.setPrefHeight(40);

        TextField nameReg = new TextField();
        nameReg.setPromptText(LanguageManager.get("prompt.displayname"));
        nameReg.setPrefHeight(40);

        PasswordField passReg = new PasswordField();
        passReg.setPromptText(LanguageManager.get("prompt.password"));
        passReg.setPrefHeight(40);

        Label regError = new Label();
        regError.setStyle("-fx-text-fill: -color-error-fg; -fx-font-weight: bold;");
        regError.setWrapText(true);

        Button btnReg = new Button(LanguageManager.get("login.register"));
        btnReg.getStyleClass().add("primary-button"); // Ensure unified branding
        btnReg.setMaxWidth(Double.MAX_VALUE);
        btnReg.setPrefHeight(45);

        Hyperlink switchToLogin = new Hyperlink(LanguageManager.get("login.back"));
        regView.getChildren().addAll(regTitle, emailReg, nameReg, passReg, regError, btnReg, switchToLogin);

        container.getChildren().addAll(loginView, regView);
        dialog.getDialogPane().setContent(container);

        // Apply CSS to Dialog
        if (getClass().getResource("/journal_styles.css") != null) {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/journal_styles.css").toExternalForm());
        }

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false); // Hide default close button logic

        // TRANSITIONS
        switchToReg.setOnAction(e -> {
            fadeSwap(loginView, regView);
        });
        switchToLogin.setOnAction(e -> {
            fadeSwap(regView, loginView);
        });

        btnLogin.setOnAction(e -> {
            String eStr = emailLogin.getText().trim();
            String pStr = passLogin.getText();
            if (eStr.isEmpty() || pStr.isEmpty()) {
                loginError.setText("Please fill in all fields.");
                return;
            }

            loginView.setDisable(true);
            loginError.setText("Signing in...");
            new Thread(() -> {
                User user = new UserManager().login(eStr, pStr);
                Platform.runLater(() -> {
                    if (user != null) {
                        this.currentUser = user;
                        dialog.setResult(user);
                        dialog.close();
                    } else {
                        loginView.setDisable(false);
                        loginError.setText("Invalid credentials or connection error.");
                    }
                });
            }).start();
        });

        btnReg.setOnAction(e -> {
            String eStr = emailReg.getText().trim();
            String nStr = nameReg.getText().trim();
            String pStr = passReg.getText();
            if (eStr.isEmpty() || pStr.isEmpty() || nStr.isEmpty()) {
                regError.setText("All fields are required.");
                return;
            }

            regView.setDisable(true);
            regError.setText("Creating account...");
            new Thread(() -> {
                try {
                    new UserManager().register(eStr, nStr, pStr);
                    Platform.runLater(() -> {
                        // Auto-login
                        new Thread(() -> {
                            User user = new UserManager().login(eStr, pStr);
                            Platform.runLater(() -> {
                                if (user != null) {
                                    this.currentUser = user;
                                    dialog.setResult(user);
                                    dialog.close();
                                } else {
                                    regView.setDisable(false);
                                    regError.setText("Registration successful, but login failed.");
                                }
                            });
                        }).start();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        regView.setDisable(false);
                        regError.setText("Error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        Optional<User> result = dialog.showAndWait();
        if (result.isPresent())
            return true;

        Platform.exit();
        return false;
    }

    private void fadeSwap(Node fadeOutNode, Node fadeInNode) {
        FadeTransition out = new FadeTransition(Duration.millis(200), fadeOutNode);
        out.setFromValue(1.0);
        out.setToValue(0.0);
        out.setOnFinished(e -> {
            fadeOutNode.setVisible(false);
            fadeInNode.setOpacity(0.0);
            fadeInNode.setVisible(true);
            FadeTransition in = new FadeTransition(Duration.millis(200), fadeInNode);
            in.setFromValue(0.0);
            in.setToValue(1.0);
            in.play();
        });
        out.play();
    }

    private void performLogout(Stage stage) {
        currentUser = null;
        stage.close();
        Platform.runLater(() -> {
            try {
                new SmartJournalApp().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private HBox createTopBar(Stage stage) {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 25, 15, 25)); // Increased padding
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("menu-bar");

        MenuButton hamburger = new MenuButton("☰");
        hamburger.getStyleClass().add("hamburger-menu-button");

        MenuItem profileItem = new MenuItem(LanguageManager.get("menu.profile"));
        profileItem.setOnAction(e -> showProfileDialog());

        MenuItem settingsItem = new MenuItem(LanguageManager.get("menu.settings"));
        settingsItem.setOnAction(e -> showSettingsDialog(stage));

        MenuItem summaryItem = new MenuItem(LanguageManager.get("menu.summary"));
        summaryItem.setOnAction(e -> showSummaryDialog());

        MenuItem logoutItem = new MenuItem(LanguageManager.get("menu.logout"));
        logoutItem.setOnAction(e -> performLogout(stage));

        hamburger.getItems().addAll(profileItem, settingsItem, summaryItem, new SeparatorMenuItem(), logoutItem);

        Label title = new Label("SmartJournal");
        title.setStyle("-fx-font-weight: 800; -fx-font-size: 18px; -fx-text-fill: -color-text-primary;");

        // Spacer to push title to right or keep left? Keeping left for standard look.
        topBar.getChildren().addAll(hamburger, title);
        return topBar;
    }

    private void showProfileDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(30));

        TextField nameField = new TextField(currentUser.getDisplayName());
        nameField.setPrefHeight(35);
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter new password");
        passField.setPrefHeight(35);

        grid.add(new Label("Display Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(passField, 1, 1);

        Label info = new Label("(Leave password blank to keep current)");
        info.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-secondary;");
        grid.add(info, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (getClass().getResource("/journal_styles.css") != null) {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/journal_styles.css").toExternalForm());
        }

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String newPass = passField.getText().isEmpty() ? currentUser.getPassword() : passField.getText();
                new UserManager().updateProfile(currentUser, nameField.getText(), newPass);
                return true;
            }
            return false;
        });

        dialog.showAndWait();
        updateDynamicUI();
    }

    private void showSettingsDialog(Stage stage) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("settings.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPrefSize(500, 350);

        // --- TAB 1: GENERAL ---
        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(20);
        generalGrid.setVgap(20);
        generalGrid.setPadding(new Insets(20));

        ComboBox<String> langBox = new ComboBox<>();
        langBox.getItems().addAll("English", "Bahasa Malaysia");
        langBox.setValue(LanguageManager.getCurrentLanguage());
        langBox.setPrefWidth(150);
        langBox.setOnAction(e -> {
            String selected = langBox.getValue();
            if (!selected.equals(LanguageManager.getCurrentLanguage())) {
                LanguageManager.setLanguage(selected);
                dialog.close();
                stage.close();
                Platform.runLater(() -> {
                    try {
                        new SmartJournalApp().start(new Stage());
                    } catch (Exception ex) {
                    }
                });
            }
        });

        ComboBox<String> weekStartBox = new ComboBox<>();
        weekStartBox.getItems().addAll("SUNDAY", "MONDAY", "SATURDAY");
        weekStartBox.setValue(currentUser.getStartOfWeek());
        weekStartBox.setPrefWidth(150);
        weekStartBox.setOnAction(e -> {
            String newStart = weekStartBox.getValue();
            if (!newStart.equals(currentUser.getStartOfWeek())) {
                new UserManager().updateProfile(currentUser, currentUser.getDisplayName(), null, newStart);
            }
        });

        generalGrid.add(new Label(LanguageManager.get("settings.language")), 0, 0);
        generalGrid.add(langBox, 1, 0);
        generalGrid.add(new Label("Start of Week:"), 0, 1);
        generalGrid.add(weekStartBox, 1, 1);

        Tab generalTab = new Tab("General", generalGrid);

        // --- TAB 2: APPEARANCE ---
        GridPane appearGrid = new GridPane();
        appearGrid.setHgap(20);
        appearGrid.setVgap(20);
        appearGrid.setPadding(new Insets(20));

        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.getItems().addAll("Light", "Dark");
        themeBox.setValue(rootLayout.getStyleClass().contains("dark-theme") ? "Dark" : "Light");
        themeBox.setPrefWidth(150);
        themeBox.setOnAction(e -> {
            String selected = themeBox.getValue();
            if (selected.equals("Dark")) {
                if (smartJournal.isThemeUnlocked("Dark")) {
                    if (!rootLayout.getStyleClass().contains("dark-theme")) {
                        rootLayout.getStyleClass().add("dark-theme");
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Dark Mode is unlocked at Level 5!");
                    alert.show();
                    themeBox.setValue("Light");
                }
            } else {
                rootLayout.getStyleClass().remove("dark-theme");
            }
        });

        appearGrid.add(new Label(LanguageManager.get("settings.theme")), 0, 0);
        appearGrid.add(themeBox, 1, 0);

        Tab appearTab = new Tab("Appearance", appearGrid);

        // --- TAB 3: INFO / ACCOUNT ---
        VBox infoBox = new VBox(15);
        infoBox.setPadding(new Insets(20));

        Label rewardsTitle = new Label("Unlockable Themes:");
        rewardsTitle.setStyle("-fx-font-weight: bold;");
        TextArea rewardsInfo = new TextArea("Lvl 5: Dark Theme\nLvl 10: Nature Theme\nLvl 15: Ocean Theme");
        rewardsInfo.setEditable(false);
        rewardsInfo.setPrefHeight(100);
        rewardsInfo.getStyleClass().add("text-area-readonly"); // Add specific styling if needed

        infoBox.getChildren().addAll(rewardsTitle, rewardsInfo);

        Tab infoTab = new Tab("Info", infoBox);

        tabPane.getTabs().addAll(generalTab, appearTab, infoTab);

        dialog.getDialogPane().setContent(tabPane);
        if (getClass().getResource("/journal_styles.css") != null) {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/journal_styles.css").toExternalForm());
        }
        dialog.showAndWait();
    }

    private void showSummaryDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("summary.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Large & Clean Dialog
        dialog.getDialogPane().setPrefWidth(800);
        dialog.getDialogPane().setPrefHeight(650);
        dialog.setResizable(true);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        if (smartJournal.getWeeklyStats().isEmpty()) {
            content.getChildren().add(new Label(LanguageManager.get("summary.noentries")));
        } else {
            TableView<SmartJournal.JournalEntry> table = new TableView<>();
            table.getStyleClass().add("summary-table");

            TableColumn<SmartJournal.JournalEntry, String> dateCol = new TableColumn<>(LanguageManager.get("col.date"));
            dateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getDate().toString()));
            dateCol.setPrefWidth(150);

            TableColumn<SmartJournal.JournalEntry, String> moodCol = new TableColumn<>(LanguageManager.get("col.mood"));
            moodCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getAiMood()));
            moodCol.setPrefWidth(120);

            TableColumn<SmartJournal.JournalEntry, String> weatherCol = new TableColumn<>("Weather");
            weatherCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getWeather()));
            weatherCol.setPrefWidth(200);

            table.getColumns().add(dateCol);
            table.getColumns().add(moodCol);
            table.getColumns().add(weatherCol);
            table.setItems(smartJournal.getWeeklyStats());
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.setPrefHeight(250);

            // Row Color Factory
            table.setRowFactory(tv -> new TableRow<SmartJournal.JournalEntry>() {
                @Override
                protected void updateItem(SmartJournal.JournalEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("table-row-very-positive", "table-row-positive",
                            "table-row-very-negative", "table-row-negative", "table-row-neutral");
                    if (item == null || empty)
                        return;

                    String mood = item.getAiMood();
                    if (mood == null)
                        mood = "Neutral";

                    if (mood.contains("5 stars") || "Very Positive".equalsIgnoreCase(mood)) {
                        getStyleClass().add("table-row-very-positive");
                    } else if (mood.contains("4 stars") || "Positive".equalsIgnoreCase(mood)) {
                        getStyleClass().add("table-row-positive");
                    } else if (mood.contains("1 star") || "Very Negative".equalsIgnoreCase(mood)) {
                        getStyleClass().add("table-row-very-negative");
                    } else if (mood.contains("2 stars") || "Negative".equalsIgnoreCase(mood)) {
                        getStyleClass().add("table-row-negative");
                    } else {
                        getStyleClass().add("table-row-neutral");
                    }
                }
            });

            Label assessmentLabel = new Label(LanguageManager.get("summary.assessment"));
            assessmentLabel.getStyleClass().add("subheader-text");

            // WEBVIEW FOR MARKDOWN CONTENT
            WebView summaryView = new WebView();
            WebEngine engine = summaryView.getEngine();
            summaryView.setPrefHeight(300);

            // Make WebView transparent-ish (requires JavaFX trickery, usually just matching
            // background color is easier)
            summaryView.setStyle("-fx-page-fill: transparent;");

            // Initial loading state
            boolean isDark = rootLayout.getStyleClass().contains("dark-theme");
            engine.loadContent(MarkdownRenderer.renderHtml("*Gathering insights for you...*", isDark));

            content.getChildren().addAll(table, assessmentLabel, summaryView);

            Button exportBtn = new Button("Export to File");
            exportBtn.getStyleClass().add("secondary-button");
            exportBtn.setOnAction(e -> exportSummaryToFile());

            content.getChildren().add(exportBtn);

            new Thread(() -> {
                try {
                    boolean isEnglish = "English".equals(LanguageManager.getCurrentLanguage());
                    String summary = SummaryGenerator.generate(smartJournal.getWeeklyStats(), isEnglish);
                    currentSummaryText = summary;
                    Platform.runLater(() -> {
                        boolean darkTheme = rootLayout.getStyleClass().contains("dark-theme");
                        String htmlContent = MarkdownRenderer.renderHtml(summary, darkTheme);
                        engine.loadContent(htmlContent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        engine.loadContent(
                                MarkdownRenderer.renderHtml("Error generating summary: " + e.getMessage(), isDark));
                    });
                }
            }).start();
        }

        dialog.getDialogPane().setContent(content);
        if (getClass().getResource("/journal_styles.css") != null) {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/journal_styles.css").toExternalForm());
        }
        dialog.showAndWait();
    }

    private void exportSummaryToFile() {
        if (currentSummaryText == null || currentSummaryText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Export Failed");
            alert.setHeaderText("No Summary Available");
            alert.setContentText("Please wait for the summary to generate before exporting.");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Summary as Text");
        fileChooser.setInitialFileName("Weekly_Summary.txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            boolean success = SummaryGenerator.saveToTextFile(currentSummaryText, file);
            if (success) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText("Summary Exported");
                alert.setContentText("Summary has been exported to " + file.getAbsolutePath());
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText("Error Exporting Summary");
                alert.setContentText("An error occurred while exporting the summary.");
                alert.showAndWait();
            }
        }
    }

    private SplitPane createMainContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.65); // Give more space to timeline

        // LEFT: Timeline
        timelineContainer = new VBox(20);
        timelineContainer.getStyleClass().add("timeline-container");
        timelineContainer.setPadding(new Insets(30));

        greetingLabel = new Label(LanguageManager.get("timeline.welcome"));
        greetingLabel.getStyleClass().add("greeting-text");

        quoteLabel = new Label("Loading inspiration...");
        quoteLabel.getStyleClass().add("quote-text");

        VBox greetingBox = new VBox(5, greetingLabel, quoteLabel);

        addEntryBtn = new Button(LanguageManager.get("btn.newentry"));
        addEntryBtn.getStyleClass().add("primary-button");
        addEntryBtn.setOnAction(e -> openJournalEditor(null));

        // Add subtle scale animation on hover for this main button
        addEntryBtn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), addEntryBtn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        addEntryBtn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), addEntryBtn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        HBox actionBox = new HBox(addEntryBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        timelineList = new ListView<>();
        timelineList.getStyleClass().add("timeline-list");
        timelineList.setItems(smartJournal.getEntries());
        timelineList.setCellFactory(param -> new TimelineCell(this));

        VBox.setVgrow(timelineList, Priority.ALWAYS);

        Label emptyState = new Label(LanguageManager.get("timeline.empty"));
        emptyState.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 16px;");

        StackPane listStack = new StackPane(timelineList, emptyState);
        emptyState.visibleProperty().bind(Bindings.isEmpty(smartJournal.getEntries()));
        timelineList.visibleProperty().bind(Bindings.isEmpty(smartJournal.getEntries()).not());

        timelineContainer.getChildren().addAll(greetingBox, actionBox, listStack);

        // RIGHT: Gamification
        Node statsPanel = createGamificationPanel();

        splitPane.getItems().addAll(timelineContainer, statsPanel);
        return splitPane;
    }

    private Node createGamificationPanel() {
        VBox vbox = new VBox(20);
        vbox.getStyleClass().add("gamification-content");
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.TOP_CENTER);

        // --- LEVEL & STREAK ---
        Label title = new Label(LanguageManager.get("gamification.progress"));
        title.getStyleClass().add("section-title");

        HBox levelBox = new HBox(20);
        levelBox.setAlignment(Pos.CENTER_LEFT);
        levelBox.getStyleClass().add("level-container");

        StackPane levelBadge = new StackPane();
        levelBadge.getStyleClass().add("level-badge");
        Label levelLabel = new Label();
        levelLabel.getStyleClass().add("level-number");
        levelLabel.textProperty().bind(smartJournal.levelProperty().asString());
        levelBadge.getChildren().add(levelLabel);

        VBox levelMeta = new VBox(5);
        levelMeta.setAlignment(Pos.CENTER_LEFT);

        Label lvlTitle = new Label(LanguageManager.get("gamification.level"));
        lvlTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-secondary;");

        Label streakLabel = new Label();
        streakLabel.getStyleClass().add("streak-label");
        streakLabel.textProperty()
                .bind(smartJournal.streakProperty().asString(LanguageManager.get("gamification.streak")));

        levelMeta.getChildren().addAll(lvlTitle, streakLabel);
        levelBox.getChildren().addAll(levelBadge, levelMeta);

        ProgressBar xpProgressBar = new ProgressBar(0);
        xpProgressBar.setMaxWidth(Double.MAX_VALUE); // Full width
        xpProgressBar.getStyleClass().add("progress-bar");
        xpProgressBar.progressProperty().bind(smartJournal.xpProperty().divide(500.0));

        Label xpLabel = new Label();
        xpLabel.getStyleClass().add("xp-label");
        xpLabel.textProperty().bind(smartJournal.xpProperty().asString("%d / 500 XP"));

        // --- QUESTS ---
        Label questsTitle = new Label(LanguageManager.get("gamification.quests"));
        questsTitle.getStyleClass().add("section-title");

        VBox questsBox = new VBox(15);
        for (Quest q : GamificationManager.getDailyQuests(currentUser)) {
            VBox qCard = new VBox(5);
            qCard.getStyleClass().add("quest-card");

            Label qDesc = new Label(q.getDescription());
            qDesc.getStyleClass().add("quest-desc");

            Label qRew = new Label("REWARD: +" + q.getXpReward() + " XP");
            qRew.getStyleClass().add("quest-reward");

            qCard.getChildren().addAll(qDesc, qRew);
            questsBox.getChildren().add(qCard);
        }

        // --- ACHIEVEMENTS ---
        Label achTitle = new Label(LanguageManager.get("gamification.achievements"));
        achTitle.getStyleClass().add("section-title");

        FlowPane achPane = new FlowPane();
        achPane.getStyleClass().add("achievement-grid");
        achPane.setPrefWrapLength(250); // Improved wrap

        for (Achievement a : GamificationManager.getAchievements(currentUser)) {
            VBox aBadge = new VBox(5);
            aBadge.getStyleClass().add("achievement-badge");
            if (a.isUnlocked()) {
                aBadge.getStyleClass().add("achievement-unlocked");
            } else {
                aBadge.setOpacity(0.4);
            }

            Label icon = new Label(a.getIcon());
            icon.getStyleClass().add("achievement-icon");
            aBadge.getChildren().add(icon);

            String tTitle = a.getTitle();
            String tDesc = a.getDescription();

            Tooltip t = new Tooltip(tTitle + "\n" + tDesc);
            t.setShowDelay(Duration.millis(100)); // Faster tooltip
            Tooltip.install(aBadge, t);

            achPane.getChildren().add(aBadge);
        }

        vbox.getChildren().addAll(
                title, levelBox,
                xpLabel, xpProgressBar,
                questsTitle, questsBox,
                achTitle, achPane);

        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("gamification-scroll");
        return scroll;
    }

    // --- EDITOR ---
    void openJournalEditor(SmartJournal.JournalEntry existingEntry) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(existingEntry == null ? LanguageManager.get("editor.new") : LanguageManager.get("editor.edit"));

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.getStyleClass().add("editor-modal");

        Label prompt = new Label(existingEntry == null ? LanguageManager.get("editor.prompt.new")
                : LanguageManager.get("editor.prompt.edit"));
        prompt.getStyleClass().add("subheader-text");

        TextArea contentArea = new TextArea();
        contentArea.setWrapText(true);
        contentArea.setPromptText("Write your thoughts here...");
        contentArea.setStyle("-fx-font-size: 16px; -fx-font-family: 'Segoe UI';");
        if (existingEntry != null)
            contentArea.setText(existingEntry.getContent());

        Button saveBtn = new Button(LanguageManager.get("btn.save"));
        saveBtn.getStyleClass().add("primary-button");
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-style: italic;");

        saveBtn.setOnAction(e -> {
            String text = contentArea.getText();
            if (text.isEmpty())
                return;

            saveBtn.setDisable(true);
            statusLabel.setText("Saving & Analyzing Sentiment...");

            // Show loading animation on button text usually, but changing label is fine

            LocalDate targetDate = (existingEntry != null) ? existingEntry.getDate() : LocalDate.now();

            new Thread(() -> {
                String weather;
                if (existingEntry != null) {
                    weather = existingEntry.getWeather();
                } else {
                    boolean isEnglish = "English".equals(LanguageManager.getCurrentLanguage());
                    // Use new robust WeatherManager
                    weather = WeatherManager.getCurrentWeather(isEnglish);
                }

                smartJournal.processEntry(targetDate, text, weather);

                Platform.runLater(() -> {
                    modal.close();
                    updateDynamicUI();
                });
            }).start();
        });

        layout.getChildren().addAll(prompt, contentArea, statusLabel, saveBtn);
        Scene scene = new Scene(layout, 600, 500); // Larger editor
        if (getClass().getResource("/journal_styles.css") != null) {
            scene.getStylesheets().setAll(getClass().getResource("/journal_styles.css").toExternalForm());
        }
        if (rootLayout.getStyleClass().contains("dark-theme")) {
            layout.getStyleClass().add("dark-theme");
        }

        modal.setScene(scene);
        modal.showAndWait();
    }

    // --- TIMELINE CELL (With Animation & New Design) ---
    static class TimelineCell extends ListCell<SmartJournal.JournalEntry> {
        private final HBox root = new HBox(0);
        private final StackPane markerPane = new StackPane(); // StackPane for continuous line
        private final Region line = new Region();
        private final StackPane dotContainer = new StackPane();
        private final Region dotShape = new Region();
        private final Label dotLabel = new Label();

        private final VBox cardContainer = new VBox(5);
        private final Label dateLabel = new Label();
        private final Label contentPreview = new Label();
        private final HBox tagsBox = new HBox(10);
        private final Label moodTag = new Label();
        private final Label weatherTag = new Label();
        private final Button editBtn = new Button("✎");
        private final BorderPane headerPane = new BorderPane();
        private final SmartJournalApp app;

        private SmartJournal.JournalEntry lastItem = null;

        public TimelineCell(SmartJournalApp app) {
            this.app = app;
            this.setPadding(new Insets(0));
            this.setStyle("-fx-padding: 0px; -fx-background-color: transparent;");

            // --- LEFT MARKER (StackPane for continuous line) ---
            markerPane.getStyleClass().add("timeline-marker-pane");
            markerPane.setMinWidth(60);
            markerPane.setPrefWidth(60);
            markerPane.setAlignment(Pos.TOP_CENTER);

            // Continuous Line
            line.getStyleClass().add("timeline-line");
            line.setMaxHeight(Double.MAX_VALUE); // Stretch full height
            line.setMaxWidth(1); // Force thin line (prevent StackPane stretch)
            line.setMinWidth(1);
            line.setPrefWidth(1);

            // Dot Bubble
            dotContainer.getStyleClass().add("timeline-dot-container");
            dotShape.getStyleClass().add("timeline-dot-shape");
            dotLabel.getStyleClass().add("timeline-dot-label");
            dotContainer.getChildren().addAll(dotShape, dotLabel);

            // Margin to align dot with the card content/header (approx 20px down)
            StackPane.setMargin(dotContainer, new Insets(15, 0, 0, 0));

            // Scroll to Item on Click
            dotContainer.setOnMouseClicked(e -> {
                if (getListView() != null && getItem() != null) {
                    getListView().scrollTo(getItem());
                }
            });

            markerPane.getChildren().addAll(line, dotContainer);

            // --- RIGHT CARD ---
            cardContainer.getStyleClass().add("timeline-card");
            HBox.setHgrow(cardContainer, Priority.ALWAYS);
            HBox.setMargin(cardContainer, new Insets(10, 20, 10, 0));

            dateLabel.getStyleClass().add("card-date");
            contentPreview.getStyleClass().add("card-preview");
            contentPreview.setWrapText(true);

            editBtn.getStyleClass().add("edit-icon-button");
            editBtn.setVisible(false);

            headerPane.setLeft(dateLabel);
            headerPane.setRight(editBtn);

            tagsBox.getChildren().addAll(moodTag, weatherTag);
            cardContainer.getChildren().addAll(headerPane, contentPreview, tagsBox);

            // Hover effect for Edit button logic
            cardContainer.setOnMouseEntered(e -> editBtn.setVisible(true));
            cardContainer.setOnMouseExited(e -> editBtn.setVisible(false));

            root.getChildren().addAll(markerPane, cardContainer);
        }

        @Override
        protected void updateItem(SmartJournal.JournalEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                lastItem = null;
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            } else {
                boolean isSameItem = (lastItem == item);
                lastItem = item;

                dateLabel.setText(item.getDate().format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")));
                dotLabel.setText(String.valueOf(item.getDate().getDayOfMonth()));

                String text = item.getContent().replace("\n", " ");
                if (text.length() > 140)
                    text = text.substring(0, 140) + "...";
                contentPreview.setText(text);

                // Mood Tag Logic (retained)
                String mood = item.getAiMood();
                if (mood == null)
                    mood = "Neutral";
                moodTag.setText(mood);
                weatherTag.setText("☁ " + item.getWeather());

                String moodClass = "tag-neutral";
                if (mood.contains("5 stars") || "Very Positive".equalsIgnoreCase(mood)) {
                    moodClass = "tag-very-positive";
                } else if (mood.contains("4 stars") || "Positive".equalsIgnoreCase(mood)) {
                    moodClass = "tag-positive";
                } else if (mood.contains("1 star") || "Very Negative".equalsIgnoreCase(mood)) {
                    moodClass = "tag-very-negative";
                } else if (mood.contains("2 stars") || "Negative".equalsIgnoreCase(mood)) {
                    moodClass = "tag-negative";
                }
                moodTag.getStyleClass().setAll("tag", moodClass);
                weatherTag.getStyleClass().setAll("tag", "tag-weather");

                editBtn.setOnAction(e -> app.openJournalEditor(item));

                if (item.getDate().equals(LocalDate.now())) {
                    dotShape.getStyleClass().add("timeline-dot-today");
                } else {
                    dotShape.getStyleClass().remove("timeline-dot-today");
                }

                setGraphic(root);

                if (!isSameItem) {
                    FadeTransition ft = new FadeTransition(Duration.millis(300), root);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.play();
                }
            }
        }
    }
}