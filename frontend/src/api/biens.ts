import { apiGet, apiPost, apiPut } from './client';

export type TypeBien = 'MAISON' | 'APPARTEMENT' | 'CHAMBRE_COLOCATION';
export type ModaliteCharges = 'FORFAIT' | 'PROVISION';

export type AdresseBienPayload = {
  numero: string;
  voie: string;
  complement?: string;
  codePostal: string;
  commune: string;
  paysIso: string;
};

export type AdresseBienResponse = {
  numero: string;
  voie: string;
  complement: string | null;
  codePostal: string;
  commune: string;
  paysIso: string;
};

export type CreerBienPayload = {
  typeBien: TypeBien;
  bienParentId?: string;
  libelleChambre?: string;
  nbPiecesPrincipales: number;
  surfaceM2: number;
  meuble: boolean;
  loyerHorsChargesEnCentimes: number;
  chargesEnCentimes: number;
  modaliteCharges: ModaliteCharges;
  adresse: AdresseBienPayload;
  disponibleAPartirDu: string;
};

export type FicheBienResponse = {
  bienId: string;
  typeBien: TypeBien;
  libelleCommercial: string;
  bienParentId: string | null;
  libelleChambre: string | null;
  nbPiecesPrincipales: number;
  surfaceM2: number;
  meuble: boolean;
  loyerHorsChargesEnCentimes: number;
  chargesEnCentimes: number;
  modaliteCharges: ModaliteCharges;
  adresse: AdresseBienResponse;
  disponibleAPartirDu: string;
  ajouteLe: string;
};

export type LignePortefeuilleResponse = {
  bienId: string;
  typeBien: TypeBien;
  libelleCommercial: string;
  bienParentId: string | null;
  surfaceM2: number;
  loyerHorsChargesEnCentimes: number;
  chargesEnCentimes: number;
  modaliteCharges: ModaliteCharges;
  adresse: AdresseBienResponse;
  disponibleAPartirDu: string;
};

export type ModifierBienPayload = {
  loyerHorsChargesEnCentimes: number;
  chargesEnCentimes: number;
  meuble: boolean;
  disponibleAPartirDu: string;
  libelleChambre?: string | null;
  nbPiecesPrincipales: number;
};

export function creerBien(payload: CreerBienPayload, token: string): Promise<FicheBienResponse> {
  return apiPost<CreerBienPayload, FicheBienResponse>('/api/biens', payload, token);
}

export function modifierBien(
  bienId: string,
  payload: ModifierBienPayload,
  token: string,
): Promise<FicheBienResponse> {
  return apiPut<ModifierBienPayload, FicheBienResponse>(`/api/biens/${bienId}`, payload, token);
}

export function obtenirPortefeuille(token: string): Promise<LignePortefeuilleResponse[]> {
  return apiGet<LignePortefeuilleResponse[]>('/api/biens', token);
}

export function obtenirFicheBien(bienId: string, token: string): Promise<FicheBienResponse> {
  return apiGet<FicheBienResponse>(`/api/biens/${bienId}`, token);
}
