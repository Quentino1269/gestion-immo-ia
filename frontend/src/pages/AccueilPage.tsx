import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { ProfilPage } from './ProfilPage';
import { PortefeuillePage } from './PortefeuillePage';
import { NouveauBienPage } from './NouveauBienPage';
import { ModifierBienPage } from './ModifierBienPage';
import { ComparateurSimulationsPage } from './ComparateurSimulationsPage';
import { NouvelleSimulationRentabilitePage } from './NouvelleSimulationRentabilitePage';
import { SimulationRentabiliteDetailPage } from './SimulationRentabiliteDetailPage';
import type { SimulationRentabiliteResponse } from '../api/rentabilite';
import { obtenirPortefeuille } from '../api/biens';

type VueAccueil =
  | 'accueil'
  | 'profil'
  | 'portefeuille'
  | 'nouveauBien'
  | 'modifierBien'
  | 'comparateurRentabilite'
  | 'nouvelleSimulation'
  | 'detailSimulation';

export function AccueilPage() {
  const { session, deconnecter } = useAuth();
  const [vue, setVue] = useState<VueAccueil>('accueil');
  const [bienSelectionne, setBienSelectionne] = useState<string | null>(null);
  const [simulationSelectionnee, setSimulationSelectionnee] = useState<string | null>(null);
  const [simulationSourceAPrefiller, setSimulationSourceAPrefiller] =
    useState<SimulationRentabiliteResponse | null>(null);
  const [simulationAModifier, setSimulationAModifier] = useState<SimulationRentabiliteResponse | null>(null);
  const [enCours, setEnCours] = useState(false);
  const [nbBiens, setNbBiens] = useState<number | null>(null);

  useEffect(() => {
    if (!session) return;
    obtenirPortefeuille(session.token)
      .then((data) => setNbBiens(data.length))
      .catch(() => setNbBiens(null));
  }, [session]);

  async function gererDeconnexion() {
    setEnCours(true);
    try {
      await deconnecter();
    } finally {
      setEnCours(false);
    }
  }

  if (!session) return null;

  if (vue === 'profil') {
    return <ProfilPage onRetour={() => setVue('accueil')} />;
  }

  if (vue === 'portefeuille') {
    return (
      <PortefeuillePage
        onNouveauBien={() => setVue('nouveauBien')}
        onSimulerRentabilite={(bienId) => {
          setBienSelectionne(bienId);
          setVue('comparateurRentabilite');
        }}
        onModifierBien={(bienId) => {
          setBienSelectionne(bienId);
          setVue('modifierBien');
        }}
        onRetour={() => setVue('accueil')}
      />
    );
  }

  if (vue === 'nouveauBien') {
    return <NouveauBienPage onRetour={() => setVue('portefeuille')} />;
  }

  if (vue === 'modifierBien' && bienSelectionne) {
    return (
      <ModifierBienPage
        bienId={bienSelectionne}
        onModifie={() => setVue('portefeuille')}
        onModifierChambre={(chambreId) => setBienSelectionne(chambreId)}
        onRetour={() => setVue('portefeuille')}
      />
    );
  }

  if (vue === 'comparateurRentabilite' && bienSelectionne) {
    return (
      <ComparateurSimulationsPage
        bienId={bienSelectionne}
        onNouvelleSimulation={() => {
          setSimulationSourceAPrefiller(null);
          setSimulationAModifier(null);
          setVue('nouvelleSimulation');
        }}
        onVoirDetail={(simulationId) => {
          setSimulationSelectionnee(simulationId);
          setVue('detailSimulation');
        }}
        onRetour={() => setVue('portefeuille')}
      />
    );
  }

  if (vue === 'nouvelleSimulation' && bienSelectionne) {
    return (
      <NouvelleSimulationRentabilitePage
        bienId={bienSelectionne}
        simulationSource={simulationSourceAPrefiller ?? undefined}
        simulationAModifier={simulationAModifier ?? undefined}
        onCree={(simulationId) => {
          setSimulationSourceAPrefiller(null);
          setSimulationAModifier(null);
          setSimulationSelectionnee(simulationId);
          setVue('detailSimulation');
        }}
        onRetour={() => setVue('comparateurRentabilite')}
      />
    );
  }

  if (vue === 'detailSimulation' && simulationSelectionnee) {
    return (
      <SimulationRentabiliteDetailPage
        simulationId={simulationSelectionnee}
        onDupliquer={(simulation) => {
          setBienSelectionne(simulation.bienId);
          setSimulationSourceAPrefiller(simulation);
          setSimulationAModifier(null);
          setVue('nouvelleSimulation');
        }}
        onModifier={(simulation) => {
          setBienSelectionne(simulation.bienId);
          setSimulationAModifier(simulation);
          setSimulationSourceAPrefiller(null);
          setVue('nouvelleSimulation');
        }}
        onRetour={() => setVue('comparateurRentabilite')}
      />
    );
  }

  const expireA = new Date(session.expireA);

  return (
    <section className="mx-auto max-w-md">
      <h2 className="text-2xl font-semibold tracking-tight text-slate-100">Bienvenue</h2>
      <p className="mt-2 text-sm text-slate-400">
        Vous êtes connecté. Votre session expire le{' '}
        <span className="font-medium text-slate-200">
          {expireA.toLocaleString('fr-FR', {
            dateStyle: 'short',
            timeStyle: 'short',
          })}
        </span>
        .
      </p>

      <div className="mt-6 space-y-3">
        <button
          type="button"
          onClick={() => setVue('portefeuille')}
          className="flex w-full items-center justify-between rounded border border-slate-300 bg-white px-4 py-2 text-left hover:bg-slate-50"
        >
          <span>
            <span className="block text-sm font-medium text-slate-800">Mon portefeuille</span>
            {nbBiens !== null && (
              <span className="mt-0.5 block text-xs text-slate-500">
                {nbBiens} bien{nbBiens > 1 ? 's' : ''} géré{nbBiens > 1 ? 's' : ''}
              </span>
            )}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setVue('profil')}
          className="flex w-full items-center justify-between rounded border border-slate-300 bg-white px-4 py-2 text-left hover:bg-slate-50"
        >
          <span>
            <span className="block text-sm font-medium text-slate-800">Mon profil civil</span>
            <span className="mt-0.5 block text-xs text-slate-500">Coordonnées et TMI de référence</span>
          </span>
        </button>

        <button
          type="button"
          onClick={gererDeconnexion}
          disabled={enCours}
          className="w-full rounded border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {enCours ? 'Déconnexion…' : 'Se déconnecter'}
        </button>
      </div>
    </section>
  );
}
