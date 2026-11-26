import { lazy, Suspense, useState } from 'react'
import type { Answers } from '../model/demo'
import { EntryFlow, Survey } from './Onboarding'
import './demo.css'

const MapExperience = lazy(() => import('./MapExperience').then(module => ({ default: module.MapExperience })))

type Phase = 'cover' | 'login' | 'welcome' | 'survey' | 'map'

export default function DemoPage() {
  const [phase, setPhase] = useState<Phase>('cover')
  const [answers, setAnswers] = useState<Answers>({})
  const [editing, setEditing] = useState(false)
  if (phase === 'map') return <Suspense fallback={<main className="demo-loading" role="status">행궁동 지도를 펼치고 있어요…</main>}><MapExperience onResurvey={() => { setEditing(true); setPhase('survey') }} onLogout={() => { setAnswers({}); setEditing(false); setPhase('cover') }} /></Suspense>
  if (phase === 'survey') return <Survey initialAnswers={answers} editing={editing} onComplete={value => { setAnswers(value); setEditing(false); setPhase('map') }} onCancel={() => setPhase(editing ? 'map' : 'welcome')} />
  return <EntryFlow stage={phase} onStage={setPhase} onLogin={() => setPhase(Object.keys(answers).length ? 'map' : 'welcome')} onStart={() => { setEditing(false); setPhase('survey') }} />
}
