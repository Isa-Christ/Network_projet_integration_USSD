export type SourceType = 'SWAGGER_URL' | 'SWAGGER_FILE' | 'POSTMAN';

export interface ApiSourceRequest {
    source_type: SourceType;
    source_url?: string;
    file_content?: string;
}

export interface CostEstimate {
    estimated_tokens: number;
    estimated_cost_usd: number;
    complexity_score: number; // 1-10
}

export interface ApiStructure {
    base_url: string;
    endpoints: Record<string, Endpoint>;
    schemas: Record<string, Schema>;
}

export interface Endpoint {
    path: string;
    method: string;
    summary: string;
    description: string;
    operation_id: string;
    has_request_body: boolean;
    required_parameters: string[];
}

export interface Schema {
    name: string;
    fields: Record<string, string>; // name -> type
}

export interface ApiAnalysisResult {
    success: boolean;
    api_structure: ApiStructure;
    cost_estimate: CostEstimate;
    error_message?: string;
}

export interface GenerationHints {
    service_name: string;
    preferred_language?: string; // "fr", "en"
    max_depth?: number;
    include_confirmation_steps?: boolean;
    custom_instructions?: string;
}

export interface GenerateProposalsRequest {
    api_structure: ApiStructure;
    hints: GenerationHints;
}

export interface WorkflowProposal {
    name: string;
    description: string;
    states: StateProposal[];
}

export interface StateProposal {
    id: string;
    name: string;
    type: 'MENU' | 'INPUT' | 'PROCESSING' | 'INFO';
    message: string;
    is_initial: boolean;
    transitions: Record<string, string>[]; // map with keys like "input", "nextState", "condition"
    linked_endpoint?: string; // id of the endpoint
    parameter_name?: string; // for INPUT states
}

export interface WorkflowProposals {
    service_name: string;
    proposals: WorkflowProposal[];
}

export interface GenerateConfigRequest {
    api_structure: ApiStructure;
    workflow_proposals: WorkflowProposals;
    selected_proposal_index: number;
}

export interface ValidationReport {
    is_valid: boolean;
    errors: string[];
    warnings: string[];
}

export interface GenerationResult {
    success: boolean;
    generated_config: any; // AutomatonDefinition (JSON)
    validation_report: ValidationReport;
    total_processing_time_ms: number;
    error_message?: string;
}
