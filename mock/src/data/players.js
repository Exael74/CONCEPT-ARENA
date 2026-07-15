export const DEMO_PLAYERS = [
  { id: 'p1', name: 'tú',     emoji: '🧑', score: 0, isYou: true,  status: 'waiting', accuracy: 1.0,  avgResponseMs: 4000, answered: false },
  { id: 'p2', name: 'maría',  emoji: '👩‍🦰', score: 0, isYou: false, status: 'waiting', accuracy: 0.75, avgResponseMs: 7000, answered: false },
  { id: 'p3', name: 'juan',   emoji: '👨‍🦱', score: 0, isYou: false, status: 'waiting', accuracy: 0.60, avgResponseMs: 11000, answered: false },
  { id: 'p4', name: 'carlos', emoji: '🧔', score: 0, isYou: false, status: 'waiting', accuracy: 0.85, avgResponseMs: 5000,  answered: false },
]

export function generateRoomCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 6; i++) {
    code += chars[Math.floor(Math.random() * chars.length)]
  }
  return code
}

export function checkRoomCode(input) {
  return input.length === 6 && /^[A-Z0-9]+$/.test(input.toUpperCase())
}

export const CATEGORIES = [
  { id: 'todo', name: 'todas las categorías', icon: '🌟' },
  { id: 'geografia', name: 'geografía', icon: '🌎' },
  { id: 'historia', name: 'historia', icon: '📜' },
  { id: 'gastronomia', name: 'gastronomía', icon: '🍲' },
  { id: 'musica', name: 'música', icon: '🎵' },
  { id: 'deportes', name: 'deportes', icon: '⚽' },
  { id: 'literatura', name: 'literatura', icon: '📖' },
  { id: 'arte', name: 'arte', icon: '🎨' },
  { id: 'fauna', name: 'fauna y flora', icon: '🐾' },
]
