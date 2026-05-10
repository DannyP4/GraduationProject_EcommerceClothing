import { useEffect, useMemo, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import QuantityStepper from '../components/QuantityStepper';
import { useToast } from '../components/Toast';
import { getProductByIdOrSlug } from '../services/productService';
import { useCart } from '../context/CartContext';

export default function ProductPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addItem } = useCart();
  const toast = useToast();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [activeImg, setActiveImg] = useState(0);
  const [selectedSize, setSelectedSize] = useState('');
  const [selectedColor, setSelectedColor] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);

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
  }, [id]);

  const colors = useMemo(() => uniqueValues(product?.variants, 'color'), [product]);
  const sizes = useMemo(() => uniqueValues(product?.variants, 'size'), [product]);
  const selectedVariant = useMemo(
    () => product?.variants?.find((v) => v.color === selectedColor && v.size === selectedSize) || null,
    [product, selectedColor, selectedSize]
  );

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
          <p className="text-sm font-bold text-[#E83354] mb-2 uppercase tracking-wider">Product not available</p>
          <p className="text-xs text-black/60 mb-6">{error || 'This product could not be found.'}</p>
          <Link
            to="/shop"
            className="inline-block text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
          >
            Back to Shop
          </Link>
        </div>
      </PageShell>
    );
  }

  const images = product.images?.length ? product.images : [];

  const handleAddToCart = async () => {
    if (!selectedSize) {
      toast.error('Please select a size');
      return;
    }
    if (!selectedVariant) {
      toast.error('This size/color combination is not available');
      return;
    }
    if (selectedVariant.stockQuantity != null && quantity > selectedVariant.stockQuantity) {
      toast.error(`Only ${selectedVariant.stockQuantity} in stock`);
      return;
    }
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
        unitPrice: Number(selectedVariant.price ?? product.basePrice),
        currency: product.currency,
      });
      toast.success(`Added ${quantity} × ${product.name} to cart`);
    } catch (err) {
      toast.error(err.message || 'Could not add to cart');
    } finally {
      setAdding(false);
    }
  };

  return (
    <PageShell>
      <nav className="flex items-center gap-2 text-[11px] font-bold tracking-[0.1em] uppercase mb-8">
        <Link to="/" className="text-[#E83354] hover:text-black transition-colors">Home</Link>
        <span className="text-black/30">›</span>
        <Link to="/shop" className="text-[#E83354] hover:text-black transition-colors">Shop</Link>
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
                No image
              </div>
            )}
          </div>
        </div>

        {/* RIGHT — Product details */}
        <div>
          <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-2">
            {product.categoryName}
          </p>
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-3">
            {product.name}
          </h1>

          <ProductStatsPlaceholder />

          <div className="flex items-center gap-3 mb-6">
            <span className="font-['Anton'] text-3xl">
              {formatPrice(selectedVariant?.price ?? product.basePrice, product.currency)}
            </span>
          </div>

          {product.description && (
            <p className="text-sm text-black/60 leading-relaxed mb-8">{product.description}</p>
          )}

          {/* Color selector */}
          {colors.length > 0 && (
            <div className="mb-6">
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm font-bold tracking-[0.12em] uppercase">Color</span>
                <span className="text-xs text-black/60 normal-case tracking-normal">{selectedColor}</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {colors.map((c) => (
                  <button
                    key={c}
                    onClick={() => setSelectedColor((prev) => (prev === c ? '' : c))}
                    className={`px-3 py-2 text-[11px] font-bold tracking-wider border transition-all ${selectedColor === c
                      ? 'bg-black text-white border-black'
                      : 'bg-white text-black border-black/20 hover:border-black'
                      }`}
                  >
                    {c}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Size selector */}
          {sizes.length > 0 && (
            <div className="mb-8">
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm font-bold tracking-[0.12em] uppercase">Size</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {sizes.map((s) => (
                  <button
                    key={s}
                    onClick={() => setSelectedSize((prev) => (prev === s ? '' : s))}
                    className={`w-12 h-12 text-[12px] font-bold border transition-all ${selectedSize === s
                      ? 'bg-black text-white border-black'
                      : 'bg-white text-black border-black/20 hover:border-black'
                      }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Quantity selector */}
          <div className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-bold tracking-[0.12em] uppercase">Quantity</span>
              {selectedVariant?.stockQuantity != null && (
                <span className="text-xs text-black/60">
                  <span className="font-bold text-black">{selectedVariant.stockQuantity}</span> in stock
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
              onClick={handleAddToCart}
              disabled={adding}
              className="w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase transition-all bg-black text-white hover:bg-[#E83354] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {adding ? 'Adding…' : '+ Add to Cart'}
            </button>
            <button
              onClick={() => navigate('/try-on')}
              className="w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase border-2 border-black text-black hover:bg-black hover:text-white transition-all flex items-center justify-center gap-3"
            >
              <span>👁</span> Virtual Try-On
            </button>
          </div>

          {product.attributes && Object.keys(product.attributes).length > 0 && (
            <div className="border-t border-black/10 pt-5">
              <h3 className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-3">Details</h3>
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

      <ReviewsPlaceholder />
    </PageShell>
  );
}

// Stats placeholder — kept until ratings/sales aggregations land. Greyed-out so users don't read it as live data.
function ProductStatsPlaceholder() {
  return (
    <div className="flex items-center gap-3 mb-4 pb-4 border-b border-black/8 text-[11px] text-black/40">
      <span className="flex items-center gap-1">
        <StarIcon /> <span className="font-bold">—</span>
      </span>
      <span className="text-black/15">|</span>
      <span><span className="font-bold">—</span> reviews</span>
      <span className="text-black/15">|</span>
      <span><span className="font-bold">—</span> sold</span>
      <span className="ml-auto text-[9px] tracking-[0.15em] uppercase bg-black/5 px-2 py-0.5">Coming soon</span>
    </div>
  );
}

function StarIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" className="text-amber-500/40">
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
    </svg>
  );
}

function ReviewsPlaceholder() {
  return (
    <section className="mt-16 pt-10 border-t border-black/10">
      <div className="flex items-end justify-between mb-6">
        <div>
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">Customer Voices</p>
          <h2 className="font-['Anton'] text-3xl md:text-4xl uppercase tracking-tight">Reviews</h2>
        </div>
        <span className="text-[10px] tracking-[0.15em] uppercase bg-black/5 text-black/40 px-2 py-1">Coming soon</span>
      </div>

      <div className="bg-white border border-dashed border-black/15 px-6 py-12 text-center">
        <div className="inline-flex flex-col items-center gap-3">
          <div className="flex gap-1 text-black/15">
            {[0, 1, 2, 3, 4].map((i) => <StarIcon key={i} />)}
          </div>
          <p className="text-sm font-bold uppercase tracking-wider text-black/60">No reviews yet</p>
          <p className="text-xs text-black/50 max-w-sm">
            Be the first to share your thoughts. Lazy-loaded reviews with infinite scroll will land in a later phase.
          </p>
          <button
            disabled
            title="Available after the reviews phase ships"
            className="mt-2 text-[11px] font-bold tracking-[0.15em] uppercase border border-black/20 text-black/40 px-4 py-2 cursor-not-allowed"
          >
            Write a Review
          </button>
        </div>
      </div>

      {/* Skeleton: shape of the future review card so the layout slot is visible to demo */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6 opacity-30 pointer-events-none">
        {[0, 1].map((i) => (
          <div key={i} className="bg-white border border-black/8 p-5">
            <div className="flex items-center gap-2 mb-2">
              <div className="w-7 h-7 rounded-full bg-black/10" />
              <div className="h-3 w-24 bg-black/10" />
              <div className="ml-auto flex gap-0.5">
                {[0, 1, 2, 3, 4].map((s) => <StarIcon key={s} />)}
              </div>
            </div>
            <div className="space-y-1.5">
              <div className="h-2 w-full bg-black/8" />
              <div className="h-2 w-5/6 bg-black/8" />
              <div className="h-2 w-2/3 bg-black/8" />
            </div>
          </div>
        ))}
      </div>
    </section>
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

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}
