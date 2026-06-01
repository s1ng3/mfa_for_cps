// A control that visually marks itself as a critical (step-up-protected) action.
export default function CriticalActionButton({ children, onClick, disabled, critical = true }) {
  return (
    <button
      className={'btn inline ' + (critical ? 'danger' : 'secondary')}
      onClick={onClick}
      disabled={disabled}
      title={critical ? 'Critical action — requires step-up MFA' : undefined}
    >
      {critical ? '🛡 ' : ''}{children}
    </button>
  );
}
