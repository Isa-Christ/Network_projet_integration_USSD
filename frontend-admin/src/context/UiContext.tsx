'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { usePathname } from 'next/navigation';

interface UiContextType {
    sidebarOpen: boolean;
    toggleSidebar: () => void;
    closeSidebar: () => void;
}

const UiContext = createContext<UiContextType | undefined>(undefined);

export function UiProvider({ children }: { children: React.ReactNode }) {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const pathname = usePathname();

    const toggleSidebar = () => setSidebarOpen(prev => !prev);
    const closeSidebar = () => setSidebarOpen(false);

    // Fermer la sidebar automatiquement lors d'un changement de page (surtout sur mobile)
    useEffect(() => {
        closeSidebar();
    }, [pathname]);

    return (
        <UiContext.Provider value={{ sidebarOpen, toggleSidebar, closeSidebar }}>
            {children}
        </UiContext.Provider>
    );
}

export function useUi() {
    const context = useContext(UiContext);
    if (context === undefined) {
        throw new Error('useUi must be used within a UiProvider');
    }
    return context;
}
