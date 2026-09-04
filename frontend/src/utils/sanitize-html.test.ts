import {describe, expect, it} from '@jest/globals';
import {sanitizeRichHtml, stripHtml} from './sanitize-html';

describe('sanitizeRichHtml', () => {
    it('removes executable content while preserving editorial markup', () => {
        const safe = sanitizeRichHtml('<h2 onclick="alert(1)">Tiêu đề</h2><script>alert(1)</script><a href="javascript:alert(1)" target="_blank">Xem</a>');
        expect(safe).toContain('<h2>Tiêu đề</h2>');
        expect(safe).not.toContain('script');
        expect(safe).not.toContain('onclick');
        expect(safe).not.toContain('javascript:');
        expect(safe).toContain('rel="noopener noreferrer"');
        expect(stripHtml(safe)).toBe('Tiêu đềXem');
    });
});
