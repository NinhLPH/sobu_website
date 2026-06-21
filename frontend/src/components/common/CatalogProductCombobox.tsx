import React, { KeyboardEvent, useEffect, useId, useMemo, useState } from 'react';
import { Check, ChevronDown, Loader2, Plus, Search } from 'lucide-react';
import { ProductListItemDTO } from '../../interface/product.model';

export interface CatalogProductSelection {
    nhanhProductId?: string;
    name: string;
    price?: number;
}

interface CatalogProductComboboxProps {
    products: ProductListItemDTO[];
    value: CatalogProductSelection;
    onChange: (selection: CatalogProductSelection) => void;
    disabled?: boolean;
    isLoading?: boolean;
    ariaLabel: string;
}

const MAX_VISIBLE_PRODUCTS = 5;

const normalizeSearchText = (value: string) =>
    value.trim().toLocaleLowerCase('vi-VN');

export default function CatalogProductCombobox({
    products,
    value,
    onChange,
    disabled = false,
    isLoading = false,
    ariaLabel
}: CatalogProductComboboxProps) {
    const listboxId = useId();
    const [query, setQuery] = useState(value.name);
    const [isOpen, setIsOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(0);

    useEffect(() => {
        if (!isOpen) {
            setQuery(value.name);
        }
    }, [isOpen, value.name, value.nhanhProductId]);

    const matchingProducts = useMemo(() => {
        const normalizedQuery = normalizeSearchText(query);
        return products
            .filter(product => {
                if (!normalizedQuery) return true;
                return normalizeSearchText(product.name).includes(normalizedQuery) ||
                    normalizeSearchText(product.code).includes(normalizedQuery);
            })
            .slice(0, MAX_VISIBLE_PRODUCTS);
    }, [products, query]);

    const normalizedQuery = normalizeSearchText(query);
    const hasExactMatch = products.some(product =>
        normalizeSearchText(product.name) === normalizedQuery ||
        normalizeSearchText(product.code) === normalizedQuery
    );
    const canCreateNewProduct = Boolean(normalizedQuery) && !hasExactMatch;
    const optionCount = matchingProducts.length + (canCreateNewProduct ? 1 : 0);

    const selectProduct = (product: ProductListItemDTO) => {
        onChange({
            nhanhProductId: product.code,
            name: product.name,
            price: product.price
        });
        setQuery(product.name);
        setIsOpen(false);
    };

    const selectNewProduct = () => {
        const productName = query.trim();
        if (!productName) return;
        onChange({
            nhanhProductId: undefined,
            name: productName,
            price: undefined
        });
        setQuery(productName);
        setIsOpen(false);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Escape') {
            setIsOpen(false);
            return;
        }

        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            setIsOpen(true);
            setActiveIndex(current => {
                if (optionCount === 0) return 0;
                const offset = event.key === 'ArrowDown' ? 1 : -1;
                return (current + offset + optionCount) % optionCount;
            });
            return;
        }

        if (event.key === 'Enter' && isOpen) {
            event.preventDefault();
            if (activeIndex < matchingProducts.length) {
                selectProduct(matchingProducts[activeIndex]);
            } else if (canCreateNewProduct) {
                selectNewProduct();
            }
        }
    };

    return (
        <div
            className="relative"
            onBlur={(event) => {
                if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
                    setIsOpen(false);
                    setQuery(value.name);
                }
            }}
        >
            <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-outline" />
                <input
                    type="text"
                    role="combobox"
                    aria-label={ariaLabel}
                    aria-autocomplete="list"
                    aria-controls={listboxId}
                    aria-expanded={isOpen}
                    value={query}
                    onFocus={() => {
                        setActiveIndex(0);
                        setIsOpen(true);
                    }}
                    onChange={(event) => {
                        const nextQuery = event.target.value;
                        setQuery(nextQuery);
                        setActiveIndex(0);
                        setIsOpen(true);
                        if (value.name || value.nhanhProductId) {
                            onChange({
                                nhanhProductId: undefined,
                                name: '',
                                price: undefined
                            });
                        }
                    }}
                    onKeyDown={handleKeyDown}
                    placeholder="Gõ tên hoặc mã sản phẩm..."
                    disabled={disabled}
                    autoComplete="off"
                    className="w-full rounded-xl border border-transparent bg-surface-container-lowest py-2 pl-9 pr-9 text-xs font-semibold text-on-surface outline-none transition-all placeholder:text-outline/40 focus:border-primary/20 focus:ring-1 focus:ring-primary disabled:cursor-not-allowed disabled:opacity-75"
                />
                <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-outline">
                    {isLoading ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                        <ChevronDown className="h-3.5 w-3.5" />
                    )}
                </span>
            </div>

            {isOpen && !disabled && (
                <div
                    id={listboxId}
                    role="listbox"
                    className="absolute z-30 mt-1 max-h-72 w-full overflow-auto rounded-xl border border-surface-container bg-surface-container-lowest p-1.5 shadow-xl"
                >
                    {isLoading && products.length === 0 ? (
                        <div className="px-3 py-2 text-xs font-semibold text-outline">
                            Đang tải catalog...
                        </div>
                    ) : (
                        <>
                            {matchingProducts.map((product, index) => (
                                <button
                                    key={product.id}
                                    type="button"
                                    role="option"
                                    aria-selected={value.nhanhProductId === product.code}
                                    onMouseDown={event => event.preventDefault()}
                                    onMouseEnter={() => setActiveIndex(index)}
                                    onClick={() => selectProduct(product)}
                                    className={`flex w-full items-center justify-between gap-3 rounded-lg px-3 py-2 text-left transition-colors ${
                                        activeIndex === index
                                            ? 'bg-primary/10 text-primary'
                                            : 'text-on-surface hover:bg-surface-container'
                                    }`}
                                >
                                    <span className="min-w-0">
                                        <span className="block truncate text-xs font-bold">
                                            {product.name}
                                        </span>
                                        <span className="block truncate text-[10px] font-semibold text-outline">
                                            {product.code} · {product.price.toLocaleString('vi-VN')}đ
                                        </span>
                                    </span>
                                    {value.nhanhProductId === product.code && (
                                        <Check className="h-3.5 w-3.5 shrink-0" />
                                    )}
                                </button>
                            ))}

                            {canCreateNewProduct && (
                                <button
                                    type="button"
                                    role="option"
                                    aria-selected={!value.nhanhProductId && value.name === query.trim()}
                                    onMouseDown={event => event.preventDefault()}
                                    onMouseEnter={() => setActiveIndex(matchingProducts.length)}
                                    onClick={selectNewProduct}
                                    className={`mt-1 flex w-full items-center gap-2 rounded-lg border-t border-surface-container px-3 py-2 text-left text-xs font-bold transition-colors ${
                                        activeIndex === matchingProducts.length
                                            ? 'bg-primary/10 text-primary'
                                            : 'text-primary hover:bg-primary/5'
                                    }`}
                                >
                                    <Plus className="h-3.5 w-3.5 shrink-0" />
                                    <span className="truncate">
                                        Tạo sản phẩm mới: “{query.trim()}”
                                    </span>
                                </button>
                            )}

                            {matchingProducts.length === 0 && !canCreateNewProduct && (
                                <div className="px-3 py-2 text-xs font-semibold text-outline">
                                    Gõ tên hoặc mã để tìm sản phẩm.
                                </div>
                            )}
                        </>
                    )}
                </div>
            )}
        </div>
    );
}
