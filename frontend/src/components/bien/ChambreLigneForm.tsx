import { ApiError } from '../../api/client';

export type ChambreUI = {
  cle: string;
  libelle: string;
  surfaceM2: string;
  loyerEuros: string;
  chargesEuros: string;
  meuble: boolean;
  statut: 'attente' | 'creee' | 'erreur';
  erreur?: string;
};

let compteur = 0;

export function nouvelleChambreVide(): ChambreUI {
  compteur += 1;
  return {
    cle: `chambre-${Date.now()}-${compteur}`,
    libelle: '',
    surfaceM2: '',
    loyerEuros: '',
    chargesEuros: '0',
    meuble: true,
    statut: 'attente',
  };
}

export function messageErreur(err: unknown): string {
  return err instanceof ApiError ? err.message : 'Une erreur inattendue est survenue.';
}

/** Une ligne de saisie pour une chambre (libellé, surface, loyer, charges, meublé), avec son
 * propre état de soumission — réutilisée par NouveauBienPage (colocation à la création) et
 * ModifierBienPage (ajout d'une chambre à un bien parent déjà existant). */
export function ChambreLigneForm({
  chambre,
  onChange,
  onRetirer,
}: {
  chambre: ChambreUI;
  onChange: (chambre: ChambreUI) => void;
  onRetirer?: () => void;
}) {
  const creee = chambre.statut === 'creee';

  return (
    <div className={`rounded-md border p-3 ${creee ? 'border-emerald-200 bg-emerald-50' : 'border-slate-200'}`}>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-slate-700">Libellé</label>
          <input
            type="text"
            placeholder="Chambre A"
            value={chambre.libelle}
            disabled={creee}
            onChange={(e) => onChange({ ...chambre, libelle: e.target.value })}
            required
            maxLength={50}
            className="mt-1 w-full rounded border border-slate-300 px-2 py-1.5 text-sm disabled:bg-slate-100"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-700">Surface (m²)</label>
          <input
            type="text"
            placeholder="12,00"
            value={chambre.surfaceM2}
            disabled={creee}
            onChange={(e) => onChange({ ...chambre, surfaceM2: e.target.value })}
            required
            className="mt-1 w-full rounded border border-slate-300 px-2 py-1.5 text-sm disabled:bg-slate-100"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-700">Loyer hors charges (€)</label>
          <input
            type="text"
            placeholder="450,00"
            value={chambre.loyerEuros}
            disabled={creee}
            onChange={(e) => onChange({ ...chambre, loyerEuros: e.target.value })}
            required
            className="mt-1 w-full rounded border border-slate-300 px-2 py-1.5 text-sm disabled:bg-slate-100"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-700">Charges (€)</label>
          <input
            type="text"
            placeholder="30,00"
            value={chambre.chargesEuros}
            disabled={creee}
            onChange={(e) => onChange({ ...chambre, chargesEuros: e.target.value })}
            className="mt-1 w-full rounded border border-slate-300 px-2 py-1.5 text-sm disabled:bg-slate-100"
          />
        </div>
      </div>

      <div className="mt-2 flex items-center justify-between">
        <label className="flex cursor-pointer items-center gap-2 text-xs font-medium text-slate-700">
          <input
            type="checkbox"
            checked={chambre.meuble}
            disabled={creee}
            onChange={(e) => onChange({ ...chambre, meuble: e.target.checked })}
            className="rounded border-slate-300"
          />
          Meublée
        </label>
        {onRetirer && !creee && (
          <button type="button" onClick={onRetirer} className="text-xs font-medium text-slate-400 hover:text-red-600">
            Retirer
          </button>
        )}
        {creee && <span className="text-xs font-medium text-emerald-700">✓ Créée</span>}
      </div>

      {chambre.statut === 'erreur' && chambre.erreur && (
        <p className="mt-2 text-xs text-red-600">{chambre.erreur}</p>
      )}
    </div>
  );
}
