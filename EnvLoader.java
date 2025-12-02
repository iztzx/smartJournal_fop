import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class EnvLoader {

    /**
     * Scans the .env file for a specific key and returns its value.
     * Example: If .env contains "BEARER_TOKEN=hf_12345", get("BEARER_TOKEN") returns "hf_12345".
     */
    public static String get(String key) {
        String filePath = ".env"; // The file must be in the project root
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignore comments or empty lines
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                // Split by the first "=" found
                String[] parts = line.split("=", 2);
                if (parts.length >= 2) {
                    String currentKey = parts[0].trim();
                    String currentValue = parts[1].trim();
                    
                    if (currentKey.equals(key)) {
                        return currentValue;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading .env file: " + e.getMessage());
        }
        
        return null; // Return null if key not found
    }
}