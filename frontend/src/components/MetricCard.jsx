export default function MetricCard({ label, value, accent = 'default' }) {
  return (
    <article className={`metric-card metric-${accent}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}
