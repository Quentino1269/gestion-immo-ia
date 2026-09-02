import type {
  AcquisitionPayload,
  AmortissementPayload,
  ChargesRecurrentesPayload,
  FinancementPayload,
  HypothesesEvolutionPayload,
  LigneRevenuPayload,
  RegimeFiscal,
} from '../api/rentabilite';

/**
 * Aperçu en direct : rejoue la formule de l'année 1 de ProjectionCalculateur.java (backend) côté
 * client, pour donner un feedback immédiat pendant la saisie sans appeler le serveur. Le résultat
 * définitif (années 2..N, arrondis exacts en centimes) reste calculé par le backend à la soumission
 * — voir docs/slices/projection-rentabilite.md §11.
 */
export type ApercuAnnee1 = {
  coutTotalAcquisitionEnCentimes: number;
  apportPersonnelEnCentimes: number;
  /** Somme des charges fixes annuelles (hors gestion locative, proportionnelle au loyer) — pour
   * affichage d'un sous-total, sans dupliquer la somme dans l'écran appelant. */
  chargesFixesEnCentimes: number;
  rendementBrutPourcent: number;
  rendementNetPourcent: number;
  rendementNetNetPourcent: number;
  cashFlowMensuelApresImpotEnCentimes: number;
};

const ABATTEMENT_MICRO_FONCIER = 0.3;
const ABATTEMENT_MICRO_BIC = 0.5;
const PLAFOND_DEFICIT_FONCIER_IMPUTABLE_REVENU_GLOBAL_EN_CENTIMES = 10_700_00;
const TAUX_PRELEVEMENTS_SOCIAUX = 0.172;

function mensualiteEtInterets(financement: FinancementPayload): {
  interetsAnnee1: number;
  capitalRembourseAnnee1: number;
  assuranceAnnee1: number;
} {
  if (financement.montantEmprunteEnCentimes <= 0) {
    return { interetsAnnee1: 0, capitalRembourseAnnee1: 0, assuranceAnnee1: 0 };
  }
  const montant = financement.montantEmprunteEnCentimes;
  const tauxMensuel = financement.tauxAnnuelPourcent / 100 / 12;
  const dureeMois = financement.dureeAnnees * 12;

  let mensualite: number;
  if (tauxMensuel === 0) {
    mensualite = Math.round(montant / dureeMois);
  } else {
    const facteur = Math.pow(1 + tauxMensuel, dureeMois);
    mensualite = Math.round((montant * tauxMensuel * facteur) / (facteur - 1));
  }
  const assuranceAnnee1 = Math.round(montant * (financement.tauxAssuranceEmprunteurPourcent / 100));

  let capitalRestant = montant;
  let interetsAnnee1 = 0;
  let capitalRembourseAnnee1 = 0;
  const moisACalculer = Math.min(12, dureeMois);
  for (let m = 1; m <= moisACalculer; m++) {
    const interetMoisExact = capitalRestant * tauxMensuel;
    const interetMoisCentimes = Math.round(interetMoisExact);
    let principalMoisCentimes = Math.round(mensualite - interetMoisExact);
    if (m === dureeMois || principalMoisCentimes > capitalRestant) {
      principalMoisCentimes = Math.round(capitalRestant);
    }
    capitalRestant -= principalMoisCentimes;
    interetsAnnee1 += interetMoisCentimes;
    capitalRembourseAnnee1 += principalMoisCentimes;
  }
  return { interetsAnnee1, capitalRembourseAnnee1, assuranceAnnee1 };
}

