public class JournalEntry {
    private String date;
    private String content;
    private String weather;
    private String mood; // Placeholder until sentiment analysis is implemented

    public JournalEntry(String date, String content, String weather, String mood) {
        this.date = date;
        this.content = content;
        this.weather = weather;
        this.mood = mood;
    }

    public String getDate() { return date; }
    public String getContent() { return content; }
    public String getWeather() { return weather; }
    public String getMood() { return mood; }
    
    public void setContent(String content) { this.content = content; }

    @Override
    public String toString() {
        // Format for saving to file
        return "DATE:" + date + "\n" +
               "WEATHER:" + weather + "\n" +
               "MOOD:" + mood + "\n" +
               "CONTENT:\n" + content;
    }
}