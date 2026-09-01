import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { obtenirPortefeuille, type LignePortefeuilleResponse } from '../api/biens';

type EtatChargement = 'chargement' | 'pret' | 'erreur';

function formaterLoyer(centimes: number): string {
  return (centimes / 100).toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' });
}

function formaterDate(iso: string): string {
  return new Date(iso).toLocaleDateString('fr-FR', { dateStyle: 'short' });
}

function CartePortefeuille({
  ligne,
  onSimulerRentabilite,
}: {
  ligne: LignePortefeuilleResponse;
  onSimulerRentabilite: (bienId: string) => void;
}) {
  const adresseResume = `${ligne.adresse.numero} ${ligne.adresse.voie}, ${ligne.adresse.codePostal} ${ligne.adresse.commune}`;
  const peutSimuler = ligne.typeBien !== 'CHAMBRE_COLOCATION';
  return (
    <div className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="font-semibold text-slate-900">{ligne.libelleCommercial}</p>
          <p className="mt-0.5 text-sm text-slate-500">{adresseResume}</p>
        </div>
        <span className="ml-4 shrink-0 text-sm font-medium text-slate-700">
          {ligne.surfaceM2} m²
        </span>
      </div>
      <div className="mt-3 flex items-center justify-between">
        <div className="flex items-center gap-4 text-sm text-slate-600">
          <span>
            {formaterLoyer(ligne.loyerHorsChargesEnCentimes)} HC
            {ligne.chargesEnCentimes > 0 &&
              ` + ${formaterLoyer(ligne.chargesEnCentimes)} charges`}
          </span>
          <span className="text-slate-400">·</span>
          <span>Dispo le {formaterDate(ligne.disponibleAPartirDu)}</span>
        </div>
        {peutSimuler && (
          <button
            type="button"
            onClick={() => onSimulerRentabilite(ligne.bienId)}
            className="shrink-0 text-sm font-medium text-slate-700 hover:text-slate-900 hover:underline"
          >
            Rentabilité →
          </button>
        )}
      </div>
    </div>
  );
}

export function PortefeuillePage({
  onNouveauBien,
  onSimulerRentabilite,
  onRetour,
}: {
  onNouveauBien: () => void;
  onSimulerRentabilite: (bienId: string) => void;
  onRetour: () => void;
}) {
  const { session } = useAuth();
  const [lignes, setLignes] = useState<LignePortefeuilleResponse[]>([]);
  const [etat, setEtat] = useState<EtatChargement>('chargement');

  useEffect(() => {
    if (!session) return;
    obtenirPortefeuille(session.token)
      .then((data) => {
        setLignes(data);
        setEtat('pret');
      })
      .catch(() => setEtat('erreur'));
  }, [session]);

  return (
    <section className="mx-auto max-w-lg">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-900">Mon portefeuille</h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-500 hover:text-slate-800"
        >
          ← Retour
        </button>
      </div>

      {etat === 'chargement' && (
        <p className="text-sm text-slate-500">Chargement…</p>
      )}
      {etat === 'erreur' && (
        <p className="text-sm text-red-600">Impossible de charger votre portefeuille.</p>
      )}
      {etat === 'pret' && (
        <>
          {lignes.length === 0 ? (
            <p className="text-sm text-slate-500">Vous n'avez pas encore de bien dans votre portefeuille.</p>
          ) : (
            <div className="space-y-4">
              {lignes.map((l) => (
                <CartePortefeuille key={l.bienId} ligne={l} onSimulerRentabilite={onSimulerRentabilite} />
              ))}
            </div>
          )}

          <button
            type="button"
            onClick={onNouveauBien}
            className="mt-6 w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
          >
            + Ajouter un bien
          </button>
        </>
      )}
    </section>
  );
}
