import { Settings as SettingsIcon, X, ChevronRight, Shield, Clock, Bell, Smartphone, Globe, User, Cloud, Lock, Info, FileText, MessageSquare } from "lucide-react";
import { BottomNav } from "./_shared/BottomNav";

function SoonBadge() {
  return (
    <span className="px-2 py-0.5 rounded text-[10px] font-bold tracking-wider flex-shrink-0"
      style={{ backgroundColor: "#2a1f0e", color: "#F5A623" }}>SOON</span>
  );
}

interface SettingRowProps {
  Icon: typeof Shield;
  label: string;
  value?: string;
  soon?: boolean;
  arrow?: boolean;
}

function SettingRow({ Icon, label, value, soon = true, arrow = false }: SettingRowProps) {
  return (
    <div className="flex items-center gap-3 px-4 py-3.5">
      <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
        style={{ backgroundColor: "#2a2a2a" }}>
        <Icon size={16} color="#aaa" />
      </div>
      <span className="text-sm text-white flex-1">{label}</span>
      <div className="flex items-center gap-2">
        {value && <span className="text-sm" style={{ color: "#666" }}>{value}</span>}
        {soon && <SoonBadge />}
        {arrow && <ChevronRight size={16} color="#555" />}
      </div>
    </div>
  );
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-xs font-semibold tracking-[0.14em] px-1 pt-4 pb-2" style={{ color: "#555" }}>
      {children}
    </p>
  );
}

function Divider() {
  return <div style={{ height: 1, backgroundColor: "#222", marginLeft: 56 }} />;
}

export function Settings() {
  return (
    <div className="h-screen w-full flex flex-col" style={{ backgroundColor: "#111111", fontFamily: "'Inter', system-ui, sans-serif", maxWidth: 390 }}>
      {/* Background app peek */}
      <div className="flex items-center justify-between px-5 pt-3 pb-1">
        <span className="text-xs font-bold tracking-[0.2em] text-white/40 uppercase">Bounds</span>
        <SettingsIcon size={20} color="#aaa" />
      </div>
      <div className="mx-4 mt-2">
        <div className="flex items-center gap-2 px-4 py-2.5 rounded-full" style={{ backgroundColor: "#1E1E1E" }}>
          <div className="w-2 h-2 rounded-full bg-orange-400" />
          <span className="text-sm text-white/80">
            Currently in: <span className="font-semibold" style={{ color: "#F5A623" }}>Bedroom (After 10 PM)</span>
          </span>
        </div>
      </div>

      {/* Bottom sheet */}
      <div
        className="flex-1 flex flex-col mt-4 overflow-hidden"
        style={{ backgroundColor: "#171717", borderTopLeftRadius: 24, borderTopRightRadius: 24 }}>
        {/* Handle */}
        <div className="flex justify-center pt-3 pb-1">
          <div className="rounded-full" style={{ width: 36, height: 4, backgroundColor: "#333" }} />
        </div>

        {/* Sheet header */}
        <div className="flex items-center justify-between px-5 py-3">
          <h2 className="text-xl font-bold text-white">Settings</h2>
          <button className="w-8 h-8 flex items-center justify-center rounded-full" style={{ backgroundColor: "#2a2a2a" }}>
            <X size={16} color="#aaa" />
          </button>
        </div>

        {/* Scrollable content */}
        <div className="flex-1 overflow-y-auto px-4 pb-4">

          {/* PROTECTION */}
          <SectionLabel>PROTECTION</SectionLabel>
          <div className="rounded-2xl overflow-hidden" style={{ backgroundColor: "#1C1C1E" }}>
            <SettingRow Icon={Shield} label="Block intensity" value="Strict" />
            <Divider />
            <SettingRow Icon={Clock} label="Grace period duration" value="5 min" />
            <Divider />
            <SettingRow Icon={Bell} label="Entry notifications" value="On" />
          </div>

          {/* BLOCKED APPS */}
          <SectionLabel>BLOCKED APPS</SectionLabel>
          <div className="rounded-2xl overflow-hidden" style={{ backgroundColor: "#1C1C1E" }}>
            <SettingRow Icon={Smartphone} label="App blocklist" value="3 apps" />
            <Divider />
            <SettingRow Icon={Globe} label="Website blocklist" value="Off" />
          </div>

          {/* ACCOUNT */}
          <SectionLabel>ACCOUNT</SectionLabel>
          <div className="rounded-2xl overflow-hidden" style={{ backgroundColor: "#1C1C1E" }}>
            <SettingRow Icon={User} label="Profile" value="" />
            <Divider />
            <SettingRow Icon={Cloud} label="Sync & backup" value="Off" />
            <Divider />
            <SettingRow Icon={Lock} label="Passcode lock" value="Off" />
          </div>

          {/* ABOUT */}
          <SectionLabel>ABOUT</SectionLabel>
          <div className="rounded-2xl overflow-hidden" style={{ backgroundColor: "#1C1C1E" }}>
            <div className="flex items-center gap-3 px-4 py-3.5">
              <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" style={{ backgroundColor: "#2a2a2a" }}>
                <Info size={16} color="#aaa" />
              </div>
              <span className="text-sm text-white flex-1">App version</span>
              <span className="text-sm" style={{ color: "#666" }}>0.1.0-beta</span>
              <ChevronRight size={16} color="#555" />
            </div>
            <Divider />
            <SettingRow Icon={FileText} label="Privacy policy" />
            <Divider />
            <SettingRow Icon={MessageSquare} label="Send feedback" />
          </div>
        </div>
      </div>

      <BottomNav active="home" />
    </div>
  );
}
