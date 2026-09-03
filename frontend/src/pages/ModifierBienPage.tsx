import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import { modifierBien, obtenirFicheBien, type FicheBienResponse } from '../api/biens';
import { ApiError } from '../api/client';
import { eurosVersCentimes, formaterEuros } from '../lib/format';
import type { EtatChargement } from '../lib/types';

/**
 * Écran d'édition d'un bien existant. Ne couvre que les champs modifiables du slice
 * (docs/slices/modification-bien.md D2) : loyer, charges, meublé, disponibilité, libellé de
 * chambre, nombre de pièces. Le type, la surface et l'adresse sont hors périmètre (D3) et
 * affichés en lecture seule pour situer le bien.
 */
export function ModifierBienPage({
  bienId,
  onModifie,
  onRetour,
}: {
  bienId: string;
  onModifie: () => void;
  onRetour: () => void;
}) {
  const { session } = useAuth();
  const [fiche, setFiche] = useState<FicheBienResponse | null>(null);
  const [etat, setEtat] = useState<EtatChargement>('chargement');

  const [libelleChambre, setLibelleChambre] = useState('');
  const [nbPieces, setNbPieces] = useState('1');
  const [meuble, setMeuble] = useState(false);
  const [loyerEuros, setLoyerEuros] = useState('');
  const [chargesEuros, setChargesEuros] = useState('0');
  const [disponibleAPartirDu, setDisponibleAPartirDu] = useState('');

  const [enSoumission, setEnSoumission] = useState(false);
  const [erreurGlobale, setErreurGlobale] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    obtenirFicheBien(bienId, session.token)
      .then((f) => {
        setFiche(f);
        setLibelleChambre(f.libelleChambre ?? '');
        setNbPieces(String(f.nbPiecesPrincipales));
        setMeuble(f.meuble);
        setLoyerEuros((f.loyerHorsChargesEnCentimes / 100).toFixed(2).replace('.', ','));
        setChargesEuros((f.chargesEnCentimes / 100).toFixed(2).replace('.', ','));
        setDisponibleAPartirDu(f.disponibleAPartirDu);
        setEtat('pret');
      })
      .catch(() => setEtat('erreur'));
  }, [session, bienId]);

  const estChambre = fiche?.typeBien === 'CHAMBRE_COLOCATION';
  const modaliteCharges = meuble ? 'FORFAIT' : 'PROVISION';

  async function soumettre(e: React.FormEvent) {
    e.preventDefault();
    if (!session || !fiche) return;
    setEnSoumission(true);
    setErreurGlobale(null);

    try {
      await modifierBien(
        bienId,
        {
          loyerHorsChargesEnCentimes: eurosVersCentimes(loyerEuros),
          chargesEnCentimes: eurosVersCentimes(chargesEuros),
          meuble,
          disponibleAPartirDu,
          libelleChambre: estChambre ? libelleChambre : undefined,
          nbPiecesPrincipales: estChambre ? 1 : parseInt(nbPieces, 10) || 1,
        },
        session.token,
      );
      onModifie();
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
        <h2 className="text-2xl font-semibold tracking-tight text-slate-900">Modifier le bien</h2>
        <button
          type="button"
          onClick={onRetour}
          className="text-sm text-slate-500 hover:text-slate-800"
        >
          ← Retour
        </button>
      </div>

      {etat === 'chargement' && <p className="text-sm text-slate-500">Chargement…</p>}
      {etat === 'erreur' && (
        <p className="text-sm text-red-600">Impossible de charger ce bien.</p>
      )}

      {etat === 'pret' && fiche && (
        <>
          <div className="mb-6 rounded-md bg-slate-50 px-4 py-3 text-sm text-slate-600">
            <p className="font-medium text-slate-800">{fiche.libelleCommercial}</p>
            <p className="mt-0.5">
              {fiche.adresse.numero} {fiche.adresse.voie}, {fiche.adresse.codePostal} {fiche.adresse.commune}
              {' · '}
              {fiche.surfaceM2} m²
            </p>
            <p className="mt-1 text-xs text-slate-400">
              Type, surface et adresse ne sont pas modifiables dans cette version.
            </p>
          </div>

          {erreurGlobale && (
            <div className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
              {erreurGlobale}
            </div>
          )}

          <form onSubmit={soumettre} className="space-y-6">
            <fieldset className="rounded-md border border-slate-200 p-4">
              <legend className="px-1 text-sm font-semibold text-slate-700">Identité</legend>
              <div className="mt-4 space-y-4">
                {estChambre ? (
                  <div>
                    <label className="block text-sm font-medium text-slate-700">
                      Libellé de la chambre
                    </label>
                    <input
                      type="text"
                      value={libelleChambre}
                      onChange={(e) => setLibelleChambre(e.target.value)}
                      required
                      maxLength={50}
                      className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
                    />
                  </div>
                ) : (
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

            <fieldset className="rounded-md border border-slate-200 p-4">
              <legend className="px-1 text-sm font-semibold text-slate-700">Loyer et charges</legend>
              <div className="mt-4 space-y-4">
                <div className="flex items-center gap-2">
                  <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-slate-700">
                    <input
                      type="checkbox"
                      checked={meuble}
                      onChange={(e) => setMeuble(e.target.checked)}
                      disabled={estChambre}
                      className="rounded border-slate-300"
                    />
                    Meublé
                  </label>
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

            <button
              type="submit"
              disabled={enSoumission}
              className="w-full rounded bg-emerald-800 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {enSoumission ? 'Enregistrement…' : 'Enregistrer les modifications'}
            </button>
          </form>

          <p className="mt-4 text-xs text-slate-400">
            Ancien loyer : {formaterEuros(fiche.loyerHorsChargesEnCentimes)} · anciennes charges :{' '}
            {formaterEuros(fiche.chargesEnCentimes)}
          </p>
        </>
      )}
    </section>
  );
}
