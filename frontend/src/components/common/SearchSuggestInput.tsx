import {useMemo, useState, type KeyboardEvent} from 'react';

export interface SearchSuggestion {
    id: string | number;
    label: string;
    description?: string;
    searchValue?: string;
}

interface SearchSuggestInputProps {
    value: string;
    onChange: (value: string) => void;
    onSubmit: (value: string) => void;
    suggestions: SearchSuggestion[];
    placeholder: string;
    ariaLabel?: string;
    id?: string;
    maxSuggestions?: number;
    className?: string;
    dropdownClassName?: string;
}

export const normalizeSearchText = (value: string) =>
    value
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd')
        .replace(/Đ/g, 'D')
        .toLowerCase()
        .trim();

const suggestionKeyword = (suggestion: SearchSuggestion) =>
    (suggestion.searchValue || suggestion.label).trim();

export default function SearchSuggestInput({
    value,
    onChange,
    onSubmit,
    suggestions,
    placeholder,
    ariaLabel,
    id,
    maxSuggestions = 5,
    className,
    dropdownClassName,
}: SearchSuggestInputProps) {
    const [isOpen, setIsOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(-1);
    const normalizedQuery = normalizeSearchText(value);

    const visibleSuggestions = useMemo(() => {
        if (!normalizedQuery) {
            return [];
        }

        const seen = new Set<string>();
        return suggestions
            .filter((suggestion) => {
                const haystack = normalizeSearchText([
                    suggestion.searchValue,
                    suggestion.label,
                    suggestion.description,
                ].filter(Boolean).join(' '));
                const key = normalizeSearchText(suggestionKeyword(suggestion));

                if (!haystack.includes(normalizedQuery) || seen.has(key)) {
                    return false;
                }

                seen.add(key);
                return true;
            })
            .slice(0, maxSuggestions);
    }, [maxSuggestions, normalizedQuery, suggestions]);

    const shouldShowDropdown = isOpen && visibleSuggestions.length > 0;

    const selectSuggestion = (suggestion: SearchSuggestion) => {
        const keyword = suggestionKeyword(suggestion);
        onChange(keyword);
        setIsOpen(false);
        setActiveIndex(-1);
        onSubmit(keyword);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'ArrowDown' && visibleSuggestions.length > 0) {
            event.preventDefault();
            setIsOpen(true);
            setActiveIndex((current) => (current + 1) % visibleSuggestions.length);
            return;
        }

        if (event.key === 'ArrowUp' && visibleSuggestions.length > 0) {
            event.preventDefault();
            setIsOpen(true);
            setActiveIndex((current) =>
                current <= 0 ? visibleSuggestions.length - 1 : current - 1
            );
            return;
        }

        if (event.key === 'Escape') {
            setIsOpen(false);
            setActiveIndex(-1);
            return;
        }

        if (event.key === 'Enter') {
            event.preventDefault();
            if (shouldShowDropdown && activeIndex >= 0) {
                selectSuggestion(visibleSuggestions[activeIndex]);
                return;
            }

            setIsOpen(false);
            setActiveIndex(-1);
            onSubmit(value.trim());
        }
    };

    return (
        <>
            {/* eslint-disable-next-line jsx-a11y/role-supports-aria-props */}
            <input
                id={id}
                value={value}
                onChange={(event) => {
                    onChange(event.target.value);
                    setIsOpen(true);
                    setActiveIndex(-1);
                }}
                onFocus={() => setIsOpen(true)}
                onBlur={() => {
                    setIsOpen(false);
                    setActiveIndex(-1);
                }}
                onKeyDown={handleKeyDown}
                className={className}
                placeholder={placeholder}
                type="search"
                aria-label={ariaLabel}
                aria-autocomplete="list"
                aria-expanded={shouldShowDropdown}
            />

            {shouldShowDropdown && (
                <div
                    role="listbox"
                    className={`absolute left-0 right-0 top-full z-[80] mt-2 overflow-hidden rounded-xl border border-outline-variant/30 bg-surface-container-lowest shadow-[0_18px_40px_-18px_rgba(14,48,78,0.35)] ${dropdownClassName || ''}`}
                >
                    {visibleSuggestions.map((suggestion, index) => (
                        <button
                            key={suggestion.id}
                            type="button"
                            role="option"
                            aria-selected={activeIndex === index}
                            onMouseDown={(event) => {
                                event.preventDefault();
                                selectSuggestion(suggestion);
                            }}
                            onMouseEnter={() => setActiveIndex(index)}
                            className={`flex w-full cursor-pointer flex-col items-start gap-0.5 px-4 py-3 text-left transition-colors ${
                                activeIndex === index
                                    ? 'bg-primary/10 text-primary'
                                    : 'text-on-surface hover:bg-surface-container'
                            }`}
                        >
                            <span className="w-full truncate text-xs font-black">
                                {suggestion.label}
                            </span>
                            {suggestion.description && (
                                <span className="w-full truncate text-[10px] font-semibold text-outline">
                                    {suggestion.description}
                                </span>
                            )}
                        </button>
                    ))}
                </div>
            )}
        </>
    );
}
