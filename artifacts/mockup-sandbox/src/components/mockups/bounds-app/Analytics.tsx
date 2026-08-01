import { BottomNav } from "./_shared/BottomNav";

const barData = [
  { day: "Mon", val: 42, h: 52 },
  { day: "Tue", val: 61, h: 76 },
  { day: "Wed", val: 27, h: 34 },
  { day: "Thu", val: 68, h: 85 },
  { day: "Fri", val: 46, h: 57 },
  { day: "Sat", val: 15, h: 19 },
  { day: "Sun", val: 34, h: 42 },
];

const zoneBreakdown = [
  { name: "Bedroom", pct: 48, color: "#F5A623" },
  { name: "Dinner Table", pct: 31, color: "#6B8EFF" },
  { name: "Office", pct: 21, color: "#4CAF82" },
];

function SoonBadge() {
  return (
    <span className="px-2 py-0.5 rounded text-[10px] font-bold tracking-wider"
      style={{ backgroundColor: "#2a1f0e", color: "#F5A623" }}>PREVIEW</span>
  );
}

export function Analytics() {
  return (
    <div className="h-screen w-full flex flex-col" style={{ backgroundColor: "#111111", fontFamily: "'Inter', system-ui, sans-serif", maxWidth: 390 }}>
      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 pt-12 pb-4 flex flex-col gap-4">

        {/* Coming soon banner */}
        <div className="flex items-center gap-3 p-3 rounded-2xl" style={{ backgroundColor: "#1C1C1E" }}>
          <div className="w-10 h-10 rounded-xl flex items-center justify-center text-xl flex-shrink-0" style={{ backgroundColor: "#222" }}>📊</div>
          <div>
            <p className="text-sm font-semibold text-white">Full analytics coming soon</p>
            <p className="text-xs mt-0.5" style={{ color: "#666" }}>Preview of what's being tracked</p>
          </div>
        </div>

        {/* 4 stat cards grid */}
        <div className="grid grid-cols-2 gap-3">
          {/* Time protected */}
          <div className="p-4 rounded-2xl flex flex-col gap-1" style={{ backgroundColor: "#1C1C1E" }}>
            <span className="text-xl">🛡️</span>
            <p className="text-2xl font-bold text-white mt-1">5h 12m</p>
            <p className="text-xs text-white/60">Time protected</p>
            <p className="text-xs font-semibold" style={{ color: "#F5A623" }}>this week</p>
          </div>
          {/* Zones triggered */}
          <div className="p-4 rounded-2xl flex flex-col gap-1" style={{ backgroundColor: "#1C1C1E" }}>
            <span className="text-xl">📍</span>
            <p className="text-2xl font-bold text-white mt-1">14</p>
            <p className="text-xs text-white/60">Zones triggered</p>
            <p className="text-xs font-semibold" style={{ color: "#F5A623" }}>this week</p>
          </div>
          {/* Unlocks avoided */}
          <div className="p-4 rounded-2xl flex flex-col gap-1" style={{ backgroundColor: "#1C1C1E" }}>
            <span className="text-xl">✅</span>
            <p className="text-2xl font-bold text-white mt-1">9</p>
            <p className="text-xs text-white/60">Unlocks avoided</p>
            <p className="text-xs font-semibold" style={{ color: "#F5A623" }}>this week</p>
          </div>
          {/* Streak */}
          <div className="p-4 rounded-2xl flex flex-col gap-1" style={{ backgroundColor: "#1C1C1E" }}>
            <span className="text-xl">🔥</span>
            <p className="text-2xl font-bold text-white mt-1">3 days</p>
            <p className="text-xs text-white/60">Streak</p>
            <p className="text-xs font-semibold" style={{ color: "#F5A623" }}>current</p>
          </div>
        </div>

        {/* Bar chart */}
        <div className="p-4 rounded-2xl" style={{ backgroundColor: "#1C1C1E" }}>
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm font-semibold text-white">Screen time blocked</p>
            <SoonBadge />
          </div>
          {/* Values row */}
          <div className="flex items-end justify-between gap-1 mb-1">
            {barData.map(b => (
              <div key={b.day} className="flex flex-col items-center gap-1 flex-1">
                <span className="text-[10px]" style={{ color: "#666" }}>{b.val}m</span>
                <div
                  className="w-full rounded-t-md"
                  style={{ height: b.h, backgroundColor: "#F5A623", minWidth: 28, maxWidth: 36 }}
                />
                <span className="text-[10px]" style={{ color: "#555" }}>{b.day}</span>
              </div>
            ))}
          </div>
        </div>

        {/* By zone */}
        <div className="p-4 rounded-2xl" style={{ backgroundColor: "#1C1C1E" }}>
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm font-semibold text-white">By zone</p>
            <SoonBadge />
          </div>
          <div className="flex flex-col gap-3">
            {zoneBreakdown.map(z => (
              <div key={z.name} className="flex items-center gap-3">
                <div className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: z.color }} />
                <span className="text-sm text-white flex-1">{z.name}</span>
                <div className="flex items-center gap-2">
                  <div className="rounded-full" style={{ width: 60, height: 5, backgroundColor: "#2a2a2a" }}>
                    <div className="h-full rounded-full" style={{ width: `${z.pct}%`, backgroundColor: z.color }} />
                  </div>
                  <span className="text-xs font-semibold" style={{ color: "#aaa", minWidth: 28 }}>{z.pct}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <BottomNav active="analytics" />
    </div>
  );
}
