import { ChangeEvent, FormEvent, KeyboardEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronDown, Loader2, Minus, Plus, Search, ShoppingBag, Trash2 } from 'lucide-react';
import { useCartStore } from '../store/useCartStore';
import { useAuthStore } from '../store/useAuthStore';
import { useLocationStore } from '../store/useLocationStore';
import { usePaymentStore } from '../store/usePaymentStore';
import { ToastService } from '../service/toast.service';
import { formatCurrency } from '../utils/format';
import { PaymentMethod } from '../enum/union-types';
import { redirectToPaymentCheckout } from '../utils/payment-session';
import { onlineCartRecovery } from '../utils/online-cart-recovery';
import { ShippingService } from '../service/shipping.service';
import { ShippingQuoteDto } from '../interface/shipping.model';
import { LocationCity, LocationDistrict, LocationWard } from '../interface/location.model';

interface QuantityControllerProps {
    quantity: number;
    disabled?: boolean;
    onIncrease: () => void;
    onDecrease: () => void;
    onChange: (value: number) => void;
}

interface CheckoutForm {
    customerName: string;
    customerMobile: string;
    customerEmail: string;
    customerAddress: string;
    customerCityName: string;
    customerDistrictName: string;
    customerWardName: string;
    customerCityId: number | null;
    customerDistrictId: number | null;
    customerWardId: number | null;
    description: string;
}

type CheckoutTextField = Exclude<
    keyof CheckoutForm,
    'customerCityId' | 'customerDistrictId' | 'customerWardId'
>;

const initialCheckoutForm: CheckoutForm = {
    customerName: '',
    customerMobile: '',
    customerEmail: '',
    customerAddress: '',
    customerCityName: '',
    customerDistrictName: '',
    customerWardName: '',
    customerCityId: null,
    customerDistrictId: null,
    customerWardId: null,
    description: ''
};

const MAX_CUSTOMER_ADDRESS_LENGTH = 500;

type CompleteShippingQuote = ShippingQuoteDto & {
    carrierId?: number | null;
    carrierName?: string | null;
    carrierServiceId?: number | null;
    carrierServiceName?: string | null;
    shipFee?: number | null;
    customerShipFee?: number | null;
};

const parseFiniteNumber = (value: unknown) => {
    if (typeof value === 'number') {
        return Number.isFinite(value) ? value : Number.NaN;
    }
    if (typeof value === 'string' && value.trim().length > 0) {
        const numberValue = Number(value);
        return Number.isFinite(numberValue) ? numberValue : Number.NaN;
    }
    return Number.NaN;
};

const parsePositiveInteger = (value: unknown) => {
    const numberValue = parseFiniteNumber(value);
    return Number.isInteger(numberValue) && numberValue > 0 ? numberValue : null;
};

const hasText = (value: unknown): value is string =>
    typeof value === 'string' && value.trim().length > 0;

const shippingQuoteFee = (quote: ShippingQuoteDto | null | undefined) => {
    const customerFee = parseFiniteNumber(quote?.customerShipFee);
    if (Number.isFinite(customerFee)) {
        return customerFee;
    }
    return parseFiniteNumber(quote?.shipFee);
};

const normalizeShippingQuote = (
    quote: ShippingQuoteDto | null | undefined
): CompleteShippingQuote | null => {
    if (!quote) {
        return null;
    }

    const fee = shippingQuoteFee(quote);

    if (!Number.isFinite(fee) || fee < 0) {
        return null;
    }

    return {
        ...quote,
        carrierId: parsePositiveInteger(quote.carrierId),
        carrierName: hasText(quote.carrierName) ? quote.carrierName.trim() : null,
        carrierServiceId: parsePositiveInteger(quote.carrierServiceId),
        carrierServiceName: hasText(quote.carrierServiceName) ? quote.carrierServiceName.trim() : null,
        shipFee: quote.shipFee === null || quote.shipFee === undefined
            ? quote.shipFee
            : parseFiniteNumber(quote.shipFee),
        customerShipFee: quote.customerShipFee === null || quote.customerShipFee === undefined
            ? quote.customerShipFee
            : parseFiniteNumber(quote.customerShipFee)
    };
};

const isUsableShippingQuote = (
    quote: ShippingQuoteDto | null | undefined
): quote is CompleteShippingQuote => normalizeShippingQuote(quote) !== null;

const normalizeSearchText = (value: unknown) =>
    typeof value === 'string'
        ? value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
        : '';

const shippingQuoteMode = (quote: CompleteShippingQuote, index: number) => {
    const serviceName = normalizeSearchText(quote.carrierServiceName);
    return serviceName.includes('hoa toc') || index === 1 ? 'express' : 'standard';
};

const shippingQuoteLabel = (quote: CompleteShippingQuote, index: number) =>
    shippingQuoteMode(quote, index) === 'express' ? 'Hỏa tốc' : 'Tiêu chuẩn';

const shippingQuoteKey = (quote: CompleteShippingQuote, index: number) =>
    [
        index,
        shippingQuoteMode(quote, index),
        shippingQuoteFee(quote)
    ].join('-');

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

interface LocationComboboxOption {
    id: number;
    name: string;
    otherName?: string | null;
}

interface LocationComboboxProps {
    label: string;
    placeholder: string;
    searchPlaceholder?: string;
    options: LocationComboboxOption[];
    value: number | null;
    disabled?: boolean;
    loading?: boolean;
    onSelect: (value: number | null) => void;
}

