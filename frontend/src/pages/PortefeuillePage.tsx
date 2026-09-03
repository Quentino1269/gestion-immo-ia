import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { obtenirPortefeuille, type LignePortefeuilleResponse } from '../api/biens';
import { obtenirComparateur } from '../api/rentabilite';
import { formaterEuros } from '../lib/format';
import type { EtatChargement } from '../lib/types';
import { StatTuile } from '../components/StatTuile';

function formaterDate(iso: string): string {
  return new Date(iso).toLocaleDateString('fr-FR', { dateStyle: 'short' });
}

function CartePortefeuille({
  ligne,
  onSimulerRentabilite,
  onModifier,
}: {
  ligne: LignePortefeuilleResponse;
  onSimulerRentabilite: (bienId: string) => void;
  onModifier: (bienId: string) => void;
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
            {formaterEuros(ligne.loyerHorsChargesEnCentimes)} HC
            {ligne.chargesEnCentimes > 0 &&
              ` + ${formaterEuros(ligne.chargesEnCentimes)} charges`}
          </span>
          <span className="text-slate-400">·</span>
          <span>Dispo le {formaterDate(ligne.disponibleAPartirDu)}</span>
        </div>
        <div className="flex shrink-0 items-center gap-3">
          <button
            type="button"
            onClick={() => onModifier(ligne.bienId)}
            className="text-sm font-medium text-slate-500 hover:text-slate-800 hover:underline"
          >
            Modifier
          </button>
          {peutSimuler && (
            <button
              type="button"
              onClick={() => onSimulerRentabilite(ligne.bienId)}
              className="text-sm font-medium text-emerald-700 hover:text-emerald-800 hover:underline"
            >
              Rentabilité →
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export function PortefeuillePage({
  onNouveauBien,
  onSimulerRentabilite,
  onModifierBien,
  onRetour,
}: {
  onNouveauBien: () => void;
  onSimulerRentabilite: (bienId: string) => void;
  onModifierBien: (bienId: string) => void;
  onRetour: () => void;
}) {
  const { session } = useAuth();
  const [lignes, setLignes] = useState<LignePortefeuilleResponse[]>([]);
  const [etat, setEtat] = useState<EtatChargement>('chargement');
  const [netNetMensuelEnCentimes, setNetNetMensuelEnCentimes] = useState<number | null>(null);

  useEffect(() => {
    if (!session) return;
    obtenirPortefeuille(session.token)
      .then((data) => {
        setLignes(data);
        setEtat('pret');
      })
      .catch(() => setEtat('erreur'));
  }, [session]);

  // Loyers net-net cumulés : simulation la plus récente de chaque bien déjà disponible aujourd'hui
  // (choix validé avec l'utilisateur — approximation par le cash-flow moyen de cette simulation,
  // faute d'un ancrage calendaire natif dans une projection qui raisonne en années relatives).
  useEffect(() => {
    if (!session || lignes.length === 0) {
      setNetNetMensuelEnCentimes(lignes.length === 0 ? 0 : null);
      return;
    }
    const aujourdHui = new Date();
    const biensSimulables = lignes.filter(
      (l) => l.typeBien !== 'CHAMBRE_COLOCATION' && new Date(l.disponibleAPartirDu) <= aujourdHui,
    );
    if (biensSimulables.length === 0) {
      setNetNetMensuelEnCentimes(0);
      return;
    }
    Promise.allSettled(biensSimulables.map((l) => obtenirComparateur(l.bienId, session.token))).then(
      (resultats) => {
        const total = resultats.reduce((somme, resultat) => {
          if (resultat.status !== 'fulfilled' || resultat.value.length === 0) return somme;
          const plusRecente = [...resultat.value].sort(
            (a, b) => new Date(b.simuleLe).getTime() - new Date(a.simuleLe).getTime(),
          )[0];
          return somme + plusRecente.cashFlowMoyenApresImpotEnCentimes / 12;
        }, 0);
        setNetNetMensuelEnCentimes(total);
      },
    );
  }, [session, lignes]);

  const loyersChargesCumulesEnCentimes = useMemo(
    () => lignes.reduce((somme, l) => somme + l.loyerHorsChargesEnCentimes + l.chargesEnCentimes, 0),
    [lignes],
  );

  return (
    <section className="mx-auto max-w-lg">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-100">Mon portefeuille</h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-400 hover:text-slate-100"
        >
          ← Retour
        </button>
      </div>

      {etat === 'chargement' && (
        <p className="text-sm text-slate-400">Chargement…</p>
      )}
      {etat === 'erreur' && (
        <p className="text-sm text-red-600">Impossible de charger votre portefeuille.</p>
      )}
      {etat === 'pret' && (
        <>
          {lignes.length === 0 ? (
            <p className="text-sm text-slate-400">Vous n'avez pas encore de bien dans votre portefeuille.</p>
          ) : (
            <>
              <div className="mb-6 grid grid-cols-3 gap-3">
                <StatTuile libelle="Biens" valeur={String(lignes.length)} />
                <StatTuile
                  libelle="Loyers cumulés (charges incl.) / mois"
                  valeur={formaterEuros(loyersChargesCumulesEnCentimes)}
                />
                <StatTuile
                  libelle="Loyers net-net cumulés / mois"
                  valeur={netNetMensuelEnCentimes === null ? '…' : formaterEuros(netNetMensuelEnCentimes)}
                  negatif={netNetMensuelEnCentimes !== null && netNetMensuelEnCentimes < 0}
                />
              </div>
              <p className="mb-4 -mt-3 text-xs text-slate-400">
                Net-net : basé sur la simulation la plus récente de chaque bien déjà disponible, cash-flow moyen de
                cette simulation.
              </p>
              <div className="space-y-4">
                {lignes.map((l) => (
                  <CartePortefeuille
                    key={l.bienId}
                    ligne={l}
                    onSimulerRentabilite={onSimulerRentabilite}
                    onModifier={onModifierBien}
                  />
                ))}
              </div>
            </>
          )}

          <button
            type="button"
            onClick={onNouveauBien}
            className="mt-6 w-full rounded bg-emerald-800 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
          >
            + Ajouter un bien
          </button>
        </>
      )}
    </section>
  );
}
