/**
 * Fixed, pure-CSS atmospheric background rendered once behind the whole app.
 * Four non-interactive layers: a deep navy base, three slow drifting glow
 * blobs (blue aurora / purple nebula / cyan glow), and a soft vignette.
 * The layers are defined in `index.css` (`.nova-bg`); this component only
 * mounts the DOM and is excluded from the accessibility tree.
 */
export function Atmosphere() {
  return (
    <div className="nova-bg" aria-hidden="true">
      <div className="nova-bg__base" />
      <div className="nova-bg__glow nova-bg__glow--blue" />
      <div className="nova-bg__glow nova-bg__glow--purple" />
      <div className="nova-bg__glow nova-bg__glow--cyan" />
      <div className="nova-bg__vignette" />
    </div>
  );
}