export function calculerApercuAnnee1(params: {
  regimeFiscal: RegimeFiscal;
  tmiFoyerPourcent: number;
  acquisition: AcquisitionPayload;
  financement: FinancementPayload;
  amortissement: AmortissementPayload;
  revenusLocatifsSimules: LigneRevenuPayload[];
  chargesRecurrentes: ChargesRecurrentesPayload;
  hypothesesEvolution: HypothesesEvolutionPayload;
}): ApercuAnnee1 {
  const { regimeFiscal, tmiFoyerPourcent, acquisition, financement, amortissement, chargesRecurrentes } = params;

  const coutTotalAcquisitionEnCentimes =
    acquisition.prixAchatEnCentimes +
    acquisition.fraisNotaireEnCentimes +
    acquisition.fraisAgenceEnCentimes +
    acquisition.travauxAlAcquisitionEnCentimes +
    acquisition.fraisDossierBancaireEnCentimes;
  const apportPersonnelEnCentimes = coutTotalAcquisitionEnCentimes - financement.montantEmprunteEnCentimes;

  const chargesFixesEnCentimes =
    chargesRecurrentes.taxeFonciereEnCentimes +
    chargesRecurrentes.assurancePnoEnCentimes +
    chargesRecurrentes.assuranceLoyersImpayesEnCentimes +
    chargesRecurrentes.chargesCoproprieteNonRecuperablesEnCentimes +
    chargesRecurrentes.provisionTravauxAnnuelleEnCentimes +
    chargesRecurrentes.fraisComptabiliteAnnuelEnCentimes;

  if (coutTotalAcquisitionEnCentimes <= 0) {
    return {
      coutTotalAcquisitionEnCentimes,
      apportPersonnelEnCentimes,
      chargesFixesEnCentimes,
      rendementBrutPourcent: 0,
      rendementNetPourcent: 0,
      rendementNetNetPourcent: 0,
      cashFlowMensuelApresImpotEnCentimes: 0,
    };
  }

  const sommeLoyersMensuels = params.revenusLocatifsSimules.reduce(
    (somme, l) => somme + l.loyerSimuleMensuelEnCentimes,
    0,
  );
  const unMoinsVacance = 1 - params.hypothesesEvolution.tauxVacanceLocativePourcent / 100;
  const loyerBrut = sommeLoyersMensuels * 12 * unMoinsVacance;

  const fraisGestion = (loyerBrut * chargesRecurrentes.fraisGestionLocativePourcentLoyer) / 100;
  const chargesNonRecuperables = chargesFixesEnCentimes + fraisGestion;

  const { interetsAnnee1, capitalRembourseAnnee1, assuranceAnnee1 } = mensualiteEtInterets(financement);

  const quotePartBatiPourcent = 100 - amortissement.quotePartTerrainPourcent - amortissement.quotePartMobilierPourcent;
  const estBic = regimeFiscal === 'REEL_BIC';
  const amortissementBati = estBic && amortissement.dureeAmortissementBatiAnnees >= 1
    ? (coutTotalAcquisitionEnCentimes * quotePartBatiPourcent) / 100 / amortissement.dureeAmortissementBatiAnnees
    : 0;
  const amortissementMobilier = estBic && amortissement.dureeAmortissementMobilierAnnees >= 1
    ? (coutTotalAcquisitionEnCentimes * amortissement.quotePartMobilierPourcent) / 100 / amortissement.dureeAmortissementMobilierAnnees
    : 0;

  let resultatImposable: number;
  let deficitImputeRevenuGlobal = 0;

  switch (regimeFiscal) {
    case 'MICRO_FONCIER':
      resultatImposable = loyerBrut * (1 - ABATTEMENT_MICRO_FONCIER);
      break;
    case 'MICRO_BIC':
      resultatImposable = loyerBrut * (1 - ABATTEMENT_MICRO_BIC);
      break;
    case 'REEL_FONCIER': {
      const chargesDeductibles = chargesNonRecuperables + interetsAnnee1 + assuranceAnnee1;
      const resultatBrut = loyerBrut - chargesDeductibles;
      if (resultatBrut >= 0) {
        resultatImposable = resultatBrut;
      } else {
        const deficitAnnee = -resultatBrut;
        deficitImputeRevenuGlobal = Math.min(deficitAnnee, PLAFOND_DEFICIT_FONCIER_IMPUTABLE_REVENU_GLOBAL_EN_CENTIMES);
        resultatImposable = 0;
      }
      break;
    }
    case 'REEL_BIC': {
      const chargesDeductibles = chargesNonRecuperables + interetsAnnee1 + assuranceAnnee1 + amortissementBati + amortissementMobilier;
      const resultatBrutBic = loyerBrut - chargesDeductibles;
      // Pas de plafond : le déficit BIC n'est jamais imputable sur le revenu global (D21).
      resultatImposable = resultatBrutBic >= 0 ? resultatBrutBic : 0;
      break;
    }
    default:
      resultatImposable = 0;
  }

  const tmiFraction = tmiFoyerPourcent / 100;
  let impotEstime = resultatImposable * (tmiFraction + TAUX_PRELEVEMENTS_SOCIAUX);
  if (regimeFiscal === 'REEL_FONCIER') {
    impotEstime -= deficitImputeRevenuGlobal * tmiFraction;
  }

  const cashFlowAvantFinancementAvantImpot = loyerBrut - chargesNonRecuperables;
  const cashFlowApresFinancementAvantImpot =
    cashFlowAvantFinancementAvantImpot - interetsAnnee1 - capitalRembourseAnnee1 - assuranceAnnee1;
  const cashFlowApresFinancementApresImpot = cashFlowApresFinancementAvantImpot - impotEstime;

  const rendementBrutPourcent = (loyerBrut / coutTotalAcquisitionEnCentimes) * 100;
  const rendementNetPourcent = (cashFlowAvantFinancementAvantImpot / coutTotalAcquisitionEnCentimes) * 100;
  const rendementNetNetPourcent =
    ((cashFlowAvantFinancementAvantImpot - impotEstime) / coutTotalAcquisitionEnCentimes) * 100;

  return {
    coutTotalAcquisitionEnCentimes,
    apportPersonnelEnCentimes,
    chargesFixesEnCentimes,
    rendementBrutPourcent,
    rendementNetPourcent,
    rendementNetNetPourcent,
    cashFlowMensuelApresImpotEnCentimes: cashFlowApresFinancementApresImpot / 12,
  };
}
