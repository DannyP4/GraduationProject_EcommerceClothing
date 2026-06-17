import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, Link, useNavigate, useLocation } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import QuantityStepper from '../components/QuantityStepper';
import { useToast } from '../components/Toast';
import { getProductByIdOrSlug, getSimilarProducts, getFrequentlyBoughtTogether } from '../services/productService';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useWishlist } from '../context/WishlistContext';
import HeartIcon from '../components/HeartIcon';
import StarRating from '../components/StarRating';
import BrandCard from '../components/BrandCard';
import ReviewsSection from '../components/ReviewsSection';
import RecommendationRow from '../components/RecommendationRow';
import { goBack } from '../lib/historyBack';
import { useTranslation } from 'react-i18next';
import { formatPrice } from '../lib/format';
import { colorLabel } from '../lib/labels';

export default function ProductPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const backToShop = location.state?.backTo || '/shop';
  const { addItem } = useCart();
  const { status: authStatus } = useAuth();
  const { isWishlisted, toggle: toggleWishlist } = useWishlist();
  const toast = useToast();
  const { t, i18n } = useTranslation();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [activeImg, setActiveImg] = useState(0);
  const [selectedSize, setSelectedSize] = useState('');
  const [selectedColor, setSelectedColor] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  const [wishlistBusy, setWishlistBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setProduct(null);
    setActiveImg(0);
    setSelectedSize('');
    setSelectedColor('');
    setQuantity(1);

    getProductByIdOrSlug(id)
      .then((data) => {
        if (cancelled) return;
        setProduct(data);
        const firstAvailable = (data?.variants ?? []).find(
          (v) => v.isActive !== false && (v.stockQuantity ?? 0) > 0
        ) ?? data?.variants?.[0];
        if (firstAvailable) {
          setSelectedColor(firstAvailable.color ?? '');
          setSelectedSize(firstAvailable.size ?? '');
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load product');
      })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [id, i18n.language]);

  const reloadProduct = useCallback(() => {
    getProductByIdOrSlug(id).then((data) => setProduct(data)).catch(() => {});
  }, [id]);

  const colors = useMemo(() => uniqueValues(product?.variants, 'color'), [product]);
  const sizes = useMemo(() => uniqueValues(product?.variants, 'size'), [product]);
  const selectedVariant = useMemo(
    () => product?.variants?.find((v) => v.color === selectedColor && v.size === selectedSize) || null,
    [product, selectedColor, selectedSize]
  );

  const availableCombos = useMemo(() => {
    const set = new Set();
    for (const v of product?.variants ?? []) {
      if (v.isActive !== false && (v.stockQuantity ?? 0) > 0) {
        set.add(`${v.color ?? ''}|${v.size ?? ''}`);
      }
    }
    return set;
  }, [product]);

  if (loading) {
    return (
      <PageShell>
        <ProductSkeleton />
      </PageShell>
    );
  }

  if (error || !product) {
    return (
      <PageShell>
        <div className="bg-white border border-[#E83354]/30 px-6 py-16 text-center max-w-xl mx-auto">
          <p className="text-sm font-bold text-[#E83354] mb-2 uppercase tracking-wider">{t('product.notAvailable')}</p>
          <p className="text-xs text-black/60 mb-6">{error || t('product.notFound')}</p>
          <Link
            to={backToShop}
            className="inline-block text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
          >
            {t('product.backToShop')}
          </Link>
        </div>
      </PageShell>
    );
  }

  const images = product.images?.length ? product.images : [];

  const displayOriginal = selectedVariant?.price ?? product.basePrice;
  const displaySale = selectedVariant ? selectedVariant.salePrice : product.salePrice;
  const displayPct = selectedVariant ? selectedVariant.discountPercent : product.discountPercent;

  const validateSelection = () => {
    if (!selectedSize) {
      toast.error(t('product.selectSize'));
      return false;
    }
    if (!selectedVariant) {
      toast.error(t('product.comboUnavailable'));
      return false;
    }
    if (selectedVariant.stockQuantity != null && quantity > selectedVariant.stockQuantity) {
      toast.error(t('product.onlyInStock', { n: selectedVariant.stockQuantity }));
      return false;
    }
    return true;
  };

  const handleBuyNow = () => {
    if (!validateSelection()) return;
    if (authStatus !== 'authenticated') {
      toast.error(t('product.signInToContinue'));
      navigate('/login', { state: { from: `/product/${id}` } });
      return;
    }
    navigate('/checkout', {
      state: {
        buyNow: {
          variantId: selectedVariant.id,
          quantity,
          productId: product.id,
          productName: product.name,
          size: selectedVariant.size,
          color: selectedVariant.color,
          colorHex: selectedVariant.colorHex,
          imageUrl: images[0]?.url,
          unitPrice: Number(selectedVariant.salePrice ?? selectedVariant.price ?? product.basePrice),
          currency: product.currency,
        },
      },
    });
  };

  const handleAddToCart = async () => {
    if (!validateSelection()) return;
    setAdding(true);
    try {
      await addItem({
        variantId: selectedVariant.id,
        quantity,
        productId: product.id,
        productSlug: product.slug,
        productName: product.name,
        size: selectedVariant.size,
        color: selectedVariant.color,
        colorHex: selectedVariant.colorHex,
        imageUrl: images[0]?.url,
        unitPrice: Number(selectedVariant.salePrice ?? selectedVariant.price ?? product.basePrice),
        originalUnitPrice: selectedVariant.salePrice != null ? Number(selectedVariant.price ?? product.basePrice) : undefined,
        currency: product.currency,
      });
      toast.success(t('product.addedToCart', { count: quantity, name: product.name }));
    } catch (err) {
      toast.error(err.message || t('product.couldNotAdd'));
    } finally {
      setAdding(false);
    }
  };

  const handleWishlist = async () => {
    if (authStatus !== 'authenticated') {
      toast.error(t('wishlist.signInRequired'));
      navigate('/login', { state: { from: `/product/${id}` } });
      return;
    }
    if (wishlistBusy) return;
    setWishlistBusy(true);
    try {
      const nowWished = await toggleWishlist(product.id);
      toast.success(nowWished ? t('wishlist.added') : t('wishlist.removed'));
    } catch {
      toast.error(t('wishlist.error'));
    } finally {
      setWishlistBusy(false);
    }
  };

  const wished = isWishlisted(product.id);

  const comboAvailable = (c, s) => availableCombos.has(`${c}|${s}`);
  const colorEnabled = (c) => (selectedSize ? comboAvailable(c, selectedSize) : sizes.some((s) => comboAvailable(c, s)));
  const sizeEnabled = (s) => (selectedColor ? comboAvailable(selectedColor, s) : colors.some((c) => comboAvailable(c, s)));

  const chooseColor = (c) => {
    const next = selectedColor === c ? '' : c;
    setSelectedColor(next);
    if (next && selectedSize && !comboAvailable(next, selectedSize)) setSelectedSize('');
  };
  const chooseSize = (s) => {
    const next = selectedSize === s ? '' : s;
    setSelectedSize(next);
    if (next && selectedColor && !comboAvailable(selectedColor, next)) setSelectedColor('');
  };

  return (
    <PageShell>
      <nav className="flex items-center gap-2 text-[11px] font-bold tracking-[0.1em] uppercase mb-8">
        <Link to="/" className="text-[#E83354] hover:text-black transition-colors">{t('product.home')}</Link>
        <span className="text-black/30">›</span>
        <button type="button" onClick={() => goBack(navigate, location, '/shop')} className="text-[#E83354] hover:text-black transition-colors">{t('nav.shop')}</button>
        <span className="text-black/30">›</span>
        <span className="text-black/60 normal-case tracking-normal font-normal">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
        {/* LEFT — Gallery */}
        <div className="flex gap-4">
          <div className="flex flex-col gap-2 w-16">
            {images.map((img, i) => (
              <button
                key={img.id ?? i}
                onClick={() => setActiveImg(i)}
                className={`w-16 h-20 overflow-hidden border-2 transition-all ${i === activeImg ? 'border-black' : 'border-transparent opacity-60 hover:opacity-100'
                  }`}
              >
                <img src={img.url} alt={img.altText || product.name} className="w-full h-full object-cover" />
              </button>
            ))}
          </div>

          <div className="flex-1 overflow-hidden bg-white relative">
            {images[activeImg] ? (
              <img
                src={images[activeImg].url}
                alt={images[activeImg].altText || product.name}
                className="w-full h-full object-cover"
                style={{ aspectRatio: '4/5' }}
              />
            ) : (
              <div className="bg-black/5 flex items-center justify-center text-black/30 text-xs" style={{ aspectRatio: '4/5' }}>
                {t('common.noImage')}
              </div>
            )}
          </div>
        </div>

        {/* RIGHT — Product details */}
        <div>
          <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-2">
            {product.categoryName}
          </p>
          <h1 className="product-title font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-3">
            {product.name}
          </h1>

          <ProductRatingLine product={product} />

          <div className="mb-6">
            <div className="flex items-center gap-3">
              {displaySale != null ? (
                <>
                  <span className="font-['Anton'] text-3xl text-[#E83354]">
                    {formatPrice(displaySale, product.currency)}
                  </span>
                  <span className="text-lg text-black/40 line-through">
                    {formatPrice(displayOriginal, product.currency)}
                  </span>
                  {displayPct != null && (
                    <span className="bg-[#E83354] text-white text-[11px] font-bold tracking-widest uppercase px-2 py-1">
                      -{displayPct}%
                    </span>
                  )}
                </>
              ) : (
                <span className="font-['Anton'] text-3xl">
                  {formatPrice(displayOriginal, product.currency)}
                </span>
              )}
            </div>
            {displaySale != null && product.saleEndsAt && (
              <p className="text-[11px] font-bold tracking-wider uppercase text-[#E83354] mt-1.5">
                {t('product.saleEnds', { date: new Date(product.saleEndsAt).toLocaleDateString(i18n.language) })}
              </p>
            )}
          </div>

          {product.description && (
            <p className="text-sm text-black/60 leading-relaxed mb-8">{product.description}</p>
          )}

          {/* Color selector */}
          {colors.length > 0 && (
            <div className="mb-6">
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm font-bold tracking-[0.12em] uppercase">{t('product.color')}</span>
                <span className="text-xs text-black/60 normal-case tracking-normal">{colorLabel(t, selectedColor)}</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {colors.map((c) => {
                  const enabled = colorEnabled(c);
                  const active = selectedColor === c;
                  return (
                    <button
                      key={c}
                      type="button"
                      onClick={() => chooseColor(c)}
                      disabled={!enabled && !active}
                      title={!enabled ? t('product.outOfStock') : undefined}
                      className={`px-3 py-2 text-[11px] font-bold tracking-wider border transition-all ${active
                        ? 'bg-black text-white border-black'
                        : enabled
                          ? 'bg-white text-black border-black/20 hover:border-black'
                          : 'bg-black/5 text-black/30 border-black/10 line-through cursor-not-allowed'
                        }`}
                    >
                      {colorLabel(t, c)}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Size selector */}
          {sizes.length > 0 && (
            <div className="mb-8">
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm font-bold tracking-[0.12em] uppercase">{t('product.size')}</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {sizes.map((s) => {
                  const enabled = sizeEnabled(s);
                  const active = selectedSize === s;
                  return (
                    <button
                      key={s}
                      type="button"
                      onClick={() => chooseSize(s)}
                      disabled={!enabled && !active}
                      title={!enabled ? t('product.outOfStock') : undefined}
                      className={`w-12 h-12 text-[12px] font-bold border transition-all ${active
                        ? 'bg-black text-white border-black'
                        : enabled
                          ? 'bg-white text-black border-black/20 hover:border-black'
                          : 'bg-black/5 text-black/30 border-black/10 line-through cursor-not-allowed'
                        }`}
                    >
                      {s}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Quantity selector */}
          <div className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-bold tracking-[0.12em] uppercase">{t('product.quantity')}</span>
              {selectedVariant?.stockQuantity != null && (
                <span className="text-xs text-black/60">
                  {t('product.inStock', { n: selectedVariant.stockQuantity })}
                </span>
              )}
            </div>
            <QuantityStepper
              value={quantity}
              min={1}
              max={selectedVariant?.stockQuantity ?? 99}
              onChange={setQuantity}
              disabled={!selectedVariant}
            />
          </div>

          <div className="flex flex-col gap-3 mb-8">
            <button
              onClick={handleBuyNow}
              disabled={adding}
              className="w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase transition-all bg-[#E83354] text-white hover:bg-black disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {t('product.buyNow')}
            </button>
            <button
              onClick={handleAddToCart}
              disabled={adding}
              className="w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase transition-all bg-black text-white hover:bg-[#E83354] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {adding ? t('product.adding') : t('product.addToCart')}
            </button>
            <button
              onClick={() => navigate(`/try-on/${product.slug || product.id}`)}
              className="w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase border-2 border-black text-black hover:bg-black hover:text-white transition-all flex items-center justify-center gap-3"
            >
              <span>👁</span> {t('product.virtualTryOn')}
            </button>
            <button
              onClick={handleWishlist}
              disabled={wishlistBusy}
              aria-pressed={wished}
              className={`w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase border-2 transition-all flex items-center justify-center gap-3 disabled:opacity-50 disabled:cursor-not-allowed ${
                wished
                  ? 'border-[#E83354] text-[#E83354] hover:bg-[#E83354] hover:text-white'
                  : 'border-black text-black hover:bg-black hover:text-white'
              }`}
            >
              <HeartIcon filled={wished} size={16} />
              {wished ? t('wishlist.inWishlist') : t('wishlist.addToWishlist')}
            </button>
          </div>

          {product.attributes && Object.keys(product.attributes).length > 0 && (
            <div className="border-t border-black/10 pt-5">
              <h3 className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-3">{t('product.details')}</h3>
              <dl className="divide-y divide-black/5">
                {Object.entries(product.attributes).map(([k, v]) => (
                  <div key={k} className="grid grid-cols-[140px_1fr] gap-4 py-2.5 items-center">
                    <dt className="text-black/50 uppercase tracking-wider text-[10px] font-bold leading-none">{k}</dt>
                    <dd className="text-sm text-black/80 break-words leading-none">{v}</dd>
                  </div>
                ))}
              </dl>
            </div>
          )}
        </div>
      </div>

      <BrandCard brandId={product.brandId} brandName={product.brandName} />

      <ReviewsSection product={product} onChanged={reloadProduct} />

      <RecommendationRow title={t('product.fbt')} productId={product.id} fetcher={getFrequentlyBoughtTogether} />
      <RecommendationRow title={t('product.alsoLike')} productId={product.id} fetcher={getSimilarProducts} />
    </PageShell>
  );
}

function ProductRatingLine({ product }) {
  const { t } = useTranslation();
  const count = product.reviewCount ?? 0;
  const avg = product.averageRating;
  const sold = product.soldCount ?? 0;
  return (
    <div className="flex items-center gap-3 mb-4 pb-4 border-b border-black/8 text-[12px] text-black/60">
      {count > 0 ? (
        <>
          <StarRating value={avg ?? 0} size={15} />
          <span className="font-bold text-black">{Number(avg ?? 0).toFixed(1)}</span>
          <a href="#reviews" className="text-black/50 hover:text-[#E83354] transition-colors">
            {t('product.reviewsCount', { count })}
          </a>
        </>
      ) : (
        <span className="flex items-center gap-2 text-black/40">
          <StarRating value={0} size={15} /> {t('product.noReviews')}
        </span>
      )}
      {sold > 0 && (
        <>
          <span className="text-black/20">·</span>
          <span className="text-black/50">{t('common.sold', { n: sold })}</span>
        </>
      )}
    </div>
  );
}

function PageShell({ children }) {
  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />
      <div className="max-w-[1440px] mx-auto px-6 py-10">{children}</div>
      <FooterFull />
    </div>
  );
}

function ProductSkeleton() {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 animate-pulse">
      <div className="bg-black/5" style={{ aspectRatio: '4/5' }} />
      <div className="space-y-4">
        <div className="h-3 w-1/4 bg-black/10" />
        <div className="h-10 w-3/4 bg-black/10" />
        <div className="h-8 w-1/3 bg-black/10" />
        <div className="h-20 w-full bg-black/5" />
        <div className="h-12 w-2/3 bg-black/10" />
        <div className="h-12 w-full bg-black/10" />
      </div>
    </div>
  );
}

function uniqueValues(items, key) {
  if (!Array.isArray(items)) return [];
  const seen = new Set();
  const out = [];
  for (const it of items) {
    const v = it?.[key];
    if (v != null && !seen.has(v)) {
      seen.add(v);
      out.push(v);
    }
  }
  return out;
}

