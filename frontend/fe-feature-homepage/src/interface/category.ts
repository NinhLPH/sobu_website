export interface Category {
    id: string;
    name: string;
    parentId?: string | null;
    children?: Category[];
}