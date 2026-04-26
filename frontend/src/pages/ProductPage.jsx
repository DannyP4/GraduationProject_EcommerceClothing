import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import { getProductById, products } from '../data/products';
import { useCart } from '../context/CartContext';

export default function ProductPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addItem } = useCart();

  const product = getProductById(id) || products[0];
  const [activeImg, setActiveImg] = useState(0);
  const [selectedSize, setSelectedSize] = useState('');
  const [selectedColor, setSelectedColor] = useState(product.colors[0]);
  const [added, setAdded] = useState(false);

  const related = product.related
    .map((rid) => getProductById(rid))
    .filter(Boolean)
    .slice(0, 4);

  const handleAddToCart = () => {
    if (!selectedSize) { alert('Please select a size'); return; }
    addItem({
      id: product.id,
      name: product.name,
      price: product.price,
      size: selectedSize,
      color: selectedColor,
      image: product.images[0],
    });
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  };

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />

      <div className="max-w-[1440px] mx-auto px-6 py-10">
        {/* Breadcrumb */}
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
            {/* Thumbnails */}
            <div className="flex flex-col gap-2 w-16">
              {product.images.map((img, i) => (
                <button
                  key={i}
                  onClick={() => setActiveImg(i)}
                  className={`w-16 h-20 overflow-hidden border-2 transition-all ${
                    i === activeImg ? 'border-black' : 'border-transparent opacity-60 hover:opacity-100'
                  }`}
                >
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>

            {/* Main image */}
            <div className="flex-1 overflow-hidden bg-white relative">
              <img
                src={product.images[activeImg]}
                alt={product.name}
                className="w-full h-full object-cover"
                style={{ aspectRatio: '4/5' }}
              />
              {product.badge && (
                <span className="absolute top-4 left-4 bg-[#E83354] text-white text-[10px] font-bold tracking-widest uppercase px-3 py-1.5">
                  {product.badge}
                </span>
              )}
            </div>
          </div>

          {/* RIGHT — Product details */}
          <div>
            <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-2">
              {product.category}
            </p>
            <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-4">
              {product.name}
            </h1>

            {/* Price */}
            <div className="flex items-center gap-3 mb-6">
              <span className="font-['Anton'] text-3xl">${product.price}</span>
              {product.originalPrice && (
                <>
                  <span className="text-lg text-black/35 line-through">${product.originalPrice}</span>
                  <span className="bg-[#E83354] text-white text-[10px] font-bold px-2 py-0.5">
                    SAVE ${product.originalPrice - product.price}
                  </span>
                </>
              )}
            </div>

            <p className="text-sm text-black/60 leading-relaxed mb-8">{product.description}</p>

            {/* Color selector */}
            <div className="mb-6">
              <div className="flex items-center justify-between mb-3">
                <span className="text-[11px] font-bold tracking-[0.15em] uppercase">Color</span>
                <span className="text-[11px] text-black/50">{selectedColor}</span>
              </div>
              <div className="flex gap-2">
                {product.colors.map((c) => (
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

            {/* Size selector */}
            <div className="mb-8">
              <div className="flex items-center justify-between mb-3">
                <span className="text-[11px] font-bold tracking-[0.15em] uppercase">Size</span>
                <a href="#" className="text-[11px] text-black/40 hover:text-black underline transition-colors">
                  Size Guide
                </a>
              </div>
              <div className="flex flex-wrap gap-2">
                {product.sizes.map((s) => (
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

            {/* CTAs */}
            <div className="flex flex-col gap-3 mb-8">
              <button
                onClick={handleAddToCart}
                className={`w-full py-4 text-[12px] font-bold tracking-[0.15em] uppercase transition-all ${
                  added
                    ? 'bg-green-600 text-white'
                    : 'bg-black text-white hover:bg-[#E83354]'
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

            {/* Details accordion */}
            <div className="border-t border-black/10 pt-4 space-y-4">
              {['Free Shipping on Orders $75+', '30-Day Free Returns', 'Ethically Manufactured'].map((feat) => (
                <div key={feat} className="flex items-center gap-3 text-sm text-black/60">
                  <span className="text-[#E83354] font-bold">✓</span>
                  {feat}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Complete The Fit */}
        <section className="mt-20">
          <div className="mb-8">
            <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">Styled With</p>
            <h2 className="font-['Anton'] text-4xl uppercase tracking-tight">Complete The Fit</h2>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {related.map((p) => (
              <div
                key={p.id}
                className="group bg-white cursor-pointer"
                onClick={() => navigate(`/product/${p.id}`)}
              >
                <div className="relative overflow-hidden" style={{ paddingTop: '125%' }}>
                  <img
                    src={p.images[0]}
                    alt={p.name}
                    className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                  {p.badge && (
                    <span className="absolute top-2 left-2 bg-[#E83354] text-white text-[9px] font-bold tracking-widest uppercase px-1.5 py-0.5">
                      {p.badge}
                    </span>
                  )}
                </div>
                <div className="p-3">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-black/40 mb-0.5">{p.category}</p>
                  <h3 className="text-xs font-bold uppercase tracking-wider mb-1">{p.name}</h3>
                  <span className="font-['Anton'] text-lg">${p.price}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <FooterFull />
    </div>
  );
}
