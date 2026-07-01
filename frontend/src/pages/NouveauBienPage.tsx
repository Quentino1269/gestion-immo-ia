import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import {
  creerBien,
  obtenirPortefeuille,
  type TypeBien,
  type ModaliteCharges,
  type LignePortefeuilleResponse,
} from '../api/biens';
import { ApiError } from '../api/client';

function eurosVersCentimes(valeur: string): number {
  const n = parseFloat(valeur.replace(',', '.'));
  return isNaN(n) ? 0 : Math.round(n * 100);
}

export function NouveauBienPage({ onRetour }: { onRetour: () => void }) {
  const { session } = useAuth();

  const [typeBien, setTypeBien] = useState<TypeBien>('APPARTEMENT');
  const [bienParentId, setBienParentId] = useState('');
  const [libelleChambre, setLibelleChambre] = useState('');
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

  const [biensPotentielsParent, setBiensPotentielsParent] = useState<LignePortefeuilleResponse[]>([]);
  const [enSoumission, setEnSoumission] = useState(false);
  const [erreurGlobale, setErreurGlobale] = useState<string | null>(null);

  // La modalité découle du statut meublé
  const modaliteCharges: ModaliteCharges = meuble ? 'FORFAIT' : 'PROVISION';

  // Pour CHAMBRE_COLOCATION, nbPieces est toujours 1
  const nbPiecesEffectif = typeBien === 'CHAMBRE_COLOCATION' ? 1 : parseInt(nbPieces, 10) || 1;

  // Charger les biens éligibles comme parent (non-chambres) à l'ouverture
  useEffect(() => {
    if (!session) return;
    obtenirPortefeuille(session.token)
      .then((lignes) =>
        setBiensPotentielsParent(lignes.filter((l) => l.typeBien !== 'CHAMBRE_COLOCATION')),
      )
      .catch(() => {
        // silencieux : la liste parent sera vide
      });
  }, [session]);

  async function soumettre(e: React.FormEvent) {
    e.preventDefault();
    if (!session) return;
    setEnSoumission(true);
    setErreurGlobale(null);

    try {
      await creerBien(
        {
          typeBien,
          bienParentId: typeBien === 'CHAMBRE_COLOCATION' ? bienParentId || undefined : undefined,
          libelleChambre: typeBien === 'CHAMBRE_COLOCATION' ? libelleChambre || undefined : undefined,
          nbPiecesPrincipales: nbPiecesEffectif,
          surfaceM2: parseFloat(surfaceM2.replace(',', '.')) || 0,
          meuble,
          loyerHorsChargesEnCentimes: eurosVersCentimes(loyerEuros),
          chargesEnCentimes: eurosVersCentimes(chargesEuros),
          modaliteCharges,
          adresse: {
            numero: adrNumero,
            voie: adrVoie,
            complement: adrComplement || undefined,
            codePostal: adrCodePostal,
            commune: adrCommune,
            paysIso: adrPaysIso,
          },
          disponibleAPartirDu,
        },
        session.token,
      );
      onRetour();
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

  return (
    <section className="mx-auto max-w-lg">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-900">Nouveau bien</h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-500 hover:text-slate-800"
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
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Type de bien</legend>
          <div className="mt-4 space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Type</label>
              <select
                value={typeBien}
                onChange={(e) => {
                  setTypeBien(e.target.value as TypeBien);
                  setBienParentId('');
                  setLibelleChambre('');
                  if (e.target.value === 'CHAMBRE_COLOCATION') setMeuble(true);
                  else setMeuble(false);
                }}
                className="mt-1 w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm"
              >
                <option value="APPARTEMENT">Appartement</option>
                <option value="MAISON">Maison</option>
                <option value="CHAMBRE_COLOCATION">Chambre en colocation</option>
              </select>
            </div>

            {typeBien === 'CHAMBRE_COLOCATION' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-slate-700">
                    Bien parent (colocation)
                  </label>
                  <select
                    value={bienParentId}
                    onChange={(e) => setBienParentId(e.target.value)}
                    required
                    className="mt-1 w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm"
                  >
                    <option value="">— Sélectionner un bien —</option>
                    {biensPotentielsParent.map((b) => (
                      <option key={b.bienId} value={b.bienId}>
                        {b.libelleCommercial} — {b.adresse.commune} ({b.surfaceM2} m²)
                      </option>
                    ))}
                  </select>
                  {biensPotentielsParent.length === 0 && (
                    <p className="mt-1 text-xs text-amber-700">
                      Aucun bien éligible. Créez d'abord un appartement ou une maison.
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700">
                    Libellé de la chambre
                  </label>
                  <input
                    type="text"
                    placeholder="Chambre A"
                    value={libelleChambre}
                    onChange={(e) => setLibelleChambre(e.target.value)}
                    required
                    maxLength={50}
                    className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </>
            )}

            {typeBien !== 'CHAMBRE_COLOCATION' && (
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
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
            )}
          </div>
        </fieldset>

        {/* Surface et loyer */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Surface et loyer</legend>
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
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div className="flex items-end pb-1">
                <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-slate-700">
                  <input
                    type="checkbox"
                    checked={meuble}
                    onChange={(e) => setMeuble(e.target.checked)}
                    disabled={typeBien === 'CHAMBRE_COLOCATION'}
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
                  value={loyerEuros}
                  onChange={(e) => setLoyerEuros(e.target.value)}
                  required
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Charges (€)</label>
                <input
                  type="text"
                  placeholder="50,00"
                  value={chargesEuros}
                  onChange={(e) => setChargesEuros(e.target.value)}
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700">
                Disponible à partir du
              </label>
              <input
                type="date"
                value={disponibleAPartirDu}
                onChange={(e) => setDisponibleAPartirDu(e.target.value)}
                required
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
        </fieldset>

        {/* Adresse du bien */}
        <fieldset className="rounded-md border border-slate-200 p-4">
          <legend className="px-1 text-sm font-semibold text-slate-700">Adresse du bien</legend>
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
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
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
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
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
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
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
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
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
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
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
                  className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm uppercase"
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
          {enSoumission ? 'Enregistrement…' : 'Ajouter le bien'}
        </button>
      </form>
    </section>
  );
}
