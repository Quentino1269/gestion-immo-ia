import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import {
  obtenirFicheBien,
  obtenirPortefeuille,
  type FicheBienResponse,
  type LignePortefeuilleResponse,
} from '../api/biens';
import {
  lancerSimulation,
  modifierSimulation,
  type LancerSimulationRentabilitePayload,
  type RegimeFiscal,
  type SimulationRentabiliteResponse,
} from '../api/rentabilite';
import { ApiError } from '../api/client';
import type { EtatChargement } from '../lib/types';
import { calculerApercuAnnee1 } from '../lib/apercuRentabilite';
import { formaterEuros, formaterPourcent, eurosVersCentimes, pourcent } from '../lib/format';
import { COULEUR_POSITIF } from '../lib/chartColors';

type LigneRevenuUI = {
  bienSourceId: string;
  libelle: string;
  surfaceM2: number;
  loyerEuros: string;
  chargesEuros: string;
};

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
          surfaceM2: c.surfaceM2,
          loyer: c.loyerHorsChargesEnCentimes,
          charges: c.chargesEnCentimes,
        }))
      : [
          {
            id: bienId,
            libelle: fiche.libelleCommercial,
            surfaceM2: fiche.surfaceM2,
            loyer: fiche.loyerHorsChargesEnCentimes,
            charges: fiche.chargesEnCentimes,
          },
        ];

  return items.map(({ id, libelle, surfaceM2, loyer, charges }) => {
    const source = parLigneSource?.get(id);
    return {
      bienSourceId: id,
      libelle,
      surfaceM2,
      loyerEuros: ((source?.loyerSimuleMensuelEnCentimes ?? loyer) / 100).toFixed(2),
      chargesEuros: ((source?.chargesSimuleesMensuellesEnCentimes ?? charges) / 100).toFixed(2),
    };
  });
}

