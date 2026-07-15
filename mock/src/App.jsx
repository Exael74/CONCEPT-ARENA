import { useState, useEffect, useCallback, useRef } from 'react'
import MainMenu from './components/MainMenu'
import CreateRoom from './components/CreateRoom'
import JoinRoom from './components/JoinRoom'
import Lobby from './components/Lobby'
import GameBoard from './components/GameBoard'
import GameOver from './components/GameOver'
import questions from './data/questions'
import { DEMO_PLAYERS, generateRoomCode } from './data/players'
import { playTick, playCorrect, playWrong, playExplosion } from './utils/sounds'

const TOTAL_TIME = 30
const QUESTIONS_PER_GAME = 10

function shuffle(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

function initPlayers(overrides = {}) {
  return DEMO_PLAYERS.map(p => ({
    ...p,
    score: 0,
    correctCount: 0,
    status: 'waiting',
    answered: false,
    totalAnswered: 0,
    ...overrides,
  }))
}

export default function App() {
  const [screen, setScreen] = useState('menu')
  const [roomCode, setRoomCode] = useState('')
  const [category, setCategory] = useState('todo')
  const [players, setPlayers] = useState([])

  const [questionIndex, setQuestionIndex] = useState(0)
  const [timeLeft, setTimeLeft] = useState(TOTAL_TIME)
  const [lastResult, setLastResult] = useState(null)
  const [roundQuestions, setRoundQuestions] = useState([])

  const timerRef = useRef(null)
  const tickIntervalRef = useRef(null)
  const botTimeoutsRef = useRef([])
  const humanAnsweredRef = useRef(false)

  const currentQuestion = roundQuestions[questionIndex]

  // keep a ref of players for use inside callbacks
  const playersRef = useRef(players)
  playersRef.current = players

  // --- Navigation ---
  const goTo = useCallback((s) => setScreen(s), [])

  const handleRoomCreated = useCallback(({ playerName, category: cat }) => {
    const code = generateRoomCode()
    setRoomCode(code)
    setCategory(cat)
    const p = initPlayers()
    p[0].name = playerName
    p[0].isYou = true
    setPlayers(p)
    setScreen('lobby')
  }, [])

  const handleJoinAttempt = useCallback(({ code, playerName }) => {
    setRoomCode(code)
    const p = initPlayers()
    p[0].name = playerName
    p[0].isYou = true
    setPlayers(p)
    setScreen('lobby')
  }, [])

  const handleStartGame = useCallback(() => {
    let qs = questions
    if (category !== 'todo') {
      qs = questions.filter(q => q.categoria === category)
      if (qs.length === 0) qs = questions
    }
    const shuffled = shuffle(qs).slice(0, QUESTIONS_PER_GAME)
    setRoundQuestions(shuffled)
    setQuestionIndex(0)
    setTimeLeft(TOTAL_TIME)
    setLastResult(null)
    humanAnsweredRef.current = false

    setPlayers(prev => prev.map(p => ({
      ...p,
      score: 0,
      correctCount: 0,
      status: 'answering',
      answered: false,
      totalAnswered: 0,
    })))

    setScreen('playing')
  }, [category])

  const handlePlayAgain = useCallback(() => {
    botTimeoutsRef.current.forEach(t => clearTimeout(t))
    setScreen('menu')
  }, [])

  // --- Timer ---
  useEffect(() => {
    if (screen !== 'playing') {
      clearInterval(timerRef.current)
      clearInterval(tickIntervalRef.current)
      return
    }

    timerRef.current = setInterval(() => {
      setTimeLeft((t) => {
        if (t <= 1) {
          clearInterval(timerRef.current)
          clearInterval(tickIntervalRef.current)
          playExplosion()
          setScreen('gameover')
          return 0
        }
        return t - 1
      })
    }, 1000)

    tickIntervalRef.current = setInterval(() => {
      playTick()
    }, 1000)

    return () => {
      clearInterval(timerRef.current)
      clearInterval(tickIntervalRef.current)
    }
  }, [screen, questionIndex])

  // --- Schedule bot answers ---
  useEffect(() => {
    if (screen !== 'playing' || !currentQuestion) return

    botTimeoutsRef.current.forEach(t => clearTimeout(t))
    botTimeoutsRef.current = []

    const cur = playersRef.current

    cur.forEach((player, idx) => {
      if (player.isYou) return

      const jitter = (Math.random() - 0.5) * 4000
      const delay = Math.max(1500, Math.min(26000, player.avgResponseMs + jitter))

      const id = setTimeout(() => {
        const pNow = playersRef.current[idx]
        if (!pNow) return

        const correct = Math.random() < pNow.accuracy
        const bonus = Math.floor(((TOTAL_TIME * 1000 - delay) / (TOTAL_TIME * 1000)) * 50)
        const pts = correct ? 100 + bonus : 0

        setPlayers(prev => prev.map((p, i) => {
          if (i !== idx) return p
          return {
            ...p,
            score: p.score + pts,
            correctCount: p.correctCount + (correct ? 1 : 0),
            status: correct ? 'correct' : 'incorrect',
            answered: true,
            totalAnswered: p.totalAnswered + 1,
          }
        }))

        if (correct) playCorrect()
        else playWrong()
      }, delay)

      botTimeoutsRef.current.push(id)
    })

    return () => {
      botTimeoutsRef.current.forEach(t => clearTimeout(t))
    }
  }, [screen, questionIndex, currentQuestion?.id])

  // --- Human answer ---
  const handleAnswer = useCallback((answer) => {
    if (humanAnsweredRef.current || !currentQuestion) return
    humanAnsweredRef.current = true

    const correct = answer === currentQuestion.respuesta
    const bonus = Math.floor((timeLeft / TOTAL_TIME) * 50)
    const points = correct ? 100 + bonus : 0

    setPlayers(prev => prev.map(p => {
      if (!p.isYou) return p
      return {
        ...p,
        score: p.score + points,
        correctCount: p.correctCount + (correct ? 1 : 0),
        status: correct ? 'correct' : 'incorrect',
        answered: true,
        totalAnswered: p.totalAnswered + 1,
      }
    }))

    setLastResult({
      correct,
      userAnswer: answer,
      correctAnswer: currentQuestion.respuesta,
      question: currentQuestion.pregunta,
      points,
    })

    if (correct) playCorrect()
    else playWrong()

    setTimeout(() => {
      setLastResult(null)
      humanAnsweredRef.current = false
      if (questionIndex + 1 >= roundQuestions.length) {
        setScreen('gameover')
      } else {
        setQuestionIndex(i => i + 1)
        setTimeLeft(TOTAL_TIME)
      }
    }, 2000)
  }, [currentQuestion, timeLeft, questionIndex, roundQuestions.length])

  // --- Render ---
  if (screen === 'menu') {
    return <MainMenu onCreateRoom={() => goTo('createRoom')} onJoinRoom={() => goTo('joinRoom')} />
  }

  if (screen === 'createRoom') {
    return <CreateRoom onBack={() => goTo('menu')} onRoomCreated={handleRoomCreated} />
  }

  if (screen === 'joinRoom') {
    return <JoinRoom onBack={() => goTo('menu')} onJoinAttempt={handleJoinAttempt} />
  }

  if (screen === 'lobby') {
    return (
      <Lobby
        roomCode={roomCode}
        players={players}
        onStart={handleStartGame}
        onLeave={() => goTo('menu')}
      />
    )
  }

  if (screen === 'gameover') {
    const human = players.find(p => p.isYou)
    return (
      <GameOver
        score={human?.score || 0}
        correctCount={human?.correctCount || 0}
        totalQuestions={human?.totalAnswered || 0}
        onRestart={handlePlayAgain}
        players={players}
      />
    )
  }

  return (
    <GameBoard
      players={players}
      currentQuestion={currentQuestion}
      lastResult={lastResult}
      timeLeft={timeLeft}
      totalTime={TOTAL_TIME}
      onAnswer={handleAnswer}
      questionIndex={questionIndex}
      totalQuestions={roundQuestions.length}
    />
  )
}