const toLocationSearchHaystack = (option: LocationComboboxOption) =>
    normalizeSearchText([option.name, option.otherName].filter(Boolean).join(' '));

function LocationCombobox({
                              label,
                              placeholder,
                              searchPlaceholder = 'Nhập từ khóa',
                              options,
                              value,
                              disabled,
                              loading,
                              onSelect
                          }: LocationComboboxProps) {
    const [isOpen, setIsOpen] = useState(false);
    const [query, setQuery] = useState('');
    const rootRef = useRef<HTMLDivElement | null>(null);
    const searchInputRef = useRef<HTMLInputElement | null>(null);
    const selectedOption = options.find((option) => option.id === value) ?? null;
    const filteredOptions = useMemo(() => {
        const normalizedQuery = normalizeSearchText(query.trim());
        if (!normalizedQuery) {
            return options;
        }
        return options.filter((option) => toLocationSearchHaystack(option).includes(normalizedQuery));
    }, [options, query]);

    useEffect(() => {
        if (!isOpen) {
            return;
        }
        const handlePointerDown = (event: MouseEvent) => {
            if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handlePointerDown);
        return () => document.removeEventListener('mousedown', handlePointerDown);
    }, [isOpen]);

    useEffect(() => {
        if (isOpen) {
            window.setTimeout(() => searchInputRef.current?.focus(), 0);
        } else {
            setQuery('');
        }
    }, [isOpen]);

    const openDropdown = () => {
        if (!disabled) {
            setIsOpen((current) => !current);
        }
    };

    const selectOption = (optionValue: number | null) => {
        onSelect(optionValue);
        setIsOpen(false);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
        if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            if (!disabled) {
                setIsOpen(true);
            }
        }
    };

    return (
        <div ref={rootRef} className="relative">
            <button
                type="button"
                aria-label={label}
                aria-haspopup="listbox"
                aria-expanded={isOpen}
                onClick={openDropdown}
                onKeyDown={handleKeyDown}
                disabled={disabled}
                className={`flex min-h-[40px] w-full cursor-pointer items-center justify-between gap-2 rounded-xl border bg-surface-container-lowest px-4 py-2.5 text-left text-xs font-medium text-on-surface outline-none transition-colors focus:ring-2 focus:ring-primary/20 disabled:cursor-not-allowed disabled:opacity-60 ${
                    isOpen ? 'border-primary/50' : 'border-surface-container'
                }`}
            >
                <span className={`min-w-0 truncate ${selectedOption ? '' : 'text-outline'}`}>
                    {loading ? 'Đang tải...' : selectedOption?.name ?? placeholder}
                </span>
                <ChevronDown className={`h-4 w-4 shrink-0 text-outline transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </button>
            {isOpen && (
                <div className="absolute left-0 right-0 top-[calc(100%+6px)] z-30 rounded-xl border border-surface-container bg-surface-container-lowest p-2 shadow-xl shadow-black/10">
                    <div className="relative mb-2">
                        <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-outline" />
                        <input
                            ref={searchInputRef}
                            type="text"
                            value={query}
                            onChange={(event) => setQuery(event.target.value)}
                            placeholder={searchPlaceholder}
                            className="h-9 w-full rounded-lg border border-surface-container bg-white pl-9 pr-3 text-xs font-medium text-on-surface outline-none transition-colors placeholder:text-outline/60 focus:border-primary/40 focus:ring-2 focus:ring-primary/15"
                        />
                    </div>
                    <div role="listbox" aria-label={label} className="max-h-[220px] overflow-y-auto pr-1">
                        <button
                            type="button"
                            role="option"
                            aria-selected={value === null}
                            onClick={() => selectOption(null)}
                            className="flex w-full cursor-pointer flex-col rounded-lg px-3 py-2 text-left text-xs font-semibold text-outline transition-colors hover:bg-surface-container"
                        >
                            {placeholder}
                        </button>
                        {filteredOptions.map((option) => (
                            <button
                                key={option.id}
                                type="button"
                                role="option"
                                aria-selected={option.id === value}
                                onClick={() => selectOption(option.id)}
                                className={`flex w-full cursor-pointer flex-col rounded-lg px-3 py-2 text-left transition-colors hover:bg-surface-container ${
                                    option.id === value ? 'bg-primary/10 text-primary' : 'text-on-surface'
                                }`}
                            >
                                <span className="truncate text-xs font-bold">{option.name}</span>
                                {option.otherName && (
                                    <span className="mt-0.5 w-full truncate text-[11px] font-medium text-outline">
                                        {option.otherName}
                                    </span>
                                )}
                            </button>
                        ))}
                        {filteredOptions.length === 0 && (
                            <p className="px-3 py-3 text-xs font-semibold text-outline">
                                Không tìm thấy địa điểm phù hợp.
                            </p>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

const cityToComboboxOption = (city: LocationCity): LocationComboboxOption => ({
    id: city.cityId,
    name: city.cityName,
    otherName: city.otherName
});

const districtToComboboxOption = (district: LocationDistrict): LocationComboboxOption => ({
    id: district.districtId,
    name: district.districtName,
    otherName: district.otherName
});

const wardToComboboxOption = (ward: LocationWard): LocationComboboxOption => ({
    id: ward.wardId,
    name: ward.wardName,
    otherName: ward.otherName
});

const locationSelectValue = (value: number | null | ChangeEvent<HTMLSelectElement>) => {
    if (value === null || typeof value === 'number') {
        return value;
    }
    return value.target.value ? Number(value.target.value) : null;
};

function QuantityController({
                                quantity,
                                disabled,
                                onIncrease,
                                onDecrease,
                                onChange
                            }: QuantityControllerProps) {
    const [inputValue, setInputValue] = useState(quantity.toString());

    useEffect(() => {
        setInputValue(quantity.toString());
    }, [quantity]);

    const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
        const value = event.target.value;
        if (value === '' || /^\d+$/.test(value)) {
            setInputValue(value);
            const numberValue = Number.parseInt(value, 10);
            if (Number.isFinite(numberValue) && numberValue > 0) {
                onChange(numberValue);
            }
        }
    };

    const handleInputBlur = () => {
        const numberValue = Number.parseInt(inputValue, 10);
        if (!Number.isFinite(numberValue) || numberValue <= 0) {
            setInputValue('1');
            onChange(1);
        }
    };

    return (
        <div className="flex w-fit items-center rounded-full border border-surface-container-high/40 bg-surface-container px-1 py-1">
            <button
                type="button"
                onClick={onDecrease}
                disabled={disabled || quantity <= 1}
                className="flex h-6 w-6 items-center justify-center rounded-full bg-surface-container-lowest text-on-surface shadow-sm transition-colors hover:text-primary disabled:opacity-50"
            >
                <Minus className="h-3 w-3" />
            </button>
            <input
                type="text"
                inputMode="numeric"
                value={inputValue}
                onChange={handleInputChange}
                onBlur={handleInputBlur}
                disabled={disabled}
                className="w-9 border-none bg-transparent p-0 text-center text-xs font-black text-on-surface outline-none focus:ring-0"
            />
            <button
                type="button"
                onClick={onIncrease}
                disabled={disabled}
                className="flex h-6 w-6 items-center justify-center rounded-full bg-surface-container-lowest text-on-surface shadow-sm transition-colors hover:text-primary disabled:opacity-50"
            >
                <Plus className="h-3 w-3" />
            </button>
        </div>
    );
}

export default function Cart() {
    const {
        items,
        isLoading,
        removeFromCart,
        updateQuantity,
        getTotals,
        submitOrder,
        isSubmitting,
        checkoutError,
        clearCheckoutError,
        fetchCart
    } = useCartStore();
    const {
        createPayment,
        isCreatingPayment
    } = usePaymentStore();
    const {
        locationTree,
        locationsLoaded,
        isLoading: isLocationsLoading,
        error: locationError,
        fetchLocations,
        cancelScheduledRetry
    } = useLocationStore();
    const { subtotal, itemCount } = getTotals();
    const { isAuthenticated, user } = useAuthStore();
    const navigate = useNavigate();
    const [form, setForm] = useState<CheckoutForm>(initialCheckoutForm);
    const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('ONLINE');
    const [validationError, setValidationError] = useState<string | null>(null);
    const [shippingQuotes, setShippingQuotes] = useState<CompleteShippingQuote[]>([]);
    const [selectedShippingQuoteKey, setSelectedShippingQuoteKey] = useState('');
    const [confirmedShippingQuote, setConfirmedShippingQuote] = useState<ShippingQuoteDto | null>(null);
    const [isLoadingShippingQuotes, setIsLoadingShippingQuotes] = useState(false);
    const [isConfirmingShippingQuote, setIsConfirmingShippingQuote] = useState(false);
    const [shippingQuoteError, setShippingQuoteError] = useState<string | null>(null);
    const confirmShippingQuoteRequestRef = useRef(0);

    useEffect(() => {
        void fetchLocations(true);
        if (isAuthenticated) {
            void fetchCart();
        }
        return cancelScheduledRetry;
    }, [fetchLocations, cancelScheduledRetry, fetchCart, isAuthenticated]);

    useEffect(() => {
        if (!user) {
            return;
        }
        setForm((current) => ({
            ...current,
            customerName: current.customerName || user.fullName || '',
            customerMobile: current.customerMobile || user.phone || '',
            customerEmail: current.customerEmail || user.email || ''
        }));
    }, [user]);

    const updateField = (
        field: CheckoutTextField,
        value: string
    ) => {
        setForm((current) => ({ ...current, [field]: value }));
        setValidationError(null);
        clearCheckoutError();
    };

    const cities = useMemo(() => locationTree?.cities ?? [], [locationTree]);
    const selectedCity = useMemo(
        () => cities.find((city) => city.cityId === form.customerCityId),
        [cities, form.customerCityId]
    );
    const districts = useMemo(() => selectedCity?.districts ?? [], [selectedCity]);
    const selectedDistrict = useMemo(
        () => districts.find((district) => district.districtId === form.customerDistrictId),
        [districts, form.customerDistrictId]
    );
    const wards = useMemo(() => selectedDistrict?.wards ?? [], [selectedDistrict]);
    const cityOptions = useMemo(() => cities.map(cityToComboboxOption), [cities]);
    const districtOptions = useMemo(() => districts.map(districtToComboboxOption), [districts]);
    const wardOptions = useMemo(() => wards.map(wardToComboboxOption), [wards]);
    const hasSelectedShippingLocation =
        form.customerCityId !== null &&
        form.customerDistrictId !== null &&
        form.customerWardId !== null &&
        Boolean(form.customerCityName) &&
        Boolean(form.customerDistrictName) &&
        Boolean(form.customerWardName);
    const selectedShippingQuote = shippingQuotes.find(
        (quote, index) => shippingQuoteKey(quote, index) === selectedShippingQuoteKey
    ) ?? null;
    const displayShippingQuote = confirmedShippingQuote ?? selectedShippingQuote;
    const selectedShippingFee = isUsableShippingQuote(displayShippingQuote)
        ? shippingQuoteFee(displayShippingQuote)
        : 0;
    const orderTotal = subtotal + selectedShippingFee;
    const isCheckoutDisabled =
        isSubmitting ||
        isCreatingPayment ||
        isLoadingShippingQuotes ||
        isConfirmingShippingQuote ||
        !locationsLoaded ||
        items.length === 0 ||
        !isUsableShippingQuote(confirmedShippingQuote);

    const buildShippingQuoteRequest = useCallback((
        quote?: { carrierId?: number | null; carrierServiceId?: number | null }
    ) => {
        const carrierId = parsePositiveInteger(quote?.carrierId);
        const carrierServiceId = parsePositiveInteger(quote?.carrierServiceId);

        return {
            customerCityId: form.customerCityId as number,
            customerDistrictId: form.customerDistrictId as number,
            customerWardId: form.customerWardId as number,
            cartSubtotal: subtotal,
            codAmount: paymentMethod === 'COD' ? subtotal : 0,
            ...(carrierId !== null && carrierServiceId !== null
                ? {
                    carrierId,
                    carrierServiceId
                }
                : {})
        };
    }, [
        form.customerCityId,
        form.customerDistrictId,
        form.customerWardId,
        paymentMethod,
        subtotal
    ]);

    useEffect(() => {
        confirmShippingQuoteRequestRef.current += 1;
        setShippingQuotes([]);
        setSelectedShippingQuoteKey('');
        setConfirmedShippingQuote(null);
        setIsConfirmingShippingQuote(false);
        setShippingQuoteError(null);

        if (!hasSelectedShippingLocation || subtotal <= 0) {
            setIsLoadingShippingQuotes(false);
            return;
        }

        let isCancelled = false;
        setIsLoadingShippingQuotes(true);

        void ShippingService.getQuotes(buildShippingQuoteRequest())
            .then((response) => {
                if (isCancelled) {
                    return;
                }
                if (!response.success) {
                    throw new Error(response.message || 'Could not load shipping quotes.');
                }
                const quotes = response.data ?? [];
                const usableQuotes = quotes
                    .map(normalizeShippingQuote)
                    .filter((quote): quote is CompleteShippingQuote => quote !== null);
                setShippingQuotes(usableQuotes);
                if (usableQuotes.length === 0) {
                    setShippingQuoteError(
                        quotes.length === 0
                            ? 'Khong co tuy chon giao hang phu hop voi dia chi nay.'
                            : 'Khong co tuy chon giao hang hop le. Vui long thu lai hoac chon dia chi khac.'
                    );
                }
            })
            .catch((error) => {
                if (!isCancelled) {
                    setShippingQuoteError(
                        getErrorMessage(error, 'Không thể tính phí giao hàng. Vui lòng thử lại.')
                    );
                }
            })
            .finally(() => {
                if (!isCancelled) {
                    setIsLoadingShippingQuotes(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [
        buildShippingQuoteRequest,
        hasSelectedShippingLocation,
        subtotal
    ]);

    const handleShippingQuoteSelect = async (quote: ShippingQuoteDto, index: number) => {
        if (!isUsableShippingQuote(quote)) {
            setSelectedShippingQuoteKey('');
            setConfirmedShippingQuote(null);
            setShippingQuoteError('Tuy chon giao hang khong hop le. Vui long chon phuong thuc khac.');
            return;
        }

        const key = shippingQuoteKey(quote, index);
        const requestId = confirmShippingQuoteRequestRef.current + 1;
        confirmShippingQuoteRequestRef.current = requestId;

        setSelectedShippingQuoteKey(key);
        setConfirmedShippingQuote(null);
        setShippingQuoteError(null);
        setValidationError(null);
        clearCheckoutError();

        if (parsePositiveInteger(quote.carrierId) === null || parsePositiveInteger(quote.carrierServiceId) === null) {
            setConfirmedShippingQuote(quote);
            setIsConfirmingShippingQuote(false);
            return;
        }

        setIsConfirmingShippingQuote(true);

        try {
            const response = await ShippingService.getQuotes(buildShippingQuoteRequest(quote));
            if (confirmShippingQuoteRequestRef.current !== requestId) {
                return;
            }
            if (!response.success) {
                throw new Error(response.message || 'Could not confirm shipping quote.');
            }

            const confirmedQuote = (response.data ?? [])
                .map(normalizeShippingQuote)
                .find((item): item is CompleteShippingQuote =>
                    item !== null && shippingQuoteKey(item, index) === key
                );
            if (!confirmedQuote) {
                throw new Error('Khong the xac nhan phi giao hang da chon. Vui long chon phuong thuc khac.');
            }

            setConfirmedShippingQuote(confirmedQuote);
        } catch (error) {
            if (confirmShippingQuoteRequestRef.current === requestId) {
                setShippingQuoteError(
                    getErrorMessage(error, 'Khong the xac nhan phi giao hang. Vui long thu lai.')
                );
            }
        } finally {
            if (confirmShippingQuoteRequestRef.current === requestId) {
                setIsConfirmingShippingQuote(false);
            }
        }
    };

    const handleCityChange = (value: number | null | ChangeEvent<HTMLSelectElement>) => {
        const cityId = locationSelectValue(value);
        const city = cities.find((item) => item.cityId === cityId);

        setForm((current) => ({
            ...current,
            customerCityId: city?.cityId ?? null,
            customerCityName: city?.cityName ?? '',
            customerDistrictId: null,
            customerDistrictName: '',
            customerWardId: null,
            customerWardName: ''
        }));
        setValidationError(null);
        clearCheckoutError();
    };

    const handleDistrictChange = (value: number | null | ChangeEvent<HTMLSelectElement>) => {
        const districtId = locationSelectValue(value);
        const district = districts.find((item) => item.districtId === districtId);

        setForm((current) => ({
            ...current,
            customerDistrictId: district?.districtId ?? null,
            customerDistrictName: district?.districtName ?? '',
            customerWardId: null,
            customerWardName: ''
        }));
        setValidationError(null);
        clearCheckoutError();
    };

    const handleWardChange = (value: number | null | ChangeEvent<HTMLSelectElement>) => {
        const wardId = locationSelectValue(value);
        const ward = wards.find((item) => item.wardId === wardId);

        setForm((current) => ({
            ...current,
            customerWardId: ward?.wardId ?? null,
            customerWardName: ward?.wardName ?? ''
        }));
        setValidationError(null);
        clearCheckoutError();
    };

    const handleSubmit = async (event: FormEvent) => {
        event.preventDefault();
        setValidationError(null);
        clearCheckoutError();

        if (!isAuthenticated) {
            navigate('/login');
            return;
        }

        if (!form.customerName.trim() || !form.customerMobile.trim()) {
            setValidationError('Vui lòng nhập họ tên và số điện thoại người nhận.');
            return;
        }

        if (form.customerAddress.trim().length > MAX_CUSTOMER_ADDRESS_LENGTH) {
            setValidationError('Dia chi giao hang khong duoc vuot qua 500 ky tu.');
            return;
        }

        if (
            form.customerCityId === null ||
            form.customerDistrictId === null ||
            form.customerWardId === null ||
            !form.customerCityName ||
            !form.customerDistrictName ||
            !form.customerWardName
        ) {
            setValidationError(
                'Vui lòng chọn đầy đủ tỉnh/thành phố, quận/huyện và phường/xã.'
            );
            return;
        }

        if (!isUsableShippingQuote(confirmedShippingQuote)) {
            setValidationError('Vui lòng chọn đơn vị giao hàng trước khi đặt hàng.');
            return;
        }

        const shippingFee = shippingQuoteFee(confirmedShippingQuote);
        const carrierId = parsePositiveInteger(confirmedShippingQuote.carrierId);
        const carrierServiceId = parsePositiveInteger(confirmedShippingQuote.carrierServiceId);

        try {
            const order = await submitOrder({
                customerName: form.customerName.trim(),
                customerMobile: form.customerMobile.trim(),
                customerEmail: form.customerEmail.trim() || undefined,
                customerAddress: form.customerAddress.trim() || undefined,
                customerCityName: form.customerCityName,
                customerDistrictName: form.customerDistrictName,
                customerWardName: form.customerWardName,
                customerCityId: form.customerCityId,
                customerDistrictId: form.customerDistrictId,
                customerWardId: form.customerWardId,
                shippingFee,
                ...(carrierId !== null && carrierServiceId !== null
                    ? {
                        carrierId,
                        carrierServiceId
                    }
                    : {}),
                description: form.description.trim() || undefined
            }, {
                clearCartOnSuccess: paymentMethod !== 'ONLINE'
            });

            try {
                const payment = await createPayment(order.id, {
                    type: 'FULL',
                    paymentMethod
                });
                if (paymentMethod === 'ONLINE') {
                    onlineCartRecovery.save(payment, items);
                    redirectToPaymentCheckout(payment);
                    return;
                }
                navigate(
                    `/tracking?orderId=${encodeURIComponent(String(order.id))}&paymentSetup=cod`,
                    { replace: true }
                );
            } catch (error: any) {
                ToastService.error(
                    error?.response?.data?.message ||
                    error?.message ||
                    'Đơn hàng đã được tạo nhưng chưa thể khởi tạo thanh toán. Bạn có thể thử lại trong trang theo dõi đơn.'
                );
                navigate(
                    `/tracking?orderId=${encodeURIComponent(String(order.id))}&paymentSetup=failed`,
                    { replace: true }
                );
            }
        } catch {
            // The cart store exposes the backend error through checkoutError.
        }
    };

    if (isLoading) {
        return (
            <main className="flex min-h-[60vh] w-full min-w-0 flex-col items-center justify-center bg-surface px-4 py-28 text-center sm:px-6 sm:py-32">
                <Loader2 className="mb-4 h-10 w-10 animate-spin text-primary" />
                <p className="text-xs font-bold text-outline">Đang tải giỏ hàng...</p>
            </main>
        );
    }

    if (items.length === 0 && !isSubmitting && !isCreatingPayment) {
        return (
            <main className="flex min-h-[60vh] w-full min-w-0 flex-col items-center justify-center bg-surface px-4 py-28 text-center sm:px-6 sm:py-32">
                <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-surface-container shadow-inner">
                    <ShoppingBag className="h-10 w-10 text-primary opacity-40" />
                </div>
                <h2 className="mb-2 text-2xl font-black text-on-surface">Giỏ hàng trống</h2>
                <p className="mb-8 text-xs font-medium text-on-surface-variant">
                    Bạn chưa chọn sản phẩm nào.
                </p>
                <Link
                    to="/products"
                    className="rounded-xl bg-primary px-8 py-3 text-xs font-bold uppercase tracking-wider text-on-primary shadow-md transition-transform hover:scale-[1.02] focus-visible:ring-2 focus-visible:ring-primary/40"
                >
                    Khám phá cửa hàng
                </Link>
            </main>
        );
    }

    if (items.length === 0 && (isSubmitting || isCreatingPayment)) {
        return (
            <main className="flex min-h-[60vh] w-full min-w-0 flex-col items-center justify-center bg-surface px-4 py-28 text-center sm:px-6 sm:py-32">
                <Loader2 className="mb-4 h-10 w-10 animate-spin text-primary" />
                <p className="text-xs font-bold text-outline">
                    {isCreatingPayment
                        ? 'Đang mở cổng thanh toán...'
                        : 'Đang tạo đơn hàng...'}
                </p>
            </main>
        );
    }

    return (
        <main className="w-full min-w-0 bg-surface px-3 pb-14 pt-24 sm:px-6 sm:pb-16">
            <header className="mb-8 flex items-center gap-3">
                <h1 className="text-2xl font-black uppercase tracking-tight text-on-surface md:text-3xl">
                    Giỏ hàng
                </h1>
                <div className="h-6 w-px bg-surface-container-highest" />
                <span className="text-sm font-black text-primary">{itemCount} sản phẩm</span>
            </header>

            <form onSubmit={handleSubmit} className="grid grid-cols-1 items-start gap-7 lg:grid-cols-12 lg:gap-10">
                <div className="space-y-8 lg:col-span-7">
                    {(validationError || checkoutError || locationError || shippingQuoteError) && (
                        <div className="rounded-xl border border-error/20 bg-error/10 px-4 py-3 text-xs font-bold text-error">
                            {validationError || checkoutError || locationError || shippingQuoteError}
                        </div>
                    )}

                    <section>
                        <h2 className="mb-4 text-base font-black uppercase tracking-tight text-on-surface">
                            Thông tin liên hệ
                        </h2>
                        <div className="space-y-3">
                            <input
                                type="text"
                                value={form.customerName}
                                onChange={(event) => updateField('customerName', event.target.value)}
                                disabled={isSubmitting || isCreatingPayment}
                                placeholder="Họ và tên khách hàng *"
                                className="w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-xs font-medium text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                                required
                            />
                            <input
                                type="tel"
                                value={form.customerMobile}
                                onChange={(event) => updateField('customerMobile', event.target.value)}
                                disabled={isSubmitting || isCreatingPayment}
                                placeholder="Số điện thoại di động *"
                                className="w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-xs font-medium text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                                required
                            />
                            <input
                                type="email"
                                value={form.customerEmail}
                                onChange={(event) => updateField('customerEmail', event.target.value)}
                                disabled={isSubmitting || isCreatingPayment}
                                placeholder="Email"
                                className="w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-xs font-medium text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                            />
                        </div>
                    </section>

                    <section>
                        <h2 className="mb-4 text-base font-black uppercase tracking-tight text-on-surface">
                            Thông tin vận chuyển
                        </h2>
                        <div className="space-y-3">
                            <input
                                type="text"
                                value={form.customerAddress}
                                onChange={(event) => updateField('customerAddress', event.target.value)}
                                disabled={isSubmitting || isCreatingPayment}
                                maxLength={MAX_CUSTOMER_ADDRESS_LENGTH}
                                placeholder="Địa chỉ giao hàng chi tiết (Địa chỉ sau sát nhập)"
                                className="w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-xs font-medium text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                            />
                            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                                <LocationCombobox
                                    label="Tỉnh/Thành phố"
                                    placeholder="Tỉnh/Thành phố *"
                                    options={cityOptions}
                                    value={form.customerCityId}
                                    onSelect={handleCityChange}
                                    disabled={
                                        isSubmitting ||
                                        isCreatingPayment ||
                                        isLocationsLoading ||
                                        !locationsLoaded
                                    }
                                    loading={isLocationsLoading}
                                />
                                <LocationCombobox
                                    label="Quận/Huyện"
                                    placeholder="Quận/Huyện *"
                                    options={districtOptions}
                                    value={form.customerDistrictId}
                                    onSelect={handleDistrictChange}
                                    disabled={
                                        isSubmitting ||
                                        isCreatingPayment ||
                                        !selectedCity
                                    }
                                />
                                <LocationCombobox
                                    label="Phường/Xã"
                                    placeholder="Phường/Xã *"
                                    options={wardOptions}
                                    value={form.customerWardId}
                                    onSelect={handleWardChange}
                                    disabled={
                                        isSubmitting ||
                                        isCreatingPayment ||
                                        !selectedDistrict
                                    }
                                />
                            </div>
                            <div hidden aria-hidden="true" className="hidden">
                                <select
                                    value={form.customerCityId ?? ''}
                                    onChange={handleCityChange}
                                    disabled={
                                        isSubmitting ||
                                        isCreatingPayment ||
                                        isLocationsLoading ||
                                        !locationsLoaded
                                    }
                                    className="w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-xs font-medium text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                                    required
                                >
                                    <option value="">
                                        {isLocationsLoading ? 'Đang tải...' : 'Tỉnh / Thành phố *'}
                                    </option>
                                    {cities.map((city) => (
                                        <option key={city.cityId} value={city.cityId}>
                                            {city.cityName}
                                        </option>
                                    ))}
                                </select>
                                <select
                                    value={form.customerDistrictId ?? ''}
                                    onChange={handleDistrictChange}
                                    disabled={
                                        isSubmitting ||
                                        isCreatingPayment ||
                                        !selectedCity
                                    }
                                    className="w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-xs font-medium text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                                    required
                                >
                                    <option value="">Quận/Huyện *</option>
                                    {districts.map((district) => (
                                        <option key={district.districtId} value={district.districtId}>
                                            {district.districtName}
                                        </option>
                                    ))}
                                </select>
                                <select
                                    value={form.customerWardId ?? ''}
                                    onChange={handleWardChange}
                                    disabled={
                                        isSubmitting ||
                                        isCreatingPayment ||
                                        !selectedDistrict
                                    }
                                    className="w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-xs font-medium text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                                    required
                                >
                                    <option value="">Phường/Xã *</option>
                                    {wards.map((ward) => (
                                        <option key={ward.wardId} value={ward.wardId}>
                                            {ward.wardName}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            {locationTree?.stale && (
                                <p className="text-xs font-semibold text-amber-700">
                                    Đang sử dụng dữ liệu địa điểm được lưu gần nhất.
                                </p>
                            )}
                            <textarea
                                value={form.description}
                                onChange={(event) => updateField('description', event.target.value)}
                                disabled={isSubmitting || isCreatingPayment}
                                placeholder="Ghi chú giao hàng"
                                className="min-h-[90px] w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-2.5 text-xs font-medium text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                            />
                        </div>
                    </section>
                </div>

                <aside className="min-w-0 lg:col-span-5">
                    <div className="rounded-2xl border border-surface-container/60 bg-surface-container-lowest p-4 shadow-sm sm:p-5 lg:sticky lg:top-28">
                        <h3 className="mb-4 border-b border-surface-container-high pb-3 text-sm font-black uppercase tracking-wider">
                            Đơn hàng của bạn
                        </h3>
                        <div className="mb-5 max-h-[320px] space-y-4 overflow-y-auto pr-1">
                            {items.map(({ product, quantity }) => (
                                <div key={product.id} className="flex items-center gap-3.5">
                                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-surface-container-low p-1.5 sm:h-14 sm:w-14">
                                        <img
                                            src={product.imageUrl}
                                            alt={product.name}
                                            className="h-full w-full object-contain"
                                        />
                                    </div>
                                    <div className="min-w-0 flex-1">
                                        <h4 className="line-clamp-1 text-xs font-bold leading-tight text-on-surface">
                                            {product.name}
                                        </h4>
                                        <div className="mt-1.5">
                                            <QuantityController
                                                quantity={quantity}
                                                disabled={isSubmitting || isCreatingPayment}
                                                onIncrease={() => updateQuantity(product.id, quantity + 1)}
                                                onDecrease={() => updateQuantity(product.id, quantity - 1)}
                                                onChange={(value) => updateQuantity(product.id, value)}
                                            />
                                        </div>
                                    </div>
                                    <div className="flex h-12 shrink-0 flex-col items-end justify-between text-right">
                                        <p className="text-xs font-black text-primary">
                                            {formatCurrency(product.price * quantity)}
                                        </p>
                                        <button
                                            type="button"
                                            onClick={() => removeFromCart(product.id)}
                                            disabled={isSubmitting || isCreatingPayment}
                                            className="flex items-center gap-1 p-0.5 text-[10px] font-black uppercase text-error hover:underline disabled:opacity-50"
                                        >
                                            <Trash2 className="h-3 w-3" /> Xóa
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <fieldset className="mb-5 border-t border-surface-container-high pt-4">
                            <legend className="text-[10px] font-black uppercase tracking-wider text-outline">
                                Đơn vị giao hàng
                            </legend>
                            {!hasSelectedShippingLocation && (
                                <p className="mt-2 text-xs font-semibold text-on-surface-variant">
                                    Chọn đầy đủ tỉnh/thành, quận/huyện và phường/xã để tính phí giao hàng.
                                </p>
                            )}
                            {hasSelectedShippingLocation && isLoadingShippingQuotes && (
                                <div className="mt-3 flex items-center gap-2 text-xs font-bold text-primary">
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                    Đang tính phí giao hàng...
                                </div>
                            )}
                            {hasSelectedShippingLocation && !isLoadingShippingQuotes && shippingQuotes.length > 0 && (
                                <div className="mt-3 space-y-2">
                                    {shippingQuotes.map((quote, index) => {
                                        const key = shippingQuoteKey(quote, index);
                                        const fee = shippingQuoteFee(quote);
                                        const label = shippingQuoteLabel(quote, index);
                                        const isSelected = selectedShippingQuoteKey === key;
                                        const hasCustomerFee = quote.customerShipFee !== undefined && quote.customerShipFee !== null;

                                        return (
                                            <button
                                                key={key}
                                                type="button"
                                                aria-pressed={isSelected}
                                                onClick={() => {
                                                    void handleShippingQuoteSelect(quote, index);
                                                }}
                                                disabled={isSubmitting || isCreatingPayment || isConfirmingShippingQuote}
                                                className={`flex cursor-pointer items-start gap-3 rounded-xl border px-3 py-3 text-xs transition-colors duration-200 focus-within:ring-2 focus-within:ring-primary/30 ${
                                                    isSelected
                                                        ? 'border-primary bg-primary/10 text-on-surface shadow-sm'
                                                        : 'border-surface-container-high bg-surface-container-lowest text-on-surface-variant hover:border-primary/60 hover:bg-primary/5'
                                                } disabled:cursor-not-allowed disabled:opacity-60 focus:outline-none focus:ring-2 focus:ring-primary/40`}
                                            >
                                                <span
                                                    aria-hidden="true"
                                                    className={`mt-1 flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded-full border ${
                                                        isSelected ? 'border-primary' : 'border-outline'
                                                    }`}
                                                >
                                                    {isSelected && (
                                                        <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                                                    )}
                                                </span>
                                                <span className="min-w-0 flex-1">
                                                    <span className="block text-sm font-black text-on-surface">
                                                        {label}
                                                    </span>
                                                    {quote.carrierName && (
                                                        <span className="mt-1 block font-medium">
                                                            {quote.carrierName}
                                                            {quote.carrierServiceName ? ` - ${quote.carrierServiceName}` : ''}
                                                        </span>
                                                    )}
                                                    {(quote.deliveryTime || quote.description) && (
                                                        <span className="mt-1 block font-medium">
                                                            {[quote.deliveryTime, quote.description].filter(Boolean).join(' - ')}
                                                        </span>
                                                    )}
                                                    <span className="mt-1 block font-medium">
                                                        Phí ship: {formatCurrency(fee)}
                                                        {hasCustomerFee
                                                            ? ` | Phí gốc: ${formatCurrency(Number(quote.shipFee ?? 0))}`
                                                            : ''}
                                                    </span>
                                                </span>
                                                <span className="shrink-0 font-black text-primary">
                                                    {formatCurrency(fee)}
                                                </span>
                                            </button>
                                        );
                                    })}
                                    {isConfirmingShippingQuote && (
                                        <div className="flex items-center gap-2 rounded-xl border border-primary/20 bg-primary/5 px-3 py-2 text-xs font-bold text-primary">
                                            <Loader2 className="h-4 w-4 animate-spin" />
                                            Dang xac nhan phi giao hang...
                                        </div>
                                    )}
                                </div>
                            )}
                        </fieldset>

                        <div className="space-y-2.5 border-t border-surface-container-high pt-4 text-xs font-bold">
                            <div className="flex justify-between text-on-surface-variant">
                                <span>Tạm tính</span>
                                <span>{formatCurrency(subtotal)}</span>
                            </div>
                            <div className="flex justify-between text-on-surface-variant">
                                <span>Phí giao hàng</span>
                                <span className="text-[10px] uppercase tracking-wider text-primary">
                                    {isConfirmingShippingQuote
                                        ? 'Dang xac nhan'
                                        : confirmedShippingQuote
                                            ? formatCurrency(selectedShippingFee)
                                            : selectedShippingQuote
                                                ? 'Can xac nhan'
                                                : 'Chưa chọn'}
                                </span>
                            </div>
                            <div className="mt-2 flex items-end justify-between border-t border-dashed border-surface-container-high pt-3">
                                <span className="text-sm font-black uppercase">Tổng thanh toán</span>
                                <span className="text-xl font-black leading-none tracking-tight text-primary sm:text-2xl">
                                    {formatCurrency(orderTotal)}
                                </span>
                            </div>
                        </div>

                        <label className="mt-5 block text-[10px] font-black uppercase tracking-wider text-outline">
                            Phương thức thanh toán
                            <select
                                aria-label="Phương thức thanh toán"
                                value={paymentMethod}
                                onChange={(event) => {
                                    setPaymentMethod(event.target.value as PaymentMethod);
                                    setValidationError(null);
                                    clearCheckoutError();
                                }}
                                disabled={isSubmitting || isCreatingPayment}
                                className="mt-2 w-full rounded-xl border border-surface-container bg-surface-container-lowest px-4 py-3 text-xs font-bold normal-case text-on-surface outline-none focus:ring-2 focus:ring-primary/20"
                                required
                            >
                                <option value="ONLINE">ONLINE - Thanh toán qua PayOS</option>
                                <option value="COD">COD - Thanh toán khi nhận hàng</option>
                            </select>
                        </label>

                        <button
                            type="submit"
                            disabled={isCheckoutDisabled}
                            className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-primary to-primary-container py-3 text-xs font-black uppercase tracking-widest text-on-primary shadow-md shadow-primary/10 transition-transform hover:scale-[1.01] focus-visible:ring-2 focus-visible:ring-primary/40 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {(isSubmitting || isCreatingPayment || isConfirmingShippingQuote) && <Loader2 className="h-4 w-4 animate-spin" />}
                            {isSubmitting
                                ? 'Đang tạo đơn hàng...'
                                : isCreatingPayment
                                    ? 'Đang khởi tạo thanh toán...'
                                    : isConfirmingShippingQuote
                                        ? 'Dang xac nhan phi giao hang...'
                                        : paymentMethod === 'COD'
                                            ? 'Đặt hàng với COD'
                                            : 'Đặt hàng và thanh toán'}
                        </button>

                        <div className="mt-3 text-center">
                            <Link
                                to="/products"
                                className="text-xs font-bold text-outline underline transition-colors hover:text-primary"
                            >
                                Tiếp tục mua sắm
                            </Link>
                        </div>
                    </div>
                </aside>
            </form>
        </main>
    );
}
