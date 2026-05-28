import { useState } from 'react';
import * as variantSvc from '../../services/adminVariantService';

const EMPTY_DRAFT = {
  sku: '',
  size: '',
  color: '',
  colorHex: '',
  stockQuantity: 0,
  priceOverride: '',
  weightGrams: '',
  isActive: true,
};

const COLOR_PRESETS = [
  { name: 'Black',    hex: '#000000' },
  { name: 'White',    hex: '#FFFFFF' },
  { name: 'Gray',     hex: '#9CA3AF' },
  { name: 'Navy',     hex: '#1F2A44' },
  { name: 'Red',      hex: '#DC2626' },
  { name: 'Burgundy', hex: '#6B1F2A' },
  { name: 'Olive',    hex: '#5A6240' },
  { name: 'Sand',     hex: '#D6C7A1' },
  { name: 'Blue',     hex: '#2563EB' },
  { name: 'Green',    hex: '#16A34A' },
  { name: 'Yellow',   hex: '#F59E0B' },
  { name: 'Pink',     hex: '#EC4899' },
];

export default function VariantTableEditor({ productId, variants, onChange, readOnly = false }) {
  const [draft, setDraft] = useState(EMPTY_DRAFT);
  const [editingId, setEditingId] = useState(null);
  const [editDraft, setEditDraft] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const handleAdd = async (e) => {
    e.preventDefault();
    setError(null);
    if (!draft.sku.trim() || !draft.size.trim() || !draft.color.trim()) {
      setError('SKU, size, and color are required.');
      return;
    }
    setBusy(true);
    try {
      await variantSvc.createVariant(productId, normalize(draft));
      setDraft(EMPTY_DRAFT);
      await onChange();
    } catch (err) {
      setError(err.message || 'Could not add variant.');
    } finally {
      setBusy(false);
    }
  };

  const startEdit = (variant) => {
    setEditingId(variant.id);
    setEditDraft({
      size: variant.size,
      color: variant.color,
      colorHex: variant.colorHex ?? '',
      stockQuantity: variant.stockQuantity,
      priceOverride: variant.priceOverride ?? '',
      weightGrams: variant.weightGrams ?? '',
      isActive: variant.isActive,
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditDraft(null);
  };

  const saveEdit = async () => {
    if (!editingId) return;
    setError(null);
    setBusy(true);
    try {
      await variantSvc.updateVariant(editingId, normalize(editDraft, true));
      cancelEdit();
      await onChange();
    } catch (err) {
      setError(err.message || 'Could not update variant.');
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async (variant) => {
    if (!window.confirm(`Delete variant ${variant.sku}? This cannot be undone.`)) return;
    setError(null);
    setBusy(true);
    try {
      await variantSvc.deleteVariant(variant.id);
      await onChange();
    } catch (err) {
      setError(err.message || 'Could not delete variant.');
    } finally {
      setBusy(false);
    }
  };

  const applyPreset = (target, setter, preset) => {
    setter({ ...target, color: preset.name, colorHex: preset.hex });
  };

  return (
    <section className="bg-white border border-black/10 p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="font-['Anton'] text-2xl uppercase tracking-tight">Variants</h2>
        <span className="text-xs text-black/50">{variants.length} total</span>
      </div>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-3 py-2 text-xs">{error}</div>
      )}

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/10">
              <th className="text-left px-2 py-2 w-28">SKU</th>
              <th className="text-left px-2 py-2 w-16">Size</th>
              <th className="text-left px-2 py-2 w-28">Color</th>
              <th className="text-left px-2 py-2 w-40">Hex</th>
              <th className="text-right px-2 py-2 w-20">Stock</th>
              <th className="text-right px-2 py-2 w-24">Price+</th>
              <th className="text-right px-2 py-2 w-20">Weight g</th>
              <th className="text-center px-2 py-2 w-20">Active</th>
              {!readOnly && <th className="text-right px-2 py-2 w-32"></th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-black/5">
            {variants.length === 0 && (
              <tr>
                <td colSpan={readOnly ? 8 : 9} className="text-center text-black/40 py-6 text-xs">
                  {readOnly ? 'No variants.' : 'No variants yet. Add the first one below.'}
                </td>
              </tr>
            )}
            {variants.map((v) => {
              const isEditing = !readOnly && editingId === v.id;
              const row = isEditing ? editDraft : v;
              return (
                <tr key={v.id} className="align-middle">
                  <td className="px-2 py-2 font-mono text-xs">{v.sku}</td>
                  <td className="px-2 py-2">{isEditing ? <Input value={row.size} onChange={(val) => setEditDraft({ ...editDraft, size: val })} /> : v.size}</td>
                  <td className="px-2 py-2">{isEditing ? <Input value={row.color} onChange={(val) => setEditDraft({ ...editDraft, color: val })} /> : v.color}</td>
                  <td className="px-2 py-2">
                    {isEditing ? (
                      <HexInput
                        value={row.colorHex}
                        onChange={(val) => setEditDraft({ ...editDraft, colorHex: val })}
                      />
                    ) : (
                      <ColorChip hex={v.colorHex} />
                    )}
                  </td>
                  <td className="px-2 py-2 text-right">
                    {isEditing ? <NumberInput value={row.stockQuantity} onChange={(val) => setEditDraft({ ...editDraft, stockQuantity: val })} /> : v.stockQuantity}
                  </td>
                  <td className="px-2 py-2 text-right text-xs">
                    {isEditing ? <NumberInput value={row.priceOverride} onChange={(val) => setEditDraft({ ...editDraft, priceOverride: val })} placeholder="0" /> : (v.priceOverride ?? '-')}
                  </td>
                  <td className="px-2 py-2 text-right text-xs">
                    {isEditing ? <NumberInput value={row.weightGrams} onChange={(val) => setEditDraft({ ...editDraft, weightGrams: val })} /> : (v.weightGrams ?? '-')}
                  </td>
                  <td className="px-2 py-2 text-center">
                    {isEditing ? (
                      <input
                        type="checkbox"
                        checked={row.isActive}
                        onChange={(e) => setEditDraft({ ...editDraft, isActive: e.target.checked })}
                      />
                    ) : (
                      <span className={v.isActive ? 'text-emerald-700' : 'text-black/30'}>{v.isActive ? 'Yes' : 'No'}</span>
                    )}
                  </td>
                  {!readOnly && (
                    <td className="px-2 py-2 text-right">
                      {isEditing ? (
                        <div className="flex justify-end gap-1">
                          <button type="button" onClick={saveEdit} disabled={busy} className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black px-2 py-1 hover:bg-black hover:text-white disabled:opacity-40">Save</button>
                          <button type="button" onClick={cancelEdit} disabled={busy} className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black/15 px-2 py-1 hover:border-black disabled:opacity-40">Cancel</button>
                        </div>
                      ) : (
                        <div className="flex justify-end gap-1">
                          <button type="button" onClick={() => startEdit(v)} className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black/15 px-2 py-1 hover:border-black">Edit</button>
                          <button type="button" onClick={() => handleDelete(v)} className="text-[10px] font-bold tracking-[0.15em] uppercase border border-[#E83354]/30 text-[#E83354] px-2 py-1 hover:bg-[#E83354] hover:text-white hover:border-[#E83354]">Del</button>
                        </div>
                      )}
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
          {!readOnly && (
            <tfoot>
              <tr className="border-t-2 border-black/10 bg-black/[0.02]">
                <td className="px-2 py-2"><Input value={draft.sku} onChange={(val) => setDraft({ ...draft, sku: val })} placeholder="UNI-001-M-BK" /></td>
                <td className="px-2 py-2"><Input value={draft.size} onChange={(val) => setDraft({ ...draft, size: val })} placeholder="M" /></td>
                <td className="px-2 py-2"><Input value={draft.color} onChange={(val) => setDraft({ ...draft, color: val })} placeholder="Black" /></td>
                <td className="px-2 py-2"><HexInput value={draft.colorHex} onChange={(val) => setDraft({ ...draft, colorHex: val })} /></td>
                <td className="px-2 py-2"><NumberInput value={draft.stockQuantity} onChange={(val) => setDraft({ ...draft, stockQuantity: val })} /></td>
                <td className="px-2 py-2"><NumberInput value={draft.priceOverride} onChange={(val) => setDraft({ ...draft, priceOverride: val })} placeholder="0" /></td>
                <td className="px-2 py-2"><NumberInput value={draft.weightGrams} onChange={(val) => setDraft({ ...draft, weightGrams: val })} /></td>
                <td className="px-2 py-2 text-center">
                  <input
                    type="checkbox"
                    checked={draft.isActive}
                    onChange={(e) => setDraft({ ...draft, isActive: e.target.checked })}
                  />
                </td>
                <td className="px-2 py-2 text-right">
                  <button
                    type="button"
                    onClick={handleAdd}
                    disabled={busy}
                    className="text-[10px] font-bold tracking-[0.15em] uppercase bg-black text-white px-3 py-1 hover:bg-[#E83354] transition-colors disabled:opacity-40"
                  >
                    + Add
                  </button>
                </td>
              </tr>
            </tfoot>
          )}
        </table>
      </div>

      {!readOnly && (
        <div>
          <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 mb-2">
            Quick palette {editingId ? '(applies to row being edited)' : '(applies to new row below)'}
          </p>
          <div className="flex flex-wrap gap-2">
            {COLOR_PRESETS.map((preset) => (
              <button
                key={preset.name}
                type="button"
                onClick={() => {
                  if (editingId) {
                    applyPreset(editDraft, setEditDraft, preset);
                  } else {
                    applyPreset(draft, setDraft, preset);
                  }
                }}
                title={`${preset.name} ${preset.hex}`}
                className="flex items-center gap-1.5 border border-black/15 pl-1 pr-2 py-1 hover:border-black transition-colors"
              >
                <span
                  className="inline-block w-4 h-4 border border-black/10"
                  style={{ background: preset.hex }}
                />
                <span className="text-[10px] font-bold tracking-[0.1em] uppercase">{preset.name}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function Input({ value, onChange, placeholder }) {
  return (
    <input
      type="text"
      value={value ?? ''}
      placeholder={placeholder}
      onChange={(e) => onChange(e.target.value)}
      className="w-full border border-black/15 px-2 py-1 text-xs focus:border-black focus:outline-none"
    />
  );
}

function NumberInput({ value, onChange, placeholder }) {
  return (
    <input
      type="number"
      min="0"
      value={value ?? ''}
      placeholder={placeholder}
      onChange={(e) => onChange(e.target.value)}
      className="w-full border border-black/15 px-2 py-1 text-xs focus:border-black focus:outline-none text-right"
    />
  );
}

function HexInput({ value, onChange }) {
  const safeColor = /^#[0-9A-Fa-f]{6}$/.test(value ?? '') ? value : '#000000';
  return (
    <div className="flex items-center gap-1">
      <input
        type="color"
        value={safeColor}
        onChange={(e) => onChange(e.target.value.toUpperCase())}
        title="Pick a color"
        className="w-7 h-7 border border-black/15 cursor-pointer p-0 bg-white"
      />
      <input
        type="text"
        value={value ?? ''}
        placeholder="#000000"
        onChange={(e) => onChange(e.target.value)}
        className="flex-1 border border-black/15 px-2 py-1 text-xs font-mono focus:border-black focus:outline-none"
      />
    </div>
  );
}

function ColorChip({ hex }) {
  if (!hex) return <span className="text-xs text-black/30">-</span>;
  return (
    <div className="flex items-center gap-2">
      <span className="inline-block w-4 h-4 border border-black/10" style={{ background: hex }} />
      <span className="text-[10px] font-mono text-black/50">{hex}</span>
    </div>
  );
}

function normalize(d, isUpdate = false) {
  const out = { isActive: d.isActive };
  if (!isUpdate) out.sku = d.sku.trim();
  if (d.size != null) out.size = String(d.size).trim();
  if (d.color != null) out.color = String(d.color).trim();
  if (d.colorHex) out.colorHex = d.colorHex.trim();
  if (d.stockQuantity !== '' && d.stockQuantity != null) out.stockQuantity = Number(d.stockQuantity);
  if (d.priceOverride !== '' && d.priceOverride != null) out.priceOverride = Number(d.priceOverride);
  if (d.weightGrams !== '' && d.weightGrams != null) out.weightGrams = Number(d.weightGrams);
  return out;
}
