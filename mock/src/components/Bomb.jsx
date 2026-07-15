import './Bomb.css'

export default function Bomb({ timeLeft, totalTime }) {
  const ratio = timeLeft / totalTime
  const pct = Math.round(ratio * 100)

  const state =
    ratio <= 0.1 ? 'critical'
    : ratio <= 0.2 ? 'danger'
    : ratio <= 0.5 ? 'ticking'
    : 'safe'

  const shakeIntensity =
    state === 'critical' ? 'shake-hard'
    : state === 'danger' ? 'shake-mid'
    : state === 'ticking' ? 'shake-light'
    : ''

  return (
    <div className={`bomb-wrapper ${shakeIntensity}`}>
      <svg viewBox="0 0 140 180" className="bomb-svg" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <radialGradient id="bombGrad2" cx="50%" cy="40%" r="55%">
            <stop offset="0%" stopColor={pct <= 20 ? '#661111' : '#444'} />
            <stop offset="100%" stopColor={pct <= 20 ? '#330000' : '#111'} />
          </radialGradient>
          <filter id="glowFilter">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
          </filter>
        </defs>

        {state === 'critical' && (
          <g>
            <animateTransform attributeName="transform" type="rotate" values="-2,70,100;2,70,100;-2,70,100" dur="0.08s" repeatCount="indefinite" />
          </g>
        )}

        <rect x="57" y="10" width="16" height="22" rx="3" fill="#777" />
        <rect x="59" y="6" width="12" height="6" rx="2" fill="#999" />

        <circle cx="65" cy="24" r="7" fill={state === 'critical' ? '#ff0000' : '#ff4400'} filter="url(#glowFilter)">
          {state !== 'safe' && (
            <animate attributeName="r" values="7;9;7" dur={state === 'critical' ? '0.15s' : '0.4s'} repeatCount="indefinite" />
          )}
        </circle>

        {state !== 'safe' && (
          <g opacity="0.6">
            <circle cx="65" cy="24" r="4" fill="#ffaa00">
              <animate attributeName="r" values="4;7;4" dur="0.3s" repeatCount="indefinite" />
              <animate attributeName="opacity" values="0.8;0.2;0.8" dur="0.3s" repeatCount="indefinite" />
            </circle>
          </g>
        )}

        <ellipse cx="65" cy="110" rx="48" ry="52" fill="url(#bombGrad2)" stroke="#444" strokeWidth="2" />

        {pct <= 30 && (
          <g opacity="0.3">
            <rect x="65" y="62" width="2" height="60" fill="#ff0000" rx="1">
              <animate attributeName="opacity" values="0.3;0.6;0.3" dur="0.5s" repeatCount="indefinite" />
            </rect>
            <rect x="40" y="62" width="2" height="40" fill="#ff0000" rx="1">
              <animate attributeName="opacity" values="0.2;0.5;0.2" dur="0.7s" repeatCount="indefinite" />
            </rect>
            <rect x="90" y="62" width="2" height="45" fill="#ff0000" rx="1">
              <animate attributeName="opacity" values="0.2;0.5;0.2" dur="0.6s" repeatCount="indefinite" />
            </rect>
          </g>
        )}

        <ellipse cx="65" cy="82" rx="20" ry="5" fill="#222" />
        <rect x="48" y="76" width="34" height="10" rx="3" fill="#333" />

        <circle cx="38" cy="100" r="3.5" fill="#555" />
        <circle cx="92" cy="98" r="3" fill="#555" />
        <circle cx="52" cy="68" r="2.5" fill="#555" />
        <circle cx="78" cy="68" r="3" fill="#555" />
        <circle cx="45" cy="118" r="2" fill="#555" />
        <circle cx="85" cy="116" r="2" fill="#555" />

        <text x="65" y="128" textAnchor="middle" fill={pct <= 20 ? '#ff3344' : pct <= 40 ? '#ff8844' : '#aaa'} fontSize="16" fontWeight="bold">{timeLeft}</text>

        {state === 'critical' && (
          <g>
            <text x="65" y="148" textAnchor="middle" fill="#ff3344" fontSize="9" fontWeight="800" letterSpacing="2">
              ¡EXPLOTA!
              <animate attributeName="opacity" values="1;0;1" dur="0.3s" repeatCount="indefinite" />
            </text>
          </g>
        )}
      </svg>

      <div className="timer-bar-container">
        <div
          className={`timer-bar ${state}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  )
}
