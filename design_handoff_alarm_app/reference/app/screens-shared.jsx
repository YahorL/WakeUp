// screens-shared.jsx — Home (alarm list) + Edit alarm, combined into one
// navigable phone shell (AppShell). Exports: AppShell.

const SAMPLE_ALARMS = [
  { id: 1, h: 6, m: 30, period: 'AM', label: 'Weekday wake-up', days: [1,2,3,4,5], on: true, squats: 15, math: true, mathLevel: 'Medium', mathCount: 3 },
  { id: 2, h: 7, m: 15, period: 'AM', label: 'Gym session',     days: [2,4,6],     on: true, squats: 25, math: false, mathLevel: 'Easy', mathCount: 0 },
  { id: 3, h: 9, m: 0,  period: 'AM', label: 'Lazy Sunday',      days: [0],         on: false, squats: 0, math: true, mathLevel: 'Hard', mathCount: 2 },
];
const DAY_LETTERS = ['S','M','T','W','T','F','S'];

function fmtTime(h, m) { return h + ':' + String(m).padStart(2, '0'); }
function daysLabel(days) {
  if (days.length === 7) return 'Every day';
  if (days.length === 5 && [1,2,3,4,5].every(d => days.includes(d))) return 'Weekdays';
  if (days.length === 2 && days.includes(0) && days.includes(6)) return 'Weekends';
  if (days.length === 0) return 'Once';
  return days.map(d => DAY_LETTERS[d]).join(' ');
}

