import { apiPost } from './client';

export type CreerUtilisateurRequest = {
  email: string;
  motDePasse: string;
  nom: string;
  prenom: string;
  telephone?: string;
  accepteCgu: boolean;
  accepteConfidentialite: boolean;
};

export type UtilisateurResponse = {
  utilisateurId: string;
};

export function creerUtilisateur(payload: CreerUtilisateurRequest) {
  return apiPost<CreerUtilisateurRequest, UtilisateurResponse>('/api/utilisateurs', payload);
}
