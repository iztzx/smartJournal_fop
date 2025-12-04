import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform; 
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartJournal {

    // Data Stores
    private final ObservableList<JournalEntry> entries = FXCollections.observableArrayList();
    private final ObservableList<JournalEntry> weeklyStats = FXCollections.observableArrayList();
    
    // Gamification
    private final IntegerProperty xp = new SimpleIntegerProperty(0);
    private final IntegerProperty level = new SimpleIntegerProperty(1);
    private final IntegerProperty streak = new SimpleIntegerProperty(0);
    
    private User currentUser;

    public void setCurrentUser(User user) { this.currentUser = user; }

    // --- BACKGROUND LOADERS ---
    public void loadUserData() {
        if (currentUser == null) return;
        int[] stats = JournalManager.loadUserProgress(currentUser);
        Platform.runLater(() -> {
            streak.set(stats[0]);
            xp.set(stats[1]);
            level.set(stats[2]);
        });
    }

    public void loadHistory() {
        if (currentUser == null) return;
        List<JournalEntry> history = JournalManager.getRecentEntries(currentUser);
        Platform.runLater(() -> {
            entries.clear();
            entries.addAll(history);
        });
    }
    
    public void loadWeeklySummary() {
        if (currentUser == null) return;
        List<JournalEntry> stats = JournalManager.getWeeklyStats(currentUser);
        Platform.runLater(() -> {
            weeklyStats.clear();
            weeklyStats.addAll(stats);
        });
    }

    // --- MAIN LOGIC (THREAD SAFE FIX) ---
    public JournalEntry processEntry(String text, String weather) {
        if (text == null || text.trim().isEmpty()) return null;
        
        // 1. Sentiment Analysis (Heavy Work - Keep on Background Thread)
        String mood = analyzeSentiment(text);
        
        // 2. Create Entry Object
        JournalEntry entryObj = new JournalEntry(LocalDate.now(), text, mood, weather);

        // 3. Database Save (Heavy Work - Keep on Background Thread)
        // Logic inside JournalManager handles upsert (insert or update), so it's safe to call here.
        JournalManager.saveJournal(currentUser, entryObj);

        // 4. UI & State Updates (CRITICAL: Must run on JavaFX Application Thread)
        Platform.runLater(() -> {
            JournalEntry existingEntry = getTodayEntry();

            if (existingEntry != null) {
                // --- UPDATE EXISTING (Edit Mode) ---
                int index = entries.indexOf(existingEntry);
                if (index >= 0) {
                    entries.set(index, entryObj); // Update the ObservableList
                }
                // We don't update stats/streak on edits to prevent farming
            } else {
                // --- CREATE NEW ---
                entries.add(0, entryObj); // Update the ObservableList
                
                // Gamification (Only on first entry)
                updateStats(text.length());
                
                // Capture current stats to save to DB
                int currentStreak = streak.get();
                int currentXp = xp.get();
                int currentLevel = level.get();
                
                // Save Stats to DB (Spawn new background thread to avoid freezing UI)
                new Thread(() -> {
                    JournalManager.saveUserProgress(currentUser, currentStreak, currentXp, currentLevel);
                }).start();
            }
        });
        
        return entryObj;
    }

    // Helper to retrieve today's entry
    public JournalEntry getTodayEntry() {
        for (JournalEntry entry : entries) {
            if (entry.getDate().equals(LocalDate.now())) {
                return entry;
            }
        }
        return null;
    }

    private String analyzeSentiment(String text) {
        // Prepare JSON Payload
        String safeText = text.replace("\"", "\\\"").replace("\n", " ");
        String jsonBody = "{\"inputs\": \"" + safeText + "\"}";

        // Call API
        String responseBody = API.post(API.MOOD_API_URL, jsonBody);

        if (responseBody == null) return "Unknown";

        // Parse JSON Response
        return parseBestSentiment(responseBody);
    }

    // Helper to parse the highest score from the JSON string
    private String parseBestSentiment(String json) {
        String bestLabel = "Neutral";
        double maxScore = -1.0;

        Pattern pattern = Pattern.compile("\\{\"label\":\"(.*?)\",\"score\":([\\d\\.]+)\\}");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String label = matcher.group(1);
            String scoreStr = matcher.group(2);
            try {
                double score = Double.parseDouble(scoreStr);
                if (score > maxScore) {
                    maxScore = score;
                    bestLabel = label;
                }
            } catch (NumberFormatException ignored) {}
        }
        
        return bestLabel; 
    }

    private void updateStats(int charCount) {
        int xpGained = 50 + charCount;
        xp.set(xp.get() + xpGained);
        int xpThreshold = level.get() * 500;
        if (xp.get() >= xpThreshold) level.set(level.get() + 1);
        streak.set(streak.get() + 1);
    }

    // --- DATA CLASS ---
    public static class JournalEntry {
        private final LocalDate date;
        private final String content;
        private final String aiMood;
        private final String weather;

        public JournalEntry(LocalDate date, String content, String mood, String weather) {
            this.date = date;
            this.content = content;
            this.aiMood = mood;
            this.weather = weather;
        }

        public LocalDate getDate() { return date; }
        public String getContent() { return content; }
        public String getAiMood() { return aiMood; }
        public String getWeather() { return weather; }
    }

    // Getters
    public ObservableList<JournalEntry> getEntries() { return entries; }
    public ObservableList<JournalEntry> getWeeklyStats() { return weeklyStats; }
    public IntegerProperty xpProperty() { return xp; }
    public IntegerProperty levelProperty() { return level; }
    public IntegerProperty streakProperty() { return streak; }
}