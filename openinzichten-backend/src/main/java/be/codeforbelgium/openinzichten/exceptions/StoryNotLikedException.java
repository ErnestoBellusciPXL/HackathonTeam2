package be.codeforbelgium.openinzichten.exceptions;

public class StoryNotLikedException extends RuntimeException {

    public StoryNotLikedException(String storyId) {
        super("Story was not liked: " + storyId);
    }
}
