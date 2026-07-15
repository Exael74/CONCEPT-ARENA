import { useState } from 'react'
import './MainMenu.css'

export default function MainMenu({ onCreateRoom, onJoinRoom }) {
  const [glowing, setGlowing] = useState(true)

  return (
    <div className="main-menu">
      <div className="menu-bg-pattern" />

      <div className="menu-content">
        <div className="menu-bomb-wrapper">
          <svg viewBox="0 0 120 160" className="menu-bomb-svg" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <radialGradient id="menuBombGrad" cx="50%" cy="40%" r="50%">
                <stop offset="0%" stopColor="#444" />
                <stop offset="100%" stopColor="#111" />
              </radialGradient>
              <radialGradient id="menuFuseGrad" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stopColor="#ff6633" />
                <stop offset="100%" stopColor="#ff2200" />
              </radialGradient>
            </defs>
            <rect x="52" y="8" width="16" height="18" rx="3" fill="#666" />
            <rect x="54" y="6" width="12" height="4" rx="2" fill="#888" />
            <circle cx="60" cy="20" r="6" fill="url(#menuFuseGrad)">
              <animate attributeName="r" values="6;7;6" dur="0.5s" repeatCount="indefinite" />
            </circle>
            <ellipse cx="60" cy="100" rx="44" ry="48" fill="url(#menuBombGrad)" stroke="#333" strokeWidth="2" />
            <ellipse cx="60" cy="76" rx="18" ry="4" fill="#222" />
            <rect x="44" y="70" width="32" height="8" rx="2" fill="#333" />
            <circle cx="38" cy="90" r="3" fill="#555" />
            <circle cx="82" cy="88" r="2.5" fill="#555" />
            <circle cx="50" cy="62" r="2" fill="#555" />
            <circle cx="70" cy="64" r="2.5" fill="#555" />
            <text x="60" y="115" textAnchor="middle" fill="#ff3344" fontSize="14" fontWeight="bold">💣</text>
          </svg>
        </div>

        <h1 className="menu-title">
          <span className="menu-title-line">concept</span>
          <span className="menu-title-line accent">arena</span>
        </h1>

        <p className="menu-subtitle">¡pasa la bomba!</p>

        <p className="menu-desc">
          responde preguntas de cultura general de colombia<br />
          antes de que la bomba explote
        </p>

        <div className="menu-buttons">
          <button
            className={`menu-btn primary ${glowing ? 'glow' : ''}`}
            onClick={onCreateRoom}
            onMouseEnter={() => setGlowing(true)}
            onMouseLeave={() => setGlowing(false)}
          >
            <span className="menu-btn-icon">🏠</span>
            crear sala
          </button>

          <button
            className="menu-btn secondary"
            onClick={onJoinRoom}
          >
            <span className="menu-btn-icon">🚪</span>
            unirse a sala
          </button>
        </div>

        <div className="menu-hint">o juega solo creando una sala</div>
      </div>
    </div>
  )
}
