'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import {
    Search,
    ArrowRight,
    ArrowLeft,
    CheckCircle,
    AlertCircle,
    Loader2,
    Code,
    FileText,
    Sparkles,
    Save
} from 'lucide-react';
import { aiGeneratorService } from '@/services/aiGenerator';
import {
    ApiStructure,
    WorkflowProposals,
    GenerationHints,
    WorkflowProposal
} from '@/types/ai-generator';

// --- Components ---

const StepIndicator = ({ currentStep }: { currentStep: number }) => {
    const steps = [
        { id: 1, label: "Analyze", icon: Search },
        { id: 2, label: "Design", icon: Sparkles },
        { id: 3, label: "Generate", icon: Code },
    ];

    return (
        <div className="flex items-center justify-center mb-8">
            {steps.map((step, index) => (
                <div key={step.id} className="flex items-center">
                    <div className={`
                        flex flex-col items-center z-10 
                        ${currentStep >= step.id ? 'text-blue-600' : 'text-slate-400'}
                    `}>
                        <div className={`
                            w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold border-2 transition-colors
                            ${currentStep >= step.id
                                ? 'bg-blue-600 border-blue-600 text-white'
                                : 'bg-white border-slate-200 text-slate-400'}
                        `}>
                            {currentStep > step.id ? <CheckCircle className="w-6 h-6" /> : <step.icon className="w-5 h-5" />}
                        </div>
                        <span className="text-xs font-medium mt-2">{step.label}</span>
                    </div>
                    {index < steps.length - 1 && (
                        <div className={`
                            w-24 h-0.5 mx-2 -mt-6 transition-colors
                            ${currentStep > step.id ? 'bg-blue-600' : 'bg-slate-200'}
                        `} />
                    )}
                </div>
            ))}
        </div>
    );
};

