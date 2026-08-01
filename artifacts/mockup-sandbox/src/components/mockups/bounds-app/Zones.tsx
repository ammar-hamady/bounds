import { BottomNav } from "./_shared/BottomNav";

interface Zone {
  name: string;
  subtitle: string;
  radius: string;
  emoji: string;
  active: boolean;
  emojiBg: string;
}

const zones: Zone[] = [
  { name: "Bedroom (After 10 PM)", subtitle: "After 10 PM", radius: "150m radius", emoji: "🌙", active: true, emojiBg: "#2a1f0e" },
  { name: "Office (9 AM – 5 PM)", subtitle: "9 AM – 5 PM", radius: "200m radius", emoji: "💼", active: false, emojiBg: "#1a1a1a" },
  { name: "The Dinner Table", subtitle: "", radius: "80m radius", emoji: "🍽️", active: true, emojiBg: "#1a1a1a" },
];

function Toggle({ on }: { on: boolean }) {
  return (
    <div
      className="relative flex-shrink-0 cursor-pointer"
      style={{
        width: 48, height: 28, borderRadius: 14,
        backgroundColor: on ? "#F5A623" : "#333",
        transition: "background 0.2s"
      }}>
      <div
        style={{
          position: "absolute", top: 3,
          left: on ? 23 : 3,
          width: 22, height: 22,
          borderRadius: "50%",
          backgroundColor: "white",
          transition: "left 0.2s",
          boxShadow: "0 1px 4px rgba(0,0,0,0.4)"
        }} />
    </div>
  );
}

export function Zones() {
  return (
    <div className="h-screen w-full flex flex-col" style={{ backgroundColor: "#111111", fontFamily: "'Inter', system-ui, sans-serif", maxWidth: 390 }}>
      {/* Header */}
      <div className="px-5 pt-12 pb-2">
        <h1 className="text-3xl font-bold text-white">Zones</h1>
        <p className="text-sm mt-1" style={{ color: "#666" }}>2 of 3 zones active</p>
      </div>

      <div className="flex-1 overflow-y-auto px-4 pb-4">
        {/* Section label */}
        <p className="text-xs font-semibold tracking-[0.14em] mb-3 mt-2" style={{ color: "#555" }}>YOUR ZONES</p>

        {/* Zone cards */}
        <div className="flex flex-col gap-0 rounded-2xl overflow-hidden" style={{ backgroundColor: "#1C1C1E" }}>
          {zones.map((zone, i) => (
            <div key={zone.name}>
              <div className="flex items-center gap-3 px-4 py-3.5">
                {/* Emoji icon */}
                <div
                  className="flex items-center justify-center flex-shrink-0 rounded-xl"
                  style={{ width: 44, height: 44, backgroundColor: zone.emojiBg, fontSize: 22 }}>
                  {zone.emoji}
                </div>
                {/* Info */}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-white truncate">{zone.name}</p>
                  <p className="text-xs mt-0.5" style={{ color: "#666" }}>
                    {zone.subtitle ? `${zone.subtitle} · ` : ""}{zone.radius}
                  </p>
                </div>
                {/* Toggle */}
                <Toggle on={zone.active} />
              </div>
              {i < zones.length - 1 && (
                <div style={{ height: 1, backgroundColor: "#2a2a2a", marginLeft: 64 }} />
              )}
            </div>
          ))}
        </div>

        {/* Add New Zone — full-width, visually separated from zone list */}
        <button
          className="flex items-center justify-center gap-2 mt-4 w-full px-5 py-3.5 rounded-2xl"
          style={{ border: "1.5px dashed #444", backgroundColor: "transparent" }}>
          <span style={{ color: "#F5A623", fontSize: 18, fontWeight: 700 }}>+</span>
          <span className="text-sm font-semibold" style={{ color: "#F5A623" }}>Add New Zone</span>
        </button>
      </div>

      <BottomNav active="zones" />
    </div>
  );
}
