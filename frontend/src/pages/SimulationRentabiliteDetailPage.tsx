import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import {
  obtenirDetailSimulation,
  obtenirHistoriqueSimulation,
  modifierSimulation,
  payloadDepuisSimulation,
  LIBELLES_REGIME,
  type LigneProjectionResponse,
  type SimulationRentabiliteResponse,
} from '../api/rentabilite';
import { ApiError } from '../api/client';
import { formaterEuros, formaterPourcent } from '../lib/format';
import type { EtatChargement } from '../lib/types';
import { COULEUR_POSITIF, COULEUR_NEGATIF, COULEUR_GRILLE, COULEUR_TEXTE_MUET } from '../lib/chartColors';
import { StatTuile } from '../components/StatTuile';

function cellule(valeur: number, accentuerNegatif = false): string {
  if (accentuerNegatif && valeur < 0) return 'px-3 py-2 text-right text-red-600';
  return 'px-3 py-2 text-right text-slate-700';
}

/** Annees affichées sous l'axe X : premier, dernier, et quelques jalons intermédiaires. */
function anneesJalons(n: number): Set<number> {
  if (n <= 6) return new Set(Array.from({ length: n }, (_, i) => i));
  const pas = Math.ceil(n / 6);
  const jalons = new Set<number>();
  for (let i = 0; i < n; i += pas) jalons.add(i);
  jalons.add(n - 1);
  return jalons;
}

function GraphiqueCashFlow({ lignes }: { lignes: LigneProjectionResponse[] }) {
  const [survole, setSurvole] = useState<number | null>(null);
  const largeur = 720;
  const hauteur = 200;
  const marge = { haut: 10, droite: 12, bas: 22, gauche: 12 };
  const largeurTrace = largeur - marge.gauche - marge.droite;
  const hauteurTrace = hauteur - marge.haut - marge.bas;
  const baseline = marge.haut + hauteurTrace / 2;

  const valeurs = lignes.map((l) => l.cashFlowApresFinancementApresImpotEnCentimes);
  const maxAbs = Math.max(1, ...valeurs.map((v) => Math.abs(v)));
  const n = lignes.length;
  const largeurCreneau = largeurTrace / n;
  const largeurBarre = Math.max(1, Math.min(24, largeurCreneau - 2));
  const aDeuxPolarites = valeurs.some((v) => v > 0) && valeurs.some((v) => v < 0);
  const jalons = anneesJalons(n);

  return (
    <div className="rounded-md border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-700">Évolution du cash-flow net</h3>
        {aDeuxPolarites && (
          <div className="flex items-center gap-3 text-xs text-slate-500">
            <span className="flex items-center gap-1">
              <span className="inline-block h-2 w-2 rounded-full" style={{ background: COULEUR_POSITIF }} />
              Positif
            </span>
            <span className="flex items-center gap-1">
              <span className="inline-block h-2 w-2 rounded-full" style={{ background: COULEUR_NEGATIF }} />
              Négatif
            </span>
          </div>
        )}
      </div>
      <svg
        viewBox={`0 0 ${largeur} ${hauteur}`}
        className="mt-2 w-full"
        role="img"
        aria-label="Évolution du cash-flow net après financement et après impôt, par année"
      >
        <line
          x1={marge.gauche} y1={baseline} x2={largeur - marge.droite} y2={baseline}
          stroke={COULEUR_GRILLE} strokeWidth={1}
        />
        {lignes.map((l, i) => {
          const v = l.cashFlowApresFinancementApresImpotEnCentimes;
          const x = marge.gauche + i * largeurCreneau + (largeurCreneau - largeurBarre) / 2;
          const h = (Math.abs(v) / maxAbs) * (hauteurTrace / 2 - 2);
          const y = v >= 0 ? baseline - h : baseline;
          const couleur = v >= 0 ? COULEUR_POSITIF : COULEUR_NEGATIF;
          return (
            <rect
              key={l.annee}
              x={x} y={y} width={largeurBarre} height={Math.max(h, 1)} rx={2}
              fill={couleur}
              opacity={survole === null || survole === i ? 1 : 0.5}
              tabIndex={0}
              role="img"
              aria-label={`Année ${l.annee} : cash-flow net ${formaterEuros(v)}`}
              onMouseEnter={() => setSurvole(i)}
              onMouseLeave={() => setSurvole(null)}
              onFocus={() => setSurvole(i)}
              onBlur={() => setSurvole(null)}
            >
              <title>{`Année ${l.annee} : ${formaterEuros(v)}`}</title>
            </rect>
          );
        })}
        {lignes.map((l, i) =>
          jalons.has(i) ? (
            <text
              key={l.annee}
              x={marge.gauche + i * largeurCreneau + largeurCreneau / 2}
              y={hauteur - 6}
              textAnchor="middle"
              fontSize={10}
              fill={COULEUR_TEXTE_MUET}
            >
              {l.annee}
            </text>
          ) : null,
        )}
      </svg>
      {survole !== null && (
        <p className="mt-1 text-xs text-slate-500">
          Année {lignes[survole].annee} :{' '}
          <span className="font-medium text-slate-800">
            {formaterEuros(lignes[survole].cashFlowApresFinancementApresImpotEnCentimes)}
          </span>
        </p>
      )}
    </div>
  );
}

