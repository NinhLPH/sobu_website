import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, jest} from '@jest/globals';
import {AdminFilterGroup, AdminFilterReset, AdminFilterSelect, AdminSearch, AdminToolbar} from './AdminUi';

describe('shared Admin toolbar controls', () => {
    it('submits search with Enter, clears it and exposes filter labels', () => {
        const onChange = jest.fn();
        const onSubmit = jest.fn();
        const onReset = jest.fn();
        render(<AdminToolbar>
            <AdminSearch value="serum" onChange={onChange} onSubmit={onSubmit}
                         placeholder="Tìm sản phẩm" ariaLabel="Tìm kiếm sản phẩm"/>
            <AdminFilterGroup><AdminFilterSelect label="Trạng thái" value="ACTIVE" onChange={jest.fn()}>
                <option value="ACTIVE">Hoạt động</option>
            </AdminFilterSelect><AdminFilterReset disabled={false} onClick={onReset}/></AdminFilterGroup>
        </AdminToolbar>);

        fireEvent.keyDown(screen.getByLabelText('Tìm kiếm sản phẩm'), {key: 'Enter'});
        expect(onSubmit).toHaveBeenCalledWith('serum');
        fireEvent.click(screen.getByRole('button', {name: 'Xóa tìm kiếm sản phẩm'}));
        expect(onChange).toHaveBeenCalledWith('');
        expect(screen.getByLabelText('Trạng thái')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', {name: 'Xóa lọc'}));
        expect(onReset).toHaveBeenCalledTimes(1);
    });
});
