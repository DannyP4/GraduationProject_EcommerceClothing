export default function ToggleSwitch({ checked, onChange, labelOn = 'Active', labelOff = 'Hidden', disabled = false }) {
  const toggle = () => { if (!disabled) onChange?.(!checked); };
  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={toggle}
        disabled={disabled}
        className={`relative inline-flex h-7 w-12 items-center rounded-full transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-40 ${
          checked ? 'bg-emerald-600 focus:ring-emerald-500' : 'bg-[#E83354] focus:ring-[#E83354]'
        }`}
      >
        <span
          className={`inline-block h-5 w-5 transform rounded-full bg-white shadow ring-1 ring-black/10 transition-transform duration-200 ${
            checked ? 'translate-x-6' : 'translate-x-1'
          }`}
        />
      </button>
      <span className="text-[11px] font-bold tracking-[0.15em] uppercase text-black/70">
        {checked ? labelOn : labelOff}
      </span>
    </div>
  );
}
