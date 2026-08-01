import { Settings } from "lucide-react";
import { BottomNav } from "./_shared/BottomNav";

export function Home() {
  return (
    <div className="h-screen w-full flex flex-col" style={{ backgroundColor: "#111111", fontFamily: "'Inter', system-ui, sans-serif", maxWidth: 390 }}>
      {/* Status bar */}
      <div className="flex items-center justify-between px-5 pt-3 pb-1">
        <span className="text-xs font-bold tracking-[0.2em] text-white/40 uppercase">Bounds</span>
        <Settings size={20} color="#aaa" />
      </div>

      {/* Currently in banner */}
      <div className="mx-4 mt-2 mb-0">
        <div className="flex items-center gap-2 px-4 py-2.5 rounded-full" style={{ backgroundColor: "#1E1E1E" }}>
          <div className="w-2 h-2 rounded-full bg-orange-400 flex-shrink-0" />
          <span className="text-sm text-white/80">
            Currently in: <span className="font-semibold" style={{ color: "#F5A623" }}>Bedroom (After 10 PM)</span>
          </span>
        </div>
      </div>

      {/* Main content */}
      <div className="flex-1 flex flex-col items-center justify-center gap-6 px-6">
        {/* Circular timer */}
        <div className="relative flex items-center justify-center" style={{ width: 230, height: 230 }}>
          {/* Outer glow ring */}
          <svg width="230" height="230" className="absolute inset-0" style={{ filter: "drop-shadow(0 0 18px #F5A62355)" }}>
            <circle cx="115" cy="115" r="105" fill="none" stroke="#222" strokeWidth="10" />
            <circle
              cx="115" cy="115" r="105"
              fill="none"
              stroke="#F5A623"
              strokeWidth="10"
              strokeLinecap="round"
              strokeDasharray="659.7"
              strokeDashoffset="100"
              transform="rotate(-90 115 115)"
            />
          </svg>
          <div className="flex flex-col items-center justify-center z-10">
            <span className="text-5xl font-bold text-white tracking-tight">5:00</span>
            <span className="text-xs tracking-[0.18em] text-white/50 mt-1 uppercase">Grace Period</span>
          </div>
        </div>

        {/* Status text */}
        <p className="text-white/50 text-sm tracking-wide">Waiting to enter a zone</p>

        {/* Locked phone illustration */}
        <div className="relative flex flex-col items-center justify-center"
          style={{ width: 90, height: 110, backgroundColor: "#1C1C1E", borderRadius: 14, border: "2px solid #333" }}>
          <div className="absolute inset-0 rounded-[12px] flex items-center justify-center flex-col gap-1">
            <div style={{ width: 32, height: 48, backgroundColor: "#2a2a2a", borderRadius: 4 }} />
          </div>
          <div className="absolute bottom-0 w-full flex justify-center pb-2">
            <span className="text-[9px] tracking-widest text-white/30 font-semibold uppercase">Feed<br />Locked</span>
          </div>
        </div>
      </div>

      {/* Buttons */}
      <div className="px-5 pb-6 flex flex-col gap-3">
        <button
          className="w-full py-4 rounded-2xl font-bold text-base text-black"
          style={{ backgroundColor: "#F5A623" }}>
          Unlock Phone
        </button>
        <div className="flex justify-start">
          <button
            className="flex items-center gap-1.5 px-4 py-2 rounded-full text-xs font-semibold"
            style={{ backgroundColor: "#1E1E1E", color: "#F5A623" }}>
            <span>⚡</span> Simulate Entry
          </button>
        </div>
      </div>

      <BottomNav active="home" />
    </div>
  );
}
