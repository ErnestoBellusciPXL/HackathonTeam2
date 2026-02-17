declare module "sockjs-client" {
    // Minimal constructor signature so the library can be `new`'d
    // Keep return type loose but avoid `any` by using `unknown`.
    class SockJS {
        constructor(url: string, opts?: Record<string, unknown>);
    }
    export default SockJS;
}
