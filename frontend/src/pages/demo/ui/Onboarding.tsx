import { useEffect, useRef, useState } from 'react'
import type { FormEvent, CSSProperties } from 'react'
import { ActionButton, TextField, TextFieldInput } from '../../../shared/ui'
import { mockQuestionnaire } from '../mock'
import { toggleAnswer, type Answers } from '../model/demo'
import { Glyph } from './Icons'

export function Brand({ compact = false }: { compact?: boolean }) {
  return <span className={`demo-brand${compact ? ' compact' : ''}`}><span className="brand-mark"><Glyph name="cup" size={22} /></span>카페 지도</span>
}

export function EntryFlow({ stage, onStage, onLogin, onStart }: {
  stage: 'cover' | 'login' | 'welcome'
  onStage: (stage: 'cover' | 'login') => void
  onLogin: () => void
  onStart: () => void
}) {
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const heading = useRef<HTMLHeadingElement>(null)
  useEffect(() => { heading.current?.focus() }, [stage])
  function login(event: FormEvent) {
    event.preventDefault()
    if (!loginId.trim() || !password) return
    setPassword('')
    onLogin()
  }
  return <main className={`entry-page stage-${stage}`}>
    <header className="entry-header"><Brand /><span className="neighborhood-label">HAENGGUNG-DONG</span></header>
    {stage === 'cover' ? <div className="cover-layout entrance" key="cover">
      <div className="cover-copy"><h1 ref={heading} tabIndex={-1}>행궁동<br /><span>카페 탐색</span></h1><ActionButton variant="brandSolid" size="large" className="cover-start" onClick={() => onStage('login')}>시작하기<Glyph name="arrow" /></ActionButton></div>
      <div className="cover-scene" aria-hidden="true"><div className="scene-orbit orbit-one" /><div className="scene-orbit orbit-two" /><div className="scene-map"><img src="/illustrations/cover-map.svg" alt="" /></div><div className="scene-cup"><img src="/illustrations/latte.svg" alt="" /></div><span className="scene-location"><Glyph name="pin" size={18} />행궁동</span><span className="scene-spark spark-one">✳</span><span className="scene-spark spark-two">✦</span><span className="scene-pin pin-one"><Glyph name="cup" size={20} /></span><span className="scene-pin pin-two"><Glyph name="cup" size={18} /></span></div>
    </div> : stage === 'login' ? <section className="flow-panel entrance" key="login">
      <button type="button" className="icon-button flow-back" aria-label="표지로 돌아가기" onClick={() => onStage('cover')}><Glyph name="back" /></button>
      <h1 ref={heading} tabIndex={-1}>로그인</h1>
      <form onSubmit={login} className="login-form">
        <TextField label="아이디" value={loginId} onValueChange={({ value }) => setLoginId(value)}><TextFieldInput name="loginId" autoComplete="off" placeholder="아이디를 입력해주세요" maxLength={128} required /></TextField>
        <TextField label="비밀번호" value={password} onValueChange={({ value }) => setPassword(value)}><TextFieldInput name="password" type="password" autoComplete="off" placeholder="비밀번호를 입력해주세요" required /></TextField>
        <p className="login-hint">시연에서는 원하는 아이디와 비밀번호로 시작할 수 있어요.</p>
        <ActionButton type="submit" variant="brandSolid" size="large" disabled={!loginId.trim() || !password}>로그인<Glyph name="arrow" /></ActionButton>
      </form>
    </section> : <section className="flow-panel welcome-panel entrance" key="welcome">
      <span className="welcome-symbol" aria-hidden="true">✳</span><h1 ref={heading} tabIndex={-1}>취향 설문</h1><p>메뉴 탐색에 사용할 취향을 선택해주세요.</p><div className="welcome-meta"><span>5개의 질문</span><i /><span>약 30초</span></div><ActionButton variant="brandSolid" size="large" onClick={onStart}>설문 시작<Glyph name="arrow" /></ActionButton>
    </section>}
    <footer className="entry-footer"><span>중간 발표용 시연</span></footer>
  </main>
}

export function Survey({ initialAnswers, editing, onComplete, onCancel }: {
  initialAnswers: Answers
  editing: boolean
  onComplete: (answers: Answers) => void
  onCancel: () => void
}) {
  const [answers, setAnswers] = useState<Answers>(() => Object.fromEntries(Object.entries(initialAnswers).map(([key, value]) => [key, [...value]])))
  const [index, setIndex] = useState(0)
  const [backward, setBackward] = useState(false)
  const heading = useRef<HTMLHeadingElement>(null)
  const question = mockQuestionnaire.questions[index]
  const selected = answers[question.id] ?? []
  useEffect(() => { heading.current?.focus() }, [index])
  function next() {
    if (selected.length < question.minSelections) return
    if (index === mockQuestionnaire.questions.length - 1) onComplete(answers)
    else { setBackward(false); setIndex(value => value + 1) }
  }
  function previous() {
    if (index === 0) onCancel()
    else { setBackward(true); setIndex(value => value - 1) }
  }
  return <main className="survey-page">
    <header className="survey-header"><button type="button" className="icon-button" aria-label="이전 단계" onClick={previous}><Glyph name="back" /></button><Brand compact /><button type="button" className="text-button" onClick={onCancel}>{editing ? '취소' : '나가기'}</button></header>
    <div className="survey-progress" aria-label={`질문 ${index + 1} / 5`}><span style={{ width: `${(index + 1) * 20}%` }} /></div>
    <section className={`question-panel${backward ? ' backwards' : ''}`} key={question.id}>
      <div className="question-count"><span>취향 설문</span><span><b>0{index + 1}</b> / 05</span></div>
      <h1 ref={heading} tabIndex={-1}>{question.label}</h1><p id="question-help">{question.description}</p>
      <fieldset className="answer-options" aria-describedby="question-help"><legend className="visually-hidden">{question.label}</legend>{question.options.map((option, optionIndex) => {
        const checked = selected.includes(option.id)
        const disabled = question.type === 'multiple' && selected.length >= question.maxSelections && !checked && option.id !== 'any'
        return <label className={`answer-option${checked ? ' is-selected' : ''}`} key={option.id} style={{ '--option-index': optionIndex } as CSSProperties}>
          <input type={question.type === 'single' ? 'radio' : 'checkbox'} name={question.id} value={option.id} checked={checked} disabled={disabled} onChange={() => setAnswers(previous => ({ ...previous, [question.id]: toggleAnswer(question, previous[question.id] ?? [], option.id) }))} />
          <span className="answer-symbol" aria-hidden="true">{option.symbol}</span><span>{option.label}</span><span className="answer-check" aria-hidden="true"><Glyph name="check" size={18} /></span>
        </label>
      })}</fieldset>
    </section>
    <div className="survey-bottom"><ActionButton variant="brandSolid" size="large" disabled={selected.length < question.minSelections} onClick={next}>{index === 4 ? '완료' : '다음'}<Glyph name="arrow" /></ActionButton></div>
  </main>
}
