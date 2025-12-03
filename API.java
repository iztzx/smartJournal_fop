import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream; // Needed for POST

public class API {

    // Keep these for reference, but we primarily use the arguments passed in
    public static final String MOOD_API_URL = "https://router.huggingface.co/hf-inference/models/tabularisai/multilingual-sentiment-analysis";
    /**
     * GENERIC GET REQUEST
     * Can be used for Weather API, IP API, etc.
     */
    public static String get(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            // Simple timeout to prevent hanging if IP API is slow
            conn.setConnectTimeout(15000); 
            conn.setReadTimeout(15000);

            return readResponse(conn);
        } catch (Exception e) {
            // e.printStackTrace(); // Uncomment for debugging
            return null;
        }
    }

    /**
     * GENERIC POST REQUEST (For Mood/AI)
     */
    public static String post(String urlString, String jsonInputString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Load Token
            String token = EnvLoader.get("BEARER_TOKEN");
            if (token != null) conn.setRequestProperty("Authorization", "Bearer " + token);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            return readResponse(conn);
        } catch (Exception e) {
            return null;
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        BufferedReader br;
        if (status >= 200 && status < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        return response.toString();
    }
}