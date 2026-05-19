import { useState } from 'react';
import { useAuth } from '../auth/useAuth';

/**
 * Écran post-connexion minimal (placeholder V1) : confirme l'authentification
 * et expose la déconnexion. Les futurs écrans (portefeuille de biens, profil)
 * arriveront avec les slices 3 et 4.
 */
export function AccueilPage() {
  const { session, deconnecter } = useAuth();
  const [enCours, setEnCours] = useState(false);

  async function gererDeconnexion() {
    setEnCours(true);
    try {
      await deconnecter();
    } finally {
      setEnCours(false);
    }
  }

  if (!session) return null;

  const expireA = new Date(session.expireA);

  return (
    <section className="mx-auto max-w-md">
      <h2 className="text-2xl font-semibold tracking-tight text-slate-900">Bienvenue</h2>
      <p className="mt-2 text-sm text-slate-600">
        Vous êtes connecté. Votre session expire le{' '}
        <span className="font-medium text-slate-800">
          {expireA.toLocaleString('fr-FR', {
            dateStyle: 'short',
            timeStyle: 'short',
          })}
        </span>
        .
      </p>

      <button
        type="button"
        onClick={gererDeconnexion}
        disabled={enCours}
        className="mt-6 w-full rounded border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {enCours ? 'Déconnexion…' : 'Se déconnecter'}
      </button>
    </section>
  );
}