export function NouvelleSimulationRentabilitePage({
  bienId,
  simulationSource,
  simulationAModifier,
  onCree,
  onRetour,
}: {
  bienId: string;
  /** Simulation existante à dupliquer : préremplit le formulaire avec ses valeurs, sous un nouveau nom. */
  simulationSource?: SimulationRentabiliteResponse;
  /** Simulation existante à modifier : préremplit le formulaire, garde le même nom et soumet un PUT
   * plutôt qu'un POST (event-sourcé, append-only côté serveur — l'historique des versions reste consultable). */
  simulationAModifier?: SimulationRentabiliteResponse;
  onCree: (simulationId: string) => void;
  onRetour: () => void;
}) {
  const estModification = !!simulationAModifier;
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
  const [apportPersonnel, setApportPersonnel] = useState('0');
  const [apportModifieManuellement, setApportModifieManuellement] = useState(false);
  const [tauxAnnuel, setTauxAnnuel] = useState('3.5');
  const [dureeAnnees, setDureeAnnees] = useState('20');
  const [tauxAssuranceEmprunteur, setTauxAssuranceEmprunteur] = useState('0.30');

  // Base de calcul de l'apport par défaut (30 %) et du montant emprunté : prix d'achat + travaux +
  // frais de notaire, à la demande explicite de l'utilisateur (hors frais d'agence/dossier).
  const baseEmpruntEnCentimes = useMemo(
    () => eurosVersCentimes(prixAchat) + eurosVersCentimes(travauxAcquisition) + eurosVersCentimes(fraisNotaire),
    [prixAchat, travauxAcquisition, fraisNotaire],
  );

  useEffect(() => {
    if (!financeACredit || apportModifieManuellement) return;
    setApportPersonnel(((baseEmpruntEnCentimes * 0.3) / 100).toFixed(2));
  }, [financeACredit, baseEmpruntEnCentimes, apportModifieManuellement]);

  const montantEmprunteEnCentimes = useMemo(
    () => Math.max(0, baseEmpruntEnCentimes - eurosVersCentimes(apportPersonnel)),
    [baseEmpruntEnCentimes, apportPersonnel],
  );

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
  const [chargesOuvert, setChargesOuvert] = useState(false);

  const [enSoumission, setEnSoumission] = useState(false);
  const [erreurGlobale, setErreurGlobale] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    Promise.all([obtenirFicheBien(bienId, session.token), obtenirPortefeuille(session.token)])
      .then(([fiche, portefeuille]) => {
        setBien(fiche);
        const chambresActives = portefeuille.filter((l) => l.bienParentId === bienId);

        const source = simulationAModifier ?? simulationSource;
        if (source) {
          setNomScenario(estModification ? source.nomScenario : `${source.nomScenario} (copie)`);
          setRegimeFiscal(source.regimeFiscal);
          setTmiFoyerPourcent(String(source.tmiFoyerPourcent));
          setHorizonAnnees(String(source.horizonAnnees));
          setPrixAchat((source.acquisition.prixAchatEnCentimes / 100).toFixed(2));
          setFraisNotaire((source.acquisition.fraisNotaireEnCentimes / 100).toFixed(2));
          setFraisAgence((source.acquisition.fraisAgenceEnCentimes / 100).toFixed(2));
          setTravauxAcquisition((source.acquisition.travauxAlAcquisitionEnCentimes / 100).toFixed(2));
          setFraisDossierBancaire((source.acquisition.fraisDossierBancaireEnCentimes / 100).toFixed(2));
          setFinanceACredit(source.financement.montantEmprunteEnCentimes > 0);
          setApportPersonnel((source.apportPersonnelEnCentimes / 100).toFixed(2));
          setApportModifieManuellement(true);
          setTauxAnnuel(String(source.financement.tauxAnnuelPourcent));
          setDureeAnnees(String(source.financement.dureeAnnees || 20));
          setTauxAssuranceEmprunteur(String(source.financement.tauxAssuranceEmprunteurPourcent));
          setQuotePartTerrain(String(source.amortissement.quotePartTerrainPourcent));
          setQuotePartMobilier(String(source.amortissement.quotePartMobilierPourcent));
          setDureeAmortissementBati(String(source.amortissement.dureeAmortissementBatiAnnees));
          setDureeAmortissementMobilier(String(source.amortissement.dureeAmortissementMobilierAnnees));
          setTaxeFonciere((source.chargesRecurrentes.taxeFonciereEnCentimes / 100).toFixed(2));
          setAssurancePno((source.chargesRecurrentes.assurancePnoEnCentimes / 100).toFixed(2));
          setAssuranceLoyersImpayes(
            (source.chargesRecurrentes.assuranceLoyersImpayesEnCentimes / 100).toFixed(2),
          );
          setFraisGestionLocative(String(source.chargesRecurrentes.fraisGestionLocativePourcentLoyer));
          setProvisionTravauxAnnuelle(
            (source.chargesRecurrentes.provisionTravauxAnnuelleEnCentimes / 100).toFixed(2),
          );
          setFraisComptabiliteAnnuel(
            (source.chargesRecurrentes.fraisComptabiliteAnnuelEnCentimes / 100).toFixed(2),
          );
          setChargesCoproprieteNonRecuperables(
            (source.chargesRecurrentes.chargesCoproprieteNonRecuperablesEnCentimes / 100).toFixed(2),
          );
          setTauxVacanceLocative(String(source.hypothesesEvolution.tauxVacanceLocativePourcent));
          setTauxIndexationLoyer(String(source.hypothesesEvolution.tauxIndexationLoyerPourcent));
          setTauxIndexationCharges(String(source.hypothesesEvolution.tauxIndexationChargesPourcent));

          const parLigneSource = new Map(
            source.revenusLocatifsSimules.map((l) => [l.bienSourceId, l]),
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
  }, [session, bienId, simulationSource, simulationAModifier]);

  function modifierLigneRevenu(index: number, champ: 'loyerEuros' | 'chargesEuros', valeur: string) {
    setLignesRevenu((lignes) =>
      lignes.map((l, i) => (i === index ? { ...l, [champ]: valeur } : l)),
    );
  }

  const surfaceTotaleM2 = useMemo(
    () => lignesRevenu.reduce((somme, l) => somme + l.surfaceM2, 0),
    [lignesRevenu],
  );
  const chargesActuellesMensuellesEnCentimes = useMemo(
    () => lignesRevenu.reduce((somme, l) => somme + eurosVersCentimes(l.chargesEuros), 0),
    [lignesRevenu],
  );

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
    // Les frais fixes (notaire, agence, travaux, dossier) ne varient pas avec la négociation :
    // c'est le complément du prix d'achat dans le coût total déjà calculé plus haut.
    const fraisFixes = coutTotalAcquisitionEnCentimes - eurosVersCentimes(prixAchat);
    // rendementBrut = (loyerMensuel × 12) / coûtTotal, constant : le coût total varie donc dans le
    // même rapport que le loyer.
    const nouveauCoutTotal = Math.round(
      coutTotalAcquisitionEnCentimes * (loyerNegocie / loyerActuelMensuelEnCentimes),
    );
    const nouveauPrixAchat = nouveauCoutTotal - fraisFixes;
    // Un loyer négocié trop bas par rapport aux frais fixes (notaire, agence, travaux, dossier)
    // peut faire ressortir un prix d'achat négatif ou nul : ce n'est plus un prix exploitable.
    if (nouveauPrixAchat <= 0) {
      return null;
    }
    return { nouveauPrixAchat };
  }, [loyerActuelMensuelEnCentimes, loyerNegocieEuros, coutTotalAcquisitionEnCentimes, prixAchat]);

  // --- Payload partagé entre l'aperçu en direct et la soumission (constat #1, #2 de l'audit ux-design) ---

  const payload: LancerSimulationRentabilitePayload = useMemo(
    () => ({
      nomScenario,
      regimeFiscal,
      tmiFoyerPourcent: parseInt(tmiFoyerPourcent, 10) || 0,
      horizonAnnees: parseInt(horizonAnnees, 10) || 1,
      acquisition: {
        prixAchatEnCentimes: eurosVersCentimes(prixAchat),
        fraisNotaireEnCentimes: eurosVersCentimes(fraisNotaire),
        fraisAgenceEnCentimes: eurosVersCentimes(fraisAgence),
        travauxAlAcquisitionEnCentimes: eurosVersCentimes(travauxAcquisition),
        fraisDossierBancaireEnCentimes: eurosVersCentimes(fraisDossierBancaire),
      },
      financement: {
        montantEmprunteEnCentimes: financeACredit ? montantEmprunteEnCentimes : 0,
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
    }),
    [
      nomScenario,
      regimeFiscal,
      tmiFoyerPourcent,
      horizonAnnees,
      prixAchat,
      fraisNotaire,
      fraisAgence,
      travauxAcquisition,
      fraisDossierBancaire,
      financeACredit,
      montantEmprunteEnCentimes,
      tauxAnnuel,
      dureeAnnees,
      tauxAssuranceEmprunteur,
      quotePartTerrain,
      quotePartMobilier,
      dureeAmortissementBati,
      dureeAmortissementMobilier,
      lignesRevenu,
      taxeFonciere,
      assurancePno,
      assuranceLoyersImpayes,
      fraisGestionLocative,
      provisionTravauxAnnuelle,
      fraisComptabiliteAnnuel,
      chargesCoproprieteNonRecuperables,
      tauxVacanceLocative,
      tauxIndexationLoyer,
      tauxIndexationCharges,
    ],
  );

  // Aperçu en direct (année 1) : corrige l'absence de feedback vivant pendant la saisie
  // (constat #1, principe ux-design #1). Rejoue côté client la formule année 1 du backend
  // (frontend/src/lib/apercuRentabilite.ts) ; le résultat définitif multi-année reste calculé
  // par le backend à la soumission.
  const apercu = useMemo(() => calculerApercuAnnee1(payload), [payload]);

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
    if (financeACredit && montantEmprunteEnCentimes <= 0) {
      setErreurGlobale('L\'apport personnel couvre déjà tout le montant : réduisez l\'apport, ou décochez "Financer à crédit" pour un achat cash.');
      return;
    }
    setEnSoumission(true);
    setErreurGlobale(null);

    try {
      const simulation = estModification && simulationAModifier
        ? await modifierSimulation(simulationAModifier.simulationId, payload, session.token)
        : await lancerSimulation(bienId, payload, session.token);
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
    return <p className="text-sm text-slate-400">Chargement…</p>;
  }
  if (etat === 'erreur' || !bien) {
    return <p className="text-sm text-red-600">Impossible de charger ce bien.</p>;
  }

  const regimesDisponibles = bien.meuble ? REGIMES_MEUBLE : REGIMES_NU;

  return (
    <section className="mx-auto max-w-5xl">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-100">
          {estModification ? 'Modifier la simulation' : 'Simuler la rentabilité'}
        </h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-400 hover:text-slate-100"
        >
          ← Retour
        </button>
      </div>
      <p className="mb-6 text-sm text-slate-400">
        {bien.libelleCommercial} — {bien.adresse.commune}
      </p>

      {erreurGlobale && (
        <div className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
          {erreurGlobale}
        </div>
      )}

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[1fr_300px]">
      <form onSubmit={soumettre} className="space-y-6">
        {/* Scénario */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4">
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
          </div>
        </fieldset>

        {/* Votre situation fiscale : séparée du scénario, propre à l'utilisateur et non au bien
            (constat #3, principe ux-design #4 "séparer les faits du bien de la situation utilisateur"). */}
        <fieldset className="rounded-md border border-slate-200 bg-slate-50 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Votre situation fiscale</legend>
          <p className="mt-1 text-xs text-slate-500">Propre à votre foyer, indépendante du bien simulé.</p>
          <div className="mt-3 max-w-[240px]">
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
        </fieldset>

        {/* Achat */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Achat</legend>
          <div className="mt-1 text-right text-xs text-slate-500">
            Total :{' '}
            <span className="font-medium text-slate-700">
              {formaterEuros(coutTotalAcquisitionEnCentimes)}
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

        {/* Simulateur de négociation : remonté après "Achat" et mis en évidence comme outil de
            décision plutôt qu'un champ de saisie de plus (constat #8 de l'audit ux-design). */}
        <div className="rounded-md border border-blue-200 bg-blue-50 p-4">
          <div className="flex items-center gap-2">
            <svg
              width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={COULEUR_POSITIF} strokeWidth={2}
              strokeLinecap="round" strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <path d="M12 16v-5" />
              <path d="M12 8h.01" />
            </svg>
            <h3 className="text-sm font-semibold text-blue-900">Simulateur de négociation</h3>
          </div>
          <p className="mt-1.5 text-xs text-blue-800">
            À rendement brut constant : si le loyer total change, quel prix d'achat faudrait-il négocier ?
          </p>
          <div className="mt-3 grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Loyer total mensuel actuel</label>
              <input
                type="text"
                disabled
                value={formaterEuros(loyerActuelMensuelEnCentimes)}
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
            <div className="mt-4 rounded-md bg-white p-3">
              <p className="text-xs text-slate-500">Prix d'achat correspondant (rendement brut inchangé)</p>
              <p className="mt-1 text-lg font-semibold text-slate-900">
                {formaterEuros(negociation.nouveauPrixAchat)}
              </p>
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={() => setLoyerNegocieEuros('')}
                  className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50"
                >
                  Réinitialiser
                </button>
                <button
                  type="button"
                  onClick={appliquerNegociation}
                  className="rounded border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-800 hover:bg-slate-50"
                >
                  Appliquer à mon projet
                </button>
              </div>
            </div>
          )}
          {!negociation && loyerNegocieEuros.trim() !== '' && loyerActuelMensuelEnCentimes > 0 && coutTotalAcquisitionEnCentimes > 0 && (
            <p className="mt-3 text-xs text-red-700">
              Ce loyer négocié aboutit à un prix d'achat nul ou négatif : essayez une valeur plus proche du loyer actuel.
            </p>
          )}
        </div>

        {/* Travaux */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4">
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
        <fieldset className="rounded-md border border-slate-200 bg-white p-4">
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
                  <label className="block text-sm font-medium text-slate-700">Apport personnel (€)</label>
                  <input
                    type="text"
                    value={apportPersonnel}
                    onChange={(e) => {
                      setApportModifieManuellement(true);
                      setApportPersonnel(e.target.value);
                    }}
                    className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                  />
                  <p className="mt-1 text-xs text-slate-500">
                    Par défaut, 30 % de (prix d'achat + travaux + frais de notaire).
                  </p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Montant emprunté (€)</label>
                  <input
                    type="text"
                    disabled
                    value={formaterEuros(montantEmprunteEnCentimes)}
                    className="mt-1 w-full rounded border border-slate-200 bg-slate-100 px-3 py-2 text-sm text-slate-500"
                  />
                  <p className="mt-1 text-xs text-slate-500">
                    Calculé : (prix d'achat + travaux + frais de notaire) − apport.
                  </p>
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
          <fieldset className="rounded-md border border-slate-200 bg-white p-4">
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

        {/* Loyers : une ligne par chambre/studio actif (coloc, co-living), avec surface et totaux
            (constat de l'utilisateur — les biens/chambres se créent depuis "Nouveau bien"). */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4">
          <div className="flex items-baseline justify-between">
            <legend className="px-1 text-sm font-semibold text-slate-700">Loyers</legend>
            {lignesRevenu.length > 1 && (
              <span className="text-xs text-slate-500">
                Total : {surfaceTotaleM2} m² · {formaterEuros(loyerActuelMensuelEnCentimes)}/mois de loyer ·{' '}
                {formaterEuros(chargesActuellesMensuellesEnCentimes)}/mois de charges
              </span>
            )}
          </div>
          <div className="mt-4 space-y-4">
            {lignesRevenu.map((l, i) => (
              <div key={l.bienSourceId} className="rounded-md bg-slate-50 p-3">
                <p className="mb-2 text-sm font-medium text-slate-700">
                  {l.libelle} <span className="font-normal text-slate-500">· {l.surfaceM2} m²</span>
                </p>
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

        {/* Charges : repliée par défaut avec sous-total toujours visible, pour réduire la charge
            perceptive du formulaire (constat #2, principe ux-design #8 "divulgation progressive"). */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4">
          <button
            type="button"
            onClick={() => setChargesOuvert((v) => !v)}
            className="flex w-full items-center justify-between text-left"
          >
            <legend className="px-1 text-sm font-semibold text-slate-700">Charges avancées</legend>
            <span className="flex items-center gap-2">
              <span className="text-xs text-slate-500">
                Total :{' '}
                <strong className="font-medium text-slate-700">
                  {formaterEuros(apercu.chargesFixesEnCentimes)}
                  /an
                </strong>
              </span>
              <svg
                width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth={2}
                strokeLinecap="round" strokeLinejoin="round"
                className={`transition-transform ${chargesOuvert ? 'rotate-180' : ''}`}
              >
                <path d="M6 9l6 6 6-6" />
              </svg>
            </span>
          </button>
          {chargesOuvert && (
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
          )}
        </fieldset>

        {/* Hypothèses d'évolution */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4">
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
          className="w-full rounded bg-emerald-800 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {enSoumission
            ? (estModification ? 'Enregistrement…' : 'Calcul en cours…')
            : (estModification ? 'Enregistrer les modifications' : 'Lancer la simulation')}
        </button>
      </form>

      {/* Aperçu en direct : corrige l'absence de feedback vivant pendant la saisie
          (constat #1, principe ux-design #1 "feedback vivant et persistant"). */}
      <div className="flex flex-col gap-3 rounded-md border border-slate-200 bg-white p-4 shadow-sm lg:sticky lg:top-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-700">Aperçu en direct</h3>
          <span className="rounded bg-green-50 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-green-700">
            Live
          </span>
        </div>
        <div className="rounded-md border border-slate-200 p-3">
          <p className="text-xs text-slate-500">Rendement brut</p>
          <p className="mt-1 text-lg font-semibold text-slate-900">
            {formaterPourcent(apercu.rendementBrutPourcent)}
          </p>
        </div>
        <div className="rounded-md border border-slate-200 p-3">
          <p className="text-xs text-slate-500">Rendement net-net</p>
          <p className="mt-1 text-lg font-semibold text-slate-900">
            {formaterPourcent(apercu.rendementNetNetPourcent)}
          </p>
        </div>
        <div className={`rounded-md border p-3 ${apercu.cashFlowMensuelApresImpotEnCentimes < 0 ? 'border-red-200' : 'border-slate-200'}`}>
          <p className="text-xs text-slate-500">Cash-flow (mois)</p>
          <p className={`mt-1 text-lg font-semibold ${apercu.cashFlowMensuelApresImpotEnCentimes < 0 ? 'text-red-600' : 'text-slate-900'}`}>
            {formaterEuros(apercu.cashFlowMensuelApresImpotEnCentimes)}
          </p>
        </div>
        <div className="rounded-md border border-slate-200 p-3">
          <p className="text-xs text-slate-500">Coût total d'acquisition</p>
          <p className="mt-1 text-base font-semibold text-slate-900">
            {formaterEuros(apercu.coutTotalAcquisitionEnCentimes)}
          </p>
        </div>
        <div className="rounded-md border border-slate-200 p-3">
          <p className="text-xs text-slate-500">Apport personnel</p>
          <p className="mt-1 text-base font-semibold text-slate-900">
            {formaterEuros(apercu.apportPersonnelEnCentimes)}
          </p>
        </div>
        <p className="text-[11px] leading-relaxed text-slate-400">
          Recalculé à chaque saisie, sans soumettre le formulaire — estimation année 1 (hors indexation
          et vacance sur l'horizon), le calcul définitif multi-année reste fait par le serveur.
        </p>
      </div>
      </div>
    </section>
  );
}
