package be.codeforbelgium.openinzichten.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoryResponse {
    private String id;
    private String title;
    private String content;
    private List<String> conditionNames;
    private String ownerUsername;
    private String ownerId;
    private long likeCount;
    private boolean likedByCurrentUser;
    private boolean livesWith;

    public StoryResponse(String id, String title, String content, List<String> conditionNames, String ownerUsername,
                         boolean livesWith) {
        this(id, title, content, conditionNames, ownerUsername, null, 0L, false, livesWith);
    }

    public StoryResponse(String id, String title, String content, List<String> conditionNames, String ownerUsername,
                         String ownerId, boolean livesWith) {
        this(id, title, content, conditionNames, ownerUsername, ownerId, 0L, false, livesWith);
    }

    public StoryResponse(String id, String title, String content, List<String> conditionNames, String ownerUsername,
                         LikeInfo likeInfo, boolean livesWith) {
        this(id, title, content, conditionNames, ownerUsername, null,
                likeInfo != null ? likeInfo.getLikeCount() : 0L,
                likeInfo != null && likeInfo.isLikedByCurrentUser(),
                livesWith);
    }

    public static class LikeInfo {
        private final long likeCount;
        private final boolean likedByCurrentUser;

        public LikeInfo(long likeCount, boolean likedByCurrentUser) {
            this.likeCount = likeCount;
            this.likedByCurrentUser = likedByCurrentUser;
        }

        public long getLikeCount() {
            return likeCount;
        }

        public boolean isLikedByCurrentUser() {
            return likedByCurrentUser;
        }
    }
}
