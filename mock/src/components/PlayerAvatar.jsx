import './PlayerAvatar.css'

export default function PlayerAvatar({ player, position }) {
  const statusIcon = {
    waiting: '⏳',
    answering: '✍️',
    correct: '✅',
    incorrect: '❌',
    timeout: '💀',
    empty: ''
  }

  const statusClass = player.status === 'empty' ? 'empty' : player.status === 'correct' ? 'correct' : player.status === 'incorrect' || player.status === 'timeout' ? 'incorrect' : ''

  return (
    <div className={`player-avatar-wrapper ${statusClass} position-${position}`}>
      <div className="player-avatar-ring">
        <div className="player-avatar-emoji">{player.emoji}</div>
        {player.isYou && <div className="player-avatar-badge">tú</div>}
      </div>

      <div className="player-avatar-info">
        <span className="player-avatar-name">{player.name}</span>
        <span className="player-avatar-score">{player.score} pts</span>
        <span className={`player-avatar-status ${statusClass}`}>
          {statusIcon[player.status] || ''}
          {player.status === 'waiting' && !player.isYou && 'esperando'}
          {player.status === 'answering' && 'respondiendo...'}
          {player.status === 'correct' && 'correcto'}
          {player.status === 'incorrect' && 'incorrecto'}
          {player.status === 'timeout' && 'tiempo agotado'}
        </span>
      </div>
    </div>
  )
}
