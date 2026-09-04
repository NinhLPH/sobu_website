import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import Toast, {DEFAULT_TOAST_AUTO_CLOSE} from './Toast';
import {useThemeStore} from '../../store/useThemeStore';

jest.mock('react-toastify', () => ({
    ToastContainer: ({
        autoClose,
        progressClassName,
        style
    }: {
        autoClose: number;
        progressClassName?: string;
        style?: Record<string, string>;
    }) => <div
        data-testid="toast-container"
        data-auto-close={autoClose}
        data-progress-class={progressClassName}
        data-auto-close-duration={style?.['--sobu-toast-auto-close']}
    />,
    Slide: 'slide'
}));

jest.mock('../../store/useThemeStore');

const mockedUseThemeStore = jest.mocked(useThemeStore);

describe('Toast', () => {
    beforeEach(() => {
        mockedUseThemeStore.mockImplementation((selector: any) => selector({theme: 'light'}));
    });

    it('keeps notifications visible for the shared three-second duration', () => {
        render(<Toast/>);

        expect(DEFAULT_TOAST_AUTO_CLOSE).toBe(3000);
        const container = screen.getByTestId('toast-container');
        expect(container.getAttribute('data-auto-close')).toBe('3000');
        expect(container.getAttribute('data-progress-class')).toBe('sobu-toast-progress');
        expect(container.getAttribute('data-auto-close-duration')).toBe('3000ms');
    });
});
