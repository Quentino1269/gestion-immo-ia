import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import {
  obtenirFicheBien,
  obtenirPortefeuille,
  type FicheBienResponse,
  type LignePortefeuilleResponse,
} from '../api/biens';
import { lancerSimulation, type RegimeFiscal, type SimulationRentabiliteResponse } from '../api/rentabilite';
import { ApiError } from '../api/client';
import type { EtatChargement } from '../lib/types';

type LigneRevenuUI = {
  bienSourceId: string;
  libelle: string;
  loyerEuros: string;
  chargesEuros: string;
};

function eurosVersCentimes(valeur: string): number {
  const n = parseFloat(valeur.replace(',', '.'));
  return isNaN(n) ? 0 : Math.round(n * 100);
}

function pourcent(valeur: string): number {
  const n = parseFloat(valeur.replace(',', '.'));
  return isNaN(n) ? 0 : n;
}

const REGIMES_NU: { valeur: RegimeFiscal; libelle: string }[] = [
  { valeur: 'MICRO_FONCIER', libelle: 'Micro-foncier (abattement 30 %)' },
  { valeur: 'REEL_FONCIER', libelle: 'Réel foncier (charges réelles)' },
];
const REGIMES_MEUBLE: { valeur: RegimeFiscal; libelle: string }[] = [
  { valeur: 'MICRO_BIC', libelle: 'Micro-BIC (abattement 50 %)' },
  { valeur: 'REEL_BIC', libelle: 'Réel BIC — LMNP (charges réelles + amortissement)' },
];

/**
 * Construit les lignes de revenu simulé pour le bien racine (ou ses chambres actives), en
 * reprenant les valeurs d'un scénario source si fourni (duplication), sinon les valeurs
 * courantes du bien/de la chambre.
 */
function construireLignesRevenu(
  chambresActives: LignePortefeuilleResponse[],
  fiche: FicheBienResponse,
  bienId: string,
  parLigneSource?: Map<string, { loyerSimuleMensuelEnCentimes: number; chargesSimuleesMensuellesEnCentimes: number }>,
): LigneRevenuUI[] {
  const items =
    chambresActives.length > 0
      ? chambresActives.map((c) => ({
          id: c.bienId,
          libelle: c.libelleCommercial,
          loyer: c.loyerHorsChargesEnCentimes,
          charges: c.chargesEnCentimes,
        }))
      : [
          {
            id: bienId,
            libelle: fiche.libelleCommercial,
            loyer: fiche.loyerHorsChargesEnCentimes,
            charges: fiche.chargesEnCentimes,
          },
        ];

  return items.map(({ id, libelle, loyer, charges }) => {
    const source = parLigneSource?.get(id);
    return {
      bienSourceId: id,
      libelle,
      loyerEuros: ((source?.loyerSimuleMensuelEnCentimes ?? loyer) / 100).toFixed(2),
      chargesEuros: ((source?.chargesSimuleesMensuellesEnCentimes ?? charges) / 100).toFixed(2),
    };
  });
}

