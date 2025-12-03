import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final String USER_DATA_FILE = "UserData.txt";
    private Map<String, User> users;

    public UserManager() {
        this.users = loadUsers();
    }

    /**
     * Loads user accounts from UserData.txt.
     * This fulfills the basic feature requirement for File I/O[cite: 52].
     */
    private Map<String, User> loadUsers() {
        Map<String, User> userMap = new HashMap<>();
        
        // Mock data as a fallback if the file cannot be read
        String mockUserData = "s100201@student.fop\nFoo Bar\npw-Stud#1\ns100202@student.fop\nJohn Doe\npw-Stud#2";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_DATA_FILE))) {
            String email;
            while ((email = reader.readLine()) != null) {
                String displayName = reader.readLine();
                String password = reader.readLine();

                if (email != null && displayName != null && password != null) {
                    userMap.put(email.trim(), new User(email.trim(), displayName.trim(), password.trim()));
                }
            }
            System.out.println("[UserManager] Loaded users from UserData.txt.");
        } catch (IOException e) {
            System.err.println("[UserManager] Warning: Could not read UserData.txt. Using mock users for demonstration.");
            // Fallback for demonstration
            String[] lines = mockUserData.split("\n");
            for (int i = 0; i < lines.length; i += 3) {
                if (i + 2 < lines.length) {
                    String email = lines[i].trim();
                    String displayName = lines[i+1].trim();
                    String password = lines[i+2].trim();
                    userMap.put(email, new User(email, displayName, password));
                }
            }
        }
        return userMap;
    }

    public User login(String email, String password) {
        User user = users.get(email);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
    
    public boolean register(String email, String displayName, String password) {
        if (users.containsKey(email)) {
            System.out.println("Registration failed: Email already exists.");
            return false;
        }
        // NOTE: New users are NOT saved to the file in this basic implementation
        users.put(email, new User(email, displayName, password));
        System.out.println("Registration successful! (Not saved persistently)");
        return true;
    }
}