import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import * as addressService from '../services/addressService';
import AddressFormModal from '../components/AddressFormModal';
import ConfirmDialog from '../components/ConfirmDialog';

const REGION_LABEL = { NORTH: 'Northern Vietnam', CENTRAL: 'Central Vietnam', SOUTH: 'Southern Vietnam' };

export default function AccountAddressesPage() {
  const { user } = useAuth();

  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionMsg, setActionMsg] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState('create'); // 'create' | 'edit'
  const [editing, setEditing] = useState(null);

  const [confirmDialog, setConfirmDialog] = useState(null); // { type, address }

  const refresh = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await addressService.listAddresses();
      setAddresses(data ?? []);
    } catch (err) {
      setError(err.message || 'Could not load addresses.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { refresh(); }, []);

  const openCreate = () => {
    setModalMode('create');
    setEditing(null);
    setModalOpen(true);
  };

  const openEdit = (addr) => {
    setModalMode('edit');
    setEditing(addr);
    setModalOpen(true);
  };

  const handleSubmit = async (payload) => {
    if (modalMode === 'edit' && editing) {
      await addressService.updateAddress(editing.id, payload);
    } else {
      await addressService.createAddress(payload);
    }
    await refresh();
  };

  const askDelete = (addr) => setConfirmDialog({ type: 'delete', address: addr });
  const askSetDefault = (addr) => setConfirmDialog({ type: 'setDefault', address: addr });

  const runConfirm = async () => {
    if (!confirmDialog) return;
    const { type, address } = confirmDialog;
    setConfirmDialog(null);
    setActionMsg(null);
    try {
      if (type === 'delete') await addressService.deleteAddress(address.id);
      else if (type === 'setDefault') await addressService.setDefaultAddress(address.id);
      await refresh();
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Action failed.' });
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="font-['Anton'] text-3xl uppercase tracking-tight">Addresses</h2>
          <p className="text-xs text-black/50 mt-1">Manage where your orders ship to.</p>
        </div>
        <button
          onClick={openCreate}
          className="bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase px-5 py-3 hover:bg-[#E83354] transition-colors"
        >
          + Add Address
        </button>
      </div>

      {error && <Banner msg={{ type: 'error', text: error }} />}
      {actionMsg && <Banner msg={actionMsg} />}

      {loading ? (
        <p className="text-sm text-black/40">Loading…</p>
      ) : addresses.length === 0 ? (
        <div className="border border-dashed border-black/15 px-6 py-16 text-center">
          <p className="text-sm text-black/50 mb-4">You have no saved addresses yet.</p>
          <button
            onClick={openCreate}
            className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
          >
            Add your first address
          </button>
        </div>
      ) : (
        <ul className="grid grid-cols-1 lg:grid-cols-2 gap-4 items-stretch">
          {addresses.map((a) => (
            <li
              key={a.id}
              className={`border p-5 flex flex-col gap-3 bg-white h-full transition-colors ${
                a.isDefault ? 'border-[#E83354]/40' : 'border-black/10'
              }`}
            >
              <div className="flex items-center gap-2 flex-wrap min-h-[22px]">
                {a.label && (
                  <span className="text-[10px] font-bold tracking-wider uppercase bg-black/8 px-2 py-0.5">
                    {a.label}
                  </span>
                )}
                {a.isDefault ? (
                  <span className="text-[10px] font-bold tracking-wider uppercase bg-[#E83354] text-white px-2 py-0.5">
                    Default
                  </span>
                ) : (
                  <span className="text-[10px] font-bold tracking-wider uppercase border border-black/15 text-black/50 px-2 py-0.5">
                    Other
                  </span>
                )}
              </div>

              <div>
                <p className="font-bold text-sm">{a.recipient}</p>
                <p className="text-xs text-black/60">{a.phone}</p>
              </div>

              <div className="text-xs text-black/70 leading-relaxed">
                {a.line1}
                {a.ward ? `, ${a.ward}` : ''}, {a.district}, {a.city}
                {a.postalCode ? ` ${a.postalCode}` : ''} · {a.country}
                {a.region && (
                  <span className="block mt-0.5 text-[10px] font-bold tracking-[0.1em] uppercase text-black/45">
                    {REGION_LABEL[a.region] ?? a.region}
                  </span>
                )}
              </div>

              <div className="mt-auto pt-3 border-t border-black/5 flex flex-wrap gap-2">
                <button
                  onClick={() => askSetDefault(a)}
                  disabled={a.isDefault}
                  className={cardBtnClass}
                >
                  {a.isDefault ? '★ Default' : 'Set Default'}
                </button>
                <button
                  onClick={() => openEdit(a)}
                  className={`${cardBtnClass} ml-auto`}
                >
                  Edit
                </button>
                <button
                  onClick={() => askDelete(a)}
                  className={cardBtnDangerClass}
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <AddressFormModal
        open={modalOpen}
        mode={modalMode}
        initial={editing}
        defaults={{ recipient: user?.fullName, phone: user?.phone }}
        onClose={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmDialog
        open={confirmDialog?.type === 'delete'}
        title="Delete this address?"
        message={confirmDialog?.address && `“${confirmDialog.address.label || confirmDialog.address.line1}” will be removed permanently.`}
        confirmLabel="Delete"
        cancelLabel="Keep"
        tone="danger"
        onCancel={() => setConfirmDialog(null)}
        onConfirm={runConfirm}
      />
      <ConfirmDialog
        open={confirmDialog?.type === 'setDefault'}
        title="Set as default?"
        message={confirmDialog?.address && `“${confirmDialog.address.label || confirmDialog.address.line1}” will be the default for new orders.`}
        confirmLabel="Set Default"
        cancelLabel="Cancel"
        onCancel={() => setConfirmDialog(null)}
        onConfirm={runConfirm}
      />
    </div>
  );
}

const cardBtnClass = 'text-[10px] font-bold tracking-[0.1em] uppercase border border-black/15 text-black/70 px-3 py-1.5 hover:border-black hover:text-black transition-colors disabled:opacity-50 disabled:cursor-not-allowed disabled:border-[#E83354]/40 disabled:text-[#E83354]';
const cardBtnDangerClass = 'text-[10px] font-bold tracking-[0.1em] uppercase border border-black/15 text-black/70 px-3 py-1.5 hover:border-[#E83354] hover:text-[#E83354] transition-colors';

function Banner({ msg }) {
  const cls = msg.type === 'success'
    ? 'border-green-600/30 bg-green-600/10 text-green-700'
    : 'border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354]';
  return <div className={`border px-4 py-3 text-xs mb-4 ${cls}`}>{msg.text}</div>;
}
