import { InscriptionPage } from './pages/InscriptionPage';

function App() {
  return (
    <main className="min-h-screen w-full bg-slate-50 px-6 py-12">
      <header className="mx-auto mb-10 max-w-md text-center">
        <h1 className="text-3xl font-semibold tracking-tight text-slate-900">Gestion Immo</h1>
        <p className="mt-2 text-sm text-slate-500">Plateforme de gestion locative</p>
      </header>
      <InscriptionPage />
    </main>
  );
}

export default App;
