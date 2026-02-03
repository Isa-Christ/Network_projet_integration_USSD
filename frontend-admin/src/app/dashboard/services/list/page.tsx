'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getServices, deleteService, toggleServiceStatus } from '@/lib/api';
import { ServiceInfoResponse } from '@/types';
import toast from 'react-hot-toast';
import { FiEdit2, FiTrash2, FiSearch, FiPlusCircle, FiCheckCircle, FiXCircle, FiFilter, FiMoreHorizontal } from 'react-icons/fi';
import Link from 'next/link';

export default function ServiceListPage() {
    const [services, setServices] = useState<ServiceInfoResponse[]>([]);
    const [filteredServices, setFilteredServices] = useState<ServiceInfoResponse[]>([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'BLOCKED'>('ALL');
    const [isLoading, setIsLoading] = useState(true);
    const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
    const router = useRouter();

    useEffect(() => {
        loadServices();
    }, []);

    useEffect(() => {
        let filtered = services;

        // Filtre par recherche
        if (searchTerm.trim()) {
            filtered = filtered.filter((service) =>
                service.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                service.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
                service.shortCode.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        // Filtre par statut
        if (statusFilter !== 'ALL') {
            const wantActive = statusFilter === 'ACTIVE';
            filtered = filtered.filter((service) => service.isActive === wantActive);
        }

        setFilteredServices(filtered);
    }, [searchTerm, statusFilter, services]);

    const loadServices = async () => {
        setIsLoading(true);
        try {
            const data = await getServices();
            setServices(data);
            setFilteredServices(data);
        } catch (error) {
            toast.error('Erreur lors du chargement des services', { id: 'load-services-error' });
        } finally {
            setIsLoading(false);
        }
    };

    const handleDelete = async (code: string) => {
        try {
            await deleteService(code);
            toast.success('Service supprimé avec succès');
            setDeleteConfirm(null);
            loadServices();
        } catch (error) {
            toast.error('Erreur lors de la suppression', { id: 'delete-error' });
        }
    };

    const handleToggleStatus = async (code: string, currentStatus: boolean) => {
        const action = currentStatus ? 'bloqué' : 'activé';
        try {
            await toggleServiceStatus(code);
            toast.success(`Le service a été ${action} avec succès`);
            loadServices();
        } catch (error) {
            toast.error(`Erreur lors de la modification du statut`, { id: 'toggle-error' });
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
                    <p className="text-slate-600 font-medium">Chargement des services...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="animate-fadeIn space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Liste des Services</h1>
                    <p className="text-slate-500 text-sm">Consultez et gérez l&apos;ensemble de vos plateformes USSD</p>
                </div>
                <Link
                    href="/dashboard/services/add"
                    className="btn-primary flex items-center justify-center gap-2 shadow-xl shadow-primary/10"
                >
                    <FiPlusCircle className="w-5 h-5" />
                    <span>Nouveau Service</span>
                </Link>
            </div>

            {/* Barre de recherche et filtres */}
            <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                    {/* Champ de recherche */}
                    <div className="flex-1 relative group">
                        <FiSearch className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5 transition-colors group-focus-within:text-primary" />
                        <input
                            type="text"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-12 pr-4 py-3 bg-slate-50/50 border border-slate-200 rounded-xl focus:bg-white focus:border-primary focus:ring-4 focus:ring-primary/5 outline-none transition-all font-medium text-slate-800"
                            placeholder="Rechercher par nom, code ou short code..."
                        />
                        {searchTerm && (
                            <button
                                onClick={() => setSearchTerm('')}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        )}
                    </div>

                    {/* Filtre de statut avec badges */}
                    <div className="flex items-center gap-2">
                        <button
                            onClick={() => setStatusFilter('ALL')}
                            className={`px-4 py-2.5 rounded-xl text-sm font-bold transition-all ${statusFilter === 'ALL'
                                    ? 'bg-primary text-white shadow-lg shadow-primary/25'
                                    : 'bg-slate-50 text-slate-600 hover:bg-slate-100'
                                }`}
                        >
                            Tous ({services.length})
                        </button>
                        <button
                            onClick={() => setStatusFilter('ACTIVE')}
                            className={`px-4 py-2.5 rounded-xl text-sm font-bold transition-all flex items-center gap-2 ${statusFilter === 'ACTIVE'
                                    ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/25'
                                    : 'bg-slate-50 text-slate-600 hover:bg-emerald-50 hover:text-emerald-600'
                                }`}
                        >
                            <span className={`w-2 h-2 rounded-full ${statusFilter === 'ACTIVE' ? 'bg-white' : 'bg-emerald-500'}`}></span>
                            Actifs ({services.filter(s => s.isActive).length})
                        </button>
                        <button
                            onClick={() => setStatusFilter('BLOCKED')}
                            className={`px-4 py-2.5 rounded-xl text-sm font-bold transition-all flex items-center gap-2 ${statusFilter === 'BLOCKED'
                                    ? 'bg-rose-500 text-white shadow-lg shadow-rose-500/25'
                                    : 'bg-slate-50 text-slate-600 hover:bg-rose-50 hover:text-rose-600'
                                }`}
                        >
                            <span className={`w-2 h-2 rounded-full ${statusFilter === 'BLOCKED' ? 'bg-white' : 'bg-rose-500'}`}></span>
                            Bloqués ({services.filter(s => !s.isActive).length})
                        </button>
                    </div>
                </div>

                {/* Résumé des filtres actifs */}
                {(searchTerm || statusFilter !== 'ALL') && (
                    <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between">
                        <p className="text-xs text-slate-500 font-medium">
                            <span className="font-bold text-primary">{filteredServices.length}</span> résultat{filteredServices.length > 1 ? 's' : ''} trouvé{filteredServices.length > 1 ? 's' : ''}
                        </p>
                        <button
                            onClick={() => {
                                setSearchTerm('');
                                setStatusFilter('ALL');
                            }}
                            className="text-xs font-bold text-slate-500 hover:text-primary transition-colors flex items-center gap-1"
                        >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                            Réinitialiser
                        </button>
                    </div>
                )}
            </div>

            {/* Tableau des services */}
            <div className="card-shadow overflow-hidden bg-white">
                {filteredServices.length === 0 ? (
                    <div className="text-center py-20 px-4">
                        <div className="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
                            <FiSearch className="w-10 h-10 text-slate-300" />
                        </div>
                        <p className="text-slate-600 font-bold mb-1">Aucun service trouvé</p>
                        <p className="text-slate-400 text-sm mb-6">Essayez d&apos;ajuster vos critères de recherche</p>
                        <button onClick={() => setSearchTerm('')} className="text-primary font-bold hover:underline">
                            Réinitialiser la recherche
                        </button>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full border-collapse">
                            <thead>
                                <tr className="bg-slate-50 border-b border-slate-200">
                                    <th className="text-left py-4 px-6 text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] leading-none">Informations</th>
                                    <th className="text-left py-4 px-6 text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] leading-none">Code & API</th>
                                    <th className="text-left py-4 px-6 text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] leading-none">Short Code</th>
                                    <th className="text-left py-4 px-6 text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] leading-none">Statut</th>
                                    <th className="text-right py-4 px-6 text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] leading-none">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100 font-medium">
                                {filteredServices.map((service) => (
                                    <tr key={service.id} className="hover:bg-slate-50/50 transition-colors group">
                                        <td className="py-5 px-6">
                                            <div className="font-bold text-slate-800 text-base">{service.name}</div>
                                            <div className="text-slate-400 text-xs mt-0.5">Créé le {new Date(service.createdAt).toLocaleDateString()}</div>
                                        </td>
                                        <td className="py-5 px-6">
                                            <code className="px-2 py-1 bg-slate-100 rounded text-[10px] font-mono text-slate-600 font-bold uppercase tracking-wider">{service.code}</code>
                                            <div className="text-slate-400 text-[10px] mt-1.5 font-bold truncate max-w-[180px]">{service.apiBaseUrl}</div>
                                        </td>
                                        <td className="py-5 px-6">
                                            <div className="inline-flex items-center px-3 py-1 bg-primary/5 text-primary rounded-lg text-xs font-bold border border-primary/10">
                                                {service.shortCode}
                                            </div>
                                        </td>
                                        <td className="py-5 px-6">
                                            <button
                                                onClick={() => handleToggleStatus(service.code, service.isActive)}
                                                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[10px] font-bold tracking-wider uppercase border transition-all ${service.isActive
                                                    ? 'bg-emerald-50 text-emerald-600 border-emerald-100 hover:bg-emerald-100'
                                                    : 'bg-rose-50 text-rose-600 border-rose-100 hover:bg-rose-100'
                                                    }`}
                                                title={service.isActive ? "Cliquer pour bloquer" : "Cliquer pour activer"}
                                            >
                                                <span className={`w-1.5 h-1.5 rounded-full ${service.isActive ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`}></span>
                                                {service.isActive ? 'Actif' : 'Bloqué'}
                                            </button>
                                        </td>
                                        <td className="py-5 px-6">
                                            <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                                <button
                                                    onClick={() => router.push(`/dashboard/services/edit/${service.code}`)}
                                                    className="w-9 h-9 flex items-center justify-center text-slate-400 hover:text-primary hover:bg-primary/10 rounded-xl transition-all"
                                                    title="Modifier"
                                                >
                                                    <FiEdit2 className="w-4 h-4" />
                                                </button>
                                                <button
                                                    onClick={() => setDeleteConfirm(service.code)}
                                                    className="w-9 h-9 flex items-center justify-center text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-all"
                                                    title="Supprimer"
                                                >
                                                    <FiTrash2 className="w-4 h-4" />
                                                </button>
                                            </div>
                                            <div className="group-hover:hidden flex justify-end">
                                                <FiMoreHorizontal className="text-slate-300 w-5 h-5" />
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Modal de confirmation de suppression */}
            {deleteConfirm && (
                <div className="fixed inset-0 bg-primary-dark/40 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-fadeIn">
                    <div className="bg-white rounded-3xl shadow-2xl p-8 max-w-sm w-full border border-slate-100">
                        <div className="text-center mb-8">
                            <div className="w-20 h-20 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-6">
                                <FiTrash2 className="w-10 h-10 text-red-500" />
                            </div>
                            <h3 className="text-xl font-bold text-slate-900 mb-3">Confirmation</h3>
                            <p className="text-slate-500 text-sm leading-relaxed">
                                Souhaitez-vous vraiment supprimer ce service ?<br />
                                <span className="font-bold text-red-500 uppercase tracking-widest text-[10px]">Cette action est irréversible</span>
                            </p>
                        </div>
                        <div className="flex gap-3">
                            <button
                                onClick={() => setDeleteConfirm(null)}
                                className="flex-1 px-5 py-3.5 bg-slate-50 hover:bg-slate-100 text-slate-600 rounded-2xl font-bold text-sm transition-all"
                            >
                                Annuler
                            </button>
                            <button
                                onClick={() => handleDelete(deleteConfirm)}
                                className="flex-1 px-5 py-3.5 bg-red-500 hover:bg-red-600 text-white rounded-2xl font-bold text-sm shadow-xl shadow-red-500/20 transition-all"
                            >
                                Supprimer
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
