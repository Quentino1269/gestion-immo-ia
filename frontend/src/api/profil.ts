import { apiGet, apiPut } from './client';

export type Civilite = 'MADAME' | 'MONSIEUR' | 'NON_RENSEIGNEE';
export type StatutProfil = 'MINIMAL' | 'COMPLET';

export type AdressePayload = {
  numero: string;
  voie: string;
  complement?: string;
  codePostal: string;
  commune: string;
  paysIso: string;
};

export type CompleterProfilPayload = {
  civilite?: Civilite;
  dateNaissance?: string;
  lieuNaissanceVille?: string;
  lieuNaissancePaysIso?: string;
  nationaliteIso?: string;
  adresseDomicile?: AdressePayload;
  telephone?: string;
};

export type ProfilIdentite = {
  nom: string;
  prenom: string;
  civilite: Civilite | null;
  dateNaissance: string | null;
  lieuNaissanceVille: string | null;
  lieuNaissancePaysIso: string | null;
  nationaliteIso: string | null;
};

export type ProfilCoordonnees = {
  email: string;
  telephone: string | null;
};

export type AdresseResponse = {
  numero: string;
  voie: string;
  complement: string | null;
  codePostal: string;
  commune: string;
  paysIso: string;
};

export type ProfilResponse = {
  utilisateurId: string;
  identite: ProfilIdentite;
  coordonnees: ProfilCoordonnees;
  adresseDomicile: AdresseResponse | null;
  statutProfil: StatutProfil;
  champsManquantsPourBail: string[];
};

export function obtenirMonProfil(token: string): Promise<ProfilResponse> {
  return apiGet<ProfilResponse>('/api/utilisateurs/moi/profil', token);
}

export function completerMonProfil(
  payload: CompleterProfilPayload,
  token: string,
): Promise<ProfilResponse> {
  return apiPut<CompleterProfilPayload, ProfilResponse>(
    '/api/utilisateurs/moi/profil',
    payload,
    token,
  );
}
