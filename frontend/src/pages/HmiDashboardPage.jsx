import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { hmiApi } from '../services/hmiApi';
import { apiError } from '../api/client';
import HmiGauge from '../components/HmiGauge';
import AlarmPanel from '../components/AlarmPanel';
import CriticalActionButton from '../components/CriticalActionButton';

export default function HmiDashboardPage() {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [status, setStatus] = useState(null);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState(null);
  const [error, setError] = useState(null);
  const [motor, setMotor] = useState(1500);
  const [temp, setTemp] = useState(55);
  const [pressure, setPressure] = useState(5);

  const poll = useCallback(async () => {
    try { setStatus(await hmiApi.status()); } catch (e) { /* handled globally on 401 */ }
  }, []);

  useEffect(() => {
    poll();
    const id = setInterval(poll, 2500);
    return () => clearInterval(id);
  }, [poll]);

  // Runs a control action and, if the gateway demands step-up MFA, routes to the step-up page
  // carrying the action so it can be replayed after verification.
  const run = async (actionType, fn, value) => {
    setBusy(true); setError(null); setToast(null);
    try {
      const res = await fn();
      if (res.requiresStepUp) {
        navigate('/step-up', { state: { actionType, value, message: res.message } });
        return;
      }
      setStatus(res.status);
      setToast(res.message);
    } catch (e) {
      setError(apiError(e));
    } finally {
      setBusy(false);
    }
  };

  if (!status) return <div className="muted">Connecting to HMI…</div>;

  const can = (p) => hasPermission(p);

  return (
    <div>
      <div className="spread mb">
        <h2 style={{ margin: 0 }}>Water Treatment Process — HMI</h2>
        <span className="pill">Live · refreshed every 2.5s</span>
      </div>

      {toast && <div className="alert-box ok">{toast}</div>}
      {error && <div className="alert-box error">{error}</div>}

      <div className="grid cols-4 mb">
        <HmiGauge label="Tank Level" value={status.tankLevel} unit="%" min={0} max={100}
                  danger={status.tankLevel > 90 || status.tankLevel < 10} />
        <HmiGauge label="Water Temp" value={status.waterTemperature} unit="°C" min={0} max={100}
                  danger={status.waterTemperature > status.temperatureSetpoint + 15} />
        <HmiGauge label="Pressure" value={status.pressure} unit="bar" min={0} max={12}
                  danger={status.pressure > status.pressureSetpoint * 1.6} />
        <HmiGauge label="Motor Speed" value={status.motorSpeed} unit="RPM" min={0} max={3000} />
      </div>

      <div className="grid cols-2">
        <div className="card">
          <h3>Pump Control</h3>
          <div className="spread mt">
            <span>Pump status</span>
            {status.pumpRunning ? <span className="badge LOW">RUNNING</span> : <span className="pill">STOPPED</span>}
          </div>
          <div className="row mt">
            {can('HMI_START_PUMP') &&
              <button className="btn inline success" disabled={busy || status.pumpRunning}
                      onClick={() => run('START_PUMP', hmiApi.startPump)}>Start Pump</button>}
            {can('HMI_STOP_PUMP') &&
              <button className="btn inline secondary" disabled={busy || !status.pumpRunning}
                      onClick={() => run('STOP_PUMP', hmiApi.stopPump)}>Stop Pump</button>}
            {!can('HMI_START_PUMP') && !can('HMI_STOP_PUMP') &&
              <span className="muted">View-only access.</span>}
          </div>
        </div>

        <AlarmPanel
          status={status}
          canAck={can('HMI_ACK_ALARM')}
          onAck={() => run('ACKNOWLEDGE_ALARM', hmiApi.acknowledgeAlarm)}
          canReset={can('HMI_RESET_EMERGENCY_STOP')}
          onReset={() => run('RESET_EMERGENCY_STOP', hmiApi.resetEmergencyStop, 'reset')}
          busy={busy}
        />
      </div>

      {(can('HMI_CHANGE_MOTOR_SPEED') || can('HMI_CHANGE_TEMPERATURE_SETPOINT') || can('HMI_CHANGE_PRESSURE_SETPOINT')) && (
        <>
          <div className="section-title">Engineering Setpoints <span className="badge HIGH">step-up MFA required</span></div>
          <div className="grid cols-3">
            {can('HMI_CHANGE_MOTOR_SPEED') && (
              <div className="card">
                <h3>Motor Speed (RPM)</h3>
                <input type="number" value={motor} min={0} max={3000} onChange={(e) => setMotor(+e.target.value)} />
                <div className="mt">
                  <CriticalActionButton disabled={busy}
                    onClick={() => run('CHANGE_MOTOR_SPEED', () => hmiApi.changeMotorSpeed(motor), motor)}>
                    Apply motor speed
                  </CriticalActionButton>
                </div>
              </div>
            )}
            {can('HMI_CHANGE_TEMPERATURE_SETPOINT') && (
              <div className="card">
                <h3>Temperature Setpoint (°C)</h3>
                <input type="number" value={temp} min={0} max={95} onChange={(e) => setTemp(+e.target.value)} />
                <div className="mt">
                  <CriticalActionButton disabled={busy}
                    onClick={() => run('CHANGE_TEMPERATURE_SETPOINT', () => hmiApi.changeTemperature(temp), temp)}>
                    Apply temperature
                  </CriticalActionButton>
                </div>
              </div>
            )}
            {can('HMI_CHANGE_PRESSURE_SETPOINT') && (
              <div className="card">
                <h3>Pressure Setpoint (bar)</h3>
                <input type="number" value={pressure} min={0} max={10} step={0.1} onChange={(e) => setPressure(+e.target.value)} />
                <div className="mt">
                  <CriticalActionButton disabled={busy}
                    onClick={() => run('CHANGE_PRESSURE_SETPOINT', () => hmiApi.changePressure(pressure), pressure)}>
                    Apply pressure
                  </CriticalActionButton>
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
