import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { render, screen } from '@testing-library/react';
import { AccountDTO } from '../../interface/account.model';
import { RoleName } from '../../interface/role.model';
import { useAuthStore } from '../../store/useAuthStore';
import ProtectedRoute from './ProtectedRoute';

jest.mock('react-router-dom', () => ({
    Navigate: ({ to }: { to: string }) => <div>Redirect to {to}</div>,
    Outlet: () => <div>Outlet</div>,
}), { virtual: true });

const accountWithRole = (name: RoleName): AccountDTO => ({
    id: 1,
    email: 'user@example.com',
    fullName: 'Test User',
    phone: '0900000000',
    status: 'ACTIVE',
    role: { id: 2, name },
});

const renderProtectedContent = (content: string, role: RoleName) => {
    useAuthStore.setState({
        isAuthenticated: true,
        user: accountWithRole(role),
    });

    return render(
        <ProtectedRoute allowedRoles={['USER', 'ADMIN']}>
            <div>{content}</div>
        </ProtectedRoute>
    );
};

describe('ProtectedRoute', () => {
    beforeEach(() => {
        useAuthStore.setState({
            isAuthenticated: false,
            user: null,
        });
    });

    it.each([
        'Cart page',
        'Requests page',
    ])('allows USER to access %s', (content) => {
        renderProtectedContent(content, 'USER');

        expect(screen.getByText(content)).toBeTruthy();
    });

    it('redirects a role outside the allow-list to home', () => {
        renderProtectedContent('Cart page', 'STAFF');

        expect(screen.getByText('Redirect to /')).toBeTruthy();
        expect(screen.queryByText('Cart page')).toBeNull();
    });
});
