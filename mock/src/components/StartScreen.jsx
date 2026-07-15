import { useState } from 'react'
import './StartScreen.css'

export default function StartScreen({ onStart }) {
  const [glowing, setGlowing] = useState(true)

  return (
    <div className="start-screen">
      <div className="start-bg-pattern" />

      <div className="start-content">
        <div className="start-bomb-wrapper">
          <svg viewBox="0 0 120 160" className="start-bomb-svg" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <radialGradient id="bombGrad" cx="50%" cy="40%" r="50%">
                <stop offset="0%" stopColor="#444" />
                <stop offset="100%" stopColor="#111" />
              </radialGradient>
              <radialGradient id="fuseGrad" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stopColor="#ff6633" />
                <stop offset="100%" stopColor="#ff2200" />
              </radialGradient>
            </defs>
            <rect x="52" y="8" width="16" height="18" rx="3" fill="#666" />
            <rect x="54" y="6" width="12" height="4" rx="2" fill="#888" />
            <circle cx="60" cy="20" r="6" fill="url(#fuseGrad)">
              <animate attributeName="r" values="6;7;6" dur="0.5s" repeatCount="indefinite" />
            </circle>
            <ellipse cx="60" cy="100" rx="44" ry="48" fill="url(#bombGrad)" stroke="#333" strokeWidth="2" />
            <ellipse cx="60" cy="76" rx="18" ry="4" fill="#222" />
            <rect x="44" y="70" width="32" height="8" rx="2" fill="#333" />
            <circle cx="38" cy="90" r="3" fill="#555" />
            <circle cx="82" cy="88" r="2.5" fill="#555" />
            <circle cx="50" cy="62" r="2" fill="#555" />
            <circle cx="70" cy="64" r="2.5" fill="#555" />
            <text x="60" y="115" textAnchor="middle" fill="#ff3344" fontSize="14" fontWeight="bold">💣</text>
          </svg>
        </div>

        <h1 className="start-title">
          <span className="title-line">concept</span>
          <span className="title-line accent">arena</span>
        </h1>

        <p className="start-subtitle">¡pasa la bomba!</p>

        <p className="start-desc">
          responde preguntas de cultura general de colombia<br />
          antes de que la bomba explote
        </p>

        <div className="start-instructions">
          <div className="instruction-item">
            <span className="instruction-icon">⏱️</span>
            <span>30 segundos por pregunta</span>
          </div>
          <div className="instruction-item">
            <span className="instruction-icon">⚡</span>
            <span>las respuestas se escriben en minúsculas</span>
          </div>
          <div className="instruction-item">
            <span className="instruction-icon">🏆</span>
            <span>a mayor rapidez, mas puntaje</span>
          </div>
        </div>

        <button
          className={`start-btn ${glowing ? 'glow' : ''}`}
          onClick={onStart}
          onMouseEnter={() => setGlowing(true)}
          onMouseLeave={() => setGlowing(false)}
        >
          ¡comenzar partida!
        </button>
      </div>
    </div>
  )
}
