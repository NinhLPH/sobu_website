import { create } from 'zustand';
import { CartItem, ProductModel } from '../interface/product.model';

interface CartState {
    items: CartItem[];
    addToCart: (product: ProductModel, quantity?: number) => void;
    removeFromCart: (productId: string) => void;
    updateQuantity: (productId: string, quantity: number) => void;
    clearCart: () => void;
    getTotals: () => { subtotal: number; tax: number; total: number; itemCount: number };
}

export const useCartStore = create<CartState>((set, get) => ({
    items: [],
    addToCart: (product, quantity = 1) => {
        set((state) => {
            const existingItem = state.items.find(item => item.product.id === product.id);
            if (existingItem) {
                return {
                    items: state.items.map(item =>
                        item.product.id === product.id
                            ? { ...item, quantity: item.quantity + quantity }
                            : item
                    )
                };
            }
            return { items: [...state.items, { product, quantity }] };
        });
    },
    removeFromCart: (productId) => {
        set((state) => ({
            items: state.items.filter(item => item.product.id !== productId)
        }));
    },
    updateQuantity: (productId, quantity) => {
        set((state) => ({
            items: state.items.map(item =>
                item.product.id === productId
                    ? { ...item, quantity: Math.max(1, quantity) }
                    : item
            )
        }));
    },
    clearCart: () => set({ items: [] }),
    getTotals: () => {
        const items = get().items;
        const subtotal = items.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
        const tax = subtotal * 0.1; // 10% VAT
        const total = subtotal + tax;
        const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);
        return { subtotal, tax, total, itemCount };
    }
}));