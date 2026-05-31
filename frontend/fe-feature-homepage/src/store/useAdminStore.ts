import { create } from 'zustand';

import {ProductModel} from "../interface/product.model";
import {CategoryModel} from "../interface/category.model";
import {Order} from "../interface/order";
import {ServiceRequest} from "../interface/service-request";

import { mockProducts, mockCategories, mockOrders, mockRequests } from '../data/mockData';

interface AdminState {
    products: ProductModel[];
    categories: CategoryModel[];
    orders: Order[];
    requests: ServiceRequest[];

    addProduct: (product: ProductModel) => void;
    updateProduct: (id: string, updates: Partial<ProductModel>) => void;
    deleteProduct: (id: string) => void;

    addCategory: (category: CategoryModel) => void;
    updateCategory: (id: string, updates: Partial<CategoryModel>) => void;
    deleteCategory: (id: string) => void;

    updateOrderStatus: (id: string, status: Order['status']) => void;

    updateRequest: (id: string, updates: Partial<ServiceRequest>) => void;
}

export const useAdminStore = create<AdminState>((set) => ({
    products: mockProducts,
    categories: mockCategories,
    orders: mockOrders,
    requests: mockRequests,

    addProduct: (product) => set((state) => ({ products: [...state.products, product] })),
    updateProduct: (id, updates) => set((state) => ({
        products: state.products.map(p => p.id === id ? { ...p, ...updates } : p)
    })),
    deleteProduct: (id) => set((state) => ({
        products: state.products.filter(p => p.id !== id)
    })),

    addCategory: (category) => set((state) => {
        if (category.parentId) {
            const addNested = (cats: CategoryModel[]): CategoryModel[] =>
                cats.map(c => c.id === category.parentId
                    ? { ...c, children: [...(c.children || []), category] }
                    : { ...c, children: c.children ? addNested(c.children) : c.children });
            return { categories: addNested(state.categories) };
        }
        return { categories: [...state.categories, category] };
    }),
    updateCategory: (id, updates) => set((state) => {
        const updateNested = (cats: CategoryModel[]): CategoryModel[] =>
            cats.map(c => c.id === id
                ? { ...c, ...updates }
                : { ...c, children: c.children ? updateNested(c.children) : c.children });
        return { categories: updateNested(state.categories) };
    }),

    deleteCategory: (id) => set((state) => {
        const deleteNested = (cats: CategoryModel[]): CategoryModel[] =>
            cats.filter(c => c.id !== id)
                .map(c => ({ ...c, children: c.children ? deleteNested(c.children) : c.children }));
        return { categories: deleteNested(state.categories) };
    }),

    updateOrderStatus: (id, status) => set((state) => ({
        orders: state.orders.map(o => o.id === id ? { ...o, status } : o)
    })),

    updateRequest: (id, updates) => set((state) => ({
        requests: state.requests.map(r => r.id === id ? { ...r, ...updates } : r)
    }))
}));
