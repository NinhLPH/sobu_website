const UNSAFE_ELEMENTS = 'script,style,iframe,object,embed,form,input,button,textarea,select,link,meta,base';
const URL_ATTRIBUTES = ['href', 'src', 'xlink:href'];

export const stripHtml = (value?: string | null): string => {
    if (!value) return '';
    if (typeof document === 'undefined') return value.replace(/<[^>]*>/g, ' ');
    const wrapper = document.createElement('div');
    wrapper.innerHTML = value;
    return (wrapper.textContent || '').replace(/\s+/g, ' ').trim();
};

export const sanitizeRichHtml = (value?: string | null): string => {
    if (!value || typeof DOMParser === 'undefined') return '';
    const documentNode = new DOMParser().parseFromString(value, 'text/html');
    documentNode.querySelectorAll(UNSAFE_ELEMENTS).forEach((node) => node.remove());
    documentNode.body.querySelectorAll('*').forEach((element) => {
        Array.from(element.attributes).forEach((attribute) => {
            const name = attribute.name.toLowerCase();
            const normalizedValue = attribute.value.trim().replace(/\s+/g, '').toLowerCase();
            if (name.startsWith('on') || name === 'srcdoc') {
                element.removeAttribute(attribute.name);
                return;
            }
            if (URL_ATTRIBUTES.includes(name) && /^(javascript|vbscript|data:text\/html):/.test(normalizedValue)) {
                element.removeAttribute(attribute.name);
            }
        });
        if (element.tagName === 'A' && element.getAttribute('target') === '_blank') {
            element.setAttribute('rel', 'noopener noreferrer');
        }
    });
    return documentNode.body.innerHTML;
};

