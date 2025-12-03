import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JournalManager {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Saves a journal entry to a file: [USER_EMAIL_DIR]/[DATE].txt
     */
    public static void saveJournal(User user, JournalEntry entry) {
        String userDir = user.getEmail().replace("@", "_at_");
        File dir = new File(userDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filePath = userDir + File.separator + entry.getDate() + ".txt";
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(entry.toString());
            // This is the file I/O for journal data [cite: 52]
        } catch (IOException e) {
            System.err.println("Error saving journal: " + e.getMessage());
        }
    }

    /**
     * Loads a journal entry from a file.
     */
    public static JournalEntry loadJournal(User user, String date) {
        String userDir = user.getEmail().replace("@", "_at_");
        String filePath = userDir + File.separator + date + ".txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String weather = null;
            String mood = null;
            StringBuilder content = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("WEATHER:")) {
                    weather = line.substring("WEATHER:".length()).trim();
                } else if (line.startsWith("MOOD:")) {
                    mood = line.substring("MOOD:".length()).trim();
                } else if (line.startsWith("CONTENT:")) {
                    // Start reading journal content from the next line
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
            }

            if (weather != null && mood != null) {
                String finalContent = content.length() > 0 ? content.substring(0, content.length() - 1) : "";
                return new JournalEntry(date, finalContent, weather, mood);
            }

        } catch (IOException e) {
            System.err.println("Error loading journal for " + date + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets a list of dates for the last 7 days up to today, for listing journals[cite: 62, 154].
     */
    public static List<String> listJournalDates(User user) {
        List<String> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // Add dates from 7 days ago up to today
        for (int i = 7; i >= 0; i--) {
            dates.add(today.minusDays(i).format(DATE_FORMATTER));
        }
        
        return dates;
    }
}