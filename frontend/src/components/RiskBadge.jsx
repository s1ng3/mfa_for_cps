// Renders the risk level + score, with an optional breakdown of contributing rules.
export default function RiskBadge({ level, score, reasons }) {
  return (
    <div>
      <div className="row">
        <span className={`badge ${level}`}>{level} RISK</span>
        <span className="mono">score {score}/100</span>
      </div>
      {reasons?.length > 0 && (
        <ul className="muted" style={{ fontSize: 12, marginTop: 8, paddingLeft: 18 }}>
          {reasons.map((r, i) => (
            <li key={i}>{r.rule} <span className="mono">(+{r.points})</span></li>
          ))}
        </ul>
      )}
    </div>
  );
}
