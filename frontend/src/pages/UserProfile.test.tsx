import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import UserProfile from './UserProfile';
import {AccountDTO} from '../interface/account.model';

const mockUpdatePhoneAction = jest.fn();

const account = {
    id: 1,
    email: 'user@example.com',
    fullName: 'Test User',
    phone: '0900000000',
    status: 'ACTIVE',
    role: {id: 1, name: 'USER'},
} as AccountDTO;

const mockAuthState = {
    user: account,
    updatePhoneAction: mockUpdatePhoneAction,
};

jest.mock('../store/useAuthStore', () => ({
    useAuthStore: (selector: (state: typeof mockAuthState) => unknown) => selector(mockAuthState),
}));

jest.mock('react-router-dom', () => {
    const React = require('react');

    return {
        Link: ({to, className, children}: {to: string; className?: string; children: React.ReactNode}) =>
            React.createElement('a', {href: to, className}, children),
    };
}, {virtual: true});

const renderProfile = () => render(<UserProfile/>);

describe('UserProfile', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockAuthState.user = account;
        mockUpdatePhoneAction.mockResolvedValue({
            ...account,
            phone: '0987654321',
        } as never);
    });

    it('renders the change phone form below the phone number', () => {
        renderProfile();

        expect(screen.getByText('Số điện thoại')).toBeTruthy();
        expect(screen.getByText('0900000000')).toBeTruthy();
        expect(screen.getByLabelText(/thay đổi số điện thoại/i)).toBeTruthy();
        expect((screen.getByRole('button', {name: /^lưu$/i}) as HTMLButtonElement).disabled).toBe(true);
    });

    it('saves a changed phone number with trimmed input', async () => {
        renderProfile();

        fireEvent.change(screen.getByLabelText(/thay đổi số điện thoại/i), {
            target: {value: ' 0987654321 '},
        });
        fireEvent.click(screen.getByRole('button', {name: /^lưu$/i}));

        await waitFor(() => {
            expect(mockUpdatePhoneAction).toHaveBeenCalledWith('0987654321');
        });
        expect(await screen.findByText('Số điện thoại đã được cập nhật.')).toBeTruthy();
    });
});
