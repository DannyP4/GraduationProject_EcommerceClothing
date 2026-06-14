import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useToast } from '../components/Toast';
import LanguageSwitcher from '../components/LanguageSwitcher';
import { getProductByIdOrSlug, getProducts } from '../services/productService';
import { uploadTryOnPhoto, createTryOn, getTryOn } from '../services/tryOnService';
import { formatPrice } from '../lib/format';

const POLL_INTERVAL_MS = 2500;
const MAX_POLLS = 60; // ~150s before giving up

export default function TryOnPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const toast = useToast();

  const [product, setProduct] = useState(null);
  const [loadingProduct, setLoadingProduct] = useState(Boolean(productId));
  const [pickList, setPickList] = useState([]);

  const [userImageUrl, setUserImageUrl] = useState(null);
  const [uploading, setUploading] = useState(false);

  const [job, setJob] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const poll = useRef({ cancelled: false, timer: null });

  useEffect(() => {
    poll.current.cancelled = true;
    clearTimeout(poll.current.timer);
    setJob(null);
    setError(null);
    setBusy(false);

    if (!productId) {
      setProduct(null);
      setLoadingProduct(false);
      return;
    }
    let cancelled = false;
    setLoadingProduct(true);
    getProductByIdOrSlug(productId)
      .then((p) => { if (!cancelled) setProduct(p); })
      .catch((e) => { if (!cancelled) toast.error(e.message || t('tryOn.productLoadFailed')); })
      .finally(() => { if (!cancelled) setLoadingProduct(false); });
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productId]);

  useEffect(() => {
    if (productId) return undefined;
    let cancelled = false;
    getProducts({ size: 12 })
      .then((res) => { if (!cancelled) setPickList(res?.content ?? []); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [productId]);

  useEffect(() => () => {
    poll.current.cancelled = true;
    clearTimeout(poll.current.timer);
  }, []);

  const garmentImage = product?.images?.[0]?.url || null;
  const canRender = Boolean(userImageUrl && product && !uploading && !busy);

  const phase = job?.status === 'SUCCEEDED' && job.resultImageUrl ? 'done'
    : (job?.status === 'FAILED' || error) ? 'error'
      : (busy || job?.status === 'PROCESSING') ? 'processing'
        : 'idle';

  const onChoosePhoto = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const { url } = await uploadTryOnPhoto(file);
      setUserImageUrl(url);
      setJob(null);
    } catch (err) {
      toast.error(err.message || t('tryOn.uploadFailed'));
    } finally {
      setUploading(false);
    }
  };

  const startPolling = (jobId) => {
    let attempts = 0;
    const tick = async () => {
      if (poll.current.cancelled) return;
      attempts += 1;
      try {
        const j = await getTryOn(jobId);
        if (poll.current.cancelled) return;
        setJob(j);
        if (j.status === 'SUCCEEDED' || j.status === 'FAILED') { setBusy(false); return; }
      } catch {
        if (poll.current.cancelled) return; // transient — keep polling
      }
      if (attempts >= MAX_POLLS) {
        setBusy(false);
        setError(t('tryOn.timeout'));
        return;
      }
      poll.current.timer = setTimeout(tick, POLL_INTERVAL_MS);
    };
    poll.current.timer = setTimeout(tick, POLL_INTERVAL_MS);
  };

  const onTryOn = async () => {
    if (!userImageUrl) { toast.error(t('tryOn.needPhoto')); return; }
    if (!product) { toast.error(t('tryOn.needProduct')); return; }
    setError(null);
    setBusy(true);
    setJob(null);
    poll.current.cancelled = false;
    try {
      const j = await createTryOn({ productId: product.id, userImageUrl });
      setJob(j);
      if (j.status === 'SUCCEEDED' || j.status === 'FAILED') { setBusy(false); return; }
      startPolling(j.id);
    } catch (err) {
      setBusy(false);
      setError(err.message || t('tryOn.failedGeneric'));
      toast.error(err.message || t('tryOn.failedGeneric'));
    }
  };

  const tryAgain = () => {
    poll.current.cancelled = true;
    clearTimeout(poll.current.timer);
    setJob(null);
    setError(null);
    setBusy(false);
  };

  const downloadResult = async () => {
    if (!job?.resultImageUrl) return;
    try {
      const resp = await fetch(job.resultImageUrl);
      const blob = await resp.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `vesta-tryon-${job.id}.png`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch {
      window.open(job.resultImageUrl, '_blank', 'noopener');
    }
  };

  return (
    <div className="min-h-screen bg-[#0A0A0A] text-white">
      <header className="flex items-center justify-between px-6 py-4 border-b border-white/10">
        <button
          onClick={() => navigate('/')}
          className="font-['Anton'] text-2xl tracking-widest hover:text-[#E83354] transition-colors"
        >
          VESTA
        </button>
        <p className="hidden sm:block text-[10px] font-bold tracking-[0.25em] uppercase text-white/50">
          {t('tryOn.studio')}
        </p>
        <div className="flex items-center gap-4">
          <LanguageSwitcher tone="dark" />
          <button
            onClick={() => navigate('/shop')}
            className="text-[11px] font-bold tracking-[0.15em] uppercase text-white/70 hover:text-white border border-white/30 px-4 py-2 hover:border-white/70 transition-all"
          >
            {t('tryOn.backToShop')}
          </button>
        </div>
      </header>

      {!productId ? (
        <ProductPicker
          t={t}
          pickList={pickList}
          onPick={(p) => navigate(`/try-on/${p.slug || p.id}`)}
        />
      ) : loadingProduct ? (
        <div className="py-32 text-center text-white/40 text-xs uppercase tracking-[0.25em]">
          {t('tryOn.loading')}
        </div>
      ) : !product ? (
        <div className="py-32 text-center">
          <p className="text-sm text-white/60 mb-6">{t('tryOn.productLoadFailed')}</p>
          <Link to="/shop" className="text-[11px] font-bold tracking-[0.15em] uppercase border border-white/40 px-5 py-3 hover:bg-white hover:text-black transition-all">
            {t('tryOn.browseProducts')}
          </Link>
        </div>
      ) : (
        <main className="max-w-[1100px] mx-auto px-6 py-8">
          <div className="grid lg:grid-cols-2 gap-6 mb-6">
            {/* Your photo */}
            <section className="bg-white/[0.04] border border-white/10 p-5">
              <h2 className="text-[11px] font-bold tracking-[0.2em] uppercase text-white/80 mb-1">
                {t('tryOn.yourPhoto')}
              </h2>
              <p className="text-[11px] text-white/40 mb-4">{t('tryOn.uploadHint')}</p>
              <div className="aspect-[3/4] bg-black/40 border border-dashed border-white/15 overflow-hidden flex items-center justify-center">
                {userImageUrl ? (
                  <img src={userImageUrl} alt="" className="w-full h-full object-cover" />
                ) : (
                  <span className="text-white/30 text-[11px] uppercase tracking-[0.2em]">{t('tryOn.noPhoto')}</span>
                )}
              </div>
              <label className="mt-4 block text-center cursor-pointer bg-white text-black text-[11px] font-bold tracking-[0.15em] uppercase px-4 py-3 hover:bg-[#E83354] hover:text-white transition-all">
                <input type="file" accept="image/*" className="hidden" onChange={onChoosePhoto} disabled={uploading} />
                {uploading ? t('tryOn.uploading') : userImageUrl ? t('tryOn.replacePhoto') : t('tryOn.choosePhoto')}
              </label>
            </section>

            {/* Garment */}
            <section className="bg-white/[0.04] border border-white/10 p-5">
              <h2 className="text-[11px] font-bold tracking-[0.2em] uppercase text-white/80 mb-4">
                {t('tryOn.garment')}
              </h2>
              <div className="aspect-[3/4] bg-white overflow-hidden flex items-center justify-center">
                {garmentImage ? (
                  <img src={garmentImage} alt={product.name} className="w-full h-full object-cover" />
                ) : (
                  <span className="text-black/30 text-[11px] uppercase tracking-[0.2em]">{t('tryOn.noPhoto')}</span>
                )}
              </div>
              <div className="mt-4">
                <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/40">{product.categoryName}</p>
                <p className="font-bold text-sm mt-0.5">{product.name}</p>
                <p className="font-['Anton'] text-lg mt-1">
                  {formatPrice(product.salePrice ?? product.basePrice, product.currency)}
                </p>
              </div>
              <button
                onClick={() => navigate('/try-on')}
                className="mt-3 text-[10px] font-bold tracking-[0.15em] uppercase text-white/50 hover:text-white underline underline-offset-4 transition-colors"
              >
                {t('tryOn.changeProduct')}
              </button>
            </section>
          </div>

          {phase === 'idle' && (
            <div>
              <button
                onClick={onTryOn}
                disabled={!canRender}
                className="w-full py-5 bg-[#E83354] text-white text-[12px] font-bold tracking-[0.2em] uppercase hover:bg-white hover:text-black transition-all disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {t('tryOn.tryItOn')}
              </button>
              {!userImageUrl && (
                <p className="text-center text-[11px] text-white/40 mt-3">{t('tryOn.needPhoto')}</p>
              )}
            </div>
          )}

          {phase === 'processing' && (
            <div className="bg-white/[0.04] border border-white/10 py-16 flex flex-col items-center">
              <div className="w-10 h-10 border-2 border-white/20 border-t-[#E83354] rounded-full animate-spin" />
              <p className="mt-5 text-[12px] font-bold tracking-[0.2em] uppercase">{t('tryOn.rendering')}</p>
              <p className="mt-2 text-[11px] text-white/40">{t('tryOn.processingHint')}</p>
            </div>
          )}

          {phase === 'error' && (
            <div className="bg-[#E83354]/10 border border-[#E83354]/40 py-12 px-6 text-center">
              <p className="text-[12px] font-bold tracking-[0.15em] uppercase text-[#E83354] mb-2">{t('tryOn.failedTitle')}</p>
              <p className="text-[12px] text-white/60 mb-6">{error || job?.errorMessage || t('tryOn.failedGeneric')}</p>
              <button
                onClick={tryAgain}
                className="text-[11px] font-bold tracking-[0.15em] uppercase border border-white/40 px-5 py-3 hover:bg-white hover:text-black transition-all"
              >
                {t('tryOn.tryAgain')}
              </button>
            </div>
          )}

          {phase === 'done' && (
            <div className="bg-white/[0.04] border border-white/10 p-5">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-[11px] font-bold tracking-[0.2em] uppercase text-white/80">{t('tryOn.result')}</h2>
                {job.cached && (
                  <span className="text-[10px] font-bold tracking-[0.12em] uppercase text-white/40">{t('tryOn.cached')}</span>
                )}
              </div>
              <div className="bg-black/40 flex items-center justify-center">
                <img src={job.resultImageUrl} alt={t('tryOn.result')} className="max-h-[70vh] w-auto object-contain" />
              </div>
              <div className="mt-5 grid grid-cols-1 sm:grid-cols-3 gap-3">
                <button
                  onClick={downloadResult}
                  className="py-3 bg-white text-black text-[11px] font-bold tracking-[0.15em] uppercase hover:bg-[#E83354] hover:text-white transition-all"
                >
                  {t('tryOn.download')}
                </button>
                <Link
                  to={`/product/${product.slug || product.id}`}
                  className="py-3 text-center border border-white/40 text-[11px] font-bold tracking-[0.15em] uppercase hover:bg-white hover:text-black transition-all"
                >
                  {t('tryOn.viewProduct')}
                </Link>
                <button
                  onClick={tryAgain}
                  className="py-3 border border-white/40 text-[11px] font-bold tracking-[0.15em] uppercase hover:bg-white hover:text-black transition-all"
                >
                  {t('tryOn.tryAnother')}
                </button>
              </div>
            </div>
          )}
        </main>
      )}
    </div>
  );
}

function ProductPicker({ t, pickList, onPick }) {
  return (
    <main className="max-w-[1100px] mx-auto px-6 py-10">
      <h1 className="font-['Anton'] text-4xl uppercase tracking-tight mb-1">{t('tryOn.title')}</h1>
      <p className="text-[12px] text-white/50 mb-8">{t('tryOn.pickHint')}</p>
      {pickList.length === 0 ? (
        <div className="py-20 text-center">
          <Link to="/shop" className="text-[11px] font-bold tracking-[0.15em] uppercase border border-white/40 px-5 py-3 hover:bg-white hover:text-black transition-all">
            {t('tryOn.browseProducts')}
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {pickList.map((p) => (
            <button
              key={p.id}
              onClick={() => onPick(p)}
              className="group text-left bg-white/[0.04] border border-white/10 overflow-hidden hover:border-white/40 transition-all"
            >
              <div className="aspect-[4/5] bg-white overflow-hidden">
                {p.primaryImageUrl ? (
                  <img src={p.primaryImageUrl} alt={p.name} className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" />
                ) : (
                  <div className="w-full h-full bg-black/20" />
                )}
              </div>
              <div className="p-3">
                <p className="text-[12px] font-bold uppercase tracking-wide line-clamp-2 min-h-[2.25rem]">{p.name}</p>
                <p className="font-['Anton'] text-base mt-1">{formatPrice(p.salePrice ?? p.basePrice, p.currency)}</p>
              </div>
            </button>
          ))}
        </div>
      )}
    </main>
  );
}
