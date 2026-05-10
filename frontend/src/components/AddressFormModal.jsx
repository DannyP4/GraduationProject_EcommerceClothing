import { useEffect, useState } from 'react';

const EMPTY = {
  label: '',
  recipient: '',
  phone: '',
  line1: '',
  ward: '',
  district: '',
  city: '',
  country: 'VN',
  postalCode: '',
  isDefault: false,
};

export default function AddressFormModal({ open, mode, initial, defaults, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) return;
    if (mode === 'edit' && initial) {
      setForm({
        label: initial.label ?? '',
        recipient: initial.recipient ?? '',
        phone: initial.phone ?? '',
        line1: initial.line1 ?? '',
        ward: initial.ward ?? '',
        district: initial.district ?? '',
        city: initial.city ?? '',
        country: initial.country ?? 'VN',
        postalCode: initial.postalCode ?? '',
        // Default flag is changed via the dedicated /default endpoint, not via PATCH.
        isDefault: false,
      });
    } else {
      // Pre-fill from profile so first-address creation is fast.
      setForm({
        ...EMPTY,
        recipient: defaults?.recipient ?? '',
        phone: defaults?.phone ?? '',
      });
    }
    setError(null);
  }, [open, mode, initial, defaults]);

  if (!open) return null;

  const handleChange = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const payload = {
        label: form.label?.trim() || null,
        recipient: form.recipient.trim(),
        phone: form.phone.trim(),
        line1: form.line1.trim(),
        ward: form.ward?.trim() || null,
        district: form.district.trim(),
        city: form.city.trim(),
        country: form.country?.trim().toUpperCase() || 'VN',
        postalCode: form.postalCode?.trim() || null,
      };
      // Only the create flow can set isDefault inline (PATCH ignores it intentionally).
      if (mode !== 'edit') payload.isDefault = !!form.isDefault;
      await onSubmit(payload);
      onClose();
    } catch (err) {
      setError(err.message || 'Could not save address.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
      <div className="bg-white max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b border-black/10">
          <h3 className="font-['Anton'] text-2xl uppercase tracking-tight">
            {mode === 'edit' ? 'Edit Address' : 'Add Address'}
          </h3>
          <button
            onClick={onClose}
            className="text-black/50 hover:text-black text-2xl leading-none"
            aria-label="Close"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <FieldRow>
            <FieldText label="Label" placeholder="Home / Office" value={form.label} onChange={handleChange('label')} maxLength={50} />
            <FieldText label="Recipient" required value={form.recipient} onChange={handleChange('recipient')} maxLength={150} />
          </FieldRow>

          <FieldText label="Phone" required type="tel" placeholder="0901234567"
                     value={form.phone} onChange={handleChange('phone')} maxLength={20} />

          <FieldText label="Address Line" required placeholder="Số nhà, tên đường"
                     value={form.line1} onChange={handleChange('line1')} maxLength={255} />

          <FieldRow>
            <FieldText label="Ward" value={form.ward} onChange={handleChange('ward')} maxLength={100} />
            <FieldText label="District" required value={form.district} onChange={handleChange('district')} maxLength={100} />
          </FieldRow>

          <FieldRow>
            <FieldText label="City" required value={form.city} onChange={handleChange('city')} maxLength={100} />
            <FieldText label="Country" placeholder="VN" value={form.country} onChange={handleChange('country')} maxLength={2} />
          </FieldRow>

          <FieldText label="Postal Code" value={form.postalCode} onChange={handleChange('postalCode')} maxLength={20} />

          {mode !== 'edit' && (
            <label className="flex items-center gap-2 text-xs">
              <input type="checkbox" checked={form.isDefault} onChange={handleChange('isDefault')} />
              <span className="text-black/70">Set as default address</span>
            </label>
          )}

          {error && (
            <div className="border border-[#E83354]/30 bg-[#E83354]/5 px-4 py-3 text-xs text-[#E83354]">{error}</div>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 border border-black/15 text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:border-black/40 transition-all"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:bg-[#E83354] transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-black"
            >
              {submitting ? 'Saving…' : mode === 'edit' ? 'Save' : 'Add'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function FieldRow({ children }) {
  return <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">{children}</div>;
}

function FieldText({ label, required, ...rest }) {
  return (
    <div>
      <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
        {label}{required && <span className="text-[#E83354]"> *</span>}
      </label>
      <input
        type={rest.type || 'text'}
        required={required}
        className="w-full border border-black/15 px-3 py-2.5 text-sm focus:outline-none focus:border-black transition-colors"
        {...rest}
      />
    </div>
  );
}
