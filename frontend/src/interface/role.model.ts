export type RoleName = 'USER' | 'ADMIN' | 'MANAGER' | 'STAFF';

export interface RoleDTO {
    id: number;
    name: RoleName;
    description?: string;
}
