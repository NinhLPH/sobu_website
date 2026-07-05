import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import AdminStaticPages from './StaticPages';
import {StaticPageService} from '../../service/static-page.service';

jest.mock('react-quill-new', () => ({
    __esModule: true,
    default: ({value, onChange}: any) => (
        <textarea
            aria-label="Noi dung HTML"
            value={value}
            onChange={(event) => onChange(event.target.value)}
        />
    ),
}));
jest.mock('react-quill-new/dist/quill.snow.css', () => ({}));
jest.mock('../../service/static-page.service');
jest.mock('../../service/toast.service');

const mockedService = jest.mocked(StaticPageService);

const pageWith = (content: any[]) => ({
    content,
    pageNumber: 1,
    pageSize: 10,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false,
});

const staticPages = [
    {
        id: 1,
        slug: 'about',
        title: 'About',
        htmlContent: '',
        isPublished: true,
        updatedAt: '2026-06-30T10:00:00',
    },
    {
        id: 2,
        slug: 'privacy-policy',
        title: 'Privacy Policy',
        htmlContent: '<p>Privacy</p>',
        isPublished: false,
        updatedAt: '2026-06-30T10:00:00',
    },
];

describe('AdminStaticPages', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockedService.searchPages.mockResolvedValue(pageWith(staticPages));
        mockedService.createPage.mockResolvedValue(staticPages[0] as any);
        mockedService.updatePage.mockResolvedValue(staticPages[0] as any);
        mockedService.deletePage.mockResolvedValue(undefined);
        jest.spyOn(window, 'confirm').mockReturnValue(true);
    });

    it('loads static pages and searches by term', async () => {
        render(<AdminStaticPages/>);

        expect(await screen.findByText('About')).toBeTruthy();
        expect(screen.getByText('privacy-policy')).toBeTruthy();

        fireEvent.change(screen.getByPlaceholderText('Tìm theo Slug hoặc Tiêu đề...'), {target: {value: 'privacy'}});
        fireEvent.click(screen.getByRole('button', {name: /^Tìm$|^Tim$/i}));

        await waitFor(() => expect(mockedService.searchPages).toHaveBeenLastCalledWith('privacy', {
            page: 1,
            pageSize: 10,
            sortBy: 'updatedAt',
            sortDirection: 'DESC',
        }));
    });

    it('selects a static page suggestion and searches immediately', async () => {
        render(<AdminStaticPages/>);

        expect(await screen.findByText('Privacy Policy')).toBeTruthy();

        fireEvent.change(screen.getByPlaceholderText('Tìm theo Slug hoặc Tiêu đề...'), {target: {value: 'privacy'}});
        fireEvent.mouseDown(screen.getByRole('option', {name: /Privacy Policy/i}));

        await waitFor(() => expect(mockedService.searchPages).toHaveBeenLastCalledWith('Privacy Policy', {
            page: 1,
            pageSize: 10,
            sortBy: 'updatedAt',
            sortDirection: 'DESC',
        }));
    });

    it('creates a page with editor html', async () => {
        render(<AdminStaticPages/>);

        fireEvent.click(await screen.findByRole('button', {name: /Thêm trang/i}));
        fireEvent.change(screen.getByLabelText(/Tiêu đề/i), {target: {value: 'Terms'}});
        fireEvent.change(screen.getByLabelText(/Slug/i), {target: {value: 'terms'}});
        fireEvent.change(screen.getByLabelText(/Noi dung HTML/i), {target: {value: '<h1>Terms</h1>'}});
        fireEvent.click(screen.getByRole('button', {name: /Lưu trang/i}));

        await waitFor(() => expect(mockedService.createPage).toHaveBeenCalledWith({
            slug: 'terms',
            title: 'Terms',
            htmlContent: '<h1>Terms</h1>',
            isPublished: true,
        }));
    });

    it('updates an existing page and toggles publish state', async () => {
        render(<AdminStaticPages/>);

        fireEvent.click(await screen.findByLabelText(/Sửa trang About/i));
        fireEvent.click(screen.getByLabelText(/Publish/i));
        fireEvent.change(screen.getByLabelText(/Noi dung HTML/i), {target: {value: '<p>Updated</p>'}});
        fireEvent.click(screen.getByRole('button', {name: /Lưu trang/i}));

        await waitFor(() => expect(mockedService.updatePage).toHaveBeenCalledWith(1, {
            slug: 'about',
            title: 'About',
            htmlContent: '<p>Updated</p>',
            isPublished: false,
        }));
    });

    it('deletes a page after confirmation', async () => {
        render(<AdminStaticPages/>);

        fireEvent.click(await screen.findByLabelText(/Xóa trang About/i));

        await waitFor(() => expect(mockedService.deletePage).toHaveBeenCalledWith(1));
    });
});
