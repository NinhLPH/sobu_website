import {describe, expect, it} from '@jest/globals';
import {groupConfigsByGroupName, mapPublicConfigs, parseJsonConfig} from './website-config';

describe('website-config utilities', () => {
    it('maps public flat config arrays to flat objects', () => {
        expect(mapPublicConfigs([
            {id: 1, key: 'site_name', value: 'SOBU API', type: 'text', isPublic: true},
            {id: 2, key: 'secret', value: 'hidden', type: 'text', isPublic: false},
            {id: 3, key: 'empty_value', value: '', type: 'text', isPublic: true},
        ] as any)).toEqual({
            site_name: 'SOBU API',
            empty_value: '',
        });
    });

    it('parses JSON config safely with fallbacks', () => {
        expect(parseJsonConfig({social_links: '{"facebook":"https://fb.test"}'}, 'social_links', {}))
            .toEqual({facebook: 'https://fb.test'});
        expect(parseJsonConfig({social_links: '{invalid'}, 'social_links', {facebook: ''}))
            .toEqual({facebook: ''});
        expect(parseJsonConfig(undefined, 'social_links', []))
            .toEqual([]);
    });

    it('groups configs by groupName and falls back to GENERAL', () => {
        expect(groupConfigsByGroupName([
            {id: 1, key: 'primary_color', value: '#000000', type: 'color', groupName: 'THEME', isPublic: true},
            {id: 2, key: 'site_name', value: 'SOBU', type: 'text', isPublic: true},
        ] as any)).toEqual({
            THEME: [expect.objectContaining({key: 'primary_color'})],
            GENERAL: [expect.objectContaining({key: 'site_name'})],
        });
    });
});
