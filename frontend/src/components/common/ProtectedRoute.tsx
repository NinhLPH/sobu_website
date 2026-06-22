// ProtectedRoute.tsx
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { RoleName } from '../../interface/role.model';

type AllowedRole = RoleName | `ROLE_${RoleName}`;

interface ProtectedRouteProps {
    allowedRoles: AllowedRole[];
    children?: React.ReactNode;
}

export default function ProtectedRoute({ allowedRoles, children }: ProtectedRouteProps) {
    const { isAuthenticated, user } = useAuthStore();

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    const userRole = user?.role?.name;
    const isAllowed = allowedRoles.some(allowedRole => {
        const cleanAllowed = allowedRole.replace('ROLE_', '').toUpperCase();
        const cleanUser = (userRole || '').replace('ROLE_', '').toUpperCase();
        return cleanAllowed === cleanUser;
    });

    if (!isAllowed) {
        return <Navigate to="/" replace />;
    }

    return children ? <>{children}</> : <Outlet />;
}
