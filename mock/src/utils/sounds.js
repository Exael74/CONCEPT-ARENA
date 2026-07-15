const AudioCtx = window.AudioContext || window.webkitAudioContext
let ctx = null

function getCtx() {
  if (!ctx) ctx = new AudioCtx()
  return ctx
}

export function playTick() {
  try {
    const c = getCtx()
    const osc = c.createOscillator()
    const gain = c.createGain()
    osc.connect(gain)
    gain.connect(c.destination)
    osc.frequency.value = 800
    osc.type = 'sine'
    gain.gain.setValueAtTime(0.1, c.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, c.currentTime + 0.08)
    osc.start(c.currentTime)
    osc.stop(c.currentTime + 0.08)
  } catch (e) {}
}

export function playCorrect() {
  try {
    const c = getCtx()
    const osc = c.createOscillator()
    const gain = c.createGain()
    osc.connect(gain)
    gain.connect(c.destination)
    osc.frequency.setValueAtTime(523, c.currentTime)
    osc.frequency.setValueAtTime(659, c.currentTime + 0.1)
    osc.frequency.setValueAtTime(784, c.currentTime + 0.2)
    osc.type = 'sine'
    gain.gain.setValueAtTime(0.15, c.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, c.currentTime + 0.4)
    osc.start(c.currentTime)
    osc.stop(c.currentTime + 0.4)
  } catch (e) {}
}

export function playWrong() {
  try {
    const c = getCtx()
    const osc = c.createOscillator()
    const gain = c.createGain()
    osc.connect(gain)
    gain.connect(c.destination)
    osc.frequency.value = 200
    osc.type = 'sawtooth'
    gain.gain.setValueAtTime(0.12, c.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, c.currentTime + 0.3)
    osc.start(c.currentTime)
    osc.stop(c.currentTime + 0.3)
  } catch (e) {}
}

export function playExplosion() {
  try {
    const c = getCtx()
    const bufferSize = c.sampleRate * 0.5
    const buffer = c.createBuffer(1, bufferSize, c.sampleRate)
    const data = buffer.getChannelData(0)
    for (let i = 0; i < bufferSize; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / bufferSize, 3)
    }
    const noise = c.createBufferSource()
    noise.buffer = buffer
    const gain = c.createGain()
    const filter = c.createBiquadFilter()
    filter.type = 'lowpass'
    filter.frequency.setValueAtTime(1000, c.currentTime)
    filter.frequency.exponentialRampToValueAtTime(50, c.currentTime + 0.5)
    noise.connect(filter)
    filter.connect(gain)
    gain.connect(c.destination)
    gain.gain.setValueAtTime(0.3, c.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, c.currentTime + 0.5)
    noise.start(c.currentTime)
    noise.stop(c.currentTime + 0.5)
  } catch (e) {}
}
