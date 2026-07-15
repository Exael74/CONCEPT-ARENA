import { useState } from 'react'
import { CATEGORIES } from '../data/players'
import './CreateRoom.css'

export default function CreateRoom({ onBack, onRoomCreated }) {
  const [playerName, setPlayerName] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('todo')

  const handleCreate = (e) => {
    e.preventDefault()
    if (!playerName.trim()) return
    onRoomCreated({ playerName: playerName.trim(), category: selectedCategory })
  }

  return (
    <div className="create-room">
      <button className="create-back" onClick={onBack}>← volver</button>

      <div className="create-content">
        <h2 className="create-title">crear sala</h2>
        <p className="create-desc">configura tu sala de juego</p>

        <form className="create-form" onSubmit={handleCreate}>
          <div className="create-field">
            <label className="create-label">tu nombre</label>
            <input
              type="text"
              className="create-input"
              placeholder="ej: stiven"
              value={playerName}
              onChange={(e) => setPlayerName(e.target.value)}
              autoComplete="off"
              autoFocus
              maxLength={20}
            />
          </div>

          <div className="create-field">
            <label className="create-label">categoría de preguntas</label>
            <div className="category-grid">
              {CATEGORIES.map((cat) => (
                <button
                  type="button"
                  key={cat.id}
                  className={`category-chip ${selectedCategory === cat.id ? 'active' : ''}`}
                  onClick={() => setSelectedCategory(cat.id)}
                >
                  <span className="category-icon">{cat.icon}</span>
                  <span className="category-name">{cat.name}</span>
                </button>
              ))}
            </div>
          </div>

          <button
            type="submit"
            className="create-btn"
            disabled={!playerName.trim()}
          >
            crear sala
          </button>
        </form>
      </div>
    </div>
  )
}
