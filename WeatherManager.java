import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WeatherManager {

    // =============================================================
    // CONFIGURATION
    // =============================================================
    private static final String IP2LOC_BASE_URL = "https://api.ip2location.io/?";
    // Added &timezone=auto to get local Malaysian time
    private static final String WEATHER_API_BASE = "https://api.open-meteo.com/v1/forecast";

    static {
        // Force IPv6 to ensure accurate detection (Skudai vs generic KL)
        System.setProperty("java.net.preferIPv6Addresses", "true");
    }

    // =============================================================
    // MAIN LOGIC
    // =============================================================

    public static String getCurrentWeather(boolean translateToEnglish) {
        try {
            // STEP 1: DETECT LOCATION (City & Coordinates)
            System.out.println("[WeatherManager] Detecting location...");
            LocationData loc = detectLocationData();

            if (loc == null) {
                return "Kuala Lumpur: Data Unavailable";
            }

            System.out.println("[WeatherManager] Found: " + loc.city + " (" + loc.lat + ", " + loc.lon + ")");

            // STEP 2: FETCH WEATHER (With Timezone)
            String weatherUrl = WEATHER_API_BASE + "?latitude=" + loc.lat + "&longitude=" + loc.lon 
                              + "&current_weather=true&timezone=auto";
            
            String jsonResponse = API.get(weatherUrl);
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                return loc.city + ": Weather Unavailable";
            }

            // STEP 3: EXTRACT DATA (Robustly)
            
            // A. Find the "current_weather" block to avoid reading "units" metadata
            int dataBlockIndex = jsonResponse.indexOf("\"current_weather\"");
            if (dataBlockIndex == -1) dataBlockIndex = 0;

            // B. Extract Weather Code
            String codeStr = extractJsonValue(jsonResponse, "weathercode", dataBlockIndex);
            int wmoCode = -1;
            if (codeStr != null) {
                try {
                    wmoCode = Integer.parseInt(codeStr.trim());
                } catch (NumberFormatException e) { /* Ignore */ }
            }

            // C. Extract Time (New Feature!)
            String timeStr = extractJsonValue(jsonResponse, "time", dataBlockIndex);
            String formattedTime = formatTime(timeStr); // Converts "2025-11-21T19:00" -> "19:00"

            // STEP 4: FORMAT OUTPUT
            String weatherDesc = decodeWMOCode(wmoCode);
            
            if (!translateToEnglish) {
                weatherDesc = translateToMalay(weatherDesc);
            }

            // Final Output: "Skudai: Thunderstorms (Updated: 7:00 PM)"
            return loc.city + ": " + weatherDesc + " (Updated: " + formattedTime + ")";

        } catch (Exception e) {
            System.out.println("[WeatherManager] Error: " + e.getMessage());
            return "Weather Unavailable";
        }
    }

    // =============================================================
    // HELPER: TIME FORMATTER
    // =============================================================
    private static String formatTime(String isoTime) {
        if (isoTime == null) return "";
        try {
            // Open-Meteo sends ISO format: "2025-10-11T19:00"
            LocalDateTime dateTime = LocalDateTime.parse(isoTime);
            // We format it to be user-friendly: "7:00 PM"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
            return dateTime.format(formatter);
        } catch (Exception e) {
            return isoTime.replace("T", " "); // Fallback
        }
    }

    // =============================================================
    // HELPER: JSON EXTRACTION (With Offset)
    // =============================================================
    /**
     * Extracts value from JSON starting search at 'fromIndex'.
     * This prevents finding metadata/units at the top of the file.
     */
    private static String extractJsonValue(String json, String keyName, int fromIndex) {
        String searchKey = "\"" + keyName + "\":";
        int startIndex = json.indexOf(searchKey, fromIndex);
        
        if (startIndex == -1) return null;

        startIndex += searchKey.length();

        // Skip any whitespace
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }

        char nextChar = json.charAt(startIndex);
        
        if (nextChar == '"') {
            // Extract String
            startIndex++; 
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) return null;
            return json.substring(startIndex, endIndex);
        } else {
            // Extract Number
            int endIndex = startIndex;
            while (endIndex < json.length() && 
                   (Character.isDigit(json.charAt(endIndex)) || 
                    json.charAt(endIndex) == '.' || 
                    json.charAt(endIndex) == '-')) {
                endIndex++;
            }
            return json.substring(startIndex, endIndex);
        }
    }

    // =============================================================
    // HELPER: WMO DECODER
    // =============================================================
    private static String decodeWMOCode(int code) {
        switch (code) {
            case 0: return "Clear Sky";
            case 1: case 2: case 3: return "Partly Cloudy";
            case 45: case 48: return "Foggy";
            case 51: case 53: case 55: return "Drizzle";
            case 61: case 63: case 65: return "Rain";
            case 80: case 81: case 82: return "Showers";
            case 95: return "Thunderstorms";
            case 96: case 99: return "Thunderstorms with Hail";
            default: return "Unknown Weather";
        }
    }

    // =============================================================
    // HELPER: LOCATION DETECTION
    // =============================================================
    private static class LocationData {
        String city;
        double lat;
        double lon;
    }

    private static LocationData detectLocationData() {
        try {
            String key = EnvLoader.get("IP2LOCATION_KEY");
            if (key == null) return null;

            String url = IP2LOC_BASE_URL + "key=" + key + "&format=json";
            String json = API.get(url);
            if (json == null) return null;

            LocationData data = new LocationData();
            // Use 0 as fromIndex for these since they are unique in IP2Location JSON
            data.city = extractJsonValue(json, "city_name", 0);
            
            String latStr = extractJsonValue(json, "latitude", 0);
            String lonStr = extractJsonValue(json, "longitude", 0);

            if (latStr != null && lonStr != null) {
                data.lat = Double.parseDouble(latStr);
                data.lon = Double.parseDouble(lonStr);
                if (data.city == null) data.city = "Unknown Location";
                return data;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String translateToMalay(String english) {
        if (english.contains("Thunder")) return "Ribut Petir";
        if (english.contains("Rain")) return "Hujan";
        if (english.contains("Cloud")) return "Mendung";
        if (english.contains("Clear")) return "Cerah";
        return english;
    }
}