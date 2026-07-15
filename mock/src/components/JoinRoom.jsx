import { useState } from 'react'
import './JoinRoom.css'

export default function JoinRoom({ onBack, onJoinAttempt }) {
  const [code, setCode] = useState('')
  const [playerName, setPlayerName] = useState('')
  const [error, setError] = useState('')

  const handleJoin = (e) => {
    e.preventDefault()
    const cleaned = code.toUpperCase().replace(/[^A-Z0-9]/g, '')
    if (cleaned.length !== 6) {
      setError('el codigo debe tener 6 caracteres')
      return
    }
    if (!playerName.trim()) {
      setError('ingresa tu nombre')
      return
    }
    setError('')
    onJoinAttempt({ code: cleaned, playerName: playerName.trim() })
  }

  return (
    <div className="join-room">
      <button className="join-back" onClick={onBack}>← volver</button>

      <div className="join-content">
        <h2 className="join-title">unirse a sala</h2>
        <p className="join-desc">ingresa el codigo que te compartieron</p>

        <form className="join-form" onSubmit={handleJoin}>
          <div className="join-field">
            <label className="join-label">codigo de sala</label>
            <input
              type="text"
              className="join-code-input"
              placeholder="ABCD12"
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6))}
              autoComplete="off"
              autoFocus
              maxLength={6}
            />
          </div>

          <div className="join-field">
            <label className="join-label">tu nombre</label>
            <input
              type="text"
              className="join-input"
              placeholder="ej: stiven"
              value={playerName}
              onChange={(e) => setPlayerName(e.target.value)}
              autoComplete="off"
              maxLength={20}
            />
          </div>

          {error && <p className="join-error">{error}</p>}

          <button
            type="submit"
            className="join-btn"
            disabled={!code.trim() || !playerName.trim()}
          >
            unirse
          </button>
        </form>
      </div>
    </div>
  )
}
