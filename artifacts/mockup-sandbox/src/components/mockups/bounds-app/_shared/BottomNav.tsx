import { Home, MapPin, BarChart2 } from "lucide-react";

type Tab = "home" | "zones" | "analytics";

export function BottomNav({ active }: { active: Tab }) {
  const tabs: { id: Tab; label: string; Icon: typeof Home }[] = [
    { id: "home", label: "Home", Icon: Home },
    { id: "zones", label: "Zones", Icon: MapPin },
    { id: "analytics", label: "Analytics", Icon: BarChart2 },
  ];
  return (
    <div
      className="flex items-center justify-around px-4 pt-3 pb-5"
      style={{ backgroundColor: "#111111", borderTop: "1px solid #222" }}>
      {tabs.map(({ id, label, Icon }) => {
        const isActive = id === active;
        return (
          <button key={id} className="flex flex-col items-center gap-1">
            <Icon
              size={22}
              color={isActive ? "#F5A623" : "#555"}
              strokeWidth={isActive ? 2.2 : 1.8}
            />
            <span
              className="text-[10px] font-medium"
              style={{ color: isActive ? "#F5A623" : "#555" }}>
              {label}
            </span>
          </button>
        );
      })}
    </div>
  );
}
