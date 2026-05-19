import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/useAuth';

export function LoginPage() {
  const { connecter } = useAuth();
  const [email, setEmail] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [enCours, setEnCours] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  async function soumettre(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEnCours(true);
    setErreur(null);
    try {
      await connecter({ email, motDePasse });
      // Succès : le AuthProvider met à jour son état, App re-rend l'écran connecté.
      setMotDePasse('');
    } catch (e) {
      // D7 : le serveur renvoie un message générique « Identifiants invalides. »
      // quel que soit le motif réel (email inconnu, mauvais mdp, compte inactif).
      // On affiche tel quel ; jamais d'indice plus précis côté client.
      if (e instanceof ApiError) {
        setErreur(e.message);
      } else {
        setErreur('Erreur réseau, réessayez plus tard.');
      }
    } finally {
      setEnCours(false);
    }
  }

  return (
    <section className="mx-auto max-w-md">
      <h2 className="text-2xl font-semibold tracking-tight text-slate-900">Se connecter</h2>
      <p className="mt-2 text-sm text-slate-600">
        Accédez à votre portefeuille de biens.
      </p>

      {erreur && (
        <div
          role="alert"
          className="mt-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-800"
        >
          {erreur}
        </div>
      )}

      <form className="mt-6 space-y-4" onSubmit={soumettre} noValidate>
        <label className="block">
          <span className="text-sm font-medium text-slate-700">Email</span>
          <input
            type="email"
            value={email}
            required
            autoComplete="username"
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-900/10"
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-slate-700">Mot de passe</span>
          <input
            type="password"
            value={motDePasse}
            required
            autoComplete="current-password"
            onChange={(e) => setMotDePasse(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-900/10"
          />
        </label>

        <button
          type="submit"
          disabled={enCours}
          className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {enCours ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>
    </section>
  );
}
