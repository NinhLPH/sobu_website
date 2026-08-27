import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, it} from '@jest/globals';
import {useState} from 'react';
import {ConfirmDialogProvider, useConfirmDialog} from './ConfirmDialog';

function Harness() {
    const confirm = useConfirmDialog();
    const [result, setResult] = useState('none');
    return <><button type="button" onClick={async () => setResult(String(await confirm({
        title: 'Xóa dữ liệu?', message: 'Thao tác này không thể hoàn tác.', confirmLabel: 'Xóa', tone: 'danger'
    })))}>Mở xác nhận</button><output>{result}</output></>;
}

const renderDialog = () => render(<ConfirmDialogProvider><Harness/></ConfirmDialogProvider>);

describe('ConfirmDialogProvider', () => {
    it('cancels without confirming and restores focus', async () => {
        renderDialog();
        const opener = screen.getByRole('button', {name: 'Mở xác nhận'});
        opener.focus();
        fireEvent.click(opener);
        expect(await screen.findByRole('alertdialog')).not.toBeNull();
        await waitFor(() => expect(document.activeElement).toBe(screen.getByRole('button', {name: 'Hủy'})));
        fireEvent.keyDown(document, {key: 'Escape'});
        await waitFor(() => expect(screen.getByText('false')).not.toBeNull());
        expect(document.activeElement).toBe(opener);
    });

    it('resolves true only once when confirmed', async () => {
        renderDialog();
        fireEvent.click(screen.getByRole('button', {name: 'Mở xác nhận'}));
        const confirmButton = await screen.findByRole('button', {name: 'Xóa'});
        fireEvent.click(confirmButton);
        fireEvent.click(confirmButton);
        await waitFor(() => expect(screen.getByText('true')).not.toBeNull());
        expect(screen.queryByRole('alertdialog')).toBeNull();
    });
});
