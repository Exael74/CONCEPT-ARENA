import { useState, useRef, useEffect } from 'react'
import './Question.css'

export default function Question({ question, onAnswer }) {
  const [input, setInput] = useState('')
  const [label, setLabel] = useState('')
  const inputRef = useRef(null)

  useEffect(() => {
    setInput('')
    setLabel('')
    inputRef.current?.focus()
  }, [question.id])

  const getLabel = (cat) => {
    const labels = {
      geografia: '🌎 geografía',
      historia: '📜 historia',
      literatura: '📖 literatura',
      gastronomia: '🍲 gastronomía',
      deportes: '⚽ deportes',
      musica: '🎵 música',
      fauna: '🐾 fauna y flora',
      arte: '🎨 arte',
      general: '💡 cultura general'
    }
    return labels[cat] || '💡 general'
  }

  useEffect(() => {
    setLabel(getLabel(question.categoria))
  }, [question.categoria])

  const handleSubmit = (e) => {
    e.preventDefault()
    const value = input.toLowerCase().trim()
    if (!value) return
    onAnswer(value)
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleSubmit(e)
    }
  }

  return (
    <div className="question-card" key={question.id}>
      <span className="question-category">{label}</span>
      <p className="question-text">{question.pregunta}</p>

      <form className="question-form" onSubmit={handleSubmit}>
        <div className="input-wrapper">
          <input
            ref={inputRef}
            type="text"
            className="question-input"
            placeholder="escribe tu respuesta..."
            value={input}
            onChange={(e) => setInput(e.target.value.toLowerCase())}
            onKeyDown={handleKeyDown}
            autoComplete="off"
            autoFocus
          />
          {input && (
            <button
              type="button"
              className="input-clear"
              onClick={() => { setInput(''); inputRef.current?.focus() }}
            >
              ✕
            </button>
          )}
        </div>

        <button
          type="submit"
          className="submit-btn"
          disabled={!input.trim()}
        >
          ¡responder!
        </button>
      </form>

      <p className="question-hint">💡 pista: {question.pista}</p>
    </div>
  )
}
