import type { User, LoginCredentials, RegisterCredentials } from '@/types';
import { apiClient } from './api';

/**
 * Sauvegarder le token d'authentification
 */
export const saveToken = (token: string): void => {
    if (typeof window !== 'undefined') {
        localStorage.setItem('token', token);
    }
};

/**
 * Récupérer le token d'authentification
 */
export const getToken = (): string | null => {
    if (typeof window !== 'undefined') {
        return localStorage.getItem('token');
    }
    return null;
};

/**
 * Supprimer le token d'authentification
 */
export const removeToken = (): void => {
    if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }
};

/**
 * Sauvegarder les informations utilisateur
 */
export const saveUser = (user: User): void => {
    if (typeof window !== 'undefined') {
        localStorage.setItem('user', JSON.stringify(user));
    }
};

/**
 * Récupérer les informations utilisateur
 */
export const getUser = (): User | null => {
    if (typeof window !== 'undefined') {
        const userStr = localStorage.getItem('user');
        if (userStr) {
            try {
                return JSON.parse(userStr);
            } catch {
                return null;
            }
        }
    }
    return null;
};

/**
 * Vérifier si l'utilisateur est authentifié
 */
export const isAuthenticated = (): boolean => {
    return getToken() !== null;
};

/**
 * Connexion utilisateur
 */
export const login = async (credentials: LoginCredentials): Promise<User> => {
    try {
        const response = await apiClient.post('/api/auth/login', credentials);
        const user: User = response.data;
        saveToken(user.token!);
        saveUser(user);
        return user;
    } catch (error: any) {
        const message = error.response?.data?.message || 'Invalid email or password';
        throw new Error(message);
    }
};

/**
 * Inscription utilisateur
 */
export const register = async (credentials: RegisterCredentials): Promise<User> => {
    try {
        const response = await apiClient.post('/api/auth/register', credentials);
        const user: User = response.data;
        saveToken(user.token!);
        saveUser(user);
        return user;
    } catch (error: any) {
        const message = error.response?.data?.message || 'Invalid registration data';
        throw new Error(message);
    }
};

/**
 * Déconnexion utilisateur
 */
export const logout = (): void => {
    removeToken();
};
