'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { createService } from '@/lib/api';
import toast from 'react-hot-toast';
import { FiSave, FiArrowLeft, FiCode, FiCheck, FiX } from 'react-icons/fi';
import Link from 'next/link';

export default function AddServicePage() {
    const [serviceName, setServiceName] = useState('');
    const [serviceCode, setServiceCode] = useState('');
    const [jsonConfig, setJsonConfig] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [isValid, setIsValid] = useState(true);
    const router = useRouter();
    // Use window.location because useSearchParams might cause static build issues if not wrapped in Suspense
    // But for client component it's usually fine. Let's stick to standard useEffect with window.

    // Check for generated config on mount
    const [isInitialized, setIsInitialized] = useState(false);

    useEffect(() => {
        if (typeof window !== 'undefined' && !isInitialized) {
            const params = new URLSearchParams(window.location.search);
            if (params.get('mode') === 'generated') {
                const storedConfig = localStorage.getItem('generated_ussd_config');
                if (storedConfig) {
                    try {
                        const parsed = JSON.parse(storedConfig);
                        setJsonConfig(JSON.stringify(parsed, null, 2));
                        if (parsed.serviceName) setServiceName(parsed.serviceName);
                        if (parsed.serviceCode) setServiceCode(parsed.serviceCode);

                        toast.success('Configuration générée par IA chargée !', {
                            icon: '🤖',
                            duration: 4000
                        });

                        // Optional: Clear it so it doesn't reload if user refreshes without the param? 
                        // Or keep it for safety. Let's keep it but maybe we don't need to clear it immedaitely.
                        // localStorage.removeItem('generated_ussd_config'); 
                    } catch (e) {
                        console.error("Failed to parse generated config", e);
                        toast.error("Erreur lors du chargement de la config générée");
                    }
                }
            }
            setIsInitialized(true);
        }
    }, [isInitialized]);

    const handleJsonChange = (value: string) => {
        setJsonConfig(value);

        if (!value.trim()) {
            setIsValid(true);
            return;
        }

        try {
            JSON.parse(value);
            setIsValid(true);
        } catch (error) {
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
        } catch (error) {
            toast.error('JSON invalide, impossible de formater');
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!jsonConfig.trim()) {
            toast.error('Veuillez fournir la configuration JSON');
            return;
        }

        if (!isValid) {
            toast.error('La configuration JSON est invalide');
            return;
        }

        setIsLoading(true);

        try {
            await createService({ jsonConfig });
            toast.success('Service ajouté avec succès !', { id: 'add-service-success' });
            router.push('/dashboard/services/list');
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || error.message || 'Erreur lors de l\'ajout du service';
            toast.error(errorMessage, { id: 'add-service-error' });
        } finally {
            setIsLoading(false);
        }
    };

    const exampleJson = {
        serviceCode: "BANK_SERVICE",
        serviceName: "Service Bancaire",
        apiConfig: {
            baseUrl: "https://api.banque.com",
            timeout: 5000
        },
        states: [
            {
                id: "welcome",
                message: "Bienvenue au service bancaire",
                transitions: []
            }
        ]
    };

    const loadExample = () => {
        const formatted = JSON.stringify(exampleJson, null, 2);
        setJsonConfig(formatted);
        setServiceName("Service Bancaire");
        setServiceCode("BANK_SERVICE");
        setIsValid(true);
        toast.success('Exemple chargé');
    };

    return (
        <div className="animate-fadeIn space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Ajouter un Nouveau Service</h1>
                    <p className="text-slate-500 text-sm">Créez un nouveau service USSD en fournissant sa configuration</p>
                </div>
                <Link
                    href="/dashboard/services/list"
                    className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-xl hover:bg-slate-50 transition-colors font-bold text-sm shadow-sm"
                >
                    <FiArrowLeft className="w-4 h-4" />
                    <span>Retour à la liste</span>
                </Link>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Section gauche - Configuration JSON */}
                    <div className="lg:col-span-2">
                        <div className="card-shadow p-6">
                            <div className="flex items-center justify-between mb-6">
                                <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                    <span className="w-8 h-8 bg-amber-50 rounded-lg flex items-center justify-center">
                                        <FiCode className="w-4 h-4 text-amber-600" />
                                    </span>
                                    Configuration JSON
                                </h2>
                                <div className="flex gap-2">
                                    <button
                                        type="button"
                                        onClick={loadExample}
                                        className="px-3 py-1.5 text-xs font-bold bg-slate-50 text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-100 transition-colors"
                                    >
                                        Charger exemple
                                    </button>
                                    <button
                                        type="button"
                                        onClick={handleFormatJson}
                                        className="px-3 py-1.5 text-xs font-bold bg-primary/10 text-primary rounded-lg hover:bg-primary/20 transition-colors flex items-center gap-1.5"
                                    >
                                        <FiCode className="w-3.5 h-3.5" />
                                        Formater
                                    </button>
                                </div>
                            </div>

                            <div className="relative">
                                <textarea
                                    value={jsonConfig}
                                    onChange={(e) => handleJsonChange(e.target.value)}
                                    className={`w-full h-[500px] p-4 font-mono text-sm border-2 rounded-xl transition-all resize-none focus:outline-none ${!isValid
                                        ? 'border-red-200 bg-red-50/30 text-red-900 focus:border-red-400'
                                        : 'border-slate-100 bg-slate-50/50 text-slate-700 focus:border-primary focus:bg-white'
                                        }`}
                                    placeholder='{ "serviceCode": "MY_SERVICE", ... }'
                                    spellCheck={false}
                                />

                                <div className="absolute bottom-4 right-4 flex items-center gap-2">
                                    {!isValid && jsonConfig.trim() && (
                                        <span className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-100 text-red-600 text-[10px] font-bold uppercase tracking-wider animate-bounce">
                                            <FiX className="w-3.5 h-3.5" />
                                            JSON Invalide
                                        </span>
                                    )}
                                    {isValid && jsonConfig.trim() && (
                                        <span className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-100 text-emerald-600 text-[10px] font-bold uppercase tracking-wider">
                                            <FiCheck className="w-3.5 h-3.5" />
                                            JSON Valide
                                        </span>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Section droite - Informations du service */}
                    <div className="lg:col-span-1 space-y-6">
                        <div className="card-shadow p-6">
                            <h2 className="text-lg font-bold text-slate-800 mb-6">Paramètres</h2>

                            <div className="space-y-5">
                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-2 px-1">
                                        Nom du Service
                                    </label>
                                    <input
                                        type="text"
                                        value={serviceName}
                                        onChange={(e) => setServiceName(e.target.value)}
                                        className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all text-slate-800 font-semibold"
                                        placeholder="Ex: Mon Super Service"
                                    />
                                </div>

                                <div>
                                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-2 px-1">
                                        Code du Service
                                    </label>
                                    <input
                                        type="text"
                                        value={serviceCode}
                                        onChange={(e) => setServiceCode(e.target.value)}
                                        className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all font-mono text-slate-700 font-bold"
                                        placeholder="EX_SERVICE_01"
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Info Pad */}
                        <div className="bg-slate-800 text-slate-300 rounded-2xl p-6 shadow-xl relative overflow-hidden group">
                            <div className="relative z-10">
                                <h3 className="font-bold text-white mb-4 flex items-center gap-2">
                                    <span className="w-6 h-6 bg-white/10 rounded-lg flex items-center justify-center">
                                        <FiCode className="w-3 h-3" />
                                    </span>
                                    Aide au formatage
                                </h3>
                                <div className="space-y-3 text-xs leading-relaxed">
                                    <p><strong className="text-white">serviceCode</strong> : Identifiant interne unique.</p>
                                    <p><strong className="text-white">serviceName</strong> : Nom public du service.</p>
                                    <p><strong className="text-white">apiConfig</strong> : URL et timeout du backend.</p>
                                    <p><strong className="text-white">states</strong> : Diagramme d&apos;états de l&apos;automate.</p>
                                </div>
                            </div>
                            <FiCode className="absolute -bottom-8 -right-8 w-32 h-32 text-white/5 transition-transform group-hover:scale-110 group-hover:rotate-12" />
                        </div>

                        {/* Bouton d'enregistrement */}
                        <button
                            type="submit"
                            disabled={isLoading || !isValid || !jsonConfig.trim()}
                            className="btn-primary w-full flex items-center justify-center gap-3 py-4 shadow-xl shadow-primary/20"
                        >
                            <FiSave className="w-5 h-5" />
                            <span className="text-lg">
                                {isLoading ? 'Enregistrement...' : 'Enregistrer'}
                            </span>
                        </button>
                    </div>
                </div>
            </form>
        </div>
    );
}
