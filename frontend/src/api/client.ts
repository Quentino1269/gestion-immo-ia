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

export async function apiPost<TRequest, TResponse>(
  chemin: string,
  payload: TRequest,
): Promise<TResponse> {
  const reponse = await fetch(chemin, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

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
