import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { creerUtilisateur } from '../api/utilisateurs';

type FormState = {
  email: string;
  motDePasse: string;
  nom: string;
  prenom: string;
  telephone: string;
  accepteCgu: boolean;
  accepteConfidentialite: boolean;
};

const ETAT_INITIAL: FormState = {
  email: '',
  motDePasse: '',
  nom: '',
  prenom: '',
  telephone: '',
  accepteCgu: false,
  accepteConfidentialite: false,
};

export function InscriptionPage() {
  const [form, setForm] = useState<FormState>(ETAT_INITIAL);
  const [enCours, setEnCours] = useState(false);
  const [erreurGenerale, setErreurGenerale] = useState<string | null>(null);
  const [erreursChamps, setErreursChamps] = useState<Record<string, string>>({});
  const [succes, setSucces] = useState<string | null>(null);

  function modifier<K extends keyof FormState>(champ: K, valeur: FormState[K]) {
    setForm((f) => ({ ...f, [champ]: valeur }));
  }

  async function soumettre(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEnCours(true);
    setErreurGenerale(null);
    setErreursChamps({});
    setSucces(null);

    try {
      const reponse = await creerUtilisateur({
        email: form.email,
        motDePasse: form.motDePasse,
        nom: form.nom,
        prenom: form.prenom,
        telephone: form.telephone.trim() === '' ? undefined : form.telephone,
        accepteCgu: form.accepteCgu,
        accepteConfidentialite: form.accepteConfidentialite,
      });
      setSucces(`Compte créé (id : ${reponse.utilisateurId}).`);
      setForm(ETAT_INITIAL);
    } catch (e) {
      if (e instanceof ApiError) {
        setErreurGenerale(e.message);
        if (e.erreurs.length > 0) {
          const map: Record<string, string> = {};
          for (const erreur of e.erreurs) {
            map[erreur.champ] = erreur.message;
          }
          setErreursChamps(map);
        }
      } else {
        setErreurGenerale('Erreur réseau, réessayez plus tard.');
      }
    } finally {
      setEnCours(false);
    }
  }

  return (
    <section className="mx-auto max-w-md">
      <h2 className="text-2xl font-semibold tracking-tight text-slate-100">Créer un compte</h2>
      <p className="mt-2 text-sm text-slate-400">
        Inscrivez-vous pour commencer à gérer votre portefeuille de biens.
      </p>

      {succes && (
        <div className="mt-4 rounded border border-green-200 bg-green-50 p-3 text-sm text-green-800">
          {succes}
        </div>
      )}
      {erreurGenerale && (
        <div className="mt-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-800">
          {erreurGenerale}
        </div>
      )}

      <form className="mt-6 space-y-4" onSubmit={soumettre} noValidate>
        <Champ
          label="Email"
          type="email"
          value={form.email}
          onChange={(v) => modifier('email', v)}
          erreur={erreursChamps['email']}
          required
        />
        <Champ
          label="Mot de passe"
          type="password"
          value={form.motDePasse}
          onChange={(v) => modifier('motDePasse', v)}
          erreur={erreursChamps['motDePasse']}
          required
          aide="12 caractères minimum."
        />
        <div className="grid grid-cols-2 gap-4">
          <Champ
            label="Prénom"
            type="text"
            value={form.prenom}
            onChange={(v) => modifier('prenom', v)}
            erreur={erreursChamps['prenom']}
            required
          />
          <Champ
            label="Nom"
            type="text"
            value={form.nom}
            onChange={(v) => modifier('nom', v)}
            erreur={erreursChamps['nom']}
            required
          />
        </div>
        <Champ
          label="Téléphone (optionnel)"
          type="tel"
          value={form.telephone}
          onChange={(v) => modifier('telephone', v)}
          erreur={erreursChamps['telephone']}
          aide="Format international, ex. +33612345678."
        />

        <fieldset className="space-y-2">
          <CaseACocher
            label="J'accepte les CGU."
            checked={form.accepteCgu}
            onChange={(v) => modifier('accepteCgu', v)}
            erreur={erreursChamps['accepteCgu']}
          />
          <CaseACocher
            label="J'accepte la politique de confidentialité."
            checked={form.accepteConfidentialite}
            onChange={(v) => modifier('accepteConfidentialite', v)}
            erreur={erreursChamps['accepteConfidentialite']}
          />
        </fieldset>

        <button
          type="submit"
          disabled={enCours}
          className="w-full rounded bg-emerald-800 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {enCours ? 'Inscription…' : 'Créer mon compte'}
        </button>
      </form>
    </section>
  );
}

type ChampProps = {
  label: string;
  type: 'text' | 'email' | 'password' | 'tel';
  value: string;
  onChange: (v: string) => void;
  erreur?: string;
  required?: boolean;
  aide?: string;
};

function Champ({ label, type, value, onChange, erreur, required, aide }: ChampProps) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-slate-300">{label}</span>
      <input
        type={type}
        value={value}
        required={required}
        onChange={(e) => onChange(e.target.value)}
        className={`mt-1 block w-full rounded border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-900/10 ${
          erreur ? 'border-red-400' : 'border-slate-300'
        }`}
      />
      {aide && !erreur && <span className="mt-1 block text-xs text-slate-400">{aide}</span>}
      {erreur && <span className="mt-1 block text-xs text-red-600">{erreur}</span>}
    </label>
  );
}

type CaseProps = {
  label: string;
  checked: boolean;
  onChange: (v: boolean) => void;
  erreur?: string;
};

function CaseACocher({ label, checked, onChange, erreur }: CaseProps) {
  return (
    <div>
      <label className="flex items-start gap-2 text-sm text-slate-300">
        <input
          type="checkbox"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
          className="mt-0.5 size-4 rounded border-slate-300"
        />
        <span>{label}</span>
      </label>
      {erreur && <span className="mt-1 block text-xs text-red-600">{erreur}</span>}
    </div>
  );
}
