import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import * as profileService from '../services/profileService';
import PasswordStrengthMeter from '../components/PasswordStrengthMeter';

export default function AccountProfilePage() {
  const { t } = useTranslation();
  const { user, refreshUser } = useAuth();

  // Language is selected in the navbar.
  const [profileForm, setProfileForm] = useState({ fullName: '', phone: '' });
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileMsg, setProfileMsg] = useState(null);

  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmNewPassword: '' });
  const [pwSaving, setPwSaving] = useState(false);
  const [pwMsg, setPwMsg] = useState(null);

  useEffect(() => {
    if (user) {
      setProfileForm({
        fullName: user.fullName ?? '',
        phone: user.phone ?? '',
      });
    }
  }, [user]);

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setProfileMsg(null);
    setProfileSaving(true);
    try {
      await profileService.updateProfile({
        fullName: profileForm.fullName.trim(),
        phone: profileForm.phone.trim(),
      });
      await refreshUser();
      setProfileMsg({ type: 'success', text: t('accountPage.profile.updated') });
    } catch (err) {
      setProfileMsg({ type: 'error', text: err.message || t('accountPage.profile.saveError') });
    } finally {
      setProfileSaving(false);
    }
  };

  const handlePwSubmit = async (e) => {
    e.preventDefault();
    setPwMsg(null);
    if (pwForm.newPassword !== pwForm.confirmNewPassword) {
      setPwMsg({ type: 'error', text: t('accountPage.password.mismatch') });
      return;
    }
    if (pwForm.newPassword.length < 8) {
      setPwMsg({ type: 'error', text: t('accountPage.password.tooShort') });
      return;
    }
    setPwSaving(true);
    try {
      await profileService.changePassword({
        currentPassword: pwForm.currentPassword,
        newPassword: pwForm.newPassword,
      });
      setPwForm({ currentPassword: '', newPassword: '', confirmNewPassword: '' });
      setPwMsg({ type: 'success', text: t('accountPage.password.changed') });
    } catch (err) {
      setPwMsg({ type: 'error', text: err.message || t('accountPage.password.changeError') });
    } finally {
      setPwSaving(false);
    }
  };

  return (
    <div className="space-y-12">
      <section>
        <h2 className="font-['Anton'] text-3xl uppercase tracking-tight mb-1">{t('accountPage.profile.heading')}</h2>
        <p className="text-xs text-black/50 mb-6">{t('accountPage.profile.subtitle')}</p>

        <form onSubmit={handleProfileSubmit} className="space-y-5 max-w-md">
          <Field label={t('accountPage.profile.email')}>
            <input
              type="email"
              value={user?.email ?? ''}
              disabled
              className="w-full border border-black/10 bg-black/5 px-4 py-3 text-sm text-black/40 cursor-not-allowed"
            />
            <p className="text-[10px] text-black/40 mt-1">{t('accountPage.profile.emailHint')}</p>
          </Field>

          <Field label={t('accountPage.profile.fullName')} required>
            <input
              type="text"
              required
              maxLength={150}
              value={profileForm.fullName}
              onChange={(e) => setProfileForm({ ...profileForm, fullName: e.target.value })}
              className={inputClass}
            />
          </Field>

          <Field label={t('accountPage.profile.phone')}>
            <input
              type="tel"
              maxLength={20}
              placeholder="0901234567"
              value={profileForm.phone}
              onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
              className={inputClass}
            />
          </Field>

          {profileMsg && <Banner msg={profileMsg} />}

          <button
            type="submit"
            disabled={profileSaving}
            className={btnPrimaryClass}
          >
            {profileSaving ? t('accountPage.profile.saving') : t('accountPage.profile.save')}
          </button>
        </form>
      </section>

      <section className="border-t border-black/10 pt-10">
        <h2 className="font-['Anton'] text-3xl uppercase tracking-tight mb-1">{t('accountPage.password.heading')}</h2>
        <p className="text-xs text-black/50 mb-6">{t('accountPage.password.subtitle')}</p>

        <form onSubmit={handlePwSubmit} className="space-y-5 max-w-md" autoComplete="on">
          <Field label={t('accountPage.password.current')} required>
            <input
              type="password"
              required
              autoComplete="current-password"
              value={pwForm.currentPassword}
              onChange={(e) => setPwForm({ ...pwForm, currentPassword: e.target.value })}
              className={inputClass}
            />
          </Field>

          <Field label={t('accountPage.password.new')} required>
            <input
              type="password"
              required
              minLength={8}
              autoComplete="new-password"
              value={pwForm.newPassword}
              onChange={(e) => setPwForm({ ...pwForm, newPassword: e.target.value })}
              className={inputClass}
            />
            <PasswordStrengthMeter value={pwForm.newPassword} />
          </Field>

          <Field label={t('accountPage.password.confirm')} required>
            <input
              type="password"
              required
              minLength={8}
              autoComplete="new-password"
              value={pwForm.confirmNewPassword}
              onChange={(e) => setPwForm({ ...pwForm, confirmNewPassword: e.target.value })}
              className={inputClass}
            />
          </Field>

          {pwMsg && <Banner msg={pwMsg} />}

          <button
            type="submit"
            disabled={pwSaving}
            className={btnPrimaryClass}
          >
            {pwSaving ? t('accountPage.password.updating') : t('accountPage.password.update')}
          </button>
        </form>
      </section>
    </div>
  );
}

const inputClass = 'w-full border border-black/15 px-4 py-3 text-sm focus:outline-none focus:border-black transition-colors';
const btnPrimaryClass = 'bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-[#E83354] transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-black';

function Field({ label, required, children }) {
  return (
    <div>
      <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
        {label}{required && <span className="text-[#E83354]"> *</span>}
      </label>
      {children}
    </div>
  );
}

function Banner({ msg }) {
  const cls = msg.type === 'success'
    ? 'border-green-600/30 bg-green-600/10 text-green-700'
    : 'border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354]';
  return <div className={`border px-4 py-3 text-xs ${cls}`}>{msg.text}</div>;
}

