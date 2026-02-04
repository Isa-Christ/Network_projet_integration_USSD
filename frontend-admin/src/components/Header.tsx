'use client';

import { useAuth } from '@/context/AuthContext';
import { useUi } from '@/context/UiContext';
import { FiBell, FiMail, FiSettings, FiMenu } from 'react-icons/fi';

export default function Header() {
    const { user } = useAuth();
    const { toggleSidebar } = useUi();

    return (
        <div className="bg-white border-b border-slate-200 px-4 md:px-8 py-4 md:py-5 z-10 sticky top-0 shadow-sm md:shadow-none">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 md:gap-8">
                    {/* Mobile Menu Button */}
                    <button
                        onClick={toggleSidebar}
                        className="p-2 -ml-2 text-slate-600 hover:bg-slate-50 rounded-lg md:hidden transition-colors"
                        aria-label="Open Menu"
                    >
                        <FiMenu className="w-6 h-6" />
                    </button>

                    <div>
                        <h2 className="text-lg md:text-xl font-bold text-slate-800 truncate max-w-[200px] md:max-w-none">
                            USSD Admin
                        </h2>
                        <p className="text-slate-400 text-[10px] md:text-xs font-semibold hidden md:block">
                            Multi-service supervision portal
                        </p>
                    </div>
                </div>

                {/* Actions and profile */}
                <div className="flex items-center gap-2 md:gap-6">
                    <div className="flex items-center gap-1 md:gap-2 border-r border-slate-200 pr-2 md:pr-6 mr-2">
                        <button className="w-8 h-8 md:w-10 md:h-10 flex items-center justify-center hover:bg-slate-50 rounded-full transition-colors relative group">
                            <FiBell className="w-4 h-4 md:w-5 md:h-5 text-slate-500 group-hover:text-primary transition-colors" />
                            <span className="absolute top-1.5 right-1.5 md:top-2 md:right-2 w-1.5 h-1.5 md:w-2 md:h-2 bg-red-500 rounded-full border-2 border-white"></span>
                        </button>

                        <button className="w-8 h-8 md:w-10 md:h-10 flex items-center justify-center hover:bg-slate-50 rounded-full transition-colors group hidden sm:flex">
                            <FiMail className="w-4 h-4 md:w-5 md:h-5 text-slate-500 group-hover:text-primary transition-colors" />
                        </button>

                        <button className="w-8 h-8 md:w-10 md:h-10 flex items-center justify-center hover:bg-slate-50 rounded-full transition-colors group hidden sm:flex">
                            <FiSettings className="w-4 h-4 md:w-5 md:h-5 text-slate-500 group-hover:text-primary transition-colors" />
                        </button>
                    </div>

                    {/* User profile */}
                    <div className="flex items-center gap-3 md:gap-4 group cursor-pointer">
                        <div className="text-right hidden md:block">
                            <p className="font-bold text-slate-800 text-sm">{user?.name || 'Administrator'}</p>
                            <p className="text-[11px] text-slate-400 font-bold uppercase tracking-wider">{user?.email || 'admin@ussd.com'}</p>
                        </div>
                        <div className="w-8 h-8 md:w-11 md:h-11 rounded-lg md:rounded-xl bg-primary-dark flex items-center justify-center text-white font-bold shadow-lg shadow-primary/20 transition-transform group-hover:scale-105 text-sm md:text-base">
                            {user?.name?.[0]?.toUpperCase() || 'A'}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
