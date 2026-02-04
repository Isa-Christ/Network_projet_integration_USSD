'use client';

import { useAuth } from '@/context/AuthContext';
import { FiBell, FiMail, FiSettings, FiSearch } from 'react-icons/fi';

export default function Header() {
    const { user } = useAuth();

    return (
        <div className="bg-white border-b border-slate-200 px-8 py-5 z-10 sticky top-0">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-8">
                    <div>
                        <h2 className="text-xl font-bold text-slate-800">USSD Administration</h2>
                        <p className="text-slate-400 text-xs font-semibold">Multi-service supervision portal</p>
                    </div>
                </div>

                {/* Actions and profile */}
                <div className="flex items-center gap-6">
                    <div className="flex items-center gap-2 border-r border-slate-200 pr-6 mr-2">
                        <button className="w-10 h-10 flex items-center justify-center hover:bg-slate-50 rounded-full transition-colors relative group">
                            <FiBell className="w-5 h-5 text-slate-500 group-hover:text-primary transition-colors" />
                            <span className="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
                        </button>

                        <button className="w-10 h-10 flex items-center justify-center hover:bg-slate-50 rounded-full transition-colors group">
                            <FiMail className="w-5 h-5 text-slate-500 group-hover:text-primary transition-colors" />
                        </button>

                        <button className="w-10 h-10 flex items-center justify-center hover:bg-slate-50 rounded-full transition-colors group">
                            <FiSettings className="w-5 h-5 text-slate-500 group-hover:text-primary transition-colors" />
                        </button>
                    </div>

                    {/* User profile */}
                    <div className="flex items-center gap-4 group cursor-pointer">
                        <div className="text-right hidden sm:block">
                            <p className="font-bold text-slate-800 text-sm">{user?.name || 'Administrator'}</p>
                            <p className="text-[11px] text-slate-400 font-bold uppercase tracking-wider">{user?.email || 'admin@ussd.com'}</p>
                        </div>
                        <div className="w-11 h-11 rounded-xl bg-primary-dark flex items-center justify-center text-white font-bold shadow-lg shadow-primary/20 transition-transform group-hover:scale-105">
                            {user?.name?.[0]?.toUpperCase() || 'A'}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
