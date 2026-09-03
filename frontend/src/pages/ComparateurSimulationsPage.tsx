import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import {
  obtenirComparateur,
  supprimerSimulation,
  LIBELLES_REGIME,
  type LigneComparateurResponse,
} from '../api/rentabilite';
import { formaterEuros, formaterPourcent } from '../lib/format';
import type { EtatChargement } from '../lib/types';
import { COULEUR_POSITIF, COULEUR_NEGATIF, COULEUR_GRILLE, COULEUR_TEXTE_MUET } from '../lib/chartColors';

/** Graphique comparatif du rendement net-net par scénario, affiché avant le tableau détaillé
 * (principe dataviz "graphique avant tableau" — constat #5 de l'audit ux-design). */
function GraphiqueComparateur({
  lignes,
  survole,
  onSurvoler,
}: {
  lignes: LigneComparateurResponse[];
  survole: number | null;
  onSurvoler: (index: number | null) => void;
}) {
  const largeur = 720;
  const hauteur = 160;
  const marge = { haut: 20, droite: 12, bas: 22, gauche: 12 };
  const largeurTrace = largeur - marge.gauche - marge.droite;
  const hauteurTrace = hauteur - marge.haut - marge.bas;
  const n = lignes.length;
  const largeurCreneau = largeurTrace / n;
  const largeurBarre = Math.min(64, largeurCreneau - 16);
  // Ligne de base au milieu (comme GraphiqueCashFlow) : un rendement net-net peut être négatif
  // (scénario perdant), la barre descend alors sous la ligne plutôt que de déborder du canevas.
  const baseline = marge.haut + hauteurTrace / 2;
  const maxAbs = Math.max(1, ...lignes.map((l) => Math.abs(l.rendementNetNetAnnee1Pourcent)));

  return (
    <div className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-700">Rendement net-net par scénario</h3>
      <svg
        viewBox={`0 0 ${largeur} ${hauteur}`}
        className="mt-2 w-full"
        role="img"
        aria-label="Comparaison du rendement net-net des scénarios"
      >
        <line
          x1={marge.gauche} y1={baseline} x2={largeur - marge.droite} y2={baseline}
          stroke={COULEUR_GRILLE} strokeWidth={1}
        />
        {lignes.map((l, i) => {
          const v = l.rendementNetNetAnnee1Pourcent;
          const h = (Math.abs(v) / maxAbs) * (hauteurTrace / 2 - 2);
          const x = marge.gauche + i * largeurCreneau + (largeurCreneau - largeurBarre) / 2;
          const y = v >= 0 ? baseline - h : baseline;
          const couleur = l.cashFlowMoyenApresImpotEnCentimes < 0 ? COULEUR_NEGATIF : COULEUR_POSITIF;
          return (
            <g key={l.simulationId}>
              <rect
                x={x} y={y} width={largeurBarre} height={Math.max(h, 1)} rx={2}
                fill={couleur}
                opacity={survole === null || survole === i ? 1 : 0.55}
                tabIndex={0}
                role="img"
                aria-label={`${l.nomScenario} : rendement net-net ${formaterPourcent(v)}`}
                onMouseEnter={() => onSurvoler(i)}
                onMouseLeave={() => onSurvoler(null)}
                onFocus={() => onSurvoler(i)}
                onBlur={() => onSurvoler(null)}
              >
                <title>{`${l.nomScenario} : ${formaterPourcent(v)}`}</title>
              </rect>
              <text
                x={x + largeurBarre / 2} y={v >= 0 ? y - 8 : y + h + 14}
                textAnchor="middle" fontSize={12} fontWeight={600} fill="#0f172a"
              >
                {formaterPourcent(v)}
              </text>
              <text x={x + largeurBarre / 2} y={hauteur - 6} textAnchor="middle" fontSize={11} fill={COULEUR_TEXTE_MUET}>
                {l.nomScenario}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

export function ComparateurSimulationsPage({
  bienId,
  onNouvelleSimulation,
  onVoirDetail,
  onRetour,
}: {
  bienId: string;
  onNouvelleSimulation: () => void;
  onVoirDetail: (simulationId: string) => void;
  onRetour: () => void;
}) {
  const { session } = useAuth();
  const [lignes, setLignes] = useState<LigneComparateurResponse[]>([]);
  const [etat, setEtat] = useState<EtatChargement>('chargement');
  const [survole, setSurvole] = useState<number | null>(null);
  const [suppressionEnCours, setSuppressionEnCours] = useState<string | null>(null);
  const [erreurSuppression, setErreurSuppression] = useState<string | null>(null);

  async function supprimer(e: React.MouseEvent, simulationId: string, nomScenario: string) {
    e.stopPropagation();
    if (!session) return;
    if (!window.confirm(`Êtes-vous sûr de vouloir supprimer cette simulation ? (${nomScenario})`)) {
      return;
    }
    setSuppressionEnCours(simulationId);
    setErreurSuppression(null);
    try {
      await supprimerSimulation(simulationId, session.token);
      const restantes = lignes.filter((l) => l.simulationId !== simulationId);
      if (restantes.length === 0) {
        onNouvelleSimulation();
        return;
      }
      setLignes(restantes);
    } catch {
      setErreurSuppression('Impossible de supprimer cette simulation.');
    } finally {
      setSuppressionEnCours(null);
    }
  }

  useEffect(() => {
    if (!session) return;
    obtenirComparateur(bienId, session.token)
      .then((data) => {
        if (data.length === 0) {
          // Aucune simulation existante : autant lancer directement le formulaire
          // plutôt que d'afficher un écran vide avec un seul bouton à cliquer.
          onNouvelleSimulation();
          return;
        }
        setLignes(data);
        setEtat('pret');
      })
      .catch(() => setEtat('erreur'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session, bienId]);

  return (
    <section className="mx-auto max-w-3xl">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-100">
          Simulations de rentabilité
        </h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-400 hover:text-slate-100"
        >
          ← Retour
        </button>
      </div>

      {etat === 'chargement' && <p className="text-sm text-slate-400">Chargement…</p>}
      {etat === 'erreur' && (
        <p className="text-sm text-red-600">Impossible de charger les simulations de ce bien.</p>
      )}

      {etat === 'pret' && (
        <>
          {/* lignes est toujours non vide ici : le useEffect redirige directement vers le
              formulaire de nouvelle simulation si aucun scénario n'existe encore pour ce bien. */}
            <div className="mb-4">
              <GraphiqueComparateur lignes={lignes} survole={survole} onSurvoler={setSurvole} />
            </div>
            {erreurSuppression && (
              <div className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
                {erreurSuppression}
              </div>
            )}
            <div className="overflow-x-auto rounded-md border border-slate-200 bg-white shadow-sm">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="px-4 py-2 text-left font-medium text-slate-600">Scénario</th>
                    <th className="px-4 py-2 text-left font-medium text-slate-600">Régime</th>
                    <th className="px-4 py-2 text-right font-medium text-slate-600">Rdt. brut</th>
                    <th className="px-4 py-2 text-right font-medium text-slate-600">Rdt. net</th>
                    <th className="px-4 py-2 text-right font-medium text-slate-600">Rdt. net-net</th>
                    <th className="px-4 py-2 text-right font-medium text-slate-600">
                      Cash-flow moyen
                    </th>
                    <th className="px-4 py-2 text-right font-medium text-slate-600"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {lignes.map((l, i) => (
                    <tr
                      key={l.simulationId}
                      onClick={() => onVoirDetail(l.simulationId)}
                      onMouseEnter={() => setSurvole(i)}
                      onMouseLeave={() => setSurvole(null)}
                      className={`cursor-pointer hover:bg-slate-50 ${survole === i ? 'bg-slate-50' : ''}`}
                    >
                      <td className="px-4 py-2 font-medium text-slate-900">{l.nomScenario}</td>
                      <td className="px-4 py-2 text-slate-600">
                        {LIBELLES_REGIME[l.regimeFiscal]}
                      </td>
                      <td className="px-4 py-2 text-right text-slate-700">
                        {formaterPourcent(l.rendementBrutAnnee1Pourcent)}
                      </td>
                      <td className="px-4 py-2 text-right text-slate-700">
                        {formaterPourcent(l.rendementNetAnnee1Pourcent)}
                      </td>
                      <td className="px-4 py-2 text-right font-medium text-slate-900">
                        {formaterPourcent(l.rendementNetNetAnnee1Pourcent)}
                      </td>
                      <td
                        className={`px-4 py-2 text-right font-medium ${
                          l.cashFlowMoyenApresImpotEnCentimes < 0 ? 'text-red-600' : 'text-green-700'
                        }`}
                      >
                        {formaterEuros(l.cashFlowMoyenApresImpotEnCentimes)}
                      </td>
                      <td className="px-4 py-2 text-right">
                        <button
                          type="button"
                          onClick={(e) => supprimer(e, l.simulationId, l.nomScenario)}
                          disabled={suppressionEnCours === l.simulationId}
                          className="text-sm font-medium text-slate-400 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          {suppressionEnCours === l.simulationId ? '…' : 'Supprimer'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

          <button
            type="button"
            onClick={onNouvelleSimulation}
            className="mt-6 w-full rounded bg-emerald-800 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
          >
            + Nouvelle simulation
          </button>
        </>
      )}
    </section>
  );
}
