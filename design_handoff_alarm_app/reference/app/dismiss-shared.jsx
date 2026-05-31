// dismiss-shared.jsx — helpers shared by the three dismissal directions.

function genProblem(level) {
  const r = (a, b) => a + Math.floor(Math.random() * (b - a + 1));
  if (level === 'Easy') {
    const a = r(2, 9), b = r(2, 9), add = Math.random() > 0.4;
    return add ? { text: `${a} + ${b}`, answer: a + b } : { text: `${Math.max(a,b)} − ${Math.min(a,b)}`, answer: Math.abs(a - b) };
  }
  if (level === 'Hard') {
    const a = r(11, 19), b = r(3, 9), c = r(2, 9);
    return { text: `${a} × ${b} − ${c}`, answer: a * b - c };
  }
  // Medium
  if (Math.random() > 0.5) { const a = r(6, 12), b = r(4, 9); return { text: `${a} × ${b}`, answer: a * b }; }
  const a = r(14, 49), b = r(13, 38); return { text: `${a} + ${b}`, answer: a + b };
}

// A pointer-driven squat rep: press down (squat) → release past threshold (stand) = +1.
// Returns { depth (0..1), down, bind } — bind spreads onto the press target.
function useSquat(onRep, theme) {
  const [depth, setDepth] = React.useState(0);
  const [down, setDown] = React.useState(false);
  const raf = React.useRef(null);

  const animateTo = (target) => {
    cancelAnimationFrame(raf.current);
    const step = () => {
      setDepth(d => {
        const nd = d + (target - d) * 0.25;
        if (Math.abs(target - nd) < 0.01) return target;
        raf.current = requestAnimationFrame(step);
        return nd;
      });
    };
    raf.current = requestAnimationFrame(step);
  };

  const press = () => { if (!onRep) return; setDown(true); animateTo(1); };
  const release = () => { setDown(false); animateTo(0); };
  const tap = () => { if (!onRep) return; onRep(); release(); };

  return {
    depth, down,
    bind: {
      onPointerDown: press, onPointerUp: release, onPointerLeave: release, onPointerCancel: release,
      onClick: tap,
    },
  };
}

Object.assign(window, { genProblem, useSquat });
