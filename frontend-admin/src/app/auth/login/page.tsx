'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import toast from 'react-hot-toast';
import { FiMail, FiLock, FiEye, FiEyeOff, FiArrowRight } from 'react-icons/fi';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const { login } = useAuth();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!email || !password) {
            toast.error('Please fill in all fields');
            return;
        }

        setIsLoading(true);

        try {
            await login({ email, password });
            toast.success('Login successful!');
        } catch (error: any) {
            toast.error(error.message || 'Incorrect email or password');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-primary-dark p-4">
            {/* Auth Card Container - Centered Square Card */}
            <div className="auth-card animate-fadeIn">
                <div className="mb-8 text-center border-b border-slate-50 pb-6">
                    <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
                        <svg className="w-10 h-10 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                    </div>
                    <h1 className="text-2xl font-bold text-slate-800">Login</h1>
                    <p className="text-slate-500 mt-2 text-sm">Access your administration space</p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Email */}
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2 uppercase tracking-wide px-1">
                            Email Address
                        </label>
                        <div className="relative">
                            <FiMail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="w-full pl-12 pr-4 py-3.5 border border-slate-200 rounded-xl focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all text-slate-800 bg-slate-50/50 font-medium"
                                placeholder="your-email@example.com"
                                required
                            />
                        </div>
                    </div>

                    {/* Password */}
                    <div>
                        <label className="block text-sm font-bold text-slate-700 mb-2 uppercase tracking-wide px-1">
                            Password
                        </label>
                        <div className="relative">
                            <FiLock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                            <input
                                type={showPassword ? 'text' : 'password'}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full pl-12 pr-12 py-3.5 border border-slate-200 rounded-xl focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all text-slate-800 bg-slate-50/50 font-medium"
                                placeholder="••••••••"
                                required
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-primary transition-colors"
                            >
                                {showPassword ? <FiEyeOff className="w-5 h-5" /> : <FiEye className="w-5 h-5" />}
                            </button>
                        </div>
                    </div>

                    {/* Login Button */}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="btn-primary w-full flex items-center justify-center gap-2 py-4 shadow-xl shadow-primary/20"
                    >
                        {isLoading ? 'Logging in...' : 'Sign In'}
                        {!isLoading && <FiArrowRight className="w-5 h-5" />}
                    </button>
                </form>

                {/* register link */}
                <div className="mt-10 pt-6 border-t border-slate-100 text-center">
                    <p className="text-slate-600 text-sm">
                        Don&apos;t have an account yet?{' '}
                        <Link href="/auth/register" className="text-primary hover:underline font-bold transition-all">
                            Create an account
                        </Link>
                    </p>
                </div>
            </div>

            {/* Copyright */}
            <p className="absolute bottom-8 text-slate-400 text-[10px] font-bold uppercase tracking-widest opacity-50">
                © 2026 USSD Admin • Management System
            </p>
        </div>
    );
}
