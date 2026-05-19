import { apiDelete, apiPost } from './client';

export type SeConnecterRequest = {
  email: string;
  motDePasse: string;
};

export type SeConnecterResponse = {
  sessionId: string;
  token: string;
  expireA: string;
};

export function seConnecter(payload: SeConnecterRequest) {
  return apiPost<SeConnecterRequest, SeConnecterResponse>('/api/sessions', payload);
}

export function seDeconnecter(sessionId: string, token: string) {
  return apiDelete<void>(`/api/sessions/${sessionId}`, token);
}