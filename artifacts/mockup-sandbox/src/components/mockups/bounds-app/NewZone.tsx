import { X, MapPin } from "lucide-react";
import { BottomNav } from "./_shared/BottomNav";

// Grid map pattern using CSS
function GridMap() {
  const COLS = 10, ROWS = 7;
  const cells = Array.from({ length: COLS * ROWS });
  return (
    <div className="relative w-full overflow-hidden rounded-2xl" style={{ height: 200, backgroundColor: "#0d1f0d" }}>
      {/* Grid cells */}
      <div
        className="absolute inset-0 grid"
        style={{
          gridTemplateColumns: `repeat(${COLS}, 1fr)`,
          gridTemplateRows: `repeat(${ROWS}, 1fr)`,
          opacity: 0.6
        }}>
        {cells.map((_, i) => (
          <div key={i} style={{ border: "0.5px solid #1a3a1a", backgroundColor: i % 3 === 0 ? "#0f2a0f" : "#0d1f0d" }} />
        ))}
      </div>

      {/* Radius circle */}
      <div
        className="absolute"
        style={{
          left: "50%", top: "50%",
          transform: "translate(-50%, -50%)",
          width: 110, height: 110,
          border: "2px solid #F5A623",
          borderRadius: "50%",
          backgroundColor: "#F5A62310"
        }}
      />

      {/* Pin */}
      <div
        className="absolute flex items-center justify-center"
        style={{ left: "50%", top: "50%", transform: "translate(-50%, -60%)" }}>
        <div
          style={{
            width: 20, height: 26,
            backgroundColor: "#FF4D6D",
            borderRadius: "50% 50% 50% 0",
            transform: "rotate(-45deg)",
            boxShadow: "0 2px 8px rgba(255,77,109,0.5)"
          }}
        />
      </div>

      {/* Hint text */}
      <p className="absolute bottom-2 right-3 text-[10px] italic" style={{ color: "#4a6a4a" }}>Drag to position (simulated)</p>
    </div>
  );
}

export function NewZone() {
  return (
    <div className="h-screen w-full flex flex-col" style={{ backgroundColor: "#111111", fontFamily: "'Inter', system-ui, sans-serif", maxWidth: 390 }}>
      {/* Top nav */}
      <div className="flex items-center gap-3 px-4 pt-12 pb-4">
        <button className="p-1">
          <X size={20} color="#aaa" />
        </button>
        <h1 className="text-lg font-semibold text-white">New Zone</h1>
      </div>

      {/* Map */}
      <div className="px-4">
        <GridMap />
      </div>

      {/* Form */}
      <div className="flex-1 px-4 pt-5 flex flex-col gap-5">
        {/* Zone name */}
        <div>
          <label className="block text-xs font-semibold tracking-[0.12em] mb-2" style={{ color: "#666" }}>ZONE NAME</label>
          <div
            className="w-full px-4 py-3.5 rounded-2xl text-sm"
            style={{ backgroundColor: "#1C1C1E", color: "#555", border: "1px solid #2a2a2a" }}>
            e.g. "Gym", "Library"
          </div>
        </div>

        {/* Radius */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-semibold tracking-[0.12em]" style={{ color: "#666" }}>RADIUS</label>
            <span className="text-sm font-bold" style={{ color: "#F5A623" }}>200m</span>
          </div>
          {/* Slider track */}
          <div className="relative flex items-center" style={{ height: 24 }}>
            <div className="w-full h-1.5 rounded-full" style={{ backgroundColor: "#2a2a2a" }}>
              <div className="h-full rounded-full" style={{ width: "35%", backgroundColor: "#F5A623" }} />
            </div>
            {/* Thumb */}
            <div
              className="absolute"
              style={{
                left: "35%", transform: "translateX(-50%)",
                width: 22, height: 22,
                borderRadius: "50%",
                backgroundColor: "#F5A623",
                boxShadow: "0 0 8px #F5A62366"
              }}
            />
          </div>
          <div className="flex justify-between mt-1.5">
            <span className="text-xs" style={{ color: "#555" }}>50m</span>
            <span className="text-xs" style={{ color: "#555" }}>500m</span>
          </div>
        </div>
      </div>

      {/* Save button */}
      <div className="px-4 pb-4">
        <button
          className="w-full py-4 rounded-2xl font-bold text-base text-black"
          style={{ backgroundColor: "#F5A623" }}>
          Save Zone
        </button>
      </div>

      <BottomNav active="zones" />
    </div>
  );
}
