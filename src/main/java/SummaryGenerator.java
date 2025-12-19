import java.util.List;
import java.io.IOException;

public class SummaryGenerator {

    public static String generate(List<SmartJournal.JournalEntry> entries, boolean isEnglish) {
        if (entries == null || entries.isEmpty()) {
            return isEnglish ? "No data available for analysis." : "Tiada data untuk analisis.";
        }

        // Limit to last 7 entries to avoid context limit issues
        int limit = Math.min(entries.size(), 7);
        List<SmartJournal.JournalEntry> recentEntries = entries.subList(0, limit);

        StringBuilder prompt = new StringBuilder();

        // Detailed User Prompt Structure
        prompt.append(
                "Goal: Analyze the provided journal entries from the last week and generate a clear, empathetic, and insight-driven summary for the user. Avoid technical jargon like \"polarity\" or \"distribution.\" Focus on feelings, moments, and patterns.\n\n");

        prompt.append("Input Data: Journal entries from the provided date range.\n\n");

        prompt.append("Output Structure & Requirements:\n");
        prompt.append(
                "Generate the summary using the following four-part, simplified structure. Use **Markdown** formatting for bolding titles.\n\n");

        prompt.append("1. Your Weekly Vibe Check\n");
        prompt.append(
                "Overall Feeling: Summarize the dominant emotional tone of the week in a short sentence or phrase.\n");
        prompt.append(
                "Vibe Trend: Briefly mention how this week compared to typical days (based on the provided entries).\n\n");

        prompt.append("2. The Week's Key Moments\n");
        prompt.append(
                "Best Day/Highlight: Identify the happiest or most positive day and entry. State what happened or what you achieved that made it so good.\n");
        prompt.append(
                "Toughest Day/Challenge: Identify the most challenging or negative day/entry. Briefly describe the main source of the difficulty.\n\n");

        prompt.append("3. What Drove Your Feelings?\n");
        prompt.append(
                "Top 3 Positive Fuel: What were the major themes that consistently made you feel good this week? (e.g., Time with Family, Creative Projects).\n");
        prompt.append("Top 3 Challenge Areas: What were the major themes that repeatedly brought down your mood?\n\n");

        prompt.append("4. Your Reflection for Next Week\n");
        prompt.append(
                "The Big Takeaway: State one clear, simple observation that connects a positive action to a positive feeling this week.\n");
        prompt.append(
                "Suggested Focus: Based on the 'Challenge Areas,' offer one gentle, actionable suggestion for the user to try in the coming week.\n\n");

        prompt.append("Translate to " + (isEnglish ? "English" : "Bahasa Malaysia")
                + ". Address the user directly as 'You'.\n\n");
        prompt.append("Entries:\n");

        for (SmartJournal.JournalEntry entry : recentEntries) {
            prompt.append("- Date: ").append(entry.getDate())
                    .append(", Mood: ").append(entry.getAiMood())
                    .append(", Weather: ").append(entry.getWeather()) // Added Weather
                    .append(", Content: ").append(entry.getContent().replace("\n", " "))
                    .append("\n");
        }

        prompt.append("\nSummary:");

        // Gemini JSON Payload: { "contents": [{ "parts": [{"text": "..."}] }] }
        String jsonInput = "{"
                + "\"contents\": [{"
                + "\"parts\": [{\"text\": \"" + escapeJson(prompt.toString()) + "\"}]"
                + "}]"
                + "}";

        String apiKey = EnvLoader.get("GEMINI_API_KEY");
        if (apiKey == null) {
            return "Error: GEMINI_API_KEY not found in .env";
        }

        String fullUrl = API.SUMMARY_API_URL + "?key=" + apiKey;
        System.out.println("DEBUG: AI Summary Prompt len=" + prompt.length());

        String response = API.post(fullUrl, jsonInput);

        if (response == null || response.isEmpty()) {
            return isEnglish ? "Could not generate summary at this time."
                    : "Tidak dapat menjana ringkasan pada masa ini.";
        }

        return cleanResponse(response);
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String cleanResponse(String jsonResponse) {
        // Gemini Response: { "candidates": [ { "content": { "parts": [ { "text": "THE
        // SUMMARY" } ] } } ] }
        // Simple extraction logic without JSON lib
        try {
            // Find "text":
            int idx = jsonResponse.indexOf("\"text\":");
            if (idx == -1)
                return jsonResponse; // or error

            // Find start quote of value
            int startQuote = jsonResponse.indexOf("\"", idx + 7);
            if (startQuote == -1)
                return jsonResponse;

            // Find end quote (handle minimal escaping if possible, though clean extract is
            // harder without lib)
            int endQuote = -1;
            for (int i = startQuote + 1; i < jsonResponse.length(); i++) {
                if (jsonResponse.charAt(i) == '"' && jsonResponse.charAt(i - 1) != '\\') {
                    endQuote = i;
                    break;
                }
            }

            if (endQuote != -1) {
                String content = jsonResponse.substring(startQuote + 1, endQuote);
                // Unescape basic JSON chars
                content = content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                // Unescape Unicode characters form
                return unescapeUnicode(content);
            }

            return jsonResponse;
        } catch (Exception e) {
            return jsonResponse;
        }
    }

    private static String unescapeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == 'u') {
                // Potential Unicode escape
                try {
                    String hex = input.substring(i + 2, i + 6);
                    int codePoint = Integer.parseInt(hex, 16);
                    sb.append((char) codePoint);
                    i += 6;
                } catch (Exception e) {
                    // Not a valid unicode escape, treat as literal
                    sb.append(c);
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    public static boolean saveToTextFile(String summary, java.io.File file) {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(summary);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}