export function NouvelleSimulationRentabilitePage({
  bienId,
  simulationSource,
  onCree,
  onRetour,
}: {
  bienId: string;
  /** Simulation existante à dupliquer : préremplit le formulaire avec ses valeurs. */
  simulationSource?: SimulationRentabiliteResponse;
  onCree: (simulationId: string) => void;
  onRetour: () => void;
}) {
  const { session } = useAuth();
  const [etat, setEtat] = useState<EtatChargement>('chargement');
  const [bien, setBien] = useState<FicheBienResponse | null>(null);
  const [lignesRevenu, setLignesRevenu] = useState<LigneRevenuUI[]>([]);

  const [nomScenario, setNomScenario] = useState('');
  const [regimeFiscal, setRegimeFiscal] = useState<RegimeFiscal>('MICRO_FONCIER');
  const [tmiFoyerPourcent, setTmiFoyerPourcent] = useState('30');
  const [horizonAnnees, setHorizonAnnees] = useState('20');

  const [prixAchat, setPrixAchat] = useState('');
  const [fraisNotaire, setFraisNotaire] = useState('0');
  const [fraisAgence, setFraisAgence] = useState('0');
  const [travauxAcquisition, setTravauxAcquisition] = useState('0');
  const [fraisDossierBancaire, setFraisDossierBancaire] = useState('0');

  const coutTotalAcquisitionEnCentimes = useMemo(
    () =>
      eurosVersCentimes(prixAchat) +
      eurosVersCentimes(fraisNotaire) +
      eurosVersCentimes(fraisAgence) +
      eurosVersCentimes(travauxAcquisition) +
      eurosVersCentimes(fraisDossierBancaire),
    [prixAchat, fraisNotaire, fraisAgence, travauxAcquisition, fraisDossierBancaire],
  );

  const [financeACredit, setFinanceACredit] = useState(false);
  const [montantEmprunte, setMontantEmprunte] = useState('0');
  const [tauxAnnuel, setTauxAnnuel] = useState('3.5');
  const [dureeAnnees, setDureeAnnees] = useState('20');
  const [tauxAssuranceEmprunteur, setTauxAssuranceEmprunteur] = useState('0.30');

  const [quotePartTerrain, setQuotePartTerrain] = useState('15');
  const [quotePartMobilier, setQuotePartMobilier] = useState('5');
  const [dureeAmortissementBati, setDureeAmortissementBati] = useState('25');
  const [dureeAmortissementMobilier, setDureeAmortissementMobilier] = useState('7');

  const [taxeFonciere, setTaxeFonciere] = useState('0');
  const [assurancePno, setAssurancePno] = useState('0');
  const [assuranceLoyersImpayes, setAssuranceLoyersImpayes] = useState('0');
  const [fraisGestionLocative, setFraisGestionLocative] = useState('0');
  const [provisionTravauxAnnuelle, setProvisionTravauxAnnuelle] = useState('0');
  const [fraisComptabiliteAnnuel, setFraisComptabiliteAnnuel] = useState('0');
  const [chargesCoproprieteNonRecuperables, setChargesCoproprieteNonRecuperables] = useState('0');

  const [tauxVacanceLocative, setTauxVacanceLocative] = useState('0');
  const [tauxIndexationLoyer, setTauxIndexationLoyer] = useState('2');
  const [tauxIndexationCharges, setTauxIndexationCharges] = useState('2');

  const [loyerNegocieEuros, setLoyerNegocieEuros] = useState('');

  const [enSoumission, setEnSoumission] = useState(false);
  const [erreurGlobale, setErreurGlobale] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    Promise.all([obtenirFicheBien(bienId, session.token), obtenirPortefeuille(session.token)])
      .then(([fiche, portefeuille]) => {
        setBien(fiche);
        const chambresActives = portefeuille.filter((l) => l.bienParentId === bienId);

        if (simulationSource) {
          setNomScenario(`${simulationSource.nomScenario} (copie)`);
          setRegimeFiscal(simulationSource.regimeFiscal);
          setTmiFoyerPourcent(String(simulationSource.tmiFoyerPourcent));
          setHorizonAnnees(String(simulationSource.horizonAnnees));
          setPrixAchat((simulationSource.acquisition.prixAchatEnCentimes / 100).toFixed(2));
          setFraisNotaire((simulationSource.acquisition.fraisNotaireEnCentimes / 100).toFixed(2));
          setFraisAgence((simulationSource.acquisition.fraisAgenceEnCentimes / 100).toFixed(2));
          setTravauxAcquisition((simulationSource.acquisition.travauxAlAcquisitionEnCentimes / 100).toFixed(2));
          setFraisDossierBancaire((simulationSource.acquisition.fraisDossierBancaireEnCentimes / 100).toFixed(2));
          setFinanceACredit(simulationSource.financement.montantEmprunteEnCentimes > 0);
          setMontantEmprunte((simulationSource.financement.montantEmprunteEnCentimes / 100).toFixed(2));
          setTauxAnnuel(String(simulationSource.financement.tauxAnnuelPourcent));
          setDureeAnnees(String(simulationSource.financement.dureeAnnees || 20));
          setTauxAssuranceEmprunteur(String(simulationSource.financement.tauxAssuranceEmprunteurPourcent));
          setQuotePartTerrain(String(simulationSource.amortissement.quotePartTerrainPourcent));
          setQuotePartMobilier(String(simulationSource.amortissement.quotePartMobilierPourcent));
          setDureeAmortissementBati(String(simulationSource.amortissement.dureeAmortissementBatiAnnees));
          setDureeAmortissementMobilier(String(simulationSource.amortissement.dureeAmortissementMobilierAnnees));
          setTaxeFonciere((simulationSource.chargesRecurrentes.taxeFonciereEnCentimes / 100).toFixed(2));
          setAssurancePno((simulationSource.chargesRecurrentes.assurancePnoEnCentimes / 100).toFixed(2));
          setAssuranceLoyersImpayes(
            (simulationSource.chargesRecurrentes.assuranceLoyersImpayesEnCentimes / 100).toFixed(2),
          );
          setFraisGestionLocative(String(simulationSource.chargesRecurrentes.fraisGestionLocativePourcentLoyer));
          setProvisionTravauxAnnuelle(
            (simulationSource.chargesRecurrentes.provisionTravauxAnnuelleEnCentimes / 100).toFixed(2),
          );
          setFraisComptabiliteAnnuel(
            (simulationSource.chargesRecurrentes.fraisComptabiliteAnnuelEnCentimes / 100).toFixed(2),
          );
          setChargesCoproprieteNonRecuperables(
            (simulationSource.chargesRecurrentes.chargesCoproprieteNonRecuperablesEnCentimes / 100).toFixed(2),
          );
          setTauxVacanceLocative(String(simulationSource.hypothesesEvolution.tauxVacanceLocativePourcent));
          setTauxIndexationLoyer(String(simulationSource.hypothesesEvolution.tauxIndexationLoyerPourcent));
          setTauxIndexationCharges(String(simulationSource.hypothesesEvolution.tauxIndexationChargesPourcent));

          const parLigneSource = new Map(
            simulationSource.revenusLocatifsSimules.map((l) => [l.bienSourceId, l]),
          );
          setLignesRevenu(construireLignesRevenu(chambresActives, fiche, bienId, parLigneSource));
        } else {
          setRegimeFiscal(fiche.meuble ? 'MICRO_BIC' : 'MICRO_FONCIER');
          // Le mode de financement n'est pas encore choisi à ce stade (chargement initial) : le nom
          // par défaut ne peut pas encore refléter "crédit" vs "cash", l'utilisateur peut le modifier.
          setNomScenario('Scénario cash');
          setLignesRevenu(construireLignesRevenu(chambresActives, fiche, bienId));
        }
        setEtat('pret');
      })
      .catch(() => setEtat('erreur'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session, bienId, simulationSource]);

  function modifierLigneRevenu(index: number, champ: 'loyerEuros' | 'chargesEuros', valeur: string) {
    setLignesRevenu((lignes) =>
      lignes.map((l, i) => (i === index ? { ...l, [champ]: valeur } : l)),
    );
  }

  // --- Simulateur de négociation : loyer -> prix d'achat, à rendement brut constant ---

  const loyerActuelMensuelEnCentimes = useMemo(
    () => lignesRevenu.reduce((somme, l) => somme + eurosVersCentimes(l.loyerEuros), 0),
    [lignesRevenu],
  );

  const negociation = useMemo(() => {
    const loyerNegocie = eurosVersCentimes(loyerNegocieEuros);
    if (loyerActuelMensuelEnCentimes <= 0 || loyerNegocie <= 0 || coutTotalAcquisitionEnCentimes <= 0) {
      return null;
    }
    const fraisFixes =
      eurosVersCentimes(fraisNotaire) +
      eurosVersCentimes(fraisAgence) +
      eurosVersCentimes(travauxAcquisition) +
      eurosVersCentimes(fraisDossierBancaire);
    // rendementBrut = (loyerMensuel × 12) / coûtTotal, constant : le coût total varie donc dans le
    // même rapport que le loyer.
    const nouveauCoutTotal = Math.round(
      coutTotalAcquisitionEnCentimes * (loyerNegocie / loyerActuelMensuelEnCentimes),
    );
    return { nouveauPrixAchat: nouveauCoutTotal - fraisFixes };
  }, [
    loyerActuelMensuelEnCentimes,
    loyerNegocieEuros,
    coutTotalAcquisitionEnCentimes,
    fraisNotaire,
    fraisAgence,
    travauxAcquisition,
    fraisDossierBancaire,
  ]);

  function appliquerNegociation() {
    if (!negociation) return;
    const facteur = eurosVersCentimes(loyerNegocieEuros) / loyerActuelMensuelEnCentimes;
    setPrixAchat((negociation.nouveauPrixAchat / 100).toFixed(2));
    setLignesRevenu((lignes) =>
      lignes.map((l) => ({
        ...l,
        loyerEuros: ((eurosVersCentimes(l.loyerEuros) * facteur) / 100).toFixed(2),
      })),
    );
    setLoyerNegocieEuros('');
  }

  async function soumettre(e: React.FormEvent) {
    e.preventDefault();
    if (!session) return;
    setEnSoumission(true);
    setErreurGlobale(null);

    try {
      const simulation = await lancerSimulation(
        bienId,
        {
          nomScenario,
          regimeFiscal,
          tmiFoyerPourcent: parseInt(tmiFoyerPourcent, 10),
          horizonAnnees: parseInt(horizonAnnees, 10) || 1,
          acquisition: {
            prixAchatEnCentimes: eurosVersCentimes(prixAchat),
            fraisNotaireEnCentimes: eurosVersCentimes(fraisNotaire),
            fraisAgenceEnCentimes: eurosVersCentimes(fraisAgence),
            travauxAlAcquisitionEnCentimes: eurosVersCentimes(travauxAcquisition),
            fraisDossierBancaireEnCentimes: eurosVersCentimes(fraisDossierBancaire),
          },
          financement: {
            montantEmprunteEnCentimes: financeACredit ? eurosVersCentimes(montantEmprunte) : 0,
            tauxAnnuelPourcent: financeACredit ? pourcent(tauxAnnuel) : 0,
            dureeAnnees: financeACredit ? parseInt(dureeAnnees, 10) || 1 : 0,
            tauxAssuranceEmprunteurPourcent: financeACredit ? pourcent(tauxAssuranceEmprunteur) : 0,
          },
          amortissement: {
            quotePartTerrainPourcent: pourcent(quotePartTerrain),
            quotePartMobilierPourcent: pourcent(quotePartMobilier),
            dureeAmortissementBatiAnnees: parseInt(dureeAmortissementBati, 10) || 1,
            dureeAmortissementMobilierAnnees: parseInt(dureeAmortissementMobilier, 10) || 1,
          },
          revenusLocatifsSimules: lignesRevenu.map((l) => ({
            bienSourceId: l.bienSourceId,
            loyerSimuleMensuelEnCentimes: eurosVersCentimes(l.loyerEuros),
            chargesSimuleesMensuellesEnCentimes: eurosVersCentimes(l.chargesEuros),
          })),
          chargesRecurrentes: {
            taxeFonciereEnCentimes: eurosVersCentimes(taxeFonciere),
            assurancePnoEnCentimes: eurosVersCentimes(assurancePno),
            assuranceLoyersImpayesEnCentimes: eurosVersCentimes(assuranceLoyersImpayes),
            fraisGestionLocativePourcentLoyer: pourcent(fraisGestionLocative),
            provisionTravauxAnnuelleEnCentimes: eurosVersCentimes(provisionTravauxAnnuelle),
            fraisComptabiliteAnnuelEnCentimes: eurosVersCentimes(fraisComptabiliteAnnuel),
            chargesCoproprieteNonRecuperablesEnCentimes: eurosVersCentimes(chargesCoproprieteNonRecuperables),
          },
          hypothesesEvolution: {
            tauxVacanceLocativePourcent: pourcent(tauxVacanceLocative),
            tauxIndexationLoyerPourcent: pourcent(tauxIndexationLoyer),
            tauxIndexationChargesPourcent: pourcent(tauxIndexationCharges),
          },
        },
        session.token,
      );
      onCree(simulation.simulationId);
    } catch (err) {
      if (err instanceof ApiError) {
        setErreurGlobale(err.message);
      } else {
        setErreurGlobale('Une erreur inattendue est survenue.');
      }
    } finally {
      setEnSoumission(false);
    }
  }

  if (etat === 'chargement') {
    return <p className="text-sm text-slate-500">Chargement…</p>;
  }
  if (etat === 'erreur' || !bien) {
    return <p className="text-sm text-red-600">Impossible de charger ce bien.</p>;
  }

  const regimesDisponibles = bien.meuble ? REGIMES_MEUBLE : REGIMES_NU;

  return (
    <section className="mx-auto max-w-lg">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-900">
          Simuler la rentabilité
        </h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-500 hover:text-slate-800"
        >
          ← Retour
        </button>
      </div>
      <p className="mb-6 text-sm text-slate-500">
        {bien.libelleCommercial} — {bien.adresse.commune}
      </p>

      {erreurGlobale && (
        <div className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
          {erreurGlobale}
        </div>
      )}

      <form onSubmit={soumettre} className="space-y-6">
        {/* Scénario et régime */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Scénario</legend>
          <div className="mt-4 space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Nom du scénario</label>
              <input
                type="text"
                value={nomScenario}
                onChange={(e) => setNomScenario(e.target.value)}
                required
                maxLength={100}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Régime fiscal</label>
                <select
                  value={regimeFiscal}
                  onChange={(e) => setRegimeFiscal(e.target.value as RegimeFiscal)}
                  className="mt-1 w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm"
                >
                  {regimesDisponibles.map((r) => (
                    <option key={r.valeur} value={r.valeur}>
                      {r.libelle}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">
                  Tranche marginale d'imposition
                </label>
                <select
                  value={tmiFoyerPourcent}
                  onChange={(e) => setTmiFoyerPourcent(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm"
                >
                  <option value="0">0 %</option>
                  <option value="11">11 %</option>
                  <option value="30">30 %</option>
                  <option value="41">41 %</option>
                  <option value="45">45 %</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">
                Horizon de la projection (années)
              </label>
              <input
                type="number"
                min={1}
                max={40}
                value={horizonAnnees}
                onChange={(e) => setHorizonAnnees(e.target.value)}
                required
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
        </fieldset>

        {/* Achat */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Achat</legend>
          <div className="mt-1 text-right text-xs text-slate-500">
            Total :{' '}
            <span className="font-medium text-slate-700">
              {(coutTotalAcquisitionEnCentimes / 100).toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' })}
            </span>
          </div>
          <div className="mt-3 space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Prix d'achat (€)</label>
              <input
                type="text"
                placeholder="200 000"
                value={prixAchat}
                onChange={(e) => setPrixAchat(e.target.value)}
                required
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Frais de notaire (€)</label>
                <input
                  type="text"
                  value={fraisNotaire}
                  onChange={(e) => setFraisNotaire(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Frais d'agence (€)</label>
                <input
                  type="text"
                  value={fraisAgence}
                  onChange={(e) => setFraisAgence(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Frais de dossier bancaire (€)</label>
                <input
                  type="text"
                  value={fraisDossierBancaire}
                  onChange={(e) => setFraisDossierBancaire(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
            </div>
          </div>
        </fieldset>

        {/* Travaux */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Travaux</legend>
          <div className="mt-4">
            <label className="block text-sm font-medium text-slate-700">Travaux à l'acquisition (€)</label>
            <input
              type="text"
              value={travauxAcquisition}
              onChange={(e) => setTravauxAcquisition(e.target.value)}
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
            />
            <p className="mt-1 text-xs text-slate-500">
              Montant unique intégré au coût total d'acquisition (D9 du slice).
            </p>
          </div>
        </fieldset>

        {/* Financement */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Financement</legend>
          <div className="mt-4 space-y-4">
            <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-slate-700">
              <input
                type="checkbox"
                checked={financeACredit}
                onChange={(e) => setFinanceACredit(e.target.checked)}
                className="rounded border-slate-300"
              />
              Financer à crédit (sinon achat cash)
            </label>

            {financeACredit && (
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Montant emprunté (€)</label>
                  <input
                    type="text"
                    value={montantEmprunte}
                    onChange={(e) => setMontantEmprunte(e.target.value)}
                    className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Durée (années)</label>
                  <input
                    type="number"
                    min={1}
                    value={dureeAnnees}
                    onChange={(e) => setDureeAnnees(e.target.value)}
                    className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Taux d'intérêt annuel (%)</label>
                  <input
                    type="text"
                    value={tauxAnnuel}
                    onChange={(e) => setTauxAnnuel(e.target.value)}
                    className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Assurance emprunteur (%/an)</label>
                  <input
                    type="text"
                    value={tauxAssuranceEmprunteur}
                    onChange={(e) => setTauxAssuranceEmprunteur(e.target.value)}
                    className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>
            )}
          </div>
        </fieldset>

        {/* Amortissement LMNP */}
        {regimeFiscal === 'REEL_BIC' && (
          <fieldset className="rounded-md border border-slate-200 p-4">
            <legend className="px-1 text-sm font-semibold text-slate-700">
              Amortissement (réel BIC / LMNP)
            </legend>
            <div className="mt-4 grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Quote-part terrain (%)</label>
                <input
                  type="text"
                  value={quotePartTerrain}
                  onChange={(e) => setQuotePartTerrain(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Quote-part mobilier (%)</label>
                <input
                  type="text"
                  value={quotePartMobilier}
                  onChange={(e) => setQuotePartMobilier(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Durée amort. bâti (années)</label>
                <input
                  type="number"
                  min={1}
                  value={dureeAmortissementBati}
                  onChange={(e) => setDureeAmortissementBati(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Durée amort. mobilier (années)</label>
                <input
                  type="number"
                  min={1}
                  value={dureeAmortissementMobilier}
                  onChange={(e) => setDureeAmortissementMobilier(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
            </div>
          </fieldset>
        )}

        {/* Loyers */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Loyers</legend>
          <div className="mt-4 space-y-4">
            {lignesRevenu.map((l, i) => (
              <div key={l.bienSourceId} className="rounded-md bg-slate-50 p-3">
                <p className="mb-2 text-sm font-medium text-slate-700">{l.libelle}</p>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs text-slate-500">Loyer simulé (€/mois)</label>
                    <input
                      type="text"
                      value={l.loyerEuros}
                      onChange={(e) => modifierLigneRevenu(i, 'loyerEuros', e.target.value)}
                      required
                      className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-500">Charges simulées (€/mois)</label>
                    <input
                      type="text"
                      value={l.chargesEuros}
                      onChange={(e) => modifierLigneRevenu(i, 'chargesEuros', e.target.value)}
                      className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </fieldset>

        {/* Simulateur de négociation */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Simulateur de négociation</legend>
          <p className="mt-1 text-xs text-slate-500">
            À rendement brut constant : si le loyer total change, quel prix d'achat faudrait-il négocier ?
          </p>
          <div className="mt-3 grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Loyer total mensuel actuel</label>
              <input
                type="text"
                disabled
                value={(loyerActuelMensuelEnCentimes / 100).toLocaleString('fr-FR', {
                  style: 'currency',
                  currency: 'EUR',
                })}
                className="mt-1 w-full rounded border border-slate-200 bg-slate-100 px-3 py-2 text-sm text-slate-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Nouveau loyer total mensuel (€)</label>
              <input
                type="text"
                value={loyerNegocieEuros}
                onChange={(e) => setLoyerNegocieEuros(e.target.value)}
                placeholder={(loyerActuelMensuelEnCentimes / 100).toFixed(2)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
          {negociation && (
            <div className="mt-4 rounded-md bg-slate-50 p-3">
              <p className="text-xs text-slate-500">Prix d'achat correspondant (rendement brut inchangé)</p>
              <p className="mt-1 text-lg font-semibold text-slate-900">
                {(negociation.nouveauPrixAchat / 100).toLocaleString('fr-FR', {
                  style: 'currency',
                  currency: 'EUR',
                })}
              </p>
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={() => setLoyerNegocieEuros('')}
                  className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-white"
                >
                  Réinitialiser
                </button>
                <button
                  type="button"
                  onClick={appliquerNegociation}
                  className="rounded border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-800 hover:bg-white"
                >
                  Appliquer à mon projet
                </button>
              </div>
            </div>
          )}
        </fieldset>

        {/* Charges */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Charges</legend>
          <div className="mt-4 grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Taxe foncière (€/an)</label>
              <input type="text" value={taxeFonciere} onChange={(e) => setTaxeFonciere(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Assurance PNO (€/an)</label>
              <input type="text" value={assurancePno} onChange={(e) => setAssurancePno(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Assurance loyers impayés (€/an)</label>
              <input type="text" value={assuranceLoyersImpayes} onChange={(e) => setAssuranceLoyersImpayes(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Gestion locative (% du loyer)</label>
              <input type="text" value={fraisGestionLocative} onChange={(e) => setFraisGestionLocative(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Provision travaux (€/an)</label>
              <input type="text" value={provisionTravauxAnnuelle} onChange={(e) => setProvisionTravauxAnnuelle(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Comptabilité (€/an)</label>
              <input type="text" value={fraisComptabiliteAnnuel} onChange={(e) => setFraisComptabiliteAnnuel(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-slate-700">Charges copropriété non récupérables (€/an)</label>
              <input type="text" value={chargesCoproprieteNonRecuperables} onChange={(e) => setChargesCoproprieteNonRecuperables(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
          </div>
        </fieldset>

        {/* Hypothèses d'évolution */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Hypothèses d'évolution</legend>
          <div className="mt-4 grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Vacance locative (%)</label>
              <input type="text" value={tauxVacanceLocative} onChange={(e) => setTauxVacanceLocative(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Indexation loyer (%/an)</label>
              <input type="text" value={tauxIndexationLoyer} onChange={(e) => setTauxIndexationLoyer(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Indexation charges (%/an)</label>
              <input type="text" value={tauxIndexationCharges} onChange={(e) => setTauxIndexationCharges(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm" />
            </div>
          </div>
        </fieldset>

        <button
          type="submit"
          disabled={enSoumission}
          className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {enSoumission ? 'Calcul en cours…' : 'Lancer la simulation'}
        </button>
      </form>
    </section>
  );
}
