'use client';

import { useState, useEffect } from 'react';
import { getServices } from '@/lib/api';
import { ServiceInfoResponse } from '@/types';
import toast from 'react-hot-toast';
import { useAuth } from '@/context/AuthContext';
import { FiServer, FiPlusCircle, FiCheckCircle, FiXCircle, FiTrendingUp } from 'react-icons/fi';
import Link from 'next/link';

export default function DashboardPage() {
    const { user } = useAuth();
    const [services, setServices] = useState<ServiceInfoResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        loadServices();
    }, []);

    const loadServices = async () => {
        try {
            const data = await getServices();
            setServices(data || []);
        } catch (error) {
            toast.error('Error loading services');
        } finally {
            setIsLoading(false);
        }
    };

    const stats = {
        total: services.length,
        active: services.filter((s) => s.isActive).length,
        inactive: services.filter((s) => !s.isActive).length,
        thisMonth: services.filter((s) => {
            const created = new Date(s.createdAt);
            const now = new Date();
            return created.getMonth() === now.getMonth() && created.getFullYear() === now.getFullYear();
        }).length,
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="text-center font-medium">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
                    <p className="text-slate-600">Loading data...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="animate-fadeIn space-y-8">
            {/* Welcome Banner */}
            <div className="bg-primary-dark rounded-2xl p-8 text-white shadow-lg overflow-hidden relative">
                <div className="relative z-10">
                    <h1 className="text-3xl font-bold mb-2 uppercase tracking-tight">Hello, {user?.name || 'Administrator'}</h1>
                    <p className="text-slate-300 max-w-2xl text-lg">
                        Manage your USSD platforms efficiently and supervise all activities from this centralized dashboard.
                    </p>
                </div>
                {/* Decorative element */}
                <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -mr-32 -mt-32 blur-3xl"></div>
            </div>

            {/* Statistics Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {/* Total Services */}
                <div className="card-shadow-hover p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div className="p-3 bg-blue-50 rounded-xl">
                            <FiServer className="w-6 h-6 text-blue-600" />
                        </div>
                        <span className="text-3xl font-bold text-slate-800">{stats.total}</span>
                    </div>
                    <h3 className="text-slate-600 font-bold text-sm uppercase tracking-wider">Total Services</h3>
                    <p className="text-xs text-slate-400 mt-2 font-medium">Services registered on the platform</p>
                </div>

                {/* Services Actifs */}
                <div className="card-shadow-hover p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div className="p-3 bg-emerald-50 rounded-xl">
                            <FiCheckCircle className="w-6 h-6 text-emerald-600" />
                        </div>
                        <span className="text-3xl font-bold text-slate-800">{stats.active}</span>
                    </div>
                    <h3 className="text-slate-600 font-bold text-sm uppercase tracking-wider">Active Services</h3>
                    <p className="text-xs text-slate-400 mt-2 font-medium">Currently running</p>
                </div>

                {/* Services Inactifs */}
                <div className="card-shadow-hover p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div className="p-3 bg-red-50 rounded-xl">
                            <FiXCircle className="w-6 h-6 text-red-600" />
                        </div>
                        <span className="text-3xl font-bold text-slate-800">{stats.inactive}</span>
                    </div>
                    <h3 className="text-slate-600 font-bold text-sm uppercase tracking-wider">Inactive Services</h3>
                    <p className="text-xs text-slate-400 mt-2 font-medium">Currently disabled</p>
                </div>

                {/* Ajoutés ce mois */}
                <div className="card-shadow-hover p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div className="p-3 bg-indigo-50 rounded-xl">
                            <FiTrendingUp className="w-6 h-6 text-indigo-600" />
                        </div>
                        <span className="text-3xl font-bold text-slate-800">{stats.thisMonth}</span>
                    </div>
                    <h3 className="text-slate-600 font-bold text-sm uppercase tracking-wider">Added This Month</h3>
                    <p className="text-xs text-slate-400 mt-2 font-medium">Services created during this month</p>
                </div>
            </div>

            {/* Recent services */}
            <div className="card-shadow overflow-hidden">
                <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                    <h2 className="text-lg font-bold text-slate-800">Recent Services</h2>
                    <Link href="/dashboard/services/list" className="text-sm font-bold text-primary hover:text-primary-light transition-colors">
                        View All
                    </Link>
                </div>

                {services.length === 0 ? (
                    <div className="text-center py-20 bg-white">
                        <div className="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
                            <FiServer className="w-10 h-10 text-slate-300" />
                        </div>
                        <p className="text-slate-500 font-semibold mb-2">No services found</p>
                        <p className="text-sm text-slate-400 mb-6">Start by adding your first USSD service</p>
                        <Link href="/dashboard/services/add" className="btn-primary flex items-center gap-2 mx-auto w-fit">
                            <FiPlusCircle className="w-5 h-5" />
                            Add Service
                        </Link>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                                <tr className="bg-slate-50 border-b border-slate-200">
                                    <th className="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-widest leading-none">Service Name</th>
                                    <th className="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-widest leading-none">Code</th>
                                    <th className="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-widest leading-none">Short Code</th>
                                    <th className="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-widest leading-none">Status</th>
                                    <th className="text-left py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-widest leading-none">Creation Date</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {services.slice(0, 5).map((service) => (
                                    <tr key={service.id} className="hover:bg-slate-50 transition-colors group">
                                        <td className="py-4 px-6">
                                            <div className="font-bold text-slate-800">{service.name}</div>
                                        </td>
                                        <td className="py-4 px-6">
                                            <code className="px-2 py-1 bg-slate-100 rounded text-xs font-mono text-slate-600">{service.code}</code>
                                        </td>
                                        <td className="py-4 px-6">
                                            <span className="px-3 py-1 bg-primary/10 text-primary rounded-full text-xs font-bold">
                                                {service.shortCode}
                                            </span>
                                        </td>
                                        <td className="py-4 px-6">
                                            {service.isActive ? (
                                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-600 border border-emerald-100">
                                                    <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse"></span>
                                                    Active
                                                </span>
                                            ) : (
                                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-500 border border-slate-200">
                                                    <span className="w-1.5 h-1.5 bg-slate-400 rounded-full"></span>
                                                    Inactive
                                                </span>
                                            )}
                                        </td>
                                        <td className="py-4 px-6 text-slate-500 text-sm font-medium">
                                            {new Date(service.createdAt).toLocaleDateString('en-US', {
                                                day: 'numeric',
                                                month: 'long',
                                                year: 'numeric'
                                            })}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}
