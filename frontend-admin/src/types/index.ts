// Types pour l'application d'administration USSD

export interface ServiceInfoResponse {
    id: number;
    code: string;
    name: string;
    shortCode: string;
    apiBaseUrl: string;
    jsonConfig?: string;
    isActive: boolean;
    createdAt: string;
}

export interface ServiceRegistrationRequest {
    jsonConfig: string;
}

export interface User {
    id: string;
    email: string;
    name: string;
    token?: string;
}

export interface LoginCredentials {
    email: string;
    password: string;
}

export interface RegisterCredentials {
    name: string;
    email: string;
    password: string;
    confirmPassword: string;
}

export interface ApiError {
    message: string;
    status?: number;
}

export interface DashboardStats {
    totalServices: number;
    servicesAdded: number;
    servicesModified: number;
    servicesDeleted: number;
    activeServices: number;
    inactiveServices: number;
}
