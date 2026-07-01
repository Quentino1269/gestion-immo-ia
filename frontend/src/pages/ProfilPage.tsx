import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import {
  obtenirMonProfil,
  completerMonProfil,
  type ProfilResponse,
  type CompleterProfilPayload,
  type Civilite,
} from '../api/profil';
import { ApiError } from '../api/client';

type EtatChargement = 'chargement' | 'pret' | 'erreur';

export function ProfilPage({ onRetour }: { onRetour: () => void }) {
  const { session } = useAuth();
  const [profil, setProfil] = useState<ProfilResponse | null>(null);
  const [etat, setEtat] = useState<EtatChargement>('chargement');
  const [enSoumission, setEnSoumission] = useState(false);
  const [messageSucces, setMessageSucces] = useState<string | null>(null);
  const [erreurGlobale, setErreurGlobale] = useState<string | null>(null);

  // Champs du formulaire
  const [civilite, setCivilite] = useState<Civilite | ''>('');
  const [dateNaissance, setDateNaissance] = useState('');
  const [lieuNaissanceVille, setLieuNaissanceVille] = useState('');
  const [lieuNaissancePaysIso, setLieuNaissancePaysIso] = useState('');
  const [nationaliteIso, setNationaliteIso] = useState('');
  const [telephone, setTelephone] = useState('');
  // Adresse
  const [adrNumero, setAdrNumero] = useState('');
  const [adrVoie, setAdrVoie] = useState('');
  const [adrComplement, setAdrComplement] = useState('');
  const [adrCodePostal, setAdrCodePostal] = useState('');
  const [adrCommune, setAdrCommune] = useState('');
  const [adrPaysIso, setAdrPaysIso] = useState('');

  useEffect(() => {
    if (!session) return;
    obtenirMonProfil(session.token)
      .then((p) => {
        setProfil(p);
        // Pré-remplir avec les valeurs déjà connues
        if (p.identite.civilite) setCivilite(p.identite.civilite);
        if (p.identite.dateNaissance) setDateNaissance(p.identite.dateNaissance);
        if (p.identite.lieuNaissanceVille) setLieuNaissanceVille(p.identite.lieuNaissanceVille);
        if (p.identite.lieuNaissancePaysIso) setLieuNaissancePaysIso(p.identite.lieuNaissancePaysIso);
        if (p.identite.nationaliteIso) setNationaliteIso(p.identite.nationaliteIso);
        if (p.coordonnees.telephone) setTelephone(p.coordonnees.telephone);
        if (p.adresseDomicile) {
          setAdrNumero(p.adresseDomicile.numero);
          setAdrVoie(p.adresseDomicile.voie);
          setAdrComplement(p.adresseDomicile.complement ?? '');
          setAdrCodePostal(p.adresseDomicile.codePostal);
          setAdrCommune(p.adresseDomicile.commune);
          setAdrPaysIso(p.adresseDomicile.paysIso);
        }
        setEtat('pret');
      })
      .catch(() => setEtat('erreur'));
  }, [session]);

  async function soumettre(e: React.FormEvent) {
    e.preventDefault();
    if (!session) return;
    setEnSoumission(true);
    setErreurGlobale(null);
    setMessageSucces(null);

    const payload: CompleterProfilPayload = {};
    if (civilite) payload.civilite = civilite;
    if (dateNaissance) payload.dateNaissance = dateNaissance;
    if (lieuNaissanceVille) payload.lieuNaissanceVille = lieuNaissanceVille;
    if (lieuNaissancePaysIso) payload.lieuNaissancePaysIso = lieuNaissancePaysIso;
    if (nationaliteIso) payload.nationaliteIso = nationaliteIso;
    if (telephone) payload.telephone = telephone;
    if (adrNumero && adrVoie && adrCodePostal && adrCommune && adrPaysIso) {
      payload.adresseDomicile = {
        numero: adrNumero,
        voie: adrVoie,
        complement: adrComplement || undefined,
        codePostal: adrCodePostal,
        commune: adrCommune,
        paysIso: adrPaysIso,
      };
    }

    try {
      const profilMisAJour = await completerMonProfil(payload, session.token);
      setProfil(profilMisAJour);
      setMessageSucces(
        profilMisAJour.statutProfil === 'COMPLET'
          ? 'Profil complété — vous pouvez maintenant signer un bail.'
          : 'Informations enregistrées.',
      );
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
    return <p className="text-sm text-slate-500">Chargement du profil…</p>;
  }
  if (etat === 'erreur') {
    return <p className="text-sm text-red-600">Impossible de charger le profil.</p>;
  }

  const dejaComplet = profil?.statutProfil === 'COMPLET';
  const champDejaRenseigne = (val: string) => val.length > 0 && profil !== null;

  return (
    <section className="mx-auto max-w-lg">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-900">Mon profil civil</h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-500 hover:text-slate-800"
        >
          ← Retour
        </button>
      </div>

      {profil && (
        <div
          className={`mb-6 rounded-md px-4 py-3 text-sm font-medium ${
            dejaComplet
              ? 'bg-green-50 text-green-800'
              : 'bg-amber-50 text-amber-800'
          }`}
        >
          {dejaComplet
            ? 'Profil complet — vous pouvez signer un bail.'
            : `Profil incomplet. Champs manquants : ${profil.champsManquantsPourBail.join(', ')}.`}
        </div>
      )}

      {messageSucces && (
        <div className="mb-4 rounded-md bg-green-50 px-4 py-3 text-sm text-green-800">
          {messageSucces}
        </div>
      )}
      {erreurGlobale && (
        <div className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
          {erreurGlobale}
        </div>
      )}

      <form onSubmit={soumettre} className="space-y-6">
        {/* Identité */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Identité</legend>
          <div className="mt-4 space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Civilité</label>
              <select
                value={civilite}
                onChange={(e) => setCivilite(e.target.value as Civilite | '')}
                disabled={champDejaRenseigne(profil?.identite.civilite ?? '')}
                className="mt-1 w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
              >
                <option value="">— Non renseignée —</option>
                <option value="MADAME">Madame</option>
                <option value="MONSIEUR">Monsieur</option>
                <option value="NON_RENSEIGNEE">Préfère ne pas préciser</option>
              </select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Date de naissance</label>
                <input
                  type="date"
                  value={dateNaissance}
                  onChange={(e) => setDateNaissance(e.target.value)}
                  disabled={!!profil?.identite.dateNaissance}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Nationalité (ISO)</label>
                <input
                  type="text"
                  placeholder="FR"
                  maxLength={2}
                  value={nationaliteIso}
                  onChange={(e) => setNationaliteIso(e.target.value.toUpperCase())}
                  disabled={!!profil?.identite.nationaliteIso}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm uppercase disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Ville de naissance</label>
                <input
                  type="text"
                  placeholder="Paris"
                  value={lieuNaissanceVille}
                  onChange={(e) => setLieuNaissanceVille(e.target.value)}
                  disabled={!!profil?.identite.lieuNaissanceVille}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Pays de naissance (ISO)</label>
                <input
                  type="text"
                  placeholder="FR"
                  maxLength={2}
                  value={lieuNaissancePaysIso}
                  onChange={(e) => setLieuNaissancePaysIso(e.target.value.toUpperCase())}
                  disabled={!!profil?.identite.lieuNaissancePaysIso}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm uppercase disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
            </div>
          </div>
        </fieldset>

        {/* Coordonnées */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Coordonnées</legend>
          <div className="mt-4">
            <label className="block text-sm font-medium text-slate-700">Téléphone (format E.164)</label>
            <input
              type="tel"
              placeholder="+33612345678"
              value={telephone}
              onChange={(e) => setTelephone(e.target.value)}
              disabled={!!profil?.coordonnees.telephone}
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
            />
          </div>
        </fieldset>

        {/* Adresse de domicile */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Adresse de domicile</legend>
          <div className="mt-4 space-y-4">
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Numéro</label>
                <input
                  type="text"
                  placeholder="12 bis"
                  value={adrNumero}
                  onChange={(e) => setAdrNumero(e.target.value)}
                  disabled={!!profil?.adresseDomicile}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-slate-700">Voie</label>
                <input
                  type="text"
                  placeholder="Rue de la Paix"
                  value={adrVoie}
                  onChange={(e) => setAdrVoie(e.target.value)}
                  disabled={!!profil?.adresseDomicile}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Complément (optionnel)</label>
              <input
                type="text"
                placeholder="Bât. B, apt. 12"
                value={adrComplement}
                onChange={(e) => setAdrComplement(e.target.value)}
                disabled={!!profil?.adresseDomicile}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
              />
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Code postal</label>
                <input
                  type="text"
                  placeholder="75001"
                  value={adrCodePostal}
                  onChange={(e) => setAdrCodePostal(e.target.value)}
                  disabled={!!profil?.adresseDomicile}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Commune</label>
                <input
                  type="text"
                  placeholder="Paris"
                  value={adrCommune}
                  onChange={(e) => setAdrCommune(e.target.value)}
                  disabled={!!profil?.adresseDomicile}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Pays (ISO)</label>
                <input
                  type="text"
                  placeholder="FR"
                  maxLength={2}
                  value={adrPaysIso}
                  onChange={(e) => setAdrPaysIso(e.target.value.toUpperCase())}
                  disabled={!!profil?.adresseDomicile}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm uppercase disabled:bg-slate-50 disabled:text-slate-500"
                />
              </div>
            </div>
          </div>
        </fieldset>

        <button
          type="submit"
          disabled={enSoumission}
          className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {enSoumission ? 'Enregistrement…' : 'Enregistrer'}
        </button>
      </form>
    </section>
  );
}
