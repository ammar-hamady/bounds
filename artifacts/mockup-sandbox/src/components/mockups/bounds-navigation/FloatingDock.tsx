import "./_group.css";
import { BarChart3, Home, MapPinned } from "lucide-react";
import { useState } from "react";

const destinations = [
  { label: "Zones", icon: MapPinned },
  { label: "Current", icon: Home },
  { label: "Analytics", icon: BarChart3 },
];

export function FloatingDock() {
  const [selected, setSelected] = useState("Current");

  return (
    <main
      style={{
        minHeight: "220px",
        display: "flex",
        flexDirection: "column",
        justifyContent: "flex-end",
        overflow: "hidden",
        background: "#111111",
      }}
    >
      <nav
        aria-label="Main navigation"
        style={{
          display: "flex",
          alignItems: "center",
          gap: "6px",
          margin: "0 16px 14px",
          padding: "8px",
          border: "1px solid #2a2a2a",
          borderRadius: "24px",
          background: "rgba(28, 28, 30, 0.96)",
          boxShadow: "0 12px 30px rgba(0, 0, 0, 0.34)",
        }}
      >
        {destinations.map(({ label, icon: Icon }) => {
          const isSelected = selected === label;
          return (
            <button
              key={label}
              onClick={() => setSelected(label)}
              aria-current={isSelected ? "page" : undefined}
              style={{
                display: "flex",
                flex: 1,
                minHeight: "54px",
                alignItems: "center",
                justifyContent: "center",
                gap: "7px",
                border: 0,
                borderRadius: "17px",
                background: isSelected ? "#f5a623" : "transparent",
                color: isSelected ? "#111111" : "#888888",
                cursor: "pointer",
                fontSize: "11px",
                fontWeight: isSelected ? 800 : 600,
                transition: "background 180ms ease, color 180ms ease, transform 180ms ease",
              }}
            >
              <Icon size={19} strokeWidth={isSelected ? 2.7 : 2} />
              <span>{label}</span>
            </button>
          );
        })}
      </nav>
    </main>
  );
}