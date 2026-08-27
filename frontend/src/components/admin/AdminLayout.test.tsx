import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import AdminLayout from './AdminLayout';

let mockPathname = '/admin/products';

jest.mock('react-router-dom', () => ({
    Link: ({children, to, ...props}: any) => <a href={to} {...props}>{children}</a>,
    Outlet: () => <div>Nội dung quản trị</div>,
    useLocation: () => ({pathname: mockPathname})
}), {virtual: true});

jest.mock('../../store/useAuthStore', () => ({
    useAuthStore: (selector: any) => selector({user: {role: {name: 'ADMIN'}}})
}));

describe('AdminLayout independent scrolling', () => {
    beforeEach(() => {
        mockPathname = '/admin/products';
        document.body.style.overflow = '';
    });

    it('provides separate desktop scroll regions and resets only main on route change', () => {
        const {container, rerender} = render(<AdminLayout/>);
        const sidebar = container.querySelector('.admin-workspace > aside') as HTMLElement;
        const main = container.querySelector('main') as HTMLElement;
        sidebar.scrollTop = 80;
        main.scrollTop = 140;

        mockPathname = '/admin/orders';
        rerender(<AdminLayout/>);

        expect(sidebar.className).toContain('overflow-y-auto');
        expect(main.className).toContain('overflow-y-auto');
        expect(main.scrollTop).toBe(0);
        expect(sidebar.scrollTop).toBe(80);
    });

    it('locks background, closes with Escape and restores focus for the mobile drawer', async () => {
        render(<AdminLayout/>);
        const opener = screen.getByRole('button', {name: 'Menu quản trị'});
        fireEvent.click(opener);
        expect(screen.getByRole('dialog', {name: 'Menu quản trị'})).not.toBeNull();
        expect(document.body.style.overflow).toBe('hidden');

        fireEvent.keyDown(document, {key: 'Escape'});
        await waitFor(() => expect(screen.queryByRole('dialog', {name: 'Menu quản trị'})).toBeNull());
        expect(document.body.style.overflow).toBe('');
        expect(document.activeElement).toBe(opener);
    });
});
