package be.codeforbelgium.openinzichten.exceptions;

public class StoryAlreadyLikedException extends RuntimeException {

    public StoryAlreadyLikedException(String storyId) {
        super("Story already liked: " + storyId);
    }
}
