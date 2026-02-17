package be.codeforbelgium.openinzichten.api.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoryResponseTests {

    @Test
    void allArgsConstructor_setsAllFields() {
        StoryResponse resp = new StoryResponse(
                "id-1",
                "Title",
                "Content",
                List.of("A", "B"),
                "ownerUser",
                "owner-uuid",
                12L,
                true,
                true
        );

        assertEquals("id-1", resp.getId());
        assertEquals("Title", resp.getTitle());
        assertEquals("Content", resp.getContent());
        assertEquals(2, resp.getConditionNames().size());
        assertEquals("ownerUser", resp.getOwnerUsername());
        assertEquals("owner-uuid", resp.getOwnerId());
        assertEquals(12L, resp.getLikeCount());
        assertTrue(resp.isLikedByCurrentUser());
        assertTrue(resp.isLivesWith());
    }

    @Test
    void convenienceConstructor_ownerAndLivesWith_setsDefaults() {
        StoryResponse resp = new StoryResponse("id-2", "T", "C", List.of(), "ownerX", true);

        assertEquals("id-2", resp.getId());
        assertEquals("ownerX", resp.getOwnerUsername());
        assertNull(resp.getOwnerId());
        assertEquals(0L, resp.getLikeCount());
        assertFalse(resp.isLikedByCurrentUser());
        assertTrue(resp.isLivesWith());
    }

    @Test
    void convenienceConstructor_ownerOwnerIdAndLivesWith_setsDefaults() {
        StoryResponse resp = new StoryResponse("id-3", "T", "C", List.of(), "ownerY", "owner-id-y", true);

        assertEquals("owner-id-y", resp.getOwnerId());
        assertEquals(0L, resp.getLikeCount());
        assertFalse(resp.isLikedByCurrentUser());
        assertTrue(resp.isLivesWith());
    }

    @Test
    void convenienceConstructor_likeCountVariant_setsOwnerIdNull() {
        StoryResponse resp = new StoryResponse("id-4", "T", "C", List.of(), "ownerZ", new StoryResponse.LikeInfo(5L, true), false);

        assertNull(resp.getOwnerId());
        assertEquals(5L, resp.getLikeCount());
        assertTrue(resp.isLikedByCurrentUser());
        assertFalse(resp.isLivesWith());
    }

    @Test
    void defaultConstructor_andSetters_work() {
        StoryResponse resp = new StoryResponse();
        resp.setId("x");
        resp.setTitle("t");
        resp.setContent("c");
        resp.setConditionNames(List.of("cond1"));
        resp.setOwnerUsername("u");
        resp.setOwnerId("oid");
        resp.setLikeCount(2L);
        resp.setLikedByCurrentUser(true);
        resp.setLivesWith(false);

        assertEquals("x", resp.getId());
        assertEquals("t", resp.getTitle());
        assertEquals("c", resp.getContent());
        assertEquals(1, resp.getConditionNames().size());
        assertEquals("u", resp.getOwnerUsername());
        assertEquals("oid", resp.getOwnerId());
        assertEquals(2L, resp.getLikeCount());
        assertTrue(resp.isLikedByCurrentUser());
        assertFalse(resp.isLivesWith());
    }
}
