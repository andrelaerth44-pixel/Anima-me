# Anima-me interaction specification

- Brush size: user-selected value.
- Brush opacity: user-selected percentage.
- Onion skin opacity: user-selected percentage; farther frames may attenuate like RoughAnimator.
- Stabilizer: user-selected percentage controlling how strongly the live stroke is stabilized.
- Smooth: an explicit post-stroke action. It is not another percentage slider. When invoked, it processes the completed stroke and replaces it with the smoothed result.
- Live preview: do not convert a stroke into visible micro-circles/dots. Render the current stroke as a continuous anti-aliased path/segment preview while drawing, then commit the stroke on lift.
- Two-finger gesture: pan + pinch zoom + rotation around the gesture centroid; never create drawing samples.
- One-finger gesture: drawing/selected tool behavior.
- UI controls must consume touches before the canvas; canvas gestures must never make the tool rail or brush list inert.

Reference behavior is based on the supplied screenshots and RoughAnimator's documented bitmap workflow, drawing tools, adjustable smoothing, onion skin and canvas navigation. RoughAnimator itself is bitmap-based rather than vector-based. 
