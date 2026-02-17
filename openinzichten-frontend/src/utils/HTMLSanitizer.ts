export function sanitizeHtml(input: string): string {
    // Allowed tags in the sanitized HTML
    const allowedTags = new Set([
        "H1",
        "H2",
        "H3",
        "H4",
        "H5",
        "H6",
        "STRONG",
        "EM",
        "U",
        "CITE",
        "P",
        "BR",
        "SPAN",
    ]);
    const allowedAttrs = new Set(["style"]); // Allow only style attribute for now

    const doc = new DOMParser().parseFromString(input, "text/html");
    const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_ELEMENT, null);

    // Remove disallowed elements and dangerous attributes
    const toRemove: Element[] = [];
    for (let n = walker.nextNode() as Element | null; n; n = walker.nextNode() as Element | null) {
        // Remove scripts, iframes, etc. by tag whitelist
        if (!allowedTags.has(n.tagName)) {
            toRemove.push(n);
            continue;
        }
        // Strip all attributes except whitelisted, and neutralize event handlers and URLs
        for (const attr of Array.from(n.attributes)) {
            const name = attr.name.toLowerCase();
            if (!allowedAttrs.has(attr.name)) {
                n.removeAttribute(attr.name);
                continue;
            }
            // restrict style to a subset or strip dangerous content
            if (name === "style") {
                // Very conservative: remove "url(" and "expression"
                const safe = attr.value.replace(/url\s*\(/gi, "").replace(/expression\s*\(/gi, "");
                n.setAttribute("style", safe);
            }
        }
    }

    // Remove nodes after traversal to not confuse walker
    for (const el of toRemove) el.replaceWith(...Array.from(el.childNodes));

    // Also ensure no script elements anywhere (DOMParser can create them)
    doc.querySelectorAll("script,noscript,iframe,object,embed,link,meta,style").forEach((el) => el.remove());

    return doc.body.innerHTML;
}

export function removeAllHtmlTags(input: string): string {
    // Allowed tags in the sanitized HTML
    const allowedTags = new Set([""]);
    const allowedAttrs = new Set(["style"]); // Allow only style attribute for now

    const doc = new DOMParser().parseFromString(input, "text/html");
    const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_ELEMENT, null);

    // Remove disallowed elements and dangerous attributes
    const toRemove: Element[] = [];
    for (let n = walker.nextNode() as Element | null; n; n = walker.nextNode() as Element | null) {
        // Remove scripts, iframes, etc. by tag whitelist
        if (!allowedTags.has(n.tagName)) {
            toRemove.push(n);
            continue;
        }
        // Strip all attributes except whitelisted, and neutralize event handlers and URLs
        for (const attr of Array.from(n.attributes)) {
            const name = attr.name.toLowerCase();
            if (!allowedAttrs.has(attr.name)) {
                n.removeAttribute(attr.name);
                continue;
            }
            // restrict style to a subset or strip dangerous content
            if (name === "style") {
                // Very conservative: remove "url(" and "expression"
                const safe = attr.value.replace(/url\s*\(/gi, "").replace(/expression\s*\(/gi, "");
                n.setAttribute("style", safe);
            }
        }
    }

    // Remove nodes after traversal to not confuse walker
    for (const el of toRemove) el.replaceWith(...Array.from(el.childNodes));

    // Also ensure no script elements anywhere (DOMParser can create them)
    doc.querySelectorAll("script,noscript,iframe,object,embed,link,meta,style").forEach((el) => el.remove());

    return doc.body.innerHTML;
}
