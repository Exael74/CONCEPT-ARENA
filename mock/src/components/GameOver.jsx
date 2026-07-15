import { useEffect, useState } from 'react'
import './GameOver.css'

function ordinal(i) {
  return ['1°', '2°', '3°', '4°'][i] || `${i+1}°`
}

export default function GameOver({ score, correctCount, totalQuestions, onRestart, players }) {
  const [phase, setPhase] = useState('explosion')
  const [particles, setParticles] = useState([])

  useEffect(() => {
    const p = Array.from({ length: 60 }, (_, i) => ({
      id: i,
      x: 50 + (Math.random() - 0.5) * 60,
      y: 50 + (Math.random() - 0.5) * 60,
      size: 4 + Math.random() * 12,
      color: ['#ff4400', '#ff8800', '#ffcc00', '#ff2244', '#ffffff'][Math.floor(Math.random() * 5)],
      angle: Math.random() * 360,
      distance: 40 + Math.random() * 120,
      duration: 0.5 + Math.random() * 1.5,
      delay: Math.random() * 0.3
    }))
    setParticles(p)

    const t = setTimeout(() => setPhase('results'), 2000)
    return () => clearTimeout(t)
  }, [])

  const pct = totalQuestions > 0 ? Math.round((correctCount / totalQuestions) * 100) : 0
  const sorted = players ? [...players].filter(p => p.status !== 'empty').sort((a, b) => b.score - a.score) : []
  const isFirst = sorted.length > 0 && sorted[0].isYou

  return (
    <div className={`gameover-screen ${phase}`}>
      {phase === 'explosion' && (
        <div className="explosion-container">
          <div className="explosion-flash" />
          <div className="explosion-ring ring-1" />
          <div className="explosion-ring ring-2" />
          <div className="explosion-ring ring-3" />
          {particles.map((p) => (
            <div
              key={p.id}
              className="particle"
              style={{
                '--x': `${p.x}%`,
                '--y': `${p.y}%`,
                '--size': `${p.size}px`,
                '--color': p.color,
                '--angle': `${p.angle}deg`,
                '--distance': `${p.distance}px`,
                '--duration': `${p.duration}s`,
                '--delay': `${p.delay}s`,
              }}
            />
          ))}
          <div className="explosion-text">💥</div>
        </div>
      )}

      {phase === 'results' && (
        <div className="results-container">
          <div className="results-trophy">
            {isFirst ? '🏆' : sorted.length > 0 && sorted[0].score > 0 ? '👏' : '😵'}
          </div>

          <h2 className="results-title">juego terminado</h2>

          <div className="results-stats">
            <div className="stat-card">
              <span className="stat-value">{score}</span>
              <span className="stat-label">tu puntaje</span>
            </div>
            <div className="stat-card">
              <span className="stat-value">{correctCount}/{totalQuestions}</span>
              <span className="stat-label">correctas</span>
            </div>
            <div className="stat-card">
              <span className="stat-value">{pct}%</span>
              <span className="stat-label">precision</span>
            </div>
          </div>

          <div className="results-message">
            {pct >= 80 && '¡sabes mucho de colombia! 🎉'}
            {pct >= 50 && pct < 80 && 'nada mal, pero puedes mejorar 💪'}
            {pct < 50 && '¡hay que estudiar mas la cultura colombiana! 📚'}
          </div>

          {sorted.length > 1 && (
            <div className="ranking-table">
              <div className="ranking-header">clasificacion</div>
              {sorted.map((p, i) => (
                <div key={p.id} className={`ranking-row ${p.isYou ? 'highlight' : ''}`}>
                  <span className="ranking-pos">{ordinal(i)}</span>
                  <span className="ranking-avatar">{p.emoji}</span>
                  <span className="ranking-name">{p.name}{p.isYou ? ' (tú)' : ''}</span>
                  <span className="ranking-score">{p.score} pts</span>
                  <span className="ranking-correct">{p.correctCount} aciertos</span>
                </div>
              ))}
            </div>
          )}

          <button className="restart-btn" onClick={onRestart}>
            jugar de nuevo
          </button>
        </div>
      )}
    </div>
  )
}
