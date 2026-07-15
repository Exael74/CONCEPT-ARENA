import './Lobby.css'

const EMPTY_SLOT = { id: null, name: 'vacío', emoji: '👻', isYou: false, status: 'empty' }

export default function Lobby({ roomCode, players, onStart, onLeave }) {
  const slots = []
  for (let i = 0; i < 4; i++) {
    slots.push(players[i] || EMPTY_SLOT)
  }

  return (
    <div className="lobby">
      <div className="lobby-header">
        <button className="lobby-back" onClick={onLeave}>← salir</button>
      </div>

      <div className="lobby-content">
        <div className="lobby-room-info">
          <span className="lobby-room-label">codigo de sala</span>
          <span className="lobby-room-code">{roomCode}</span>
          <span className="lobby-room-hint">comparte este codigo con tus compañeros</span>
        </div>

        <div className="lobby-players-title">
          <span className="lobby-players-label">jugadores</span>
          <span className="lobby-players-count">{players.length}/4</span>
        </div>

        <div className="lobby-players-grid">
          {slots.map((slot, i) => (
            <div key={i} className={`lobby-player-slot ${slot.status === 'empty' ? 'empty' : 'filled'}`}>
              <div className="lobby-player-avatar">{slot.emoji}</div>
              <span className="lobby-player-name">{slot.name}</span>
              {slot.isYou && <span className="lobby-player-you">(tú)</span>}
              {slot.status === 'empty' && <span className="lobby-player-waiting">esperando...</span>}
            </div>
          ))}
        </div>

        <button
          className="lobby-start-btn"
          onClick={onStart}
          disabled={players.length < 1}
        >
          {players.length < 2 ? 'iniciar partida (demo individual)' : '¡iniciar partida!'}
        </button>
      </div>
    </div>
  )
}
