/**
 * Wrapper fetch typé. Gère la convention d'erreur ApiError côté backend :
 * { message: string, erreurs: [{ champ, message }] }.
 */

export type ChampEnErreur = { champ: string; message: string };
export type ApiErrorBody = { message: string; erreurs: ChampEnErreur[] };

export class ApiError extends Error {
  readonly status: number;
  readonly erreurs: ChampEnErreur[];

  constructor(status: number, message: string, erreurs: ChampEnErreur[] = []) {
    super(message);
    this.status = status;
    this.erreurs = erreurs;
  }
}

async function fetchTyped<TResponse>(
  chemin: string,
  init: RequestInit,
  token?: string,
): Promise<TResponse> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const reponse = await fetch(chemin, { ...init, headers });

  if (!reponse.ok) {
    let body: ApiErrorBody = { message: 'Erreur inattendue.', erreurs: [] };
    try {
      body = (await reponse.json()) as ApiErrorBody;
    } catch {
      // body non parsable
    }
    throw new ApiError(reponse.status, body.message ?? 'Erreur.', body.erreurs ?? []);
  }

  if (reponse.status === 204) {
    return undefined as TResponse;
  }
  return (await reponse.json()) as TResponse;
}

export function apiPost<TRequest, TResponse>(
  chemin: string,
  payload: TRequest,
  token?: string,
): Promise<TResponse> {
  return fetchTyped<TResponse>(
    chemin,
    { method: 'POST', body: JSON.stringify(payload) },
    token,
  );
}

export function apiGet<TResponse>(chemin: string, token?: string): Promise<TResponse> {
  return fetchTyped<TResponse>(chemin, { method: 'GET' }, token);
}

export function apiPut<TRequest, TResponse>(
  chemin: string,
  payload: TRequest,
  token?: string,
): Promise<TResponse> {
  return fetchTyped<TResponse>(
    chemin,
    { method: 'PUT', body: JSON.stringify(payload) },
    token,
  );
}

export function apiDelete<TResponse>(chemin: string, token?: string): Promise<TResponse> {
  return fetchTyped<TResponse>(chemin, { method: 'DELETE' }, token);
}
