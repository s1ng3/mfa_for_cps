// A compact process-value gauge with a fill bar.
export default function HmiGauge({ label, value, unit, min = 0, max = 100, danger }) {
  const pct = Math.max(0, Math.min(100, ((value - min) / (max - min)) * 100));
  const color = danger ? 'var(--critical)' : pct > 85 ? 'var(--high)' : 'var(--accent)';
  return (
    <div className="card gauge">
      <h3>{label}</h3>
      <div className="value" style={{ color }}>
        {value} <span className="unit">{unit}</span>
      </div>
      <div className="bar"><span style={{ width: `${pct}%`, background: color }} /></div>
    </div>
  );
}
