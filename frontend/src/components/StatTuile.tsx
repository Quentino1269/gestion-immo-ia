/** Tuile "libellé + valeur" partagée par les bandeaux de KPIs (portefeuille, détail de simulation). */
export function StatTuile({
  libelle,
  valeur,
  negatif = false,
}: {
  libelle: string;
  valeur: string;
  /** Accentue la tuile en rouge, cohérent avec les cellules négatives des tableaux de rentabilité. */
  negatif?: boolean;
}) {
  return (
    <div className={`rounded-md border p-3 ${negatif ? 'border-red-200 bg-white' : 'border-slate-200 bg-white'}`}>
      <p className="text-xs text-slate-500">{libelle}</p>
      <p className={`mt-1 text-lg font-semibold ${negatif ? 'text-red-600' : 'text-slate-900'}`}>{valeur}</p>
    </div>
  );
}
