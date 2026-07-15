import Bomb from './Bomb'
import Question from './Question'
import ResultFeedback from './ResultFeedback'
import PlayerAvatar from './PlayerAvatar'
import './GameBoard.css'

export default function GameBoard({ players, currentQuestion, lastResult, timeLeft, totalTime, onAnswer, questionIndex, totalQuestions }) {
  const slots = []
  for (let i = 0; i < 4; i++) {
    slots.push(players[i] || { id: null, name: 'vacío', emoji: '👻', isYou: false, status: 'empty' })
  }

  return (
    <div className="game-board">
      <div className="gb-header">
        <div className="gb-stat">
          <span className="gb-stat-label">puntaje</span>
          <span className="gb-stat-value score">{players.find(p => p.isYou)?.score || 0}</span>
        </div>
        <div className="gb-stat">
          <span className="gb-stat-label">pregunta</span>
          <span className="gb-stat-value">{questionIndex + 1} / {totalQuestions}</span>
        </div>
      </div>

      <div className="gb-grid">
        <div className="gb-cell gb-tl">
          <PlayerAvatar player={slots[0]} position="tl" />
        </div>
        <div className="gb-cell gb-tr">
          <PlayerAvatar player={slots[1]} position="tr" />
        </div>
        <div className="gb-cell gb-bl">
          <PlayerAvatar player={slots[2]} position="bl" />
        </div>
        <div className="gb-cell gb-br">
          <PlayerAvatar player={slots[3]} position="br" />
        </div>

        <div className="gb-cell gb-center">
          <Bomb timeLeft={timeLeft} totalTime={totalTime} />
          {lastResult ? (
            <ResultFeedback result={lastResult} />
          ) : (
            currentQuestion && <Question question={currentQuestion} onAnswer={onAnswer} />
          )}
        </div>
      </div>
    </div>
  )
}
