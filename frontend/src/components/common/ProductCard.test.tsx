import {describe, expect, it, jest} from '@jest/globals';
import {render, screen} from '@testing-library/react';
import ProductCard from './ProductCard';
import {ProductModel} from '../../interface/product.model';

const mockAddToCart = jest.fn();
const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    useNavigate: () => mockNavigate,
}), {virtual: true});

jest.mock('../../store/useCartStore', () => ({
    useCartStore: (selector: any) => selector({
        addToCart: mockAddToCart,
    }),
}));

const product: ProductModel = {
    id: '101',
    name: 'SOBU Figure',
    price: 500000,
    brand: 'SOBU',
    imageUrl: 'figure.jpg',
    description: '',
    stock: 5,
    rating: 4.5,
    reviewsCount: 12,
};

describe('ProductCard', () => {
    it('renders backend review summary with five stars, rating, and review count', () => {
        render(<ProductCard product={product} />);

        expect(screen.getByLabelText('4.5 trên 5 sao từ 12 đánh giá')).toBeTruthy();
        expect(screen.getByText('4.5')).toBeTruthy();
        expect(screen.getByText('(12)')).toBeTruthy();
    });
});
