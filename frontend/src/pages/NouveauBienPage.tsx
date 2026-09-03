import { useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { creerBien, type TypeBien, type ModaliteCharges } from '../api/biens';
import {
  ChambreLigneForm,
  commandeChambreDepuis,
  nouvelleChambreVide,
  type ChambreUI,
} from '../components/bien/ChambreLigneForm';
import { eurosVersCentimes, messageErreur } from '../lib/format';

/**
 * Formulaire de création d'un appartement ou d'une maison. Une colocation se déclare et se
 * peuple ici directement (case à cocher + chambres saisies dans le même formulaire) — il n'y a
 * plus de type de bien "Chambre en colocation" isolé à la création (cf. docs/slices/creation-bien.md
 * D15, révisé). Ajouter une chambre à un bien déjà existant se fait depuis sa fiche
 * (ModifierBienPage).
 */
export function NouveauBienPage({ onRetour }: { onRetour: () => void }) {
  const { session } = useAuth();

  const [typeBien, setTypeBien] = useState<TypeBien>('APPARTEMENT');
  const [nbPieces, setNbPieces] = useState('1');
  const [surfaceM2, setSurfaceM2] = useState('');
  const [meuble, setMeuble] = useState(false);
  const [loyerEuros, setLoyerEuros] = useState('');
  const [chargesEuros, setChargesEuros] = useState('0');
  const [disponibleAPartirDu, setDisponibleAPartirDu] = useState('');

  // Adresse du bien
  const [adrNumero, setAdrNumero] = useState('');
  const [adrVoie, setAdrVoie] = useState('');
  const [adrComplement, setAdrComplement] = useState('');
  const [adrCodePostal, setAdrCodePostal] = useState('');
  const [adrCommune, setAdrCommune] = useState('');
  const [adrPaysIso, setAdrPaysIso] = useState('FR');

  const [estColocation, setEstColocation] = useState(false);
  const [chambres, setChambres] = useState<ChambreUI[]>([]);
  // Une fois le bien parent créé (premier essai), on garde son id pour ne pas le recréer si
  // une ou plusieurs chambres ont échoué et que l'utilisateur corrige puis revalide.
  const [parentBienId, setParentBienId] = useState<string | null>(null);

  const [enSoumission, setEnSoumission] = useState(false);
  const [erreurGlobale, setErreurGlobale] = useState<string | null>(null);

  const modaliteCharges: ModaliteCharges = meuble ? 'FORFAIT' : 'PROVISION';
  const surfaceChambresEnM2 = chambres.reduce((s, c) => s + (parseFloat(c.surfaceM2.replace(',', '.')) || 0), 0);

  // Pour une colocation avec au moins une chambre saisie, le loyer et les charges du bien
  // parent sont dérivés (somme des chambres) plutôt que saisis manuellement — un immeuble en
  // colocation perçoit mécaniquement la somme des loyers/charges de ses chambres.
  const loyerDeriveDesChambres = estColocation && chambres.length > 0;
  const loyerChambresEnCentimes = chambres.reduce((s, c) => s + eurosVersCentimes(c.loyerEuros), 0);
  const chargesChambresEnCentimes = chambres.reduce((s, c) => s + eurosVersCentimes(c.chargesEuros), 0);

  function majChambre(cle: string, chambre: ChambreUI) {
    setChambres((prev) => prev.map((c) => (c.cle === cle ? chambre : c)));
  }

  function retirerChambre(cle: string) {
    setChambres((prev) => prev.filter((c) => c.cle !== cle));
  }

  async function soumettre(e: React.FormEvent) {
    e.preventDefault();
    if (!session) return;
    setEnSoumission(true);
    setErreurGlobale(null);

    const adresse = {
      numero: adrNumero,
      voie: adrVoie,
      complement: adrComplement || undefined,
      codePostal: adrCodePostal,
      commune: adrCommune,
      paysIso: adrPaysIso,
    };

    let idParent = parentBienId;
    if (!idParent) {
      try {
        const parent = await creerBien(
          {
            typeBien,
            nbPiecesPrincipales: parseInt(nbPieces, 10) || 1,
            surfaceM2: parseFloat(surfaceM2.replace(',', '.')) || 0,
            meuble,
            loyerHorsChargesEnCentimes: loyerDeriveDesChambres ? loyerChambresEnCentimes : eurosVersCentimes(loyerEuros),
            chargesEnCentimes: loyerDeriveDesChambres ? chargesChambresEnCentimes : eurosVersCentimes(chargesEuros),
            modaliteCharges,
            adresse,
            disponibleAPartirDu,
          },
          session.token,
        );
        idParent = parent.bienId;
        setParentBienId(idParent);
      } catch (err) {
        setErreurGlobale(messageErreur(err));
        setEnSoumission(false);
        return;
      }
    }

    if (estColocation && chambres.length > 0) {
      const chambresMaj = [...chambres];
      let resteEnErreur = false;
      for (let i = 0; i < chambresMaj.length; i++) {
        if (chambresMaj[i].statut === 'creee') continue;
        const c = chambresMaj[i];
        try {
          await creerBien(commandeChambreDepuis(c, idParent, adresse, disponibleAPartirDu), session.token);
          chambresMaj[i] = { ...c, statut: 'creee', erreur: undefined };
        } catch (err) {
          chambresMaj[i] = { ...c, statut: 'erreur', erreur: messageErreur(err) };
          resteEnErreur = true;
        }
      }
      setChambres(chambresMaj);
      if (resteEnErreur) {
        setErreurGlobale(
          'Le bien a été enregistré. Corrigez les chambres en erreur ci-dessous puis validez à nouveau.',
        );
        setEnSoumission(false);
        return;
      }
    }

    onRetour();
  }

  return (
    <section className="mx-auto max-w-lg">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-100">Nouveau bien</h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-400 hover:text-slate-100"
        >
          ← Retour
        </button>
      </div>

      {erreurGlobale && (
        <div className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
          {erreurGlobale}
        </div>
      )}

      <form onSubmit={soumettre} className="space-y-6">
        {/* Type et identité du bien */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4" disabled={!!parentBienId}>
          <legend className="rounded bg-white px-1 text-sm font-semibold text-slate-700">Type de bien</legend>
          <div className="mt-4 space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Type</label>
              <select
                value={typeBien}
                onChange={(e) => setTypeBien(e.target.value as TypeBien)}
                className="mt-1 w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm disabled:bg-slate-100"
              >
                <option value="APPARTEMENT">Appartement</option>
                <option value="MAISON">Maison</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700">
                Nombre de pièces principales
              </label>
              <input
                type="number"
                min={1}
                value={nbPieces}
                onChange={(e) => setNbPieces(e.target.value)}
                required
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
              />
            </div>

            <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-slate-700">
              <input
                type="checkbox"
                checked={estColocation}
                onChange={(e) => setEstColocation(e.target.checked)}
                className="rounded border-slate-300"
              />
              C'est une colocation
            </label>
          </div>
        </fieldset>

        {estColocation && (
          <fieldset className="rounded-md border border-slate-200 bg-white p-4">
            <legend className="rounded bg-white px-1 text-sm font-semibold text-slate-700">Chambres</legend>
            <div className="mt-4 space-y-3">
              {chambres.map((c) => (
                <ChambreLigneForm
                  key={c.cle}
                  chambre={c}
                  onChange={(maj) => majChambre(c.cle, maj)}
                  onRetirer={() => retirerChambre(c.cle)}
                />
              ))}
              {chambres.length === 0 && (
                <p className="text-xs text-slate-500">
                  Aucune chambre saisie. Ajoutez-en, ou enregistrez le bien et ajoutez des chambres plus tard
                  depuis sa fiche.
                </p>
              )}
              <button
                type="button"
                onClick={() => setChambres((prev) => [...prev, nouvelleChambreVide()])}
                className="text-sm font-medium text-emerald-700 hover:text-emerald-800 hover:underline"
              >
                + Ajouter une chambre
              </button>
            </div>
          </fieldset>
        )}

        {/* Surface et loyer (du bien parent) */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4" disabled={!!parentBienId}>
          <legend className="rounded bg-white px-1 text-sm font-semibold text-slate-700">Surface et loyer</legend>
          <div className="mt-4 space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Surface (m²)</label>
                <input
                  type="text"
                  placeholder="55,00"
                  value={surfaceM2}
                  onChange={(e) => setSurfaceM2(e.target.value)}
                  required
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
                />
                {estColocation && surfaceChambresEnM2 > 0 && (
                  <p className="mt-1 text-xs text-slate-500">
                    Somme des chambres : {surfaceChambresEnM2.toFixed(2)} m²
                  </p>
                )}
              </div>
              <div className="flex items-end pb-1">
                <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-slate-700">
                  <input
                    type="checkbox"
                    checked={meuble}
                    onChange={(e) => setMeuble(e.target.checked)}
                    className="rounded border-slate-300"
                  />
                  Meublé
                </label>
              </div>
            </div>

            <div className="rounded-md bg-slate-50 px-3 py-2 text-xs text-slate-500">
              Modalité des charges :{' '}
              <span className="font-medium text-slate-700">
                {modaliteCharges === 'FORFAIT'
                  ? 'Forfait (logement meublé)'
                  : 'Provision avec régularisation'}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">
                  Loyer hors charges (€)
                </label>
                <input
                  type="text"
                  placeholder="800,00"
                  value={loyerDeriveDesChambres ? (loyerChambresEnCentimes / 100).toFixed(2).replace('.', ',') : loyerEuros}
                  onChange={(e) => setLoyerEuros(e.target.value)}
                  disabled={loyerDeriveDesChambres}
                  required
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Charges (€)</label>
                <input
                  type="text"
                  placeholder="50,00"
                  value={loyerDeriveDesChambres ? (chargesChambresEnCentimes / 100).toFixed(2).replace('.', ',') : chargesEuros}
                  onChange={(e) => setChargesEuros(e.target.value)}
                  disabled={loyerDeriveDesChambres}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
                />
              </div>
            </div>
            {loyerDeriveDesChambres && (
              <p className="-mt-2 text-xs text-slate-500">
                Dérivés de la somme des {chambres.length} chambre{chambres.length > 1 ? 's' : ''} saisie
                {chambres.length > 1 ? 's' : ''} ci-dessus.
              </p>
            )}

            <div>
              <label className="block text-sm font-medium text-slate-700">
                Disponible à partir du
              </label>
              <input
                type="date"
                value={disponibleAPartirDu}
                onChange={(e) => setDisponibleAPartirDu(e.target.value)}
                required
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
              />
            </div>
          </div>
        </fieldset>

        {/* Adresse du bien */}
        <fieldset className="rounded-md border border-slate-200 bg-white p-4" disabled={!!parentBienId}>
          <legend className="rounded bg-white px-1 text-sm font-semibold text-slate-700">Adresse du bien</legend>
          <div className="mt-4 space-y-4">
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Numéro</label>
                <input
                  type="text"
                  placeholder="12 bis"
                  value={adrNumero}
                  onChange={(e) => setAdrNumero(e.target.value)}
                  required
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
                />
              </div>
              <div className="col-span-2">
                <label className="block text-sm font-medium text-slate-700">Voie</label>
                <input
                  type="text"
                  placeholder="Rue de la Paix"
                  value={adrVoie}
                  onChange={(e) => setAdrVoie(e.target.value)}
                  required
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">
                Complément (optionnel)
              </label>
              <input
                type="text"
                placeholder="Bât. B, apt. 12"
                value={adrComplement}
                onChange={(e) => setAdrComplement(e.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
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
                  required
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Commune</label>
                <input
                  type="text"
                  placeholder="Paris"
                  value={adrCommune}
                  onChange={(e) => setAdrCommune(e.target.value)}
                  required
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
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
                  required
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm uppercase disabled:bg-slate-100"
                />
              </div>
            </div>
          </div>
        </fieldset>

        <button
          type="submit"
          disabled={enSoumission}
          className="w-full rounded bg-emerald-800 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {enSoumission ? 'Enregistrement…' : 'Ajouter le bien'}
        </button>
      </form>
    </section>
  );
}
