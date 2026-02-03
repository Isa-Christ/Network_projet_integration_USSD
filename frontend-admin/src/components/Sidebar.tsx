'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { FiHome, FiPlusCircle, FiList, FiLogOut, FiCpu } from 'react-icons/fi';
import { useAuth } from '@/context/AuthContext';

const menuItems = [
    {
        name: 'Dashboard',
        href: '/dashboard',
        icon: FiHome,
    },
    {
        name: 'Ajouter un service',
        href: '/dashboard/services/add',
        icon: FiPlusCircle,
    },
    {
        name: 'Liste des services',
        href: '/dashboard/services/list',
        icon: FiList,
    },
    {
        name: 'Générateur IA',
        href: '/dashboard/ai-generator',
        icon: FiCpu,
    },
];

export default function Sidebar() {
    const pathname = usePathname();
    const { logout } = useAuth();

    const isActive = (href: string) => {
        if (href === '/dashboard') {
            return pathname === href;
        }
        return pathname?.startsWith(href);
    };

    return (
        <div className="w-64 min-h-screen bg-primary-dark text-white flex flex-col shadow-xl z-20">
            {/* Header / Logo */}
            <div className="p-8 border-b border-white/5">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-white/10 rounded-xl flex items-center justify-center p-2">
                        <svg className="w-full h-full text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                    </div>
                    <div>
                        <h1 className="text-xl font-bold tracking-tight">USSD Admin</h1>
                        <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-0.5">Plateforme de gestion</p>
                    </div>
                </div>
            </div>

            {/* Menu de navigation */}
            <nav className="flex-1 px-4 py-8 space-y-2">
                {menuItems.map((item) => {
                    const Icon = item.icon;
                    const active = isActive(item.href);

                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            className={`
                                flex items-center gap-3 px-4 py-3.5 rounded-xl transition-all duration-300 group
                                ${active
                                    ? 'bg-primary text-white shadow-lg shadow-primary/20 scale-[1.02]'
                                    : 'text-slate-400 hover:text-white hover:bg-white/5'
                                }
                            `}
                        >
                            <Icon className={`w-5 h-5 transition-transform group-hover:scale-110 ${active ? 'text-white' : 'text-slate-500 group-hover:text-white'}`} />
                            <span className="font-semibold text-sm">{item.name}</span>
                        </Link>
                    );
                })}
            </nav>

            {/* Footer / Logout */}
            <div className="p-6 mt-auto">
                <button
                    onClick={logout}
                    className="w-full flex items-center justify-center gap-3 px-4 py-3.5 bg-white/5 hover:bg-red-500/10 text-slate-400 hover:text-red-500 rounded-xl transition-all duration-300 font-bold text-sm group"
                >
                    <FiLogOut className="w-5 h-5 transition-transform group-hover:-translate-x-1" />
                    <span>Déconnexion</span>
                </button>
            </div>
        </div>
    );
}
