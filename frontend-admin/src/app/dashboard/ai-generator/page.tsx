import { ExternalLink, Bot, CheckCircle, ArrowRight, Loader2, Sparkles, AlertCircle, FileJson } from 'lucide-react';
import Link from 'next/link';

export default function AiGeneratorPage() {
    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
                        AI Generator
                    </h1>
                    <p className="text-slate-500 mt-1">Create USSD services automatically from your APIs</p>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Step 1 */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow group">
                    <div className="h-12 w-12 bg-blue-50 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                        <ExternalLink className="w-6 h-6 text-blue-600" />
                    </div>
                    <h3 className="font-semibold text-lg text-slate-800 mb-2">1. Analysis</h3>
                    <p className="text-slate-500 text-sm mb-4">
                        Scan your API documentation (Swagger/OpenAPI) to understand available endpoints.
                    </p>
                    <div className="flex items-center text-xs font-medium text-slate-400">
                        <span className="bg-slate-100 px-2 py-1 rounded">Input</span>
                        <ArrowRight className="w-3 h-3 mx-1" />
                        <span className="bg-blue-50 text-blue-700 px-2 py-1 rounded">API Structure</span>
                    </div>
                </div>

                {/* Step 2 */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow group">
                    <div className="h-12 w-12 bg-purple-50 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                        <Sparkles className="w-6 h-6 text-purple-600" />
                    </div>
                    <h3 className="font-semibold text-lg text-slate-800 mb-2">2. Design</h3>
                    <p className="text-slate-500 text-sm mb-4">
                        AI proposes multiple USSD menu architectures tailored to your needs.
                    </p>
                    <div className="flex items-center text-xs font-medium text-slate-400">
                        <span className="bg-blue-50 text-blue-700 px-2 py-1 rounded">API Structure</span>
                        <ArrowRight className="w-3 h-3 mx-1" />
                        <span className="bg-purple-50 text-purple-700 px-2 py-1 rounded">Proposals</span>
                    </div>
                </div>

                {/* Step 3 */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 hover:shadow-md transition-shadow group">
                    <div className="h-12 w-12 bg-green-50 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                        <FileJson className="w-6 h-6 text-green-600" />
                    </div>
                    <h3 className="font-semibold text-lg text-slate-800 mb-2">3. Generation</h3>
                    <p className="text-slate-500 text-sm mb-4">
                        Get a complete, validated JSON configuration ready to be deployed.
                    </p>
                    <div className="flex items-center text-xs font-medium text-slate-400">
                        <span className="bg-purple-50 text-purple-700 px-2 py-1 rounded">Choice</span>
                        <ArrowRight className="w-3 h-3 mx-1" />
                        <span className="bg-green-50 text-green-700 px-2 py-1 rounded">USSD Service</span>
                    </div>
                </div>
            </div>

            <div className="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-2xl p-8 text-white relative overflow-hidden">
                <div className="absolute top-0 right-0 p-12 opacity-10">
                    <Bot size={150} />
                </div>
                <div className="relative z-10 max-w-2xl">
                    <h2 className="text-3xl font-bold mb-4">Ready to create your service?</h2>
                    <p className="text-blue-100 mb-8 text-lg">
                        Let artificial intelligence design the optimal architecture for your USSD service in seconds.
                    </p>
                    <Link
                        href="/dashboard/ai-generator/wizard"
                        className="inline-flex items-center px-6 py-3 bg-white text-blue-700 hover:bg-blue-50 font-semibold rounded-lg transition-colors shadow-lg shadow-blue-900/20"
                    >
                        <Sparkles className="w-5 h-5 mr-2" />
                        Start Assistant
                    </Link>
                </div>
            </div>
        </div>
    );
}
