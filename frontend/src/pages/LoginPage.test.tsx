import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import LoginPage from './LoginPage';

const mockNavigate = jest.fn();
const mockLoginAction = jest.fn();
const mockRegisterAction = jest.fn();
const mockGoogleLoginAction = jest.fn();
const mockClearError = jest.fn();

jest.mock('react-router-dom', () => ({
    useNavigate: () => mockNavigate,
}), {virtual: true});

jest.mock('../store/useAuthStore', () => ({
    useAuthStore: () => ({
        loginAction: mockLoginAction,
        registerAction: mockRegisterAction,
        googleLoginAction: mockGoogleLoginAction,
        isLoading: false,
        error: null,
        clearError: mockClearError,
        isAuthenticated: false,
    }),
}));

const setInputValue = (input: Element, value: string) => {
    fireEvent.change(input, { target: { value } });
};

describe('LoginPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        delete process.env.REACT_APP_GOOGLE_CLIENT_ID;
        delete window.google;
        document.head.querySelector('#google-identity-services-script')?.remove();
    });

    it('keeps traditional login working when Google client id is missing', async () => {
        mockLoginAction.mockResolvedValue({} as never);
        const { container } = render(<LoginPage />);

        const emailInput = container.querySelector('input[type="email"]') as HTMLInputElement;
        const passwordInput = container.querySelector('input[type="password"]') as HTMLInputElement;
        setInputValue(emailInput, 'user@example.com');
        setInputValue(passwordInput, 'password123');

        fireEvent.click(screen.getByRole('button', { name: /tiếp tục với google/i }));
        expect(screen.getByText(/REACT_APP_GOOGLE_CLIENT_ID/)).toBeTruthy();

        const loginButtons = screen.getAllByRole('button', { name: /^Đăng nhập$/i });
        fireEvent.click(loginButtons[loginButtons.length - 1]);

        await waitFor(() => {
            expect(mockLoginAction).toHaveBeenCalledWith('user@example.com', 'password123');
        });
        expect(mockNavigate).toHaveBeenCalledWith('/');
    });

    it('registers and navigates without activation or resend messaging', async () => {
        mockRegisterAction.mockResolvedValue({} as never);
        const { container } = render(<LoginPage />);

        fireEvent.click(screen.getByRole('button', { name: /^Đăng ký$/i }));

        const inputs = container.querySelectorAll('input');
        setInputValue(inputs[0], 'Test User');
        setInputValue(inputs[1], 'user@example.com');
        setInputValue(inputs[2], '0900000000');
        setInputValue(inputs[3], 'password123');
        setInputValue(inputs[4], 'password123');

        const registerButtons = screen.getAllByRole('button', { name: /^Đăng ký$/i });
        fireEvent.click(registerButtons[registerButtons.length - 1]);

        await waitFor(() => {
            expect(mockRegisterAction).toHaveBeenCalledWith({
                email: 'user@example.com',
                password: 'password123',
                fullName: 'Test User',
                phone: '0900000000',
            });
        });
        expect(mockNavigate).toHaveBeenCalledWith('/');
        expect(screen.queryByText(/kích hoạt/i)).toBeNull();
        expect(screen.queryByText(/gửi lại/i)).toBeNull();
    });

    it('sends the Google credential to the auth store callback', async () => {
        process.env.REACT_APP_GOOGLE_CLIENT_ID = 'google-client-id';
        mockGoogleLoginAction.mockResolvedValue({} as never);
        let credentialCallback: ((response: GoogleCredentialResponse) => void) | undefined;
        const initialize = jest.fn((config: { callback: (response: GoogleCredentialResponse) => void }) => {
            credentialCallback = config.callback;
        });
        const renderButton = jest.fn((parent: HTMLElement) => {
            parent.appendChild(document.createElement('button'));
        });

        window.google = {
            accounts: {
                id: {
                    initialize,
                    renderButton,
                },
            },
        };

        render(<LoginPage />);

        await waitFor(() => {
            expect(initialize).toHaveBeenCalledWith(expect.objectContaining({
                client_id: 'google-client-id',
                callback: expect.any(Function),
            }));
            expect(renderButton).toHaveBeenCalled();
        });

        await act(async () => {
            credentialCallback?.({ credential: 'google-id-token' });
        });

        expect(mockGoogleLoginAction).toHaveBeenCalledWith('google-id-token');
        expect(mockNavigate).toHaveBeenCalledWith('/');
    });
});