export default function AiWizardPage() {
    const router = useRouter();
    const [step, setStep] = useState(1);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Data State
    const [swaggerUrl, setSwaggerUrl] = useState('');
    const [apiStructure, setApiStructure] = useState<ApiStructure | null>(null);
    const [confHints, setConfHints] = useState<GenerationHints>({
        service_name: '',
        custom_instructions: ''
    });
    const [proposals, setProposals] = useState<WorkflowProposals | null>(null);
    const [selectedProposalIndex, setSelectedProposalIndex] = useState<number>(0);

    // --- Actions ---

    const handleAnalyze = async () => {
        if (!swaggerUrl) return;
        setLoading(true);
        setError(null);
        try {
            const result = await aiGeneratorService.analyzeApi({
                source_type: 'SWAGGER_URL',
                source_url: swaggerUrl
            });

            if (result.success) {
                setApiStructure(result.api_structure);
                setStep(2);
            } else {
                setError(result.error_message || "Unknown error during analysis");
            }
        } catch (err: any) {
            console.error("Analysis Error:", err);
            const backendMsg = err.response?.data?.error_message || err.response?.data?.message;
            setError(backendMsg || err.message || "Server connection error");
        } finally {
            setLoading(false);
        }
    };

    const handleGenerateProposals = async () => {
        if (!apiStructure || !confHints.service_name) return;
        setLoading(true);
        setError(null);
        try {
            const result = await aiGeneratorService.generateProposals({
                api_structure: apiStructure,
                hints: confHints
            });
            setProposals(result);
            setStep(3);
        } catch (err: any) {
            setError(err.message || "Error generating proposals");
        } finally {
            setLoading(false);
        }
    };

    const handleFinalGenerate = async () => {
        if (!proposals || !apiStructure) return;
        setLoading(true);
        setError(null);
        try {
            const result = await aiGeneratorService.generateConfig({
                api_structure: apiStructure,
                workflow_proposals: proposals,
                selected_proposal_index: selectedProposalIndex
            });

            if (result.success) {
                // Redirect to the regular Add Service page with the config pre-filled
                // We'll use localStorage to pass the data effectively
                localStorage.setItem('generated_ussd_config', JSON.stringify(result.generated_config));
                router.push('/dashboard/services/add?mode=generated');
            } else {
                setError(result.error_message || "Error during final generation");
            }
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    // --- Render ---

    return (
        <div className="max-w-4xl mx-auto py-8">
            <StepIndicator currentStep={step} />

            {error && (
                <div className="bg-red-50 text-red-700 p-4 rounded-lg mb-6 flex items-center border border-red-200">
                    <AlertCircle className="w-5 h-5 mr-2 flex-shrink-0" />
                    {error}
                </div>
            )}

            {/* STEP 1: ANALYZE */}
            {step === 1 && (
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8 animate-in fade-in slide-in-from-bottom-4">
                    <div className="text-center mb-8">
                        <h2 className="text-2xl font-bold text-slate-800">Analyze your API</h2>
                        <p className="text-slate-500 mt-2">
                            Enter the URL of your Swagger/OpenAPI documentation to get started.
                        </p>
                    </div>

                    <div className="max-w-xl mx-auto">
                        <label className="block text-sm font-medium text-slate-700 mb-2">
                            URL Swagger / OpenAPI JSON
                        </label>
                        <div className="flex gap-2">
                            <input
                                type="url"
                                placeholder="https://api.example.com/v3/api-docs"
                                className="flex-1 px-4 py-3 rounded-lg border border-slate-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all"
                                value={swaggerUrl}
                                onChange={(e) => setSwaggerUrl(e.target.value)}
                            />
                            <button
                                onClick={handleAnalyze}
                                disabled={loading || !swaggerUrl}
                                className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                            >
                                {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : "Analyze"}
                            </button>
                        </div>
                        <p className="text-xs text-slate-400 mt-2 flex items-center">
                            <FileText className="w-3 h-3 mr-1" />
                            Supported formats: OpenAPI 3.0+, Swagger 2.0 (JSON)
                        </p>
                    </div>
                </div>
            )}

            {/* STEP 2: CONFIGURE & PROPOSE */}
            {step === 2 && apiStructure && (
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8 animate-in fade-in slide-in-from-bottom-4">
                    <div className="flex justify-between items-start mb-6">
                        <div>
                            <h2 className="text-xl font-bold text-slate-800">Service Configuration</h2>
                            <p className="text-slate-500 text-sm">Define your requirements for the AI.</p>
                        </div>
                        <span className="bg-green-100 text-green-700 text-xs px-2 py-1 rounded-full font-medium flex items-center">
                            <CheckCircle className="w-3 h-3 mr-1" />
                            {Object.keys(apiStructure.endpoints).length} endpoints detected
                        </span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                        {/* Configuration Form */}
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    USSD Service Name *
                                </label>
                                <input
                                    type="text"
                                    placeholder="Ex: MyBank Service"
                                    className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-blue-500 outline-none"
                                    value={confHints.service_name}
                                    onChange={(e) => setConfHints({ ...confHints, service_name: e.target.value })}
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700 mb-1">
                                    Specific Instructions for AI
                                </label>
                                <textarea
                                    className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-blue-500 outline-none h-32 resize-none"
                                    placeholder="Ex: I want a main menu with 3 options: Balance, Transfer, and Support. Transfer should ask for a PIN."
                                    value={confHints.custom_instructions}
                                    onChange={(e) => setConfHints({ ...confHints, custom_instructions: e.target.value })}
                                />
                                <p className="text-xs text-slate-400 mt-1">
                                    The more precise you are, the better the result will be.
                                </p>
                            </div>
                        </div>

                        {/* API Preview */}
                        <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
                            <h3 className="text-sm font-semibold text-slate-700 mb-3 flex items-center">
                                <Search className="w-4 h-4 mr-2" />
                                Available Endpoints
                            </h3>
                            <div className="space-y-2 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                                {Object.values(apiStructure.endpoints).slice(0, 10).map((ep, idx) => (
                                    <div key={idx} className="bg-white p-2 rounded border border-slate-200 text-xs shadow-sm">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className={`
                                                uppercase font-bold px-1.5 py-0.5 rounded-[3px] text-[10px]
                                                ${ep.method === 'GET' ? 'bg-blue-100 text-blue-700' :
                                                    ep.method === 'POST' ? 'bg-green-100 text-green-700' :
                                                        'bg-slate-100 text-slate-700'}
                                            `}>
                                                {ep.method}
                                            </span>
                                            <span className="font-mono text-slate-600 truncate">{ep.path}</span>
                                        </div>
                                        <p className="text-slate-500 truncate pl-1 border-l-2 border-slate-100">
                                            {ep.summary || "No description"}
                                        </p>
                                    </div>
                                ))}
                                {Object.values(apiStructure.endpoints).length > 10 && (
                                    <p className="text-center text-xs text-slate-400 italic py-2">
                                        + {Object.values(apiStructure.endpoints).length - 10} more...
                                    </p>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="flex justify-end gap-3 mt-8 pt-6 border-t border-slate-100">
                        <button
                            onClick={() => setStep(1)}
                            className="px-6 py-2 text-slate-600 hover:text-slate-900 font-medium transition-colors"
                        >
                            Back
                        </button>
                        <button
                            onClick={handleGenerateProposals}
                            disabled={loading || !confHints.service_name}
                            className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                        >
                            {loading ? (
                                <>
                                    <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                                    Thinking...
                                </>
                            ) : (
                                <>
                                    <Sparkles className="w-4 h-4 mr-2" />
                                    Generate Proposals
                                </>
                            )}
                        </button>
                    </div>
                </div>
            )}

            {/* STEP 3: SELECT & GENERATE */}
            {step === 3 && proposals && (
                <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8 animate-in fade-in slide-in-from-bottom-4">
                    <div className="mb-6">
                        <h2 className="text-xl font-bold text-slate-800">Choose an Architecture</h2>
                        <p className="text-slate-500 text-sm">AI generated {proposals.proposals.length} proposals.</p>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                        {proposals.proposals.map((proposal, idx) => (
                            <div
                                key={idx}
                                onClick={() => setSelectedProposalIndex(idx)}
                                className={`
                                    cursor-pointer rounded-xl p-5 border-2 transition-all relative
                                    ${selectedProposalIndex === idx
                                        ? 'border-blue-600 bg-blue-50/50 shadow-md ring-1 ring-blue-600'
                                        : 'border-slate-100 hover:border-slate-300 hover:bg-slate-50'}
                                `}
                            >
                                <div className="flex justify-between items-start mb-2">
                                    <h3 className="font-bold text-slate-800">{proposal.name}</h3>
                                    {selectedProposalIndex === idx && (
                                        <CheckCircle className="w-5 h-5 text-blue-600" />
                                    )}
                                </div>
                                <p className="text-sm text-slate-600 mb-4 line-clamp-3">
                                    {proposal.description}
                                </p>
                                <div className="flex gap-2">
                                    <span className="text-xs bg-white border border-slate-200 px-2 py-1 rounded text-slate-500 font-medium">
                                        {proposal.states.length} states
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="bg-slate-900 text-slate-300 p-6 rounded-xl font-mono text-sm overflow-x-auto mb-8 relative group">
                        <div className="absolute top-4 right-4 text-xs bg-slate-800 px-2 py-1 rounded opacity-50">
                            JSON Preview
                        </div>
                        <pre className="custom-scrollbar max-h-60">
                            {JSON.stringify(proposals.proposals[selectedProposalIndex].states.slice(0, 2), null, 2)}
                            {'\n'}
                            {proposals.proposals[selectedProposalIndex].states.length > 2 && "  ... (other states)"}
                        </pre>
                    </div>

                    <div className="flex justify-end gap-3 pt-6 border-t border-slate-100">
                        <button
                            onClick={() => setStep(2)}
                            className="px-6 py-2 text-slate-600 hover:text-slate-900 font-medium transition-colors"
                        >
                            Back
                        </button>
                        <button
                            onClick={handleFinalGenerate}
                            disabled={loading}
                            className="bg-green-600 hover:bg-green-700 text-white px-8 py-3 rounded-lg font-bold transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center shadow-lg shadow-green-900/20 hover:shadow-xl hover:-translate-y-0.5"
                        >
                            {loading ? (
                                <>
                                    <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                                    Building service...
                                </>
                            ) : (
                                <>
                                    <Sparkles className="w-4 h-4 mr-2" />
                                    Create this service
                                </>
                            )}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
