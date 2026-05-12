import { formatLabel } from '../lib/format';

export default function StatusBadge({ value }) {
  const normalized = (value ?? 'SEM_STATUS').toString().toLowerCase();
  return <span className={`status-badge status-${normalized}`}>{formatLabel(value)}</span>;
}
