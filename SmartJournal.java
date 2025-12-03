import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
// NOTE: API and EnvLoader classes are assumed to be present and compiled.

public class SmartJournal {
    
    // --- STATIC HELPERS ---
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static String getUserInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
    
    private static void pause() {
        System.out.println("\nPress Enter to go back.");
        scanner.nextLine();
    }
    
    // --- MAIN APPLICATION LOGIC ---

    public static void main(String[] args) {
        System.out.println("=== Welcome to Smart Journaling ===");
        
        UserManager userManager = new UserManager();
        User currentUser = null;

        // 1. LOGIN/REGISTRATION Loop [cite: 30-34]
        while (currentUser == null) {
            System.out.println("\n--- Authentication ---");
            System.out.println("1. Login");
            System.out.println("2. Register (for testing, not persistent)");
            System.out.println("3. Exit");
            String choice = getUserInput("> ");

            switch (choice) {
                case "1":
                    String email = getUserInput("Enter Email: ");
                    String password = getUserInput("Enter Password: ");
                    currentUser = userManager.login(email, password);
                    if (currentUser == null) {
                        System.out.println("Login Failed. Invalid email or password.");
                    }
                    break;
                case "2":
                    String newEmail = getUserInput("Enter NEW Email: ");
                    String newDisplayName = getUserInput("Enter Display Name: ");
                    String newPassword = getUserInput("Enter Password: ");
                    userManager.register(newEmail, newDisplayName, newPassword);
                    break;
                case "3":
                    System.out.println("Exiting application. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
        
        // 2. MAIN MENU Loop
        mainMenu(currentUser);
        
        System.out.println("\nThank you for using Smart Journaling. Goodbye!");
    }

    private static void mainMenu(User user) {
        String greeting = getGreeting();
        System.out.println("\n==================================");
        // Welcome Message [cite: 44-46]
        System.out.println(greeting + ", " + user.getDisplayName() + "!"); 
        System.out.println("==================================");
        
        boolean running = true;
        while (running) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Create, Edit & View Journals"); // Functionality 1 [cite: 49]
            System.out.println("2. View Weekly Summary");          // Functionality 2 [cite: 50]
            System.out.println("3. Logout");
            String choice = getUserInput("> ");
            
            switch (choice) {
                case "1":
                    journalPage(user);
                    break;
                case "2":
                    weeklySummaryPage(user);
                    break;
                case "3":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    /**
     * Determines the greeting based on the current time (GMT+8). [cite: 47, 48]
     */
    private static String getGreeting() {
        int hour = LocalTime.now().getHour();
        
        if (hour >= 0 && hour <= 11) {
            return "Good Morning"; // 00:00-11:59 [cite: 48]
        } else if (hour >= 12 && hour <= 16) {
            return "Good Afternoon"; // 12:00-16:59 [cite: 48]
        } else { // 17:00 to 23:59
            return "Good Evening"; // 17:00-23:59 [cite: 48]
        }
    }
    
    // --- JOURNAL LOGIC ---

    private static void journalPage(User user) {
        boolean backToMenu = false;
        while (!backToMenu) {
            System.out.println("\n=== Journal Dates ==="); //
            List<String> dates = JournalManager.listJournalDates(user);
            
            LocalDate today = LocalDate.now();
            String todayStr = today.format(DATE_FORMATTER);

            for (int i = 0; i < dates.size(); i++) {
                String date = dates.get(i);
                String tag = date.equals(todayStr) ? " (Today)" : "";
                System.out.println((i + 1) + ". " + date + tag);
            }

            // Determine prompt based on whether today's journal exists [cite: 65, 71]
            JournalEntry todayEntry = JournalManager.loadJournal(user, todayStr);
            String actionPrompt = (todayEntry == null) 
                ? "Select a date to view journal, or create a new journal for today:" 
                : "Select a date to view journal, or edit the journal for today:";

            System.out.println("\n" + actionPrompt);
            System.out.println("Enter a number (1-" + dates.size() + "), or '0' to go back:");
            String input = getUserInput("> ");

            if (input.equals("0")) {
                backToMenu = true;
                continue;
            }

            try {
                int index = Integer.parseInt(input);
                if (index > 0 && index <= dates.size()) {
                    String selectedDate = dates.get(index - 1);

                    if (selectedDate.equals(todayStr)) {
                        manageTodayJournal(user, selectedDate);
                    } else {
                        viewPastJournal(user, selectedDate); // View past date [cite: 63]
                    }
                } else {
                    System.out.println("Invalid number.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        }
    }

    private static void viewPastJournal(User user, String date) {
        JournalEntry entry = JournalManager.loadJournal(user, date);
        
        System.out.println("\n=== Journal Entry for " + date + " ==="); //
        if (entry != null) {
            System.out.println("Weather: " + entry.getWeather()); 
            System.out.println("Mood: " + entry.getMood());       
            System.out.println("------------------------------------");
            System.out.println(entry.getContent());               
        } else {
            System.out.println("No journal entry found for this date.");
        }
        pause(); 
    }

    private static void manageTodayJournal(User user, String date) {
        JournalEntry entry = JournalManager.loadJournal(user, date);
        
        if (entry == null) {
            System.out.println("\nNo journal entry found for today.");
            System.out.println("1. Create Journal"); // [cite: 65]
            System.out.println("2. Back to Dates");
            String choice = getUserInput("> ");
            
            if (choice.equals("1")) {
                createOrEditJournal(user, date, false);
            }
        } else {
            boolean managing = true;
            while(managing) {
                System.out.println("\nJournal entry found for today. Would you like to:");
                System.out.println("1. View Journal"); // [cite: 66]
                System.out.println("2. Edit Journal"); // [cite: 66]
                System.out.println("3. Back to Dates");
                String choice = getUserInput("> ");

                switch (choice) {
                    case "1":
                        viewPastJournal(user, date); 
                        break;
                    case "2":
                        createOrEditJournal(user, date, true);
                        // Reload entry for the next loop iteration to reflect changes
                        entry = JournalManager.loadJournal(user, date); 
                        break;
                    case "3":
                        managing = false;
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            }
        }
    }

    private static void createOrEditJournal(User user, String date, boolean isEdit) {
        JournalEntry existingEntry = JournalManager.loadJournal(user, date);
        
        System.out.print("\n");
        if (isEdit) {
            System.out.println("Edit your journal entry for " + date + ":"); 
            System.out.println("--- Current Content ---");
            System.out.println(existingEntry.getContent());
            System.out.println("-----------------------");
        } else {
            System.out.println("Enter your journal entry for " + date + ":"); 
        }
        
        String content = getUserInput("> ");
        
        if (content.trim().isEmpty()) {
            System.out.println("Journal entry cannot be empty. Aborting save.");
            return;
        }

        // 1. Get Weather (using your code) [cite: 74-76]
        System.out.println("\nRetrieving current weather...");
        String weather = WeatherManager.getCurrentWeather(true); 
        
        // 2. Get Mood (Sentiment Analysis - POST API Call) [cite: 80-86]
        String mood = "N/A (API Failed)";
        try {
            System.out.println("Analyzing sentiment...");
            
            // Construct the JSON body for the POST request
            // Format: {"inputs": "Your journal text"}
            String jsonInputString = String.format("{\"inputs\": \"%s\"}", 
                                                // Escape double quotes in the content
                                                content.replace("\"", "\\\"")); 
            
            // Call the POST API method
            String jsonResponse = API.post(API.MOOD_API_URL, jsonInputString);

            if (jsonResponse != null && !jsonResponse.isEmpty()) {
                // Find the first label, assuming it has the highest score [cite: 147]
                int labelIndex = jsonResponse.indexOf("\"label\":\"");
                if (labelIndex != -1) {
                    labelIndex += "\"label\":\"".length();
                    int endIndex = jsonResponse.indexOf("\"", labelIndex);
                    if (endIndex != -1) {
                        mood = jsonResponse.substring(labelIndex, endIndex);
                        System.out.println("Analysis complete. Mood classified as: " + mood);
                    }
                } else {
                    System.err.println("Error: Could not parse sentiment response.");
                    if (jsonResponse.contains("loading")) {
                         System.err.println("Model is loading, try again in a few seconds.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Sentiment Analysis failed: " + e.getMessage());
        }


        // 3. Save Entry
        JournalEntry newEntry = new JournalEntry(date, content, weather, mood);
        JournalManager.saveJournal(user, newEntry);
        
        System.out.println("Journal saved successfully!"); 
    }
    
    // --- WEEKLY SUMMARY LOGIC ---
    
    private static void weeklySummaryPage(User user) {
        System.out.println("\n=== Weekly Summary ==="); 
        System.out.println("Overview of Mood and Weather for the last 7 days:"); // [cite: 148]

        LocalDate today = LocalDate.now();
        
        // List dates for the last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);
            
            JournalEntry entry = JournalManager.loadJournal(user, dateStr);
            
            String weather, mood;
            
            if (entry != null) {
                // Extract only the description for the summary
                String[] parts = entry.getWeather().split(":", 2);
                weather = parts.length > 1 ? parts[1].trim() : "N/A";
                mood = entry.getMood();
            } else {
                weather = "N/A (No Entry)";
                mood = "N/A (No Entry)";
            }
            
            // Format: Date | Mood | Weather 
            System.out.printf("%s | Mood: %-30s | Weather: %s\n", 
                dateStr, 
                mood, 
                weather
            );
        }
        
        pause();
    }
}