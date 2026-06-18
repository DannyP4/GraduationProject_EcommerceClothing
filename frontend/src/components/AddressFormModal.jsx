import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import * as shippingService from '../services/shippingService';
import SearchableSelect from './SearchableSelect';

const EMPTY = {
  label: '',
  recipient: '',
  phone: '',
  line1: '',
  ward: '',
  district: '',
  city: '',
  ghnProvinceId: '',
  ghnDistrictId: '',
  ghnWardCode: '',
  country: 'VN',
  postalCode: '',
  isDefault: false,
};

export default function AddressFormModal({ open, mode, initial, defaults, onClose, onSubmit }) {
  const { t } = useTranslation();
  const tf = (key) => t(`accountPage.addresses.form.${key}`);
  const [form, setForm] = useState(EMPTY);
  const [provinces, setProvinces] = useState([]);
  const [districts, setDistricts] = useState([]);
  const [wards, setWards] = useState([]);
  const [loadingDistricts, setLoadingDistricts] = useState(false);
  const [loadingWards, setLoadingWards] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) return undefined;
    let cancelled = false;
    setError(null);

    const base = (mode === 'edit' && initial) ? {
      label: initial.label ?? '',
      recipient: initial.recipient ?? '',
      phone: initial.phone ?? '',
      line1: initial.line1 ?? '',
      ward: initial.ward ?? '',
      district: initial.district ?? '',
      city: initial.city ?? '',
      ghnProvinceId: initial.ghnProvinceId != null ? String(initial.ghnProvinceId) : '',
      ghnDistrictId: initial.ghnDistrictId != null ? String(initial.ghnDistrictId) : '',
      ghnWardCode: initial.ghnWardCode ?? '',
      country: initial.country ?? 'VN',
      postalCode: initial.postalCode ?? '',
      isDefault: false,
    } : {
      ...EMPTY,
      recipient: defaults?.recipient ?? '',
      phone: defaults?.phone ?? '',
    };
    setForm(base);
    setDistricts([]);
    setWards([]);

    (async () => {
      try {
        const provs = await shippingService.getProvinces();
        if (cancelled) return;
        setProvinces(provs || []);
        if (base.ghnProvinceId) {
          const ds = await shippingService.getDistricts(base.ghnProvinceId);
          if (!cancelled) setDistricts(ds || []);
        }
        if (base.ghnDistrictId) {
          const ws = await shippingService.getWards(base.ghnDistrictId);
          if (!cancelled) setWards(ws || []);
        }
      } catch {
        if (!cancelled) setProvinces([]);
      }
    })();

    return () => { cancelled = true; };
  }, [open, mode, initial, defaults]);

  if (!open) return null;

  const handleChange = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const selectProvince = async (id) => {
    const p = provinces.find((x) => String(x.id) === String(id));
    setForm((f) => ({ ...f, ghnProvinceId: String(id), city: p?.name ?? '', ghnDistrictId: '', district: '', ghnWardCode: '', ward: '' }));
    setDistricts([]);
    setWards([]);
    setLoadingDistricts(true);
    try { setDistricts((await shippingService.getDistricts(id)) || []); }
    catch { setDistricts([]); }
    finally { setLoadingDistricts(false); }
  };

  const selectDistrict = async (id) => {
    const d = districts.find((x) => String(x.id) === String(id));
    setForm((f) => ({ ...f, ghnDistrictId: String(id), district: d?.name ?? '', ghnWardCode: '', ward: '' }));
    setWards([]);
    setLoadingWards(true);
    try { setWards((await shippingService.getWards(id)) || []); }
    catch { setWards([]); }
    finally { setLoadingWards(false); }
  };

  const selectWard = (code) => {
    const w = wards.find((x) => String(x.code) === String(code));
    setForm((f) => ({ ...f, ghnWardCode: String(code), ward: w?.name ?? '' }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.ghnProvinceId || !form.ghnDistrictId || !form.ghnWardCode) {
      setError(tf('selectLocationError'));
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const payload = {
        label: form.label?.trim() || null,
        recipient: form.recipient.trim(),
        phone: form.phone.trim(),
        line1: form.line1.trim(),
        ward: form.ward || null,
        district: form.district,
        city: form.city,
        country: form.country?.trim().toUpperCase() || 'VN',
        postalCode: form.postalCode?.trim() || null,
        ghnProvinceId: form.ghnProvinceId ? Number(form.ghnProvinceId) : null,
        ghnDistrictId: form.ghnDistrictId ? Number(form.ghnDistrictId) : null,
        ghnWardCode: form.ghnWardCode || null,
      };
      if (mode !== 'edit') payload.isDefault = !!form.isDefault;
      await onSubmit(payload);
      onClose();
    } catch (err) {
      setError(err.message || tf('saveError'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
      <div className="bg-white max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b border-black/10">
          <h3 className="font-['Anton'] text-2xl uppercase tracking-tight">
            {mode === 'edit' ? tf('titleEdit') : tf('titleAdd')}
          </h3>
          <button
            onClick={onClose}
            className="text-black/50 hover:text-black text-2xl leading-none"
            aria-label={tf('close')}
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <FieldRow>
            <FieldText label={tf('label')} placeholder={tf('labelPlaceholder')} value={form.label} onChange={handleChange('label')} maxLength={50} />
            <FieldText label={tf('recipient')} required value={form.recipient} onChange={handleChange('recipient')} maxLength={150} />
          </FieldRow>

          <FieldText label={tf('phone')} required type="tel" placeholder={tf('phonePlaceholder')}
                     value={form.phone} onChange={handleChange('phone')} maxLength={20} />

          <SearchableSelect
            label={tf('province')} required
            value={form.ghnProvinceId} onChange={selectProvince}
            options={provinces.map((p) => ({ value: p.id, label: p.name }))}
            placeholder={tf('selectProvince')}
          />

          <FieldRow>
            <SearchableSelect
              label={tf('district')} required
              value={form.ghnDistrictId} onChange={selectDistrict}
              options={districts.map((d) => ({ value: d.id, label: d.name }))}
              placeholder={tf('selectDistrict')}
              loading={loadingDistricts} loadingPlaceholder={tf('loadingOptions')}
              disabled={!form.ghnProvinceId || loadingDistricts}
            />
            <SearchableSelect
              label={tf('ward')} required
              value={form.ghnWardCode} onChange={selectWard}
              options={wards.map((w) => ({ value: w.code, label: w.name }))}
              placeholder={tf('selectWard')}
              loading={loadingWards} loadingPlaceholder={tf('loadingOptions')}
              disabled={!form.ghnDistrictId || loadingWards}
            />
          </FieldRow>

          <FieldText label={tf('line1')} required placeholder={tf('line1Placeholder')}
                     value={form.line1} onChange={handleChange('line1')} maxLength={255} />

          <FieldRow>
            <FieldText label={tf('country')} placeholder={tf('countryPlaceholder')} value={form.country} onChange={handleChange('country')} maxLength={2} />
            <FieldText label={tf('postalCode')} value={form.postalCode} onChange={handleChange('postalCode')} maxLength={20} />
          </FieldRow>

          {mode !== 'edit' && (
            <label className="flex items-center gap-2 text-xs">
              <input type="checkbox" checked={form.isDefault} onChange={handleChange('isDefault')} />
              <span className="text-black/70">{tf('setDefault')}</span>
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
              {tf('cancel')}
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:bg-[#E83354] transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-black"
            >
              {submitting ? tf('saving') : mode === 'edit' ? tf('save') : tf('add')}
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

