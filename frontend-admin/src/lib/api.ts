import axios from 'axios';
import type { ServiceInfoResponse, ServiceRegistrationRequest } from '@/types';

// Configuration de l'API client
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Créer une instance axios avec la configuration de base
export const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Intercepteur pour ajouter le token JWT et l'ID admin à chaque requête
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        const userStr = localStorage.getItem('user');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        if (userStr) {
            try {
                const user = JSON.parse(userStr);
                if (user.id) {
                    config.headers['X-Admin-Id'] = user.id;
                }
            } catch (e) {
                console.error('Error parsing user data', e);
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Intercepteur pour gérer les erreurs de réponse
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Token expiré ou invalide - rediriger vers login
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            if (typeof window !== 'undefined') {
                window.location.href = '/auth/login';
            }
        }
        return Promise.reject(error);
    }
);

// ========== Services API ==========

/**
 * Récupérer tous les services
 */
export const getServices = async (): Promise<ServiceInfoResponse[]> => {
    const response = await apiClient.get('/api/admin/services');
    return response.data;
};

/**
 * Récupérer un service par son code
 */
export const getService = async (code: string): Promise<ServiceInfoResponse> => {
    const response = await apiClient.get(`/api/admin/services/${code}`);
    return response.data;
};

/**
 * Créer un nouveau service
 */
export const createService = async (
    data: ServiceRegistrationRequest
): Promise<ServiceInfoResponse> => {
    const response = await apiClient.post('/api/admin/services', data);
    return response.data;
};

/**
 * Mettre à jour un service existant
 */
export const updateService = async (
    code: string,
    data: ServiceRegistrationRequest
): Promise<ServiceInfoResponse> => {
    const response = await apiClient.put(`/api/admin/services/${code}`, data);
    return response.data;
};

/**
 * Supprimer un service
 */
export const deleteService = async (code: string): Promise<void> => {
    await apiClient.delete(`/api/admin/services/${code}`);
};

/**
 * Activer/Désactiver un service
 */
export const toggleServiceStatus = async (
    code: string
): Promise<ServiceInfoResponse> => {
    const response = await apiClient.patch(`/api/admin/services/${code}/status`);
    return response.data;
};
