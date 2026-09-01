import { apiGet, apiPost } from './client';

export type RegimeFiscal = 'MICRO_FONCIER' | 'REEL_FONCIER' | 'MICRO_BIC' | 'REEL_BIC';

export const LIBELLES_REGIME: Record<RegimeFiscal, string> = {
  MICRO_FONCIER: 'Micro-foncier',
  REEL_FONCIER: 'Réel foncier',
  MICRO_BIC: 'Micro-BIC',
  REEL_BIC: 'Réel BIC (LMNP)',
};

export type AcquisitionPayload = {
  prixAchatEnCentimes: number;
  fraisNotaireEnCentimes: number;
  fraisAgenceEnCentimes: number;
  travauxAlAcquisitionEnCentimes: number;
  fraisDossierBancaireEnCentimes: number;
};

export type FinancementPayload = {
  montantEmprunteEnCentimes: number;
  tauxAnnuelPourcent: number;
  dureeAnnees: number;
  tauxAssuranceEmprunteurPourcent: number;
};

export type AmortissementPayload = {
  quotePartTerrainPourcent: number;
  quotePartMobilierPourcent: number;
  dureeAmortissementBatiAnnees: number;
  dureeAmortissementMobilierAnnees: number;
};

export type LigneRevenuPayload = {
  bienSourceId: string;
  loyerSimuleMensuelEnCentimes: number;
  chargesSimuleesMensuellesEnCentimes: number;
};

export type ChargesRecurrentesPayload = {
  taxeFonciereEnCentimes: number;
  assurancePnoEnCentimes: number;
  assuranceLoyersImpayesEnCentimes: number;
  fraisGestionLocativePourcentLoyer: number;
  provisionTravauxAnnuelleEnCentimes: number;
  fraisComptabiliteAnnuelEnCentimes: number;
  chargesCoproprieteNonRecuperablesEnCentimes: number;
};

export type HypothesesEvolutionPayload = {
  tauxVacanceLocativePourcent: number;
  tauxIndexationLoyerPourcent: number;
  tauxIndexationChargesPourcent: number;
};

export type LancerSimulationRentabilitePayload = {
  nomScenario: string;
  regimeFiscal: RegimeFiscal;
  tmiFoyerPourcent: number;
  horizonAnnees: number;
  acquisition: AcquisitionPayload;
  financement: FinancementPayload;
  amortissement: AmortissementPayload;
  revenusLocatifsSimules: LigneRevenuPayload[];
  chargesRecurrentes: ChargesRecurrentesPayload;
  hypothesesEvolution: HypothesesEvolutionPayload;
};

export type LigneProjectionResponse = {
  annee: number;
  loyerBrutAnnuelEnCentimes: number;
  chargesNonRecuperablesAnnuellesEnCentimes: number;
  interetsEmpruntAnnuelsEnCentimes: number;
  capitalRembourseAnnuelEnCentimes: number;
  assuranceEmprunteurAnnuelleEnCentimes: number;
  capitalRestantDuFinAnneeEnCentimes: number;
  amortissementBatiAnnuelEnCentimes: number;
  amortissementMobilierAnnuelEnCentimes: number;
  resultatImposableEnCentimes: number;
  deficitReportableUtiliseEnCentimes: number;
  soldeDeficitFoncierReportableFinAnneeEnCentimes: number;
  soldeDeficitBicReportableFinAnneeEnCentimes: number;
  impotEstimeEnCentimes: number;
  cashFlowAvantFinancementAvantImpotEnCentimes: number;
  cashFlowApresFinancementAvantImpotEnCentimes: number;
  cashFlowApresFinancementApresImpotEnCentimes: number;
  rendementBrutPourcent: number;
  rendementNetPourcent: number;
  rendementNetNetPourcent: number;
  rendementSurFondsPropresPourcent: number | null;
};

export type SimulationRentabiliteResponse = {
  simulationId: string;
  bienId: string;
  nomScenario: string;
  regimeFiscal: RegimeFiscal;
  tmiFoyerPourcent: number;
  horizonAnnees: number;
  acquisition: AcquisitionPayload;
  financement: FinancementPayload;
  amortissement: AmortissementPayload;
  revenusLocatifsSimules: LigneRevenuPayload[];
  chargesRecurrentes: ChargesRecurrentesPayload;
  hypothesesEvolution: HypothesesEvolutionPayload;
  coutTotalAcquisitionEnCentimes: number;
  apportPersonnelEnCentimes: number;
  projectionAnnuelle: LigneProjectionResponse[];
  simuleLe: string;
};

export type LigneComparateurResponse = {
  simulationId: string;
  nomScenario: string;
  regimeFiscal: RegimeFiscal;
  rendementBrutAnnee1Pourcent: number;
  rendementNetAnnee1Pourcent: number;
  rendementNetNetAnnee1Pourcent: number;
  cashFlowMoyenApresImpotEnCentimes: number;
  simuleLe: string;
};

export function lancerSimulation(
  bienId: string,
  payload: LancerSimulationRentabilitePayload,
  token: string,
): Promise<SimulationRentabiliteResponse> {
  return apiPost<LancerSimulationRentabilitePayload, SimulationRentabiliteResponse>(
    `/api/biens/${bienId}/simulations-rentabilite`,
    payload,
    token,
  );
}

export function obtenirComparateur(bienId: string, token: string): Promise<LigneComparateurResponse[]> {
  return apiGet<LigneComparateurResponse[]>(`/api/biens/${bienId}/simulations-rentabilite`, token);
}

export function obtenirDetailSimulation(
  simulationId: string,
  token: string,
): Promise<SimulationRentabiliteResponse> {
  return apiGet<SimulationRentabiliteResponse>(`/api/simulations-rentabilite/${simulationId}`, token);
}
