export interface SuccessOrError {
    success: boolean;
    error?: string;
}

export interface ErrorResponse {
    error: string;
    status?: number;
}

type ResponseFailedState = {
    state: "failed";
    errorResponse: ErrorResponse;
};

type ResponseSuccessState<T> = {
    state: "success";
    response: T;
};

export type ResponseState<T> = ResponseFailedState | ResponseSuccessState<T>;
