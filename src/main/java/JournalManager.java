import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class JournalManager {
    private static final Connection dbConnection = DbManager.getConnection();

    // --- SAVE & UPDATE ---
    public static void saveJournal(User user, SmartJournal.JournalEntry entry) {
        if (dbConnection == null) return;

        // UPSERT Logic: Insert new or update existing for the same day
        String sql = "INSERT INTO journals (user_email, entry_date, content, weather, mood) "
                   + "VALUES (?, ?, ?, ?, ?) "
                   + "ON CONFLICT (user_email, entry_date) DO UPDATE "
                   + "SET content = EXCLUDED.content, weather = EXCLUDED.weather, mood = EXCLUDED.mood";

        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setDate(2, Date.valueOf(entry.getDate()));
            ps.setString(3, entry.getContent());
            ps.setString(4, entry.getWeather());
            ps.setString(5, entry.getAiMood());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveUserProgress(User user, int streak, int xp, int level) {
        if (dbConnection == null) return;
        String sql = "INSERT INTO user_progress (user_email, current_streak, total_xp, current_level, last_journal_date) " +
                     "VALUES (?, ?, ?, ?, CURRENT_DATE) " +
                     "ON CONFLICT (user_email) DO UPDATE SET " +
                     "current_streak = ?, total_xp = ?, current_level = ?, last_journal_date = CURRENT_DATE";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setInt(2, streak);
            ps.setInt(3, xp);
            ps.setInt(4, level);
            ps.setInt(5, streak);
            ps.setInt(6, xp);
            ps.setInt(7, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- FETCH DATA ---
    public static List<SmartJournal.JournalEntry> getRecentEntries(User user) {
        List<SmartJournal.JournalEntry> history = new ArrayList<>();
        if (dbConnection == null) return history;

        String sql = "SELECT entry_date, content, mood, weather FROM journals WHERE user_email = ? ORDER BY entry_date DESC LIMIT 20";

        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("entry_date").toLocalDate();
                String content = rs.getString("content");
                String mood = rs.getString("mood");
                String weather = rs.getString("weather");
                
                // Reconstruct entry
                history.add(new SmartJournal.JournalEntry(date, content, mood, weather));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    // NEW: Fetch last 7 days for the Summary Page
    public static List<SmartJournal.JournalEntry> getWeeklyStats(User user) {
        List<SmartJournal.JournalEntry> weekStats = new ArrayList<>();
        if (dbConnection == null) return weekStats;

        // Get entries from the last 7 days
        String sql = "SELECT entry_date, content, mood, weather FROM journals " +
                     "WHERE user_email = ? AND entry_date >= CURRENT_DATE - INTERVAL '7 days' " +
                     "ORDER BY entry_date ASC";

        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("entry_date").toLocalDate();
                String mood = rs.getString("mood");
                String weather = rs.getString("weather");
                // Content isn't needed for the chart, but we keep the object structure
                weekStats.add(new SmartJournal.JournalEntry(date, "", mood, weather));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return weekStats;
    }

    public static int[] loadUserProgress(User user) {
        int[] stats = {0, 0, 1}; 
        if (dbConnection == null) return stats;
        String sql = "SELECT current_streak, total_xp, current_level FROM user_progress WHERE user_email = ?";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                stats[0] = rs.getInt("current_streak");
                stats[1] = rs.getInt("total_xp");
                stats[2] = rs.getInt("current_level");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }
    
    // --- AUTH UTILS ---
    public static boolean isEmailTaken(String email) {
        if (dbConnection == null) return false;
        try (PreparedStatement ps = dbConnection.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        } catch (SQLException e) { return true; }
    }
}