import { useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { ProfilPage } from './ProfilPage';
import { PortefeuillePage } from './PortefeuillePage';
import { NouveauBienPage } from './NouveauBienPage';
import { ComparateurSimulationsPage } from './ComparateurSimulationsPage';
import { NouvelleSimulationRentabilitePage } from './NouvelleSimulationRentabilitePage';
import { SimulationRentabiliteDetailPage } from './SimulationRentabiliteDetailPage';
import type { SimulationRentabiliteResponse } from '../api/rentabilite';

type VueAccueil =
  | 'accueil'
  | 'profil'
  | 'portefeuille'
  | 'nouveauBien'
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
        onRetour={() => setVue('accueil')}
      />
    );
  }

  if (vue === 'nouveauBien') {
    return <NouveauBienPage onRetour={() => setVue('portefeuille')} />;
  }

  if (vue === 'comparateurRentabilite' && bienSelectionne) {
    return (
      <ComparateurSimulationsPage
        bienId={bienSelectionne}
        onNouvelleSimulation={() => {
          setSimulationSourceAPrefiller(null);
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
        onCree={(simulationId) => {
          setSimulationSourceAPrefiller(null);
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
          setVue('nouvelleSimulation');
        }}
        onRetour={() => setVue('comparateurRentabilite')}
      />
    );
  }

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

      <div className="mt-6 space-y-3">
        <button
          type="button"
          onClick={() => setVue('portefeuille')}
          className="w-full rounded border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50"
        >
          Mon portefeuille
        </button>

        <button
          type="button"
          onClick={() => setVue('profil')}
          className="w-full rounded border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-800 hover:bg-slate-50"
        >
          Mon profil civil
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
