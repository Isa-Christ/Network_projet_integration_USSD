import { ExternalLink, Bot, CheckCircle, ArrowRight, Loader2, Sparkles, AlertCircle, FileJson } from 'lucide-react';
import Link from 'next/link';

export default function AiGeneratorPage() {
    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
                        Générateur IA
                    </h1>
                    <p className="text-slate-500 mt-1">Créez des services USSD automatiquement à partir de vos APIs</p>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Step 1 */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow group">
                    <div className="h-12 w-12 bg-blue-50 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                        <ExternalLink className="w-6 h-6 text-blue-600" />
                    </div>
                    <h3 className="font-semibold text-lg text-slate-800 mb-2">1. Analyse</h3>
                    <p className="text-slate-500 text-sm mb-4">
                        Scannez votre documentation API (Swagger/OpenAPI) pour comprendre les endpoints disponibles.
                    </p>
                    <div className="flex items-center text-xs font-medium text-slate-400">
                        <span className="bg-slate-100 px-2 py-1 rounded">Entrée</span>
                        <ArrowRight className="w-3 h-3 mx-1" />
                        <span className="bg-blue-50 text-blue-700 px-2 py-1 rounded">Structure API</span>
                    </div>
                </div>

                {/* Step 2 */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow group">
                    <div className="h-12 w-12 bg-purple-50 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                        <Sparkles className="w-6 h-6 text-purple-600" />
                    </div>
                    <h3 className="font-semibold text-lg text-slate-800 mb-2">2. Conception</h3>
                    <p className="text-slate-500 text-sm mb-4">
                        L'IA propose plusieurs architectures de menus USSD adaptées à vos besoins.
                    </p>
                    <div className="flex items-center text-xs font-medium text-slate-400">
                        <span className="bg-blue-50 text-blue-700 px-2 py-1 rounded">Structure API</span>
                        <ArrowRight className="w-3 h-3 mx-1" />
                        <span className="bg-purple-50 text-purple-700 px-2 py-1 rounded">Propositions</span>
                    </div>
                </div>

                {/* Step 3 */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow group">
                    <div className="h-12 w-12 bg-green-50 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                        <FileJson className="w-6 h-6 text-green-600" />
                    </div>
                    <h3 className="font-semibold text-lg text-slate-800 mb-2">3. Génération</h3>
                    <p className="text-slate-500 text-sm mb-4">
                        Obtenez une configuration JSON complète, validée et prête à être déployée.
                    </p>
                    <div className="flex items-center text-xs font-medium text-slate-400">
                        <span className="bg-purple-50 text-purple-700 px-2 py-1 rounded">Choix</span>
                        <ArrowRight className="w-3 h-3 mx-1" />
                        <span className="bg-green-50 text-green-700 px-2 py-1 rounded">Service USSD</span>
                    </div>
                </div>
            </div>

            <div className="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-2xl p-8 text-white relative overflow-hidden">
                <div className="absolute top-0 right-0 p-12 opacity-10">
                    <Bot size={150} />
                </div>
                <div className="relative z-10 max-w-2xl">
                    <h2 className="text-3xl font-bold mb-4">Prêt à créer votre service ?</h2>
                    <p className="text-blue-100 mb-8 text-lg">
                        Laissez l'intelligence artificielle concevoir l'architecture optimale pour votre service USSD en quelques secondes.
                    </p>
                    <Link
                        href="/dashboard/ai-generator/wizard"
                        className="inline-flex items-center px-6 py-3 bg-white text-blue-700 hover:bg-blue-50 font-semibold rounded-lg transition-colors shadow-lg shadow-blue-900/20"
                    >
                        <Sparkles className="w-5 h-5 mr-2" />
                        Commencer l'assistant
                    </Link>
                </div>
            </div>
        </div>
    );
}
