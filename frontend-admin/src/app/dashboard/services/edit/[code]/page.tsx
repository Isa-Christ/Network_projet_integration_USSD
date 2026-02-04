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
            toast.error('Error loading service');
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
            toast.success('JSON formatted successfully');
        } catch {
            toast.error('Invalid JSON, cannot format');
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!jsonConfig.trim()) {
            toast.error('Please provide a JSON configuration');
            return;
        }

        if (!isValid) {
            toast.error('The JSON is invalid');
            return;
        }

        setIsSaving(true);

        try {
            await updateService(code, { jsonConfig });
            toast.success('Service updated successfully!', { id: 'edit-service-success' });
            router.push('/dashboard/services/list');
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || error.message || 'Error updating service';
            toast.error(errorMessage, { id: 'edit-service-error' });
        } finally {
            setIsSaving(false);
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
                    <p className="text-slate-600 font-medium">Loading service...</p>
                </div>
            </div>
        );
    }

    if (!service) {
        return (
            <div className="text-center py-20 px-4">
                <p className="text-slate-600 font-bold mb-4">Service not found</p>
                <Link
                    href="/dashboard/services/list"
                    className="text-primary font-bold hover:underline"
                >
                    Back to List
                </Link>
            </div>
        );
    }

    return (
        <div className="animate-fadeIn">
            {/* Header */}
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800 mb-1">
                        Edit Service
                    </h2>
                    <p className="text-slate-500 text-sm">
                        Editing <span className="font-bold text-primary">{service.name}</span>
                    </p>
                </div>
                <Link
                    href="/dashboard/services/list"
                    className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-xl hover:bg-slate-50 transition-colors font-bold text-sm shadow-sm"
                >
                    <FiArrowLeft className="w-4 h-4" />
                    <span>Back</span>
                </Link>
            </div>

            {/* Service Information */}
            <div className="bg-primary/5 border border-primary/10 rounded-2xl p-6 mb-8">
                <h3 className="text-sm font-bold text-primary uppercase tracking-widest mb-4">📋 Current Information</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 text-sm">
                    <div>
                        <span className="text-slate-500 font-bold block uppercase text-[10px] tracking-wider mb-1">Code</span>
                        <span className="text-slate-800 font-mono font-bold bg-white px-2 py-1 rounded border border-slate-100">{service.code}</span>
                    </div>
                    <div>
                        <span className="text-slate-500 font-bold block uppercase text-[10px] tracking-wider mb-1">Short Code</span>
                        <span className="text-slate-800 font-bold bg-white px-2 py-1 rounded border border-slate-100">{service.shortCode}</span>
                    </div>
                    <div>
                        <span className="text-slate-500 font-bold block uppercase text-[10px] tracking-wider mb-1">API URL</span>
                        <div className="text-slate-800 font-medium truncate font-mono bg-white px-2 py-1 rounded border border-slate-100" title={service.apiBaseUrl}>{service.apiBaseUrl}</div>
                    </div>
                    <div>
                        <span className="text-slate-500 font-bold block uppercase text-[10px] tracking-wider mb-1">Status</span>
                        <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider ${service.isActive ? 'bg-emerald-50 text-emerald-600 border border-emerald-100' : 'bg-rose-50 text-rose-600 border border-rose-100'}`}>
                            <span className={`w-1.5 h-1.5 rounded-full ${service.isActive ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`}></span>
                            {service.isActive ? 'Active' : 'Inactive'}
                        </span>
                    </div>
                </div>
            </div>

            {/* Formulaire */}
            <div className="bg-white rounded-xl shadow-md p-6 border border-gray-100">
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <div className="flex items-center justify-between mb-4">
                            <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest px-1">
                                New JSON Configuration
                            </label>
                            <button
                                type="button"
                                onClick={handleFormatJson}
                                className="px-3 py-1.5 text-xs font-bold bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-colors flex items-center gap-1.5"
                            >
                                <FiCode className="w-3.5 h-3.5" />
                                Format
                            </button>
                        </div>

                        <textarea
                            value={jsonConfig}
                            onChange={(e) => handleJsonChange(e.target.value)}
                            className={`w-full h-96 p-4 font-mono text-sm border-2 rounded-lg focus:outline-none focus:ring-2 transition-all ${!isValid
                                ? 'border-red-300 focus:border-red-500 focus:ring-red-200 bg-red-50'
                                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-200'
                                }`}
                            placeholder='{\n  "serviceCode": "MY_SERVICE",\n  "serviceName": "My Service",\n  ...\n}'
                        />

                        <div className="flex justify-end mt-3">
                            {!isValid && jsonConfig.trim() && (
                                <span className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-100 text-red-600 text-[10px] font-bold uppercase tracking-wider animate-bounce">
                                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                    </svg>
                                    Invalid JSON
                                </span>
                            )}

                            {isValid && jsonConfig.trim() && (
                                <span className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-100 text-emerald-600 text-[10px] font-bold uppercase tracking-wider">
                                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                    </svg>
                                    Valid JSON
                                </span>
                            )}
                        </div>
                    </div>

                    <div className="flex gap-4">
                        <button
                            type="submit"
                            disabled={isSaving || !isValid || !jsonConfig.trim()}
                            className="btn-primary w-full flex items-center justify-center gap-3 py-4 shadow-xl shadow-primary/20"
                        >
                            <FiSave className="w-5 h-5" />
                            <span className="text-lg">
                                {isSaving ? 'Saving...' : 'Save Changes'}
                            </span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
