import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import {
  seConnecter as apiSeConnecter,
  seDeconnecter as apiSeDeconnecter,
  type SeConnecterRequest,
} from '../api/sessions';

export type SessionAuthentifiee = {
  sessionId: string;
  token: string;
  expireA: string;
};

export type AuthContextValeur = {
  session: SessionAuthentifiee | null;
  connecter: (payload: SeConnecterRequest) => Promise<void>;
  deconnecter: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextValeur | null>(null);

// localStorage retenu en V1 pour que la session survive au rechargement de page.
// Le slice authentification §12 recommande in-memory pour limiter XSS, mais
// accepte localStorage si la CSP est stricte. À reconsidérer quand on aura une CSP.
const CLE_STORAGE = 'gestion-immo:session';

function chargerDepuisStorage(): SessionAuthentifiee | null {
  try {
    const brut = localStorage.getItem(CLE_STORAGE);
    if (!brut) return null;
    const parsed = JSON.parse(brut) as SessionAuthentifiee;
    if (!parsed.token || !parsed.sessionId || !parsed.expireA) return null;
    if (Date.parse(parsed.expireA) <= Date.now()) return null;
    return parsed;
  } catch {
    return null;
  }
}

function persister(session: SessionAuthentifiee | null) {
  if (session) {
    localStorage.setItem(CLE_STORAGE, JSON.stringify(session));
  } else {
    localStorage.removeItem(CLE_STORAGE);
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<SessionAuthentifiee | null>(chargerDepuisStorage);

  // Purge automatique à l'arrivée de expireA (I-7 : pas de réactivation).
  useEffect(() => {
    if (!session) return;
    const delai = Date.parse(session.expireA) - Date.now();
    if (delai <= 0) {
      setSession(null);
      persister(null);
      return;
    }
    const handle = setTimeout(() => {
      setSession(null);
      persister(null);
    }, delai);
    return () => clearTimeout(handle);
  }, [session]);

  const connecter = useCallback(async (payload: SeConnecterRequest) => {
    const reponse = await apiSeConnecter(payload);
    const s: SessionAuthentifiee = {
      sessionId: reponse.sessionId,
      token: reponse.token,
      expireA: reponse.expireA,
    };
    persister(s);
    setSession(s);
  }, []);

  const deconnecter = useCallback(async () => {
    if (!session) return;
    try {
      await apiSeDeconnecter(session.sessionId, session.token);
    } catch {
      // Côté serveur la session peut déjà être expirée/fermée. On purge le
      // client de toute façon : I-7 garantit qu'elle n'est plus utilisable.
    }
    persister(null);
    setSession(null);
  }, [session]);

  const valeur = useMemo<AuthContextValeur>(
    () => ({ session, connecter, deconnecter }),
    [session, connecter, deconnecter],
  );

  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>;
}