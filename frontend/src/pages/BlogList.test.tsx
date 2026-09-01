import {describe, expect, it, jest} from '@jest/globals';
import {render, screen, waitFor} from '@testing-library/react';
import BlogList from './BlogList';

const mockSetSearchParams = jest.fn();
var mockGetPublishedArticles = jest.fn<Promise<any>, any[]>();
jest.mock('react-router-dom', () => ({
    Link: ({to, children, ...props}: any) => <a href={to} {...props}>{children}</a>,
    useSearchParams: () => [new URLSearchParams(''), mockSetSearchParams],
}), {virtual: true});
jest.mock('../service/article.service', () => ({
    ArticleService: {getPublishedArticles: (...args: any[]) => mockGetPublishedArticles(...args)},
}));

describe('BlogList', () => {
    it('renders published API articles with slug links and accessible image alt text', async () => {
        mockGetPublishedArticles.mockResolvedValue({
            content: [{id: 1, title: 'Bí quyết trưng bày', slug: 'bi-quyet-trung-bay', thumbnailUrl: '/uploads/blog.jpg', thumbnailAlt: 'Tủ trưng bày mô hình', excerpt: 'Gợi ý cho collector', category: 'Kinh nghiệm', status: 'PUBLISHED'}],
            pageNumber: 0, pageSize: 9, totalElements: 1, totalPages: 1, first: true, last: true,
        });
        render(<BlogList/>);

        await waitFor(() => expect(screen.getByText('Bí quyết trưng bày')).toBeTruthy());
        expect(screen.getByRole('img', {name: 'Tủ trưng bày mô hình'})).toBeTruthy();
        expect(screen.getAllByRole('link', {name: /Bí quyết trưng bày/})[0].getAttribute('href')).toBe('/blog/bi-quyet-trung-bay');
        expect(mockGetPublishedArticles).toHaveBeenCalledWith({page: 0, size: 9, category: undefined});
    });
});
