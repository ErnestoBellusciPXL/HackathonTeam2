// Source - https://stackoverflow.com/questions/46946380/fetch-api-request-timeout/57888548#57888548
// Posted by Aadit M Shah, modified by community.
// Retrieved 2025-11-13, License - CC BY-SA 4.0

/**
 * Helper function to perform a fetch request with a timeout, especially useful for HTTP request that may take a long time to complete.
 * See source link in this file for more details.
 * @param url URL to make the request to
 * @param ms milliseconds to wait before aborting the request
 * @param options other request options
 * @returns completed Promise<Response> when fetch completes or an AbortError when timeout is reached
 */
const fetchTimeout = (url: string, ms: number, options: RequestInit = {}) => {
    // Get any existing abort signal from the options
    const { signal, ...rest } = options;

    // Create a new AbortController to handle the timeout
    const controller = new AbortController();

    // Start the fetch request with the new abort signal
    const promise = fetch(url, { signal: controller.signal, ...rest });

    // If an external signal is provided, listen for its abort event to also abort this controller
    if (signal) signal.addEventListener("abort", () => controller.abort());

    // Set a timeout to abort the fetch request after the specified time
    const timeout = setTimeout(() => controller.abort(), ms);

    // Clear the timeout when the fetch completes or is aborted
    return promise.finally(() => clearTimeout(timeout));
};

export default fetchTimeout;
