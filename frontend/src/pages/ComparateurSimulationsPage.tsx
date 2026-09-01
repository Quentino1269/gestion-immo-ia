import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { obtenirComparateur, LIBELLES_REGIME, type LigneComparateurResponse } from '../api/rentabilite';
import { formaterEuros, formaterPourcent } from '../lib/format';
import type { EtatChargement } from '../lib/types';

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
        <h2 className="text-2xl font-semibold tracking-tight text-slate-900">
          Simulations de rentabilité
        </h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-500 hover:text-slate-800"
        >
          ← Retour
        </button>
      </div>

      {etat === 'chargement' && <p className="text-sm text-slate-500">Chargement…</p>}
      {etat === 'erreur' && (
        <p className="text-sm text-red-600">Impossible de charger les simulations de ce bien.</p>
      )}

      {etat === 'pret' && (
        <>
          {/* lignes est toujours non vide ici : le useEffect redirige directement vers le
              formulaire de nouvelle simulation si aucun scénario n'existe encore pour ce bien. */}
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
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {lignes.map((l) => (
                    <tr
                      key={l.simulationId}
                      onClick={() => onVoirDetail(l.simulationId)}
                      className="cursor-pointer hover:bg-slate-50"
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
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

          <button
            type="button"
            onClick={onNouvelleSimulation}
            className="mt-6 w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
          >
            + Nouvelle simulation
          </button>
        </>
      )}
    </section>
  );
}
