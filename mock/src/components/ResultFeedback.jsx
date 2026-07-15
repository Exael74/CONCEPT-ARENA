import './ResultFeedback.css'

export default function ResultFeedback({ result }) {
  return (
    <div className={`result-feedback ${result.correct ? 'correct' : 'wrong'}`}>
      <div className="result-icon">{result.correct ? '✅' : '💥'}</div>
      <div className="result-text">
        <span className="result-status">
          {result.correct ? '¡correcto!' : '¡incorrecto!'}
        </span>
        {!result.correct && (
          <span className="result-answer">
            respuesta correcta: <strong>{result.correctAnswer}</strong>
          </span>
        )}
        {result.correct && result.points && (
          <span className="result-points">+{result.points} puntos</span>
        )}
      </div>
    </div>
  )
}