function GraphiqueCapitalRestantDu({ lignes }: { lignes: LigneProjectionResponse[] }) {
  const [survole, setSurvole] = useState<number | null>(null);
  const largeur = 720;
  const hauteur = 160;
  const marge = { haut: 16, droite: 16, bas: 22, gauche: 12 };
  const largeurTrace = largeur - marge.gauche - marge.droite;
  const hauteurTrace = hauteur - marge.haut - marge.bas;

  const n = lignes.length;
  const maxValeur = Math.max(1, ...lignes.map((l) => l.capitalRestantDuFinAnneeEnCentimes));
  const jalons = anneesJalons(n);

  const points = lignes.map((l, i) => ({
    x: marge.gauche + (n === 1 ? 0 : (i / (n - 1)) * largeurTrace),
    y: marge.haut + hauteurTrace - (l.capitalRestantDuFinAnneeEnCentimes / maxValeur) * hauteurTrace,
    ligne: l,
  }));
  const chemin = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
  const dernier = points[points.length - 1];

  return (
    <div className="rounded-md border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-700">Évolution du capital restant dû</h3>
      <svg
        viewBox={`0 0 ${largeur} ${hauteur}`}
        className="mt-2 w-full"
        role="img"
        aria-label="Évolution du capital restant dû par année"
      >
        <line
          x1={marge.gauche} y1={marge.haut + hauteurTrace} x2={largeur - marge.droite} y2={marge.haut + hauteurTrace}
          stroke={COULEUR_GRILLE} strokeWidth={1}
        />
        <path d={chemin} fill="none" stroke={COULEUR_POSITIF} strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />
        <circle cx={dernier.x} cy={dernier.y} r={4} fill={COULEUR_POSITIF} stroke="#ffffff" strokeWidth={2} />
        <text x={dernier.x} y={dernier.y - 10} textAnchor="end" fontSize={11} fill="#52514e">
          {formaterEuros(dernier.ligne.capitalRestantDuFinAnneeEnCentimes)}
        </text>
        {points.map((p, i) => (
          <circle
            key={p.ligne.annee}
            cx={p.x} cy={p.y} r={9} fill="transparent"
            tabIndex={0}
            role="img"
            aria-label={`Année ${p.ligne.annee} : capital restant dû ${formaterEuros(p.ligne.capitalRestantDuFinAnneeEnCentimes)}`}
            onMouseEnter={() => setSurvole(i)}
            onMouseLeave={() => setSurvole(null)}
            onFocus={() => setSurvole(i)}
            onBlur={() => setSurvole(null)}
          >
            <title>{`Année ${p.ligne.annee} : ${formaterEuros(p.ligne.capitalRestantDuFinAnneeEnCentimes)}`}</title>
          </circle>
        ))}
        {points.map((p, i) =>
          jalons.has(i) ? (
            <text key={p.ligne.annee} x={p.x} y={hauteur - 6} textAnchor="middle" fontSize={10} fill={COULEUR_TEXTE_MUET}>
              {p.ligne.annee}
            </text>
          ) : null,
        )}
      </svg>
      {survole !== null && (
        <p className="mt-1 text-xs text-slate-500">
          Année {lignes[survole].annee} :{' '}
          <span className="font-medium text-slate-800">
            {formaterEuros(lignes[survole].capitalRestantDuFinAnneeEnCentimes)}
          </span>
        </p>
      )}
    </div>
  );
}

