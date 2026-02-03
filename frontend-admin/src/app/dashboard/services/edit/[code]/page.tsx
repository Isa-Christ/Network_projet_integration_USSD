'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { getService, updateService } from '@/lib/api';
import { ServiceInfoResponse } from '@/types';
import toast from 'react-hot-toast';
import { FiSave, FiArrowLeft, FiCode } from 'react-icons/fi';
import Link from 'next/link';

export default function EditServicePage() {
    const params = useParams();
    const code = params?.code as string;
    const [service, setService] = useState<ServiceInfoResponse | null>(null);
    const [jsonConfig, setJsonConfig] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [isValid, setIsValid] = useState(true);
    const router = useRouter();

    useEffect(() => {
        if (code) {
            loadService();
        }
    }, [code]);

    const loadService = async () => {
        setIsLoading(true);
        try {
            const data = await getService(code);
            setService(data);
            if (data.jsonConfig) {
                setJsonConfig(data.jsonConfig);
            }
        } catch (error) {
            toast.error('Erreur lors du chargement du service');
            console.error(error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleJsonChange = (value: string) => {
        setJsonConfig(value);

        if (!value.trim()) {
            setIsValid(true);
            return;
        }

        try {
            JSON.parse(value);
            setIsValid(true);
        } catch {
            setIsValid(false);
        }
    };

    const handleFormatJson = () => {
        if (!jsonConfig.trim()) return;

        try {
            const parsed = JSON.parse(jsonConfig);
            const formatted = JSON.stringify(parsed, null, 2);
            setJsonConfig(formatted);
            setIsValid(true);
            toast.success('JSON formaté avec succès');
        } catch {
            toast.error('JSON invalide, impossible de formater');
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!jsonConfig.trim()) {
            toast.error('Veuillez fournir une configuration JSON');
            return;
        }

        if (!isValid) {
            toast.error('Le JSON est invalide');
            return;
        }

        setIsSaving(true);

        try {
            await updateService(code, { jsonConfig });
            toast.success('Service mis à jour avec succès !', { id: 'edit-service-success' });
            router.push('/dashboard/services/list');
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || error.message || 'Erreur lors de la mise à jour du service';
            toast.error(errorMessage, { id: 'edit-service-error' });
        } finally {
            setIsSaving(false);
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-gray-600">Chargement du service...</p>
                </div>
            </div>
        );
    }

    if (!service) {
        return (
            <div className="text-center py-12">
                <p className="text-gray-600 mb-4">Service non trouvé</p>
                <Link
                    href="/dashboard/services/list"
                    className="text-blue-600 hover:underline"
                >
                    Retour à la liste
                </Link>
            </div>
        );
    }

    return (
        <div className="animate-fadeIn">
            {/* Header */}
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">
                        Modifier le Service
                    </h1>
                    <p className="text-gray-600">
                        Modification de <span className="font-semibold text-blue-600">{service.name}</span>
                    </p>
                </div>
                <Link
                    href="/dashboard/services/list"
                    className="flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg transition-colors"
                >
                    <FiArrowLeft />
                    <span>Retour</span>
                </Link>
            </div>

            {/* Informations du service */}
            <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 mb-6">
                <h3 className="font-semibold text-blue-900 mb-3">📋 Informations actuelles</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                    <div>
                        <span className="text-blue-700 font-medium">Code :</span>
                        <span className="ml-2 text-blue-900">{service.code}</span>
                    </div>
                    <div>
                        <span className="text-blue-700 font-medium">Short Code :</span>
                        <span className="ml-2 text-blue-900">{service.shortCode}</span>
                    </div>
                    <div>
                        <span className="text-blue-700 font-medium">URL API :</span>
                        <span className="ml-2 text-blue-900 truncate">{service.apiBaseUrl}</span>
                    </div>
                    <div>
                        <span className="text-blue-700 font-medium">Statut :</span>
                        <span className={`ml-2 ${service.isActive ? 'text-green-600' : 'text-orange-600'} font-semibold`}>
                            {service.isActive ? 'Actif' : 'Inactif'}
                        </span>
                    </div>
                </div>
            </div>

            {/* Formulaire */}
            <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <div className="flex items-center justify-between mb-2">
                            <label className="block text-sm font-semibold text-gray-700">
                                Nouvelle Configuration JSON
                            </label>
                            <button
                                type="button"
                                onClick={handleFormatJson}
                                className="text-xs px-3 py-1 bg-purple-100 text-purple-700 rounded hover:bg-purple-200 transition-colors"
                            >
                                <FiCode className="inline mr-1" />
                                Formater
                            </button>
                        </div>

                        <textarea
                            value={jsonConfig}
                            onChange={(e) => handleJsonChange(e.target.value)}
                            className={`w-full h-96 p-4 font-mono text-sm border-2 rounded-lg focus:outline-none focus:ring-2 transition-all ${!isValid
                                ? 'border-red-300 focus:border-red-500 focus:ring-red-200 bg-red-50'
                                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-200'
                                }`}
                            placeholder='{\n  "serviceCode": "MY_SERVICE",\n  "serviceName": "Mon Service",\n  ...\n}'
                        />

                        {!isValid && jsonConfig.trim() && (
                            <p className="mt-2 text-sm text-red-600 flex items-center gap-1">
                                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                </svg>
                                Le JSON n&apos;est pas valide
                            </p>
                        )}

                        {isValid && jsonConfig.trim() && (
                            <p className="mt-2 text-sm text-green-600 flex items-center gap-1">
                                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                </svg>
                                JSON valide
                            </p>
                        )}
                    </div>

                    <div className="flex gap-4">
                        <button
                            type="submit"
                            disabled={isSaving || !isValid || !jsonConfig.trim()}
                            className="flex-1 flex items-center justify-center gap-2 px-6 py-3 bg-gradient-to-r from-blue-600 to-cyan-600 text-white font-semibold rounded-lg shadow-lg hover:shadow-xl hover:from-blue-700 hover:to-cyan-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transform hover:scale-105 transition-all duration-200"
                        >
                            <FiSave />
                            {isSaving ? 'Enregistrement...' : 'Enregistrer les modifications'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
