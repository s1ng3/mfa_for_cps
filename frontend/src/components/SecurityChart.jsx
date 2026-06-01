import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

// Bar chart of recent audit events grouped by event type.
export default function SecurityChart({ events }) {
  const counts = {};
  (events || []).forEach((e) => { counts[e.eventType] = (counts[e.eventType] || 0) + 1; });
  const data = Object.entries(counts)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);

  const colorFor = (name) => name.includes('FAILED') || name.includes('UNAUTHORIZED')
    ? 'var(--critical)' : name.includes('STEP_UP') || name.includes('INCIDENT')
      ? 'var(--high)' : 'var(--accent)';

  if (data.length === 0) return <div className="muted">No recent events to chart.</div>;

  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={data} layout="vertical" margin={{ left: 40, right: 16 }}>
        <XAxis type="number" stroke="#93a3c4" fontSize={11} allowDecimals={false} />
        <YAxis type="category" dataKey="name" stroke="#93a3c4" fontSize={10} width={150} />
        <Tooltip contentStyle={{ background: '#16203a', border: '1px solid #2a3a5e', borderRadius: 8 }} />
        <Bar dataKey="value" radius={[0, 4, 4, 0]}>
          {data.map((d, i) => <Cell key={i} fill={colorFor(d.name)} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
