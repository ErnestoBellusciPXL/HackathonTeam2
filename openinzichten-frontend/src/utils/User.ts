export interface User {
    id: number;
    username: string;
    email: string;
}

export interface RegisterPayload {
    username: string;
    email: string;
    password: string;
}

export interface CompleteProfilePayload {
    zipcode: string;
    conditions: string[];
    hasCondition: boolean;
}

export interface CompleteProfileResponse {
    id: string;
    username: string;
    email: string;
    zipcode: string;
    hasCondition: boolean;
    conditions: string[];
    communities: string[];
}

export interface DecodedJWT {
    sub: string;
    id: string;
    role: string[];
    iat: number;
    exp: number;
    email: string;
}
