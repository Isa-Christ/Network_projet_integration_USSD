'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function Home() {
  const router = useRouter();

  useEffect(() => {
    // Rediriger automatiquement vers la page de connexion
    router.push('/auth/login');
  }, [router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-900 via-blue-700 to-cyan-600">
      <div className="text-center">
        <div className="animate-pulse">
          <h1 className="text-4xl font-bold text-white mb-4">USSD Administration</h1>
          <p className="text-blue-100">Redirecting...</p>
        </div>
      </div>
    </div>
  );
}
