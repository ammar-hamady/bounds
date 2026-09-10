import "./_group.css";
import { BarChart3, Home, MapPinned } from "lucide-react";
import { useState } from "react";

const destinations = [
  { label: "Zones", icon: MapPinned },
  { label: "Current", icon: Home },
  { label: "Analytics", icon: BarChart3 },
];

export function Current() {
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
          justifyContent: "space-around",
          alignItems: "center",
          minHeight: "80px",
          padding: "10px 8px 12px",
          background: "#1c1c1e",
          borderTop: "1px solid #2a2a2a",
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
                minWidth: "88px",
                flexDirection: "column",
                alignItems: "center",
                gap: "5px",
                border: 0,
                background: "transparent",
                color: isSelected ? "#f5a623" : "#888888",
                cursor: "pointer",
                fontSize: "11px",
                fontWeight: isSelected ? 700 : 500,
              }}
            >
              <span
                style={{
                  display: "grid",
                  width: "56px",
                  height: "28px",
                  placeItems: "center",
                  borderRadius: "16px",
                  background: isSelected ? "#2a1f0e" : "transparent",
                }}
              >
                <Icon size={20} strokeWidth={isSelected ? 2.5 : 2} />
              </span>
              {label}
            </button>
          );
        })}
      </nav>
    </main>
  );
}