package be.codeforbelgium.openinzichten.exceptions;

public class StoryNotFoundException extends RuntimeException {

    public StoryNotFoundException(String storyId) {
        super("Story not found: " + storyId);
    }
}
