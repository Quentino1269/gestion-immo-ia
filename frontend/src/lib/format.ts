export function formaterEuros(centimes: number): string {
  return (centimes / 100).toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' });
}

export function formaterPourcent(valeur: number | null): string {
  if (valeur === null) return '—';
  return `${valeur.toFixed(2).replace('.', ',')} %`;
}

export function eurosVersCentimes(valeur: string): number {
  const n = parseFloat(valeur.replace(',', '.'));
  return isNaN(n) ? 0 : Math.round(n * 100);
}

export function pourcent(valeur: string): number {
  const n = parseFloat(valeur.replace(',', '.'));
  return isNaN(n) ? 0 : n;
}
