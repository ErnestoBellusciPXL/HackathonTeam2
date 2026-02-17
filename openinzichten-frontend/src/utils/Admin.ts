export interface AllUsersPagedResponse {
    content: Content[];
    pageable: Pageable;
    last: boolean;
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
    sort: Sort;
    first: boolean;
    numberOfElements: number;
    empty: boolean;
}

export interface Content {
    id: string;
    username: string;
    /** Whether the account is disabled (gedeactiveerd) */
    disabled?: boolean;
    hasCondition: boolean;
    conditions: string[];
    communities: string[];
}

export interface Pageable {
    pageNumber: number;
    pageSize: number;
    sort: Sort;
    offset: number;
    paged: boolean;
    unpaged: boolean;
}

export interface Sort {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
}

export interface AdminTicket {
    id: string;
    createdAt: string;
    state: string;
    type: string;
    reportReason: string;
    otherReason?: string | null;
    reporterId?: string;
    reporterUsername?: string | null;
    reporteeId?: string;
    reporteeUsername?: string | null;
    storyId?: string;
    storyTitle?: string | null;
    storyTitleSnapshot?: string | null;
    storyContentSnapshot?: string | null;
    storyConditionsSnapshot?: string | string[] | null;
}

export interface AdminTicketsPagedResponse {
    content: AdminTicket[];
    pageable: Pageable;
    last: boolean;
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
    sort: Sort;
    first: boolean;
    numberOfElements: number;
    empty: boolean;
}

export interface AdminUser {
    id: string;
    username: string;
    email?: string | null;
    postcode?: string | null;
    hasCondition?: boolean;
    conditions?: string[];
    disabled?: boolean;
}
