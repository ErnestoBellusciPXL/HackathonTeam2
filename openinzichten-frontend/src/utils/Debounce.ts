let searchTimeout: ReturnType<typeof setTimeout> | null = null;

export function debounce<T extends (...args: unknown[]) => unknown>(
    fn: T,
    delay: number,
): (...args: Parameters<T>) => void {
    return (...args: Parameters<T>) => {
        if (searchTimeout) {
            clearTimeout(searchTimeout);
        }
        searchTimeout = setTimeout(() => fn(...args), delay);
    };
}
