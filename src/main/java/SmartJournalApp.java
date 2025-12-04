import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings; // Import added for binding logic
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SmartJournalApp extends Application {

    private SmartJournal smartJournal;
    private User currentUser;
    private BorderPane rootLayout;
    
    // UI Components
    private ListView<SmartJournal.JournalEntry> timelineList;
    private VBox timelineContainer;
    private Label streakLabel, levelLabel, xpLabel;
    private ProgressBar xpProgressBar;

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize Backend
        smartJournal = new SmartJournal();

        // 2. Authentication (Login with Password)
        if (!performLogin()) {
            return; 
        }

        // 3. Load Data
        smartJournal.setCurrentUser(currentUser);
        smartJournal.loadUserData();
        smartJournal.loadHistory();

        // 4. Main Layout
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root-pane");

        // --- TOP MENU ---
        MenuBar menuBar = createTopMenu(primaryStage);
        rootLayout.setTop(menuBar);

        // --- MAIN CONTENT (Timeline + Stats) ---
        SplitPane mainContent = createMainContent();
        rootLayout.setCenter(mainContent);

        // 5. Scene Setup
        Scene scene = new Scene(rootLayout, 1000, 700);
        if (getClass().getResource("journal_styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("journal_styles.css").toExternalForm());
        }

        primaryStage.setTitle("SmartJournal - " + currentUser.getDisplayName());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- AUTHENTICATION ---
    private boolean performLogin() {
        Dialog<User> loginDialog = new Dialog<>();
        loginDialog.setTitle("Login to SmartJournal");
        loginDialog.setHeaderText("Welcome Back!");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        loginDialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        loginDialog.getDialogPane().setContent(grid);

        // Convert result to User object
        loginDialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                String email = emailField.getText();
                String pass = passwordField.getText();
                // Validate with UserManager
                UserManager userManager = new UserManager();
                return userManager.login(email, pass);
            }
            return null;
        });

        Optional<User> result = loginDialog.showAndWait();
        if (result.isPresent()) {
            this.currentUser = result.get();
            return true;
        }
        return false;
    }

    private void performLogout(Stage stage) {
        this.currentUser = null;
        stage.close();
        Platform.runLater(() -> {
            try {
                new SmartJournalApp().start(new Stage());
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // --- UI STRUCTURE ---

    private MenuBar createTopMenu(Stage stage) {
        MenuBar menuBar = new MenuBar();
        
        Menu viewMenu = new Menu("View");
        MenuItem timelineItem = new MenuItem("Timeline");
        timelineItem.setOnAction(e -> rootLayout.setCenter(createMainContent()));
        
        MenuItem summaryItem = new MenuItem("Weekly Summary");
        summaryItem.setOnAction(e -> showSummaryPage()); // Placeholder for summary page
        
        viewMenu.getItems().addAll(timelineItem, summaryItem);

        Menu settingsMenu = new Menu("Settings");
        MenuItem profileItem = new MenuItem("Profile");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> performLogout(stage));
        
        settingsMenu.getItems().addAll(profileItem, new SeparatorMenuItem(), logoutItem);

        menuBar.getMenus().addAll(viewMenu, settingsMenu);
        return menuBar;
    }

    private SplitPane createMainContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.7); // 70% Timeline, 30% Stats

        // LEFT: Timeline (Slidable)
        timelineContainer = new VBox();
        timelineContainer.getStyleClass().add("timeline-container");
        
        // Header for Timeline
        Label timelineTitle = new Label("Your Journey");
        timelineTitle.getStyleClass().add("header-text");
        
        // Add Button (Floating Action Button style in header)
        Button addEntryBtn = new Button("+ New Entry");
        addEntryBtn.getStyleClass().add("primary-button");
        addEntryBtn.setOnAction(e -> openJournalEditor(null)); // New Entry
        
        HBox headerBox = new HBox(20, timelineTitle, addEntryBtn);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(20));

        // Timeline List
        timelineList = new ListView<>();
        timelineList.getStyleClass().add("timeline-list");
        timelineList.setItems(smartJournal.getEntries());
        timelineList.setCellFactory(param -> new TimelineCell());
        timelineList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                SmartJournal.JournalEntry selected = timelineList.getSelectionModel().getSelectedItem();
                if (selected != null) openJournalEditor(selected);
            }
        });
        
        VBox.setVgrow(timelineList, Priority.ALWAYS);
        
        // Empty State Logic
        Label emptyState = new Label("No memories yet. Start your journey today!");
        emptyState.getStyleClass().add("empty-state-text");
        Button startBtn = new Button("Start Journaling");
        startBtn.getStyleClass().add("accent-button");
        startBtn.setOnAction(e -> openJournalEditor(null));
        
        VBox emptyBox = new VBox(20, emptyState, startBtn);
        emptyBox.setAlignment(Pos.CENTER);
        
        // FIX: Using Bindings.isEmpty() instead of emptyProperty()
        StackPane listStack = new StackPane(timelineList, emptyBox);
        emptyBox.visibleProperty().bind(Bindings.isEmpty(smartJournal.getEntries()));
        timelineList.visibleProperty().bind(Bindings.isEmpty(smartJournal.getEntries()).not());

        timelineContainer.getChildren().addAll(headerBox, listStack);

        // RIGHT: Gamification
        VBox statsPanel = createGamificationPanel();

        splitPane.getItems().addAll(timelineContainer, statsPanel);
        return splitPane;
    }

    private VBox createGamificationPanel() {
        VBox vbox = new VBox(20);
        vbox.getStyleClass().add("gamification-panel");
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Progress");
        title.getStyleClass().add("subheader-text");

        // Level Circle
        StackPane levelBadge = new StackPane();
        levelBadge.getStyleClass().add("level-badge");
        levelLabel = new Label("1");
        levelLabel.getStyleClass().add("level-number");
        levelBadge.getChildren().add(levelLabel);

        // Labels
        Label lvlTitle = new Label("Current Level");
        streakLabel = new Label("🔥 0 Day Streak");
        streakLabel.getStyleClass().add("streak-label");
        
        // XP Bar
        xpProgressBar = new ProgressBar(0);
        xpProgressBar.prefWidthProperty().bind(vbox.widthProperty());
        xpLabel = new Label("0 XP");
        
        // Bindings
        levelLabel.textProperty().bind(smartJournal.levelProperty().asString());
        streakLabel.textProperty().bind(smartJournal.streakProperty().asString("🔥 %d Day Streak"));
        xpLabel.textProperty().bind(smartJournal.xpProperty().asString("%d XP"));
        // Simple mock progress calculation
        xpProgressBar.progressProperty().bind(smartJournal.xpProperty().divide(1000.0)); 

        vbox.getChildren().addAll(title, levelBadge, lvlTitle, streakLabel, new Separator(), xpProgressBar, xpLabel);
        return vbox;
    }
    
    private void showSummaryPage() {
        // Simple Placeholder for the requested Weekly Summary tab
        VBox summaryBox = new VBox(20);
        summaryBox.setAlignment(Pos.CENTER);
        summaryBox.getChildren().add(new Label("Weekly Summary Chart Coming Soon..."));
        rootLayout.setCenter(summaryBox);
    }

    // --- EDITOR MODAL ---
    private void openJournalEditor(SmartJournal.JournalEntry existingEntry) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle(existingEntry == null ? "New Entry" : "Edit Entry - " + existingEntry.getDate());

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("editor-modal");

        Label prompt = new Label(existingEntry == null ? "How was your day?" : "Update your memory:");
        prompt.getStyleClass().add("subheader-text");

        TextArea contentArea = new TextArea();
        contentArea.setWrapText(true);
        if (existingEntry != null) contentArea.setText(existingEntry.getContent());

        Button saveBtn = new Button("Save Journal");
        saveBtn.getStyleClass().add("primary-button");
        
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #636e72;");

        saveBtn.setOnAction(e -> {
            String text = contentArea.getText();
            if (text.isEmpty()) return;

            saveBtn.setDisable(true);
            contentArea.setDisable(true);
            statusLabel.setText("Analyzing Sentiment & Fetching Weather...");

            new Thread(() -> {
                // If editing old entry, preserve old weather/mood unless we want to re-analyze
                // For now, we re-analyze for simplicity
                String weather = WeatherManager.getCurrentWeather(true);
                SmartJournal.JournalEntry entry = smartJournal.processEntry(text, weather);
                
                Platform.runLater(() -> {
                    modalStage.close();
                    timelineList.refresh();
                });
            }).start();
        });

        layout.getChildren().addAll(prompt, contentArea, statusLabel, saveBtn);
        Scene modalScene = new Scene(layout, 500, 400);
        if (getClass().getResource("journal_styles.css") != null) {
            modalScene.getStylesheets().add(getClass().getResource("journal_styles.css").toExternalForm());
        }
        modalStage.setScene(modalScene);
        modalStage.showAndWait();
    }

    // --- TIMELINE LIST CELL ---
    static class TimelineCell extends ListCell<SmartJournal.JournalEntry> {
        private final VBox container = new VBox(5);
        private final Label dateLabel = new Label();
        private final Label contentPreview = new Label();
        private final HBox tagsBox = new HBox(10);
        private final Label moodTag = new Label();
        private final Label weatherTag = new Label();

        public TimelineCell() {
            container.getStyleClass().add("timeline-card");
            dateLabel.getStyleClass().add("card-date");
            contentPreview.getStyleClass().add("card-preview");
            contentPreview.setWrapText(true);
            
            tagsBox.getChildren().addAll(moodTag, weatherTag);
            container.getChildren().addAll(dateLabel, contentPreview, tagsBox);
        }

        @Override
        protected void updateItem(SmartJournal.JournalEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                dateLabel.setText(item.getDate().format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")));
                
                // Truncate text
                String text = item.getContent().replace("\n", " ");
                if (text.length() > 120) text = text.substring(0, 120) + "...";
                contentPreview.setText(text);

                // Tags
                moodTag.setText(getMoodEmoji(item.getAiMood()) + " " + item.getAiMood());
                weatherTag.setText("☁ " + item.getWeather());
                
                // Style Mood Tag
                String moodClass = "tag-neutral";
                if ("Positive".equalsIgnoreCase(item.getAiMood())) moodClass = "tag-positive";
                if ("Negative".equalsIgnoreCase(item.getAiMood())) moodClass = "tag-negative";
                
                moodTag.getStyleClass().setAll("tag", moodClass);
                weatherTag.getStyleClass().setAll("tag", "tag-weather");

                setGraphic(container);
            }
        }

        private String getMoodEmoji(String mood) {
            if (mood == null) return "😐";
            return switch (mood.toLowerCase()) {
                case "positive" -> "😊";
                case "negative" -> "😔";
                default -> "😐";
            };
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}