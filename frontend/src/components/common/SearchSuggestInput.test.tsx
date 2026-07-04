import {describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen} from '@testing-library/react';
import SearchSuggestInput from './SearchSuggestInput';

const suggestions = [
    {id: 1, label: 'Sodu Serum', description: 'Skincare'},
    {id: 2, label: 'Sodu Cream', description: 'Kem dưỡng'},
    {id: 3, label: 'Son lì Melia', description: 'Trang điểm'},
    {id: 4, label: 'Mô hình Gundam', description: 'Mecha'},
    {id: 5, label: 'Mô hình Tomica', description: 'Vehicle'},
    {id: 6, label: 'Mô hình Hot Wheels', description: 'Vehicle'},
];

describe('SearchSuggestInput', () => {
    it('filters without accents and limits suggestions to five items', () => {
        const handleChange = jest.fn();
        render(
            <SearchSuggestInput
                value="mo hinh"
                onChange={handleChange}
                onSubmit={jest.fn()}
                suggestions={suggestions}
                placeholder="Tìm kiếm"
            />
        );

        fireEvent.focus(screen.getByPlaceholderText('Tìm kiếm'));

        expect(screen.getAllByRole('option')).toHaveLength(3);
        expect(screen.getByText('Mô hình Gundam')).toBeTruthy();
    });

    it('dedupes and keeps at most five visible options', () => {
        render(
            <SearchSuggestInput
                value="sodu"
                onChange={jest.fn()}
                onSubmit={jest.fn()}
                suggestions={[
                    ...suggestions,
                    {id: 7, label: 'Sodu Serum', description: 'Duplicate'},
                    {id: 8, label: 'Sodu Toner'},
                    {id: 9, label: 'Sodu Mask'},
                    {id: 10, label: 'Sodu Cleanser'},
                    {id: 11, label: 'Sodu Lotion'},
                ]}
                placeholder="Tìm kiếm"
            />
        );

        fireEvent.focus(screen.getByPlaceholderText('Tìm kiếm'));

        expect(screen.getAllByRole('option')).toHaveLength(5);
        expect(screen.getAllByText('Sodu Serum')).toHaveLength(1);
    });

    it('submits the clicked suggestion', () => {
        const handleChange = jest.fn();
        const handleSubmit = jest.fn();
        render(
            <SearchSuggestInput
                value="ser"
                onChange={handleChange}
                onSubmit={handleSubmit}
                suggestions={suggestions}
                placeholder="Tìm kiếm"
            />
        );

        fireEvent.focus(screen.getByPlaceholderText('Tìm kiếm'));
        fireEvent.mouseDown(screen.getByRole('option', {name: /Sodu Serum/i}));

        expect(handleChange).toHaveBeenCalledWith('Sodu Serum');
        expect(handleSubmit).toHaveBeenCalledWith('Sodu Serum');
    });

    it('supports arrow navigation, Enter submit, and Escape close', () => {
        const handleSubmit = jest.fn();
        const {rerender} = render(
            <SearchSuggestInput
                value="sodu"
                onChange={jest.fn()}
                onSubmit={handleSubmit}
                suggestions={suggestions}
                placeholder="Tìm kiếm"
            />
        );

        const input = screen.getByPlaceholderText('Tìm kiếm');
        fireEvent.focus(input);
        fireEvent.keyDown(input, {key: 'ArrowDown'});
        fireEvent.keyDown(input, {key: 'ArrowDown'});
        fireEvent.keyDown(input, {key: 'Enter'});

        expect(handleSubmit).toHaveBeenCalledWith('Sodu Cream');

        rerender(
            <SearchSuggestInput
                value="sodu"
                onChange={jest.fn()}
                onSubmit={handleSubmit}
                suggestions={suggestions}
                placeholder="Tìm kiếm"
            />
        );

        fireEvent.focus(screen.getByPlaceholderText('Tìm kiếm'));
        expect(screen.getAllByRole('option').length).toBeGreaterThan(0);
        fireEvent.keyDown(screen.getByPlaceholderText('Tìm kiếm'), {key: 'Escape'});
        expect(screen.queryByRole('listbox')).toBeNull();
    });
});
