// dismiss-a.jsx — Direction A · "Minimal Counter" (by the book, vertical stack).

function DismissA({ theme, accent, config, initialPhase = 'ring' }) {
  const cfg = config || { squats: 10, math: 2, level: 'Medium', h: 6, m: 30, period: 'AM', label: 'Weekday wake-up' };
  const [phase, setPhase] = React.useState(initialPhase); // ring | squats | math | done
  const [reps, setReps] = React.useState(0);
  const [qi, setQi] = React.useState(0);
  const [problem, setProblem] = React.useState(() => genProblem(cfg.level));
  const [entry, setEntry] = React.useState('');
  const [wrong, setWrong] = React.useState(false);

  const reset = () => { setPhase(initialPhase === 'done' ? 'done' : 'ring'); setReps(0); setQi(0); setEntry(''); setProblem(genProblem(cfg.level)); };
  const onRep = () => setReps(r => {
    const n = r + 1;
    if (n >= cfg.squats) setTimeout(() => setPhase(cfg.math ? 'math' : 'done'), 350);
    return Math.min(n, cfg.squats);
  });
  const squat = useSquat(onRep, theme);

  const pressKey = (k) => {
    if (k === 'back') return setEntry(e => e.slice(0, -1));
    if (k === 'clear') return setEntry('');
    const next = (entry + k).slice(0, 5);
    setEntry(next);
    if (parseInt(next, 10) === problem.answer) {
      setTimeout(() => {
        if (qi + 1 >= cfg.math) setPhase('done');
        else { setQi(qi + 1); setProblem(genProblem(cfg.level)); setEntry(''); }
      }, 260);
    } else if (next.length >= String(problem.answer).length) {
      setWrong(true); setTimeout(() => { setWrong(false); setEntry(''); }, 420);
    }
  };

  const Header = ({ kicker, step }) => (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '4px 4px 0' }}>
      <span style={{ fontSize: 12, letterSpacing: 1.5, fontWeight: 700, color: theme.textDim }}>{kicker}</span>
      <span style={{ fontFamily: theme.fonts.num, fontSize: 13, fontWeight: 600, color: accent, fontVariantNumeric: 'tabular-nums' }}>{step}</span>
    </div>
  );

  const wrap = (inner) => (
    <div style={{ height: '100%', background: 'transparent', color: theme.text, fontFamily: theme.fonts.ui, display: 'flex', flexDirection: 'column', padding: '16px 22px 22px', position: 'relative' }}>
      {phase !== 'ring' && phase !== 'done' && (
        <button onClick={reset} style={{ position: 'absolute', top: 14, right: 16, width: 34, height: 34, borderRadius: '50%', border: 'none', background: 'transparent', color: theme.textMute, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2 }}><Icon name="repeat" size={16} stroke={2} /></button>
      )}
      {inner}
    </div>
  );

  if (phase === 'ring') return wrap(
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
      <div style={{ position: 'relative', marginBottom: 30 }}>
        <span style={{ position: 'absolute', inset: -16, borderRadius: '50%', border: `2px solid ${accent}`, opacity: 0.25, animation: 'aPulse 2s ease-out infinite' }} />
        <span style={{ width: 64, height: 64, borderRadius: '50%', background: theme.accentSoft, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Icon name="clock" size={30} color={accent} stroke={2} /></span>
      </div>
      <div style={{ fontSize: 13, letterSpacing: 2, fontWeight: 700, color: accent, marginBottom: 10 }}>ALARM</div>
      <div style={{ fontFamily: theme.fonts.num, fontWeight: theme.fonts.numWeight, fontSize: 74, letterSpacing: -2, lineHeight: 0.9 }}>{cfg.h}:{String(cfg.m).padStart(2,'0')}<span style={{ fontFamily: theme.fonts.ui, fontSize: 22, fontWeight: 600, color: theme.textDim, marginLeft: 8 }}>{cfg.period}</span></div>
      <div style={{ marginTop: 12, fontSize: 16, color: theme.textDim }}>{cfg.label}</div>
      <div style={{ flex: 1 }} />
      <button onClick={() => setPhase('squats')} style={{ width: '100%', height: 58, borderRadius: 18, border: 'none', background: accent, color: theme.onAccent, fontWeight: 700, fontSize: 16, cursor: 'pointer', fontFamily: theme.fonts.ui }}>Begin · {cfg.squats} squats + {cfg.math} problems</button>
      <button onClick={reset} style={{ marginTop: 14, background: 'none', border: 'none', color: theme.textDim, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>Snooze 5 min</button>
    </div>
  );

  if (phase === 'squats') {
    const pct = reps / cfg.squats;
    return wrap(<>
      <Header kicker="SQUATS" step={`${reps} / ${cfg.squats}`} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ fontFamily: theme.fonts.num, fontWeight: theme.fonts.numWeight, fontSize: 132, lineHeight: 0.85, letterSpacing: -4, transform: `translateY(${squat.depth * 14}px)`, transition: squat.down ? 'none' : 'transform .25s', fontVariantNumeric: 'tabular-nums' }}>{reps}</div>
        <div style={{ fontSize: 15, color: theme.textDim, marginTop: 8 }}>of {cfg.squats} squats</div>
      </div>
      {/* segmented progress */}
      <div style={{ display: 'flex', gap: 4, marginBottom: 20 }}>
        {Array.from({ length: cfg.squats }).map((_, i) => (
          <div key={i} style={{ flex: 1, height: 5, borderRadius: 3, background: i < reps ? accent : theme.hair, transition: 'background .2s' }} />
        ))}
      </div>
      {/* press zone */}
      <div {...squat.bind} style={{ userSelect: 'none', touchAction: 'none', height: 92, borderRadius: 20, border: `1.5px solid ${squat.down ? accent : theme.hairStrong}`, background: squat.down ? theme.accentSoft : theme.card, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 4, cursor: 'pointer', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: `${squat.depth * 100}%`, background: theme.accentSoft, transition: squat.down ? 'none' : 'height .25s' }} />
        <div style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 8, color: squat.down ? accent : theme.text, fontWeight: 600, fontSize: 15 }}>
          <Icon name={squat.down ? 'arrowUp' : 'arrowDown'} size={20} color={squat.down ? accent : theme.text} stroke={2.2} />
          {squat.down ? 'Stand up to count' : 'Press down as you squat'}
        </div>
        <div style={{ position: 'relative', fontSize: 12, color: theme.textMute }}>Motion sensor counts each rep</div>
      </div>
    </>);
  }

  if (phase === 'math') return wrap(<>
    <Header kicker="MATH" step={`${qi + 1} / ${cfg.math}`} />
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ fontSize: 13, color: theme.textDim, marginBottom: 14 }}>{cfg.level} · solve to silence</div>
      <div style={{ fontFamily: theme.fonts.num, fontWeight: theme.fonts.numWeight, fontSize: 56, letterSpacing: -1, color: theme.text }}>{problem.text}</div>
      <div style={{ marginTop: 22, minHeight: 56, minWidth: 130, padding: '8px 24px', borderRadius: 14, border: `1.5px solid ${wrong ? '#e5484d' : (entry ? accent : theme.hairStrong)}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: theme.fonts.num, fontSize: 34, color: wrong ? '#e5484d' : theme.text, animation: wrong ? 'aShake .4s' : 'none', fontVariantNumeric: 'tabular-nums' }}>{entry || <span style={{ color: theme.textMute }}>?</span>}</div>
    </div>
    <NumPad onKey={pressKey} theme={theme} accent={accent} />
  </>);

  // done
  return wrap(
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center' }}>
      <span style={{ width: 72, height: 72, borderRadius: '50%', background: accent, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 26 }}><Icon name="check" size={38} color={theme.onAccent} stroke={2.6} /></span>
      <div style={{ fontFamily: theme.fonts.display, fontSize: 32, fontWeight: 500, letterSpacing: -0.5 }}>Alarm off</div>
      <div style={{ marginTop: 10, fontSize: 16, color: theme.textDim, maxWidth: 240, lineHeight: 1.5 }}>Nicely done — {cfg.squats} squats and {cfg.math} problems before {cfg.h}:{String(cfg.m).padStart(2,'0')}. Good morning.</div>
      <button onClick={reset} style={{ marginTop: 34, padding: '12px 26px', borderRadius: 14, border: `1.5px solid ${theme.hairStrong}`, background: 'transparent', color: theme.text, fontWeight: 600, fontSize: 15, cursor: 'pointer', fontFamily: theme.fonts.ui }}>Replay flow</button>
    </div>
  );
}

Object.assign(window, { DismissA });
