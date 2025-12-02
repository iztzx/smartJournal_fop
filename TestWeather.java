public class TestWeather {
    public static void main(String[] args) {
        System.out.println("=== SMART WEATHER TEST ===");
        
        // This should automatically find your city (e.g., "Petaling Jaya" or "Georgetown")
        // and then get the weather for THAT city.
        String result = WeatherManager.getCurrentWeather(true);
        
        System.out.println("\n[FINAL RESULT]");
        System.out.println(result);
    }
}