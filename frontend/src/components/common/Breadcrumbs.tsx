import {ChevronRight} from 'lucide-react';
import {Link} from 'react-router-dom';

export type BreadcrumbItem = {
    label: string;
    to?: string;
};

type BreadcrumbsProps = {
    items: BreadcrumbItem[];
};

export default function Breadcrumbs({items}: BreadcrumbsProps) {
    return (
        <nav
            aria-label="Điều hướng phân cấp"
            className="mb-6 flex min-h-5 items-center gap-2 overflow-x-auto whitespace-nowrap text-xs font-bold text-on-surface-variant sm:mb-8 sm:text-sm"
        >
            {items.map((item, index) => (
                <div key={`${item.to || 'current'}-${item.label}`} className="flex shrink-0 items-center gap-2">
                    {index > 0 && <ChevronRight aria-hidden="true" className="h-3.5 w-3.5 text-outline"/>}
                    {item.to ? (
                        <Link to={item.to} className="transition-colors hover:text-primary">
                            {item.label}
                        </Link>
                    ) : (
                        <span aria-current="page" className="text-primary">{item.label}</span>
                    )}
                </div>
            ))}
        </nav>
    );
}
