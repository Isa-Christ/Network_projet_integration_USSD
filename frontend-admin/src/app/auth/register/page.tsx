'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import toast from 'react-hot-toast';
import { FiUser, FiMail, FiLock, FiEye, FiEyeOff, FiArrowRight } from 'react-icons/fi';

export default function RegisterPage() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const { register } = useAuth();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!name || !email || !password || !confirmPassword) {
            toast.error('Please fill in all fields');
            return;
        }

        if (password !== confirmPassword) {
            toast.error('Passwords do not match');
            return;
        }

        if (password.length < 6) {
            toast.error('Password must be at least 6 characters long');
            return;
        }

        setIsLoading(true);

        try {
            await register({ name, email, password, confirmPassword });
            toast.success('Account created successfully!');
        } catch (error: any) {
            toast.error(error.message || 'An error occurred during registration');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-primary-dark p-4">
            {/* Auth Card - Centered Square Card */}
            <div className="auth-card animate-fadeIn max-w-lg">
                <div className="mb-8 text-center border-b border-slate-50 pb-6">
                    <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
                        <svg className="w-10 h-10 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                        </svg>
                    </div>
                    <h1 className="text-2xl font-bold text-slate-800">Create an account</h1>
                    <p className="text-slate-500 mt-2 text-sm">Join USSD Administration</p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    {/* Name */}
                    <div>
                        <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wide px-1">
                            Full Name
                        </label>
                        <div className="relative">
                            <FiUser className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full pl-12 pr-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all text-slate-800 bg-slate-50/50 font-medium"
                                placeholder="John Doe"
                                required
                            />
                        </div>
                    </div>

                    {/* Email */}
                    <div>
                        <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wide px-1">
                            Email Address
                        </label>
                        <div className="relative">
                            <FiMail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" />
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="w-full pl-12 pr-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all text-slate-800 bg-slate-50/50 font-medium"
                                placeholder="nom@exemple.com"
                                required
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {/* Password */}
                        <div>
                            <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wide px-1">
                                Password
                            </label>
                            <div className="relative">
                                <FiLock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
                                <input
                                    type={showPassword ? 'text' : 'password'}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    className="w-full pl-11 pr-10 py-3 border border-slate-200 rounded-xl focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all text-slate-800 bg-slate-50/50 text-sm font-medium"
                                    placeholder="••••••••"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-primary transition-colors"
                                >
                                    {showPassword ? <FiEyeOff className="w-4 h-4" /> : <FiEye className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        {/* Confirmation */}
                        <div>
                            <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wide px-1">
                                Confirm Password
                            </label>
                            <div className="relative">
                                <FiLock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
                                <input
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    className="w-full pl-11 pr-10 py-3 border border-slate-200 rounded-xl focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all text-slate-800 bg-slate-50/50 text-sm font-medium"
                                    placeholder="••••••••"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-primary transition-colors"
                                >
                                    {showConfirmPassword ? <FiEyeOff className="w-4 h-4" /> : <FiEye className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* register button */}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="btn-primary w-full flex items-center justify-center gap-2 mt-4 py-4 shadow-xl shadow-primary/20"
                    >
                        {isLoading ? 'Creating account...' : 'Create Account'}
                        {!isLoading && <FiArrowRight className="w-5 h-5" />}
                    </button>
                </form>

                {/* Login link */}
                <div className="mt-8 pt-6 border-t border-slate-100 text-center">
                    <p className="text-slate-600 text-sm">
                        Already have an account?{' '}
                        <Link href="/auth/login" className="text-primary hover:underline font-bold transition-all">
                            Sign In
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