export function SimulationRentabiliteDetailPage({
  simulationId,
  onDupliquer,
  onModifier,
  onRetour,
}: {
  simulationId: string;
  onDupliquer: (simulation: SimulationRentabiliteResponse) => void;
  /** Édite ce scénario en place (même id) — distinct de dupliquer, qui en crée un nouveau. */
  onModifier: (simulation: SimulationRentabiliteResponse) => void;
  onRetour: () => void;
}) {
  const { session } = useAuth();
  const [simulation, setSimulation] = useState<SimulationRentabiliteResponse | null>(null);
  const [etat, setEtat] = useState<EtatChargement>('chargement');
  const [detailComplet, setDetailComplet] = useState(false);

  const [historiqueOuvert, setHistoriqueOuvert] = useState(false);
  const [historique, setHistorique] = useState<SimulationRentabiliteResponse[] | null>(null);
  const [historiqueEtat, setHistoriqueEtat] = useState<EtatChargement>('chargement');
  const [versionEnCoursDeRetour, setVersionEnCoursDeRetour] = useState<string | null>(null);
  const [erreurHistorique, setErreurHistorique] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    obtenirDetailSimulation(simulationId, session.token)
      .then((data) => {
        setSimulation(data);
        setEtat('pret');
      })
      .catch(() => setEtat('erreur'));
  }, [session, simulationId]);

  function basculerHistorique() {
    setHistoriqueOuvert((v) => !v);
    if (historique === null && session) {
      setHistoriqueEtat('chargement');
      obtenirHistoriqueSimulation(simulationId, session.token)
        .then((data) => {
          setHistorique(data);
          setHistoriqueEtat('pret');
        })
        .catch(() => setHistoriqueEtat('erreur'));
    }
  }

  async function revenirACetteVersion(version: SimulationRentabiliteResponse) {
    if (!session) return;
    setVersionEnCoursDeRetour(version.simuleLe);
    setErreurHistorique(null);
    try {
      const miseAJour = await modifierSimulation(simulationId, payloadDepuisSimulation(version), session.token);
      setSimulation(miseAJour);
      setHistorique(null);
      setHistoriqueOuvert(false);
    } catch (err) {
      setErreurHistorique(err instanceof ApiError ? err.message : 'Impossible de revenir à cette version.');
    } finally {
      setVersionEnCoursDeRetour(null);
    }
  }

  if (etat === 'chargement') {
    return <p className="text-sm text-slate-500">Chargement…</p>;
  }
  if (etat === 'erreur' || !simulation) {
    return <p className="text-sm text-red-600">Impossible de charger cette simulation.</p>;
  }

  const financementEnCentimes = simulation.coutTotalAcquisitionEnCentimes - simulation.apportPersonnelEnCentimes;
  // Basé sur le montant emprunté, pas sur capitalRestantDuFinAnneeEnCentimes : un prêt intégralement
  // remboursé dès la première année (durée courte) aurait un capital restant dû nul sur toutes les
  // années projetées, alors que le financement à crédit a bien existé.
  const aUnEmprunt = financementEnCentimes > 0;

  // Bandeau du haut : vue mensuelle année 1, avant impôt (aligné sur l'exploitation courante du
  // bien, indépendamment de la fiscalité). Le loyer/les charges annuels sont déjà nets de la
  // vacance locative simulée (§11 du slice).
  const annee1 = simulation.projectionAnnuelle[0];
  const loyerMensuelEnCentimes = annee1 ? annee1.loyerBrutAnnuelEnCentimes / 12 : 0;
  const chargesMensuellesEnCentimes = annee1
    ? (annee1.loyerBrutAnnuelEnCentimes - annee1.cashFlowApresFinancementAvantImpotEnCentimes) / 12
    : 0;
  const cashFlowMensuelEnCentimes = annee1 ? annee1.cashFlowApresFinancementAvantImpotEnCentimes / 12 : 0;

  return (
    <section className="mx-auto max-w-5xl">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold tracking-tight text-slate-900">
            {simulation.nomScenario}
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            {LIBELLES_REGIME[simulation.regimeFiscal]} · TMI {simulation.tmiFoyerPourcent} % ·
            Horizon {simulation.horizonAnnees} an{simulation.horizonAnnees > 1 ? 's' : ''}
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-4">
          <button
            type="button"
            onClick={() => onModifier(simulation)}
            className="text-sm font-medium text-emerald-700 hover:text-emerald-800 hover:underline"
          >
            Modifier ce scénario
          </button>
          <button
            type="button"
            onClick={() => onDupliquer(simulation)}
            className="text-sm font-medium text-emerald-700 hover:text-emerald-800 hover:underline"
          >
            Dupliquer ce scénario
          </button>
          <button
            type="button"
            onClick={onRetour}
            className="text-sm text-slate-500 hover:text-slate-800"
          >
            ← Retour
          </button>
        </div>
      </div>

      <div className="mb-3 grid grid-cols-2 gap-3 sm:grid-cols-5">
        <StatTuile libelle="Total des loyers (mois)" valeur={formaterEuros(loyerMensuelEnCentimes)} />
        <StatTuile libelle="Charges + financement (mois)" valeur={formaterEuros(chargesMensuellesEnCentimes)} />
        <StatTuile
          libelle="Cash-flow (mois)"
          valeur={formaterEuros(cashFlowMensuelEnCentimes)}
          negatif={cashFlowMensuelEnCentimes < 0}
        />
        <StatTuile libelle="Rendement brut" valeur={formaterPourcent(annee1?.rendementBrutPourcent ?? null)} />
        <StatTuile libelle="Rendement net-net" valeur={formaterPourcent(annee1?.rendementNetNetPourcent ?? null)} />
      </div>
      <div className="mb-6 grid grid-cols-3 gap-3">
        <StatTuile libelle="Coût total d'acquisition" valeur={formaterEuros(simulation.coutTotalAcquisitionEnCentimes)} />
        <StatTuile libelle="Apport personnel" valeur={formaterEuros(simulation.apportPersonnelEnCentimes)} />
        <StatTuile libelle="Financé à crédit" valeur={formaterEuros(financementEnCentimes)} />
      </div>

      <div className={`mb-6 grid gap-4 ${aUnEmprunt ? 'sm:grid-cols-2' : ''}`}>
        <GraphiqueCashFlow lignes={simulation.projectionAnnuelle} />
        {aUnEmprunt && <GraphiqueCapitalRestantDu lignes={simulation.projectionAnnuelle} />}
      </div>

      <div className="overflow-x-auto rounded-md border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr className="[&>th]:px-3 [&>th]:py-2 [&>th]:text-right [&>th]:font-medium">
              <th className="text-left">Année</th>
              <th>Loyer brut</th>
              <th>Charges</th>
              <th>Cash-flow net</th>
              <th>Rendement net-net</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {simulation.projectionAnnuelle.map((l) => (
              <tr key={l.annee} className="hover:bg-slate-50">
                <td className="px-3 py-2 font-medium text-slate-900">{l.annee}</td>
                <td className={cellule(l.loyerBrutAnnuelEnCentimes)}>{formaterEuros(l.loyerBrutAnnuelEnCentimes)}</td>
                <td className={cellule(l.chargesNonRecuperablesAnnuellesEnCentimes)}>
                  {formaterEuros(l.chargesNonRecuperablesAnnuellesEnCentimes)}
                </td>
                <td className={`${cellule(l.cashFlowApresFinancementApresImpotEnCentimes, true)} font-medium`}>
                  {formaterEuros(l.cashFlowApresFinancementApresImpotEnCentimes)}
                </td>
                <td className={`${cellule(l.rendementNetNetPourcent)} font-medium`}>
                  {formaterPourcent(l.rendementNetNetPourcent)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <button
        type="button"
        onClick={() => setDetailComplet((v) => !v)}
        className="mt-3 text-sm font-medium text-slate-600 hover:text-slate-900 hover:underline"
      >
        {detailComplet ? '− Masquer le détail' : '+ Afficher le détail (financement, fiscalité, amortissement)'}
      </button>

      {detailComplet && (
        <div className="mt-3 overflow-x-auto rounded-md border border-slate-200 bg-white shadow-sm">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th rowSpan={2} className="px-3 py-2 text-left align-bottom">Année</th>
                <th colSpan={2} className="border-l border-slate-200 px-3 py-1 text-center">Revenus &amp; charges</th>
                <th colSpan={4} className="border-l border-slate-200 px-3 py-1 text-center">Financement</th>
                <th colSpan={5} className="border-l border-slate-200 px-3 py-1 text-center">Fiscalité</th>
                <th colSpan={3} className="border-l border-slate-200 px-3 py-1 text-center">Cash-flow</th>
                <th colSpan={4} className="border-l border-slate-200 px-3 py-1 text-center">Rendements</th>
              </tr>
              <tr className="[&>th]:px-3 [&>th]:py-2 [&>th]:text-right [&>th]:font-medium">
                <th className="border-l border-slate-200">Loyer brut</th>
                <th>Charges</th>
                <th className="border-l border-slate-200">Intérêts</th>
                <th>Capital remb.</th>
                <th>Assurance</th>
                <th>Capital restant dû</th>
                <th className="border-l border-slate-200">Amort. bâti</th>
                <th>Amort. mobilier</th>
                <th>Résultat imposable</th>
                <th>Solde déficit</th>
                <th>Impôt estimé</th>
                <th className="border-l border-slate-200">Avant financement</th>
                <th>Après financement (avant IR)</th>
                <th>Net (après IR)</th>
                <th className="border-l border-slate-200">Brut</th>
                <th>Net</th>
                <th>Net-net</th>
                <th>Fonds propres</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {simulation.projectionAnnuelle.map((l) => (
                <tr key={l.annee} className="hover:bg-slate-50">
                  <td className="px-3 py-2 font-medium text-slate-900">{l.annee}</td>
                  <td className={`${cellule(l.loyerBrutAnnuelEnCentimes)} border-l border-slate-200`}>
                    {formaterEuros(l.loyerBrutAnnuelEnCentimes)}
                  </td>
                  <td className={cellule(l.chargesNonRecuperablesAnnuellesEnCentimes)}>
                    {formaterEuros(l.chargesNonRecuperablesAnnuellesEnCentimes)}
                  </td>
                  <td className={`${cellule(l.interetsEmpruntAnnuelsEnCentimes)} border-l border-slate-200`}>
                    {formaterEuros(l.interetsEmpruntAnnuelsEnCentimes)}
                  </td>
                  <td className={cellule(l.capitalRembourseAnnuelEnCentimes)}>
                    {formaterEuros(l.capitalRembourseAnnuelEnCentimes)}
                  </td>
                  <td className={cellule(l.assuranceEmprunteurAnnuelleEnCentimes)}>
                    {formaterEuros(l.assuranceEmprunteurAnnuelleEnCentimes)}
                  </td>
                  <td className={cellule(l.capitalRestantDuFinAnneeEnCentimes)}>
                    {formaterEuros(l.capitalRestantDuFinAnneeEnCentimes)}
                  </td>
                  <td className={`${cellule(l.amortissementBatiAnnuelEnCentimes)} border-l border-slate-200`}>
                    {formaterEuros(l.amortissementBatiAnnuelEnCentimes)}
                  </td>
                  <td className={cellule(l.amortissementMobilierAnnuelEnCentimes)}>
                    {formaterEuros(l.amortissementMobilierAnnuelEnCentimes)}
                  </td>
                  <td className={cellule(l.resultatImposableEnCentimes)}>
                    {formaterEuros(l.resultatImposableEnCentimes)}
                  </td>
                  <td className={cellule(l.soldeDeficitFoncierReportableFinAnneeEnCentimes + l.soldeDeficitBicReportableFinAnneeEnCentimes)}>
                    {formaterEuros(l.soldeDeficitFoncierReportableFinAnneeEnCentimes + l.soldeDeficitBicReportableFinAnneeEnCentimes)}
                  </td>
                  <td className={cellule(l.impotEstimeEnCentimes, true)}>
                    {formaterEuros(l.impotEstimeEnCentimes)}
                  </td>
                  <td className={`${cellule(l.cashFlowAvantFinancementAvantImpotEnCentimes, true)} border-l border-slate-200`}>
                    {formaterEuros(l.cashFlowAvantFinancementAvantImpotEnCentimes)}
                  </td>
                  <td className={cellule(l.cashFlowApresFinancementAvantImpotEnCentimes, true)}>
                    {formaterEuros(l.cashFlowApresFinancementAvantImpotEnCentimes)}
                  </td>
                  <td className={`${cellule(l.cashFlowApresFinancementApresImpotEnCentimes, true)} font-medium`}>
                    {formaterEuros(l.cashFlowApresFinancementApresImpotEnCentimes)}
                  </td>
                  <td className={`${cellule(l.rendementBrutPourcent)} border-l border-slate-200`}>
                    {formaterPourcent(l.rendementBrutPourcent)}
                  </td>
                  <td className={cellule(l.rendementNetPourcent)}>{formaterPourcent(l.rendementNetPourcent)}</td>
                  <td className={`${cellule(l.rendementNetNetPourcent)} font-medium`}>
                    {formaterPourcent(l.rendementNetNetPourcent)}
                  </td>
                  <td className={cellule(l.rendementSurFondsPropresPourcent ?? 0)}>
                    {formaterPourcent(l.rendementSurFondsPropresPourcent)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <p className="mt-3 text-xs text-slate-400">
        « Solde déficit » cumule le déficit foncier et le déficit BIC reportables restant à
        absorber sur les résultats futurs (D11, D21 du slice). Version calculée le{' '}
        {new Date(simulation.simuleLe).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })}.
      </p>

      {/* Historique des versions : la modification est event-sourcée (append-only) — chaque
          version reste consultable et on peut y revenir sans jamais rien effacer. */}
      <button
        type="button"
        onClick={basculerHistorique}
        className="mt-3 block text-sm font-medium text-slate-600 hover:text-slate-900 hover:underline"
      >
        {historiqueOuvert ? '− Masquer' : '+ Afficher'} l'historique des versions
      </button>

      {historiqueOuvert && (
        <div className="mt-3 overflow-x-auto rounded-md border border-slate-200 bg-white shadow-sm">
          {historiqueEtat === 'chargement' && (
            <p className="p-4 text-sm text-slate-500">Chargement de l'historique…</p>
          )}
          {historiqueEtat === 'erreur' && (
            <p className="p-4 text-sm text-red-600">Impossible de charger l'historique.</p>
          )}
          {historiqueEtat === 'pret' && historique && (
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-4 py-2 text-left font-medium text-slate-600">Version</th>
                  <th className="px-4 py-2 text-left font-medium text-slate-600">Calculée le</th>
                  <th className="px-4 py-2 text-left font-medium text-slate-600">Nom</th>
                  <th className="px-4 py-2 text-right font-medium text-slate-600">Rdt. net-net (an 1)</th>
                  <th className="px-4 py-2 text-right font-medium text-slate-600"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {[...historique].reverse().map((version, indexInverse) => {
                  const numeroVersion = historique.length - indexInverse;
                  const estVersionActuelle = numeroVersion === historique.length;
                  return (
                    <tr key={version.simuleLe} className="hover:bg-slate-50">
                      <td className="px-4 py-2 font-medium text-slate-900">
                        {numeroVersion}
                        {estVersionActuelle && (
                          <span className="ml-2 rounded bg-emerald-50 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-emerald-700">
                            Actuelle
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-2 text-slate-600">
                        {new Date(version.simuleLe).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })}
                      </td>
                      <td className="px-4 py-2 text-slate-700">{version.nomScenario}</td>
                      <td className="px-4 py-2 text-right text-slate-700">
                        {formaterPourcent(version.projectionAnnuelle[0]?.rendementNetNetPourcent ?? null)}
                      </td>
                      <td className="px-4 py-2 text-right">
                        {!estVersionActuelle && (
                          <button
                            type="button"
                            disabled={versionEnCoursDeRetour !== null}
                            onClick={() => revenirACetteVersion(version)}
                            className="text-sm font-medium text-emerald-700 hover:text-emerald-800 hover:underline disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {versionEnCoursDeRetour === version.simuleLe ? 'Retour en cours…' : 'Revenir à cette version'}
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      )}
      {erreurHistorique && (
        <p className="mt-2 text-xs text-red-600">{erreurHistorique}</p>
      )}
    </section>
  );
}
