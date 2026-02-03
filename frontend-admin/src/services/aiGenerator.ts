import { apiClient } from '@/lib/api';
import {
    ApiSourceRequest,
    ApiAnalysisResult,
    GenerateProposalsRequest,
    WorkflowProposals,
    GenerateConfigRequest,
    GenerationResult
} from '@/types/ai-generator';

const BASE_URL = '/api/admin/ai-generator';

export const aiGeneratorService = {
    /**
     * Step 1: Analyze an API source (Swagger URL, etc.)
     */
    analyzeApi: async (request: ApiSourceRequest): Promise<ApiAnalysisResult> => {
        const response = await apiClient.post(`${BASE_URL}/analyze`, request);
        return response.data;
    },

    /**
     * Step 2: Generate workflow proposals based on the analyzed API
     */
    generateProposals: async (request: GenerateProposalsRequest): Promise<WorkflowProposals> => {
        const response = await apiClient.post(`${BASE_URL}/generate-proposals`, request);
        return response.data;
    },

    /**
     * Step 3: Generate the final USSD configuration from a selected proposal
     */
    generateConfig: async (request: GenerateConfigRequest): Promise<GenerationResult> => {
        const response = await apiClient.post(`${BASE_URL}/generate-config`, request);
        return response.data;
    }
};