// ── Home ─────────────────────────────────────────────────────
function HomeScreen({ theme, accent, alarms, onToggle, onEdit, onAdd, onSettings }) {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: 'transparent', fontFamily: theme.fonts.ui, color: theme.text }}>
      {/* header */}
      <div style={{ padding: '18px 22px 12px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div>
          <div style={{ fontFamily: theme.fonts.display, fontSize: 34, fontWeight: 500, letterSpacing: -0.5, lineHeight: 1 }}>Alarms</div>
          <div style={{ marginTop: 8, fontSize: 13.5, color: theme.textDim, display: 'flex', alignItems: 'center', gap: 6 }}>
            <Icon name="clock" size={15} color={accent} stroke={2} />
            Next ring in <span style={{ color: theme.text, fontWeight: 600 }}>7h 04m</span>
          </div>
        </div>
        <button onClick={onSettings} style={{ width: 40, height: 40, borderRadius: '50%', border: 'none', background: 'transparent', color: theme.textDim, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <Icon name="settings" size={22} stroke={1.8} />
        </button>
      </div>

      {/* list */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 16px 12px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        {alarms.map(a => (
          <AlarmCard key={a.id} a={a} theme={theme} accent={accent} onToggle={() => onToggle(a.id)} onEdit={() => onEdit(a.id)} />
        ))}
        <div style={{ height: 4 }} />
      </div>

      {/* add */}
      <div style={{ padding: '10px 16px 16px' }}>
        <button onClick={onAdd} style={{
          width: '100%', height: 56, borderRadius: 18, border: 'none', cursor: 'pointer',
          background: accent, color: theme.onAccent, fontFamily: theme.fonts.ui, fontWeight: 600, fontSize: 16,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        }}>
          <Icon name="plus" size={20} color={theme.onAccent} stroke={2.4} /> New alarm
        </button>
      </div>
    </div>
  );
}

function AlarmCard({ a, theme, accent, onToggle, onEdit }) {
  const dim = a.on ? 1 : 0.5;
  return (
    <div style={{
      background: theme.card, borderRadius: 22, border: `1px solid ${theme.hair}`,
      padding: '16px 18px', boxShadow: theme.dark ? 'none' : '0 1px 2px rgba(0,0,0,0.03)',
    }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <button onClick={onEdit} style={{ background: 'none', border: 'none', padding: 0, textAlign: 'left', cursor: 'pointer', opacity: dim, transition: 'opacity .2s' }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
            <span style={{ fontFamily: theme.fonts.num, fontWeight: theme.fonts.numWeight, fontSize: 42, letterSpacing: -1, lineHeight: 0.9, color: theme.text, fontVariantNumeric: 'tabular-nums' }}>{fmtTime(a.h, a.m)}</span>
            <span style={{ fontFamily: theme.fonts.ui, fontSize: 15, fontWeight: 600, color: theme.textDim }}>{a.period}</span>
          </div>
          <div style={{ marginTop: 7, fontSize: 14, color: theme.text, fontWeight: 500 }}>{a.label}</div>
        </button>
        <Switch on={a.on} onChange={onToggle} accent={accent} theme={theme} />
      </div>

      {/* day row */}
      <div style={{ display: 'flex', gap: 5, marginTop: 14, opacity: dim }}>
        {DAY_LETTERS.map((d, i) => {
          const active = a.days.includes(i);
          return (
            <span key={i} style={{
              width: 24, height: 24, borderRadius: '50%', fontSize: 11, fontWeight: 600,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: active ? theme.text : theme.textMute,
              background: active ? (theme.dark ? '#2a2c2d' : '#ececea') : 'transparent',
            }}>{d}</span>
          );
        })}
      </div>

      {/* challenge badges */}
      <div style={{ display: 'flex', gap: 7, marginTop: 14, flexWrap: 'wrap', opacity: dim }}>
        {a.squats > 0 && <Badge theme={theme} accent={accent} solid><Icon name="arrowDown" size={13} color={accent} stroke={2.2} />{a.squats} squats</Badge>}
        {a.math && <Badge theme={theme} accent={accent} solid><Icon name="bolt" size={12} color={accent} stroke={2} />Math · {a.mathLevel}{a.mathCount ? ' ×' + a.mathCount : ''}</Badge>}
        {a.squats === 0 && !a.math && <Badge theme={theme} accent={accent}>Tap to dismiss</Badge>}
      </div>
    </div>
  );
}

// ── Edit ─────────────────────────────────────────────────────
function EditScreen({ theme, accent, alarm, onBack, onChange, onDelete }) {
  const a = alarm;
  const set = (patch) => onChange({ ...a, ...patch });
  const adjMin = (d) => { let m = a.m + d; if (m < 0) m = 55; if (m > 59) m = 0; set({ m: Math.round(m/5)*5 % 60 }); };
  const adjHr = (d) => { let h = a.h + d; if (h < 1) h = 12; if (h > 12) h = 1; set({ h }); };

  const SectionLabel = ({ children }) => (
    <div style={{ fontSize: 12, letterSpacing: 1, textTransform: 'uppercase', color: theme.textMute, fontWeight: 600, margin: '0 0 12px' }}>{children}</div>
  );
  const Row = ({ children, top }) => (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, paddingTop: top ? 16 : 0 }}>{children}</div>
  );
  const chevBtn = (name, fn) => (
    <button onClick={fn} style={{ width: 38, height: 30, border: 'none', background: 'transparent', color: theme.textDim, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <Icon name={name} size={22} stroke={2} />
    </button>
  );

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: 'transparent', fontFamily: theme.fonts.ui, color: theme.text }}>
      {/* top bar */}
      <div style={{ padding: '14px 14px 6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button onClick={onBack} style={{ width: 40, height: 40, borderRadius: '50%', border: 'none', background: 'transparent', color: theme.text, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Icon name="chevL" size={24} stroke={2} />
        </button>
        <div style={{ fontSize: 15, fontWeight: 600 }}>{a.isNew ? 'New alarm' : 'Edit alarm'}</div>
        <button onClick={onDelete} style={{ width: 40, height: 40, borderRadius: '50%', border: 'none', background: 'transparent', color: theme.textDim, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Icon name="trash" size={20} stroke={1.8} />
        </button>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: '8px 20px 16px' }}>
        {/* time picker */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4, padding: '6px 0 18px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            {chevBtn('chevD', () => adjHr(1))}
            <span style={{ fontFamily: theme.fonts.num, fontWeight: theme.fonts.numWeight, fontSize: 64, letterSpacing: -2, lineHeight: 1, color: theme.text, fontVariantNumeric: 'tabular-nums' }}>{a.h}</span>
            {chevBtn('chevD', () => adjHr(-1))}
          </div>
          <span style={{ fontFamily: theme.fonts.num, fontSize: 56, color: theme.textMute, lineHeight: 1, paddingBottom: 6 }}>:</span>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            {chevBtn('chevD', () => adjMin(5))}
            <span style={{ fontFamily: theme.fonts.num, fontWeight: theme.fonts.numWeight, fontSize: 64, letterSpacing: -2, lineHeight: 1, color: theme.text, fontVariantNumeric: 'tabular-nums' }}>{String(a.m).padStart(2,'0')}</span>
            {chevBtn('chevD', () => adjMin(-5))}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginLeft: 10 }}>
            {['AM','PM'].map(p => (
              <button key={p} onClick={() => set({ period: p })} style={{
                padding: '6px 12px', borderRadius: 11, border: 'none', cursor: 'pointer', fontWeight: 600, fontSize: 14,
                background: a.period === p ? accent : (theme.dark ? '#1f2122' : '#ececea'),
                color: a.period === p ? theme.onAccent : theme.textDim,
              }}>{p}</button>
            ))}
          </div>
        </div>

        {/* label */}
        <div style={{ marginBottom: 22 }}>
          <SectionLabel>Label</SectionLabel>
          <div style={{ background: theme.card, border: `1px solid ${theme.hair}`, borderRadius: 14, padding: '13px 16px', fontSize: 15, color: theme.text }}>{a.label}</div>
        </div>

        {/* repeat */}
        <div style={{ marginBottom: 22 }}>
          <SectionLabel>Repeat</SectionLabel>
          <div style={{ display: 'flex', gap: 7, justifyContent: 'space-between' }}>
            {DAY_LETTERS.map((d, i) => {
              const active = a.days.includes(i);
              return (
                <button key={i} onClick={() => set({ days: active ? a.days.filter(x => x !== i) : [...a.days, i].sort() })} style={{
                  width: 38, height: 38, borderRadius: '50%', border: `1.5px solid ${active ? accent : theme.hair}`, cursor: 'pointer',
                  background: active ? accent : 'transparent', color: active ? theme.onAccent : theme.textDim,
                  fontSize: 14, fontWeight: 600,
                }}>{d}</button>
              );
            })}
          </div>
        </div>

        {/* challenge builder */}
        <SectionLabel>To turn it off</SectionLabel>
        <div style={{ background: theme.card, border: `1px solid ${theme.hair}`, borderRadius: 18, padding: '4px 18px', marginBottom: 18 }}>
          {/* squats */}
          <div style={{ padding: '16px 0', borderBottom: `1px solid ${theme.hair}` }}>
            <Row>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span style={{ width: 36, height: 36, borderRadius: 11, background: theme.accentSoft, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Icon name="arrowDown" size={19} color={accent} stroke={2.2} /></span>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontSize: 15, fontWeight: 600, whiteSpace: 'nowrap' }}>Squats</div>
                  <div style={{ fontSize: 12.5, color: theme.textDim }}>Counted by motion sensor</div>
                </div>
              </div>
              <Switch on={a.squats > 0} onChange={(v) => set({ squats: v ? (a.squats || 15) : 0 })} accent={accent} theme={theme} />
            </Row>
            {a.squats > 0 && (
              <div style={{ marginTop: 14, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ fontSize: 13.5, color: theme.textDim }}>Reps required</span>
                <Stepper value={a.squats} min={1} max={50} onChange={(v) => set({ squats: v })} theme={theme} />
              </div>
            )}
          </div>
          {/* math */}
          <div style={{ padding: '16px 0' }}>
            <Row>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span style={{ width: 36, height: 36, borderRadius: 11, background: theme.accentSoft, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Icon name="bolt" size={18} color={accent} stroke={2} /></span>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontSize: 15, fontWeight: 600, whiteSpace: 'nowrap' }}>Math problems</div>
                  <div style={{ fontSize: 12.5, color: theme.textDim }}>Solve to silence</div>
                </div>
              </div>
              <Switch on={a.math} onChange={(v) => set({ math: v })} accent={accent} theme={theme} />
            </Row>
            {a.math && (
              <div style={{ marginTop: 14 }}>
                <div style={{ display: 'flex', gap: 7, marginBottom: 14 }}>
                  {['Easy','Medium','Hard'].map(l => (
                    <button key={l} onClick={() => set({ mathLevel: l })} style={{
                      flex: 1, height: 38, borderRadius: 11, border: `1px solid ${a.mathLevel === l ? 'transparent' : theme.hair}`, cursor: 'pointer',
                      background: a.mathLevel === l ? (theme.dark ? '#2a2c2d' : '#ececea') : 'transparent',
                      color: a.mathLevel === l ? theme.text : theme.textDim, fontSize: 13.5, fontWeight: 600,
                    }}>{l}</button>
                  ))}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: 13.5, color: theme.textDim }}>How many</span>
                  <Stepper value={a.mathCount || 1} min={1} max={10} onChange={(v) => set({ mathCount: v })} theme={theme} />
                </div>
              </div>
            )}
          </div>
        </div>

        {(a.squats > 0 && a.math) && (
          <div style={{ fontSize: 12.5, color: theme.textDim, textAlign: 'center', padding: '0 10px 8px', lineHeight: 1.5 }}>
            You'll do <b style={{ color: theme.text }}>{a.squats} squats</b> and solve <b style={{ color: theme.text }}>{a.mathCount} {a.mathLevel.toLowerCase()} problems</b> to turn this alarm off.
          </div>
        )}
      </div>

      {/* save */}
      <div style={{ padding: '10px 20px 16px', borderTop: `1px solid ${theme.hair}`, background: theme.bg }}>
        <button onClick={onBack} style={{ width: '100%', height: 54, borderRadius: 16, border: 'none', cursor: 'pointer', background: accent, color: theme.onAccent, fontWeight: 600, fontSize: 16, fontFamily: theme.fonts.ui }}>Save alarm</button>
      </div>
    </div>
  );
}

// ── Settings ──────────────────────────────────────
function SettingsScreen({ theme, accent, onBack, onSetAccent, dark, onSetDark, fontPairing, onSetFont }) {
  const SectionLabel = ({ children }) => (
    <div style={{ fontSize: 12, letterSpacing: 1, textTransform: 'uppercase', color: theme.textMute, fontWeight: 600, margin: '0 0 12px' }}>{children}</div>
  );
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: 'transparent', fontFamily: theme.fonts.ui, color: theme.text }}>
      {/* top bar */}
      <div style={{ padding: '14px 14px 6px', display: 'flex', alignItems: 'center', gap: 6 }}>
        <button onClick={onBack} style={{ width: 40, height: 40, borderRadius: '50%', border: 'none', background: 'transparent', color: theme.text, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Icon name="chevL" size={24} stroke={2} />
        </button>
        <div style={{ fontFamily: theme.fonts.display, fontSize: 26, fontWeight: 500, letterSpacing: -0.4 }}>Settings</div>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: '14px 20px 20px' }}>
        {/* accent */}
        <SectionLabel>Accent color</SectionLabel>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12, marginBottom: 12 }}>
          {ACCENT_OPTIONS.map(o => {
            const sel = o.value === accent;
            return (
              <button key={o.name} onClick={() => onSetAccent && onSetAccent(o.value)} title={o.name} style={{
                aspectRatio: '1', borderRadius: '50%', cursor: 'pointer',
                border: `2px solid ${sel ? theme.text : 'transparent'}`,
                background: 'transparent', padding: 3,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <span style={{ width: '100%', height: '100%', borderRadius: '50%', background: o.value, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {sel && <Icon name="check" size={18} color="#fff" stroke={2.6} />}
                </span>
              </button>
            );
          })}
        </div>
        <div style={{ fontSize: 13, color: theme.textDim, marginBottom: 26 }}>
          {ACCENT_OPTIONS.find(o => o.value === accent)?.name || 'Custom'} · used across alarms, buttons &amp; the wake-up challenge.
        </div>

        {/* appearance */}
        <SectionLabel>Appearance</SectionLabel>
        <div style={{ background: theme.card, border: `1px solid ${theme.hair}`, borderRadius: 18, padding: '4px 18px', marginBottom: 26 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '15px 0', borderBottom: `1px solid ${theme.hair}` }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ width: 36, height: 36, borderRadius: 11, background: theme.accentSoft, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Icon name={dark ? 'clock' : 'sun'} size={18} color={accent} stroke={1.9} /></span>
              <div>
                <div style={{ fontSize: 15, fontWeight: 600 }}>Dark mode</div>
                <div style={{ fontSize: 12.5, color: theme.textDim }}>Gentle on 6am eyes</div>
              </div>
            </div>
            <Switch on={dark} onChange={onSetDark} accent={accent} theme={theme} />
          </div>
          <div style={{ padding: '15px 0' }}>
            <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Typeface</div>
            <div style={{ display: 'flex', gap: 7 }}>
              {['Grotesk','Serif','Mono'].map(f => (
                <button key={f} onClick={() => onSetFont && onSetFont(f)} style={{
                  flex: 1, height: 40, borderRadius: 11, cursor: 'pointer',
                  border: `1px solid ${fontPairing === f ? 'transparent' : theme.hair}`,
                  background: fontPairing === f ? (theme.dark ? '#2a2c2d' : '#ececea') : 'transparent',
                  color: fontPairing === f ? theme.text : theme.textDim, fontSize: 13.5, fontWeight: 600,
                  fontFamily: (FONTS[f.toLowerCase()] || FONTS.grotesk).display,
                }}>{f}</button>
              ))}
            </div>
          </div>
        </div>

        <SectionLabel>About</SectionLabel>
        <div style={{ fontSize: 13.5, color: theme.textDim, lineHeight: 1.6 }}>
          Squats are counted by your phone&rsquo;s motion sensor. Pair them with math problems so you&rsquo;re truly awake before the alarm goes quiet.
        </div>
      </div>
    </div>
  );
}

// ── Shell: navigate home <-> edit ────────────────────────────
function AppShell({ theme, accent, initialScreen = 'home', initialEditId = 1, onSetAccent, dark, onSetDark, fontPairing, onSetFont }) {
  const [alarms, setAlarms] = React.useState(SAMPLE_ALARMS);
  const [screen, setScreen] = React.useState(initialScreen); // 'home' | 'edit' | 'settings'
  const [editId, setEditId] = React.useState(initialEditId);
  const editing = alarms.find(x => x.id === editId);

  const toggle = (id) => setAlarms(as => as.map(a => a.id === id ? { ...a, on: !a.on } : a));
  const openEdit = (id) => { setEditId(id); setScreen('edit'); };
  const addNew = () => {
    const id = Date.now();
    setAlarms(as => [...as, { id, h: 6, m: 30, period: 'AM', label: 'New alarm', days: [1,2,3,4,5], on: true, squats: 15, math: true, mathLevel: 'Medium', mathCount: 3, isNew: true }]);
    setEditId(id); setScreen('edit');
  };
  const changeAlarm = (next) => setAlarms(as => as.map(a => a.id === next.id ? next : a));
  const del = () => { setAlarms(as => as.filter(a => a.id !== editId)); setScreen('home'); };

  if (screen === 'settings')
    return <SettingsScreen theme={theme} accent={accent} onBack={() => setScreen('home')}
      onSetAccent={onSetAccent} dark={dark} onSetDark={onSetDark} fontPairing={fontPairing} onSetFont={onSetFont} />;
  if (screen === 'edit')
    return <EditScreen theme={theme} accent={accent} alarm={editing} onBack={() => setScreen('home')} onChange={changeAlarm} onDelete={del} />;
  return <HomeScreen theme={theme} accent={accent} alarms={alarms} onToggle={toggle} onEdit={openEdit} onAdd={addNew} onSettings={() => setScreen('settings')} />;
}

Object.assign(window, { AppShell, HomeScreen, EditScreen, SettingsScreen, SAMPLE_ALARMS });
