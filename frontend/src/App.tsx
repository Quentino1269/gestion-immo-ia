import { useState } from 'react';
import { AuthProvider } from './auth/AuthContext';
import { useAuth } from './auth/useAuth';
import { AccueilPage } from './pages/AccueilPage';
import { InscriptionPage } from './pages/InscriptionPage';
import { LoginPage } from './pages/LoginPage';

function App() {
  return (
    <AuthProvider>
      <main className="min-h-screen w-full bg-slate-50 px-6 py-12">
        <header className="mx-auto mb-10 max-w-md text-center">
          <h1 className="text-3xl font-semibold tracking-tight text-slate-900">Gestion Immo</h1>
          <p className="mt-2 text-sm font-medium text-emerald-700">Plateforme de gestion locative</p>
        </header>
        <Routeur />
      </main>
    </AuthProvider>
  );
}

type Onglet = 'connexion' | 'inscription';

function Routeur() {
  const { session } = useAuth();
  const [onglet, setOnglet] = useState<Onglet>('connexion');

  if (session) {
    return <AccueilPage />;
  }

  return (
    <div>
      <nav className="mx-auto mb-6 flex max-w-md gap-2 rounded-md bg-white p-1 shadow-sm">
        <BoutonOnglet actif={onglet === 'connexion'} onClick={() => setOnglet('connexion')}>
          Connexion
        </BoutonOnglet>
        <BoutonOnglet actif={onglet === 'inscription'} onClick={() => setOnglet('inscription')}>
          Inscription
        </BoutonOnglet>
      </nav>
      {onglet === 'connexion' ? <LoginPage /> : <InscriptionPage />}
    </div>
  );
}

function BoutonOnglet({
  actif,
  onClick,
  children,
}: {
  actif: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex-1 rounded px-3 py-2 text-sm font-medium transition ${
        actif ? 'bg-emerald-800 text-white' : 'text-slate-600 hover:bg-slate-100'
      }`}
    >
      {children}
    </button>
  );
}

export default App;
