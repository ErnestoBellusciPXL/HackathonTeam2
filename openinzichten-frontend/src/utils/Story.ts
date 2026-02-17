import { ref } from "vue";

export interface StoryPayload {
    id: string;
    title: string;
    content: string;
    conditionNames: string[];
    ownerUsername: string;
    ownerId?: string;

    likeCount?: number;
    likedByCurrentUser?: boolean;
    // Indicates the user lives with (i.e. personally experiences) the condition(s)
    livesWith?: boolean;
}

export interface ReformattedStoryPayload {
    reformattedStoryContent: string;
}

export interface ReformatStoryRequestDate {
    formattedOn: Date;
}

export enum StoryOrderBy {
    NEWEST = "RECENT",
    MOST_LIKED = "POPULAR",
    ALL = "ALL",
}

export const storySort = ref([
    { label: "Geen Sortering", value: StoryOrderBy.ALL },
    { label: "Nieuwste", value: StoryOrderBy.NEWEST },
    { label: "Populair", value: StoryOrderBy.MOST_LIKED },
]);
