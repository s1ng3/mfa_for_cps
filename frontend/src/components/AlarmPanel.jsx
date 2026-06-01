// Shows alarm + emergency-stop state with optional action buttons.
export default function AlarmPanel({ status, canAck, onAck, canReset, onReset, busy }) {
  return (
    <div className="card">
      <h3>Alarms & Safety</h3>
      <div className="spread mt">
        <span>Alarm</span>
        {status.alarmActive
          ? <span className="badge HIGH">ACTIVE — {status.alarmMessage}</span>
          : <span className="badge LOW">CLEAR</span>}
      </div>
      <div className="spread mt">
        <span>Emergency Stop</span>
        {status.emergencyStop
          ? <span className="badge CRITICAL">ENGAGED</span>
          : <span className="badge LOW">NORMAL</span>}
      </div>
      <div className="row mt">
        {canAck && (
          <button className="btn inline secondary" disabled={busy || !status.alarmActive} onClick={onAck}>
            Acknowledge Alarm
          </button>
        )}
        {canReset && (
          <button className="btn inline danger" disabled={busy || !status.emergencyStop} onClick={onReset}>
            Reset E-STOP (critical)
          </button>
        )}
      </div>
    </div>
  );
}
