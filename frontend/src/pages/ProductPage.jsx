import { useEffect, useMemo, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import { getProductByIdOrSlug } from '../services/productService';
import { useCart } from '../context/CartContext';

export default function ProductPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addItem } = useCart();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [activeImg, setActiveImg] = useState(0);
  const [selectedSize, setSelectedSize] = useState('');
  const [selectedColor, setSelectedColor] = useState('');
  const [added, setAdded] = useState(false);
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setProduct(null);
    setActiveImg(0);
    setSelectedSize('');
    setSelectedColor('');

    getProductByIdOrSlug(id)
      .then((data) => {
        if (cancelled) return;
        setProduct(data);
        const firstColor = uniqueValues(data?.variants, 'color')[0];
        if (firstColor) setSelectedColor(firstColor);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load product');
      })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [id]);

  const colors = useMemo(() => uniqueValues(product?.variants, 'color'), [product]);
  const sizes  = useMemo(() => uniqueValues(product?.variants, 'size'),  [product]);
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

  const handleAddToCart = () => {
    if (!selectedSize) {
      setFeedback({ type: 'error', message: 'Please select a size' });
      setTimeout(() => setFeedback(null), 2500);
      return;
    }
    addItem({
      id: product.id,
      name: product.name,
      price: Number(selectedVariant?.price ?? product.basePrice),
      size: selectedSize,
      color: selectedColor || '—',
      image: images[0]?.url,
    });
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  };

  return (
    <PageShell>
      <nav className="flex items-center gap-2 text-[11px] font-bold tracking-[0.1em] uppercase text-black/40 mb-8">
        <Link to="/" className="hover:text-black transition-colors">Home</Link>
        <span>/</span>
        <Link to="/shop" className="hover:text-black transition-colors">Shop</Link>
        <span>/</span>
        <span className="text-black">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
        {/* LEFT — Gallery */}
        <div className="flex gap-4">
          <div className="flex flex-col gap-2 w-16">
            {images.map((img, i) => (
              <button
                key={img.id ?? i}
                onClick={() => setActiveImg(i)}
                className={`w-16 h-20 overflow-hidden border-2 transition-all ${
                  i === activeImg ? 'border-black' : 'border-transparent opacity-60 hover:opacity-100'
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
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-4">
            {product.name}
          </h1>

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
                <span className="text-[11px] font-bold tracking-[0.15em] uppercase">Color</span>
                <span className="text-[11px] text-black/50">{selectedColor}</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {colors.map((c) => (
                  <button
                    key={c}
                    onClick={() => setSelectedColor(c)}
                    className={`px-3 py-2 text-[11px] font-bold tracking-wider border transition-all ${
                      selectedColor === c
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
                <span className="text-[11px] font-bold tracking-[0.15em] uppercase">Size</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {sizes.map((s) => (
                  <button
                    key={s}
                    onClick={() => setSelectedSize(s)}
                    className={`w-12 h-12 text-[12px] font-bold border transition-all ${
                      selectedSize === s
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

          {feedback && (
            <div className={`mb-4 text-xs font-bold tracking-wider uppercase ${feedback.type === 'error' ? 'text-[#E83354]' : 'text-green-700'}`}>
              {feedback.message}
            </div>
          )}

          <div className="flex flex-col gap-3 mb-8">
            <button
              onClick={handleAddToCart}
              className={`w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase transition-all ${
                added ? 'bg-green-600 text-white' : 'bg-black text-white hover:bg-[#E83354]'
              }`}
            >
              {added ? '✓ Added to Cart!' : '+ Add to Cart'}
            </button>
            <button
              onClick={() => navigate('/try-on')}
              className="w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase border-2 border-black text-black hover:bg-black hover:text-white transition-all flex items-center justify-center gap-3"
            >
              <span>👁</span> Virtual Try-On
            </button>
          </div>

          {/* Attributes */}
          {product.attributes && Object.keys(product.attributes).length > 0 && (
            <div className="border-t border-black/10 pt-4 grid grid-cols-2 gap-x-6 gap-y-2 text-sm text-black/70">
              {Object.entries(product.attributes).map(([k, v]) => (
                <div key={k} className="flex justify-between border-b border-black/5 pb-1">
                  <span className="text-black/50 uppercase tracking-wider text-[10px] font-bold">{k}</span>
                  <span>{v}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </PageShell>
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
  if (currency === 'VND') {
    return `${num.toLocaleString('vi-VN')} ₫`;
  }
  return `${currency || '$'} ${num.toFixed(2)}`;
}
