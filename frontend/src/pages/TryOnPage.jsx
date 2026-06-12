import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useToast } from '../components/Toast';
import { products } from '../data/products';

const GARMENTS = [
  { id: 'varsity-bomber', label: 'Varsity Bomber', img: 'https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=600&q=80' },
  { id: 'campus-hoodie', label: 'Arch Hoodie', img: 'https://images.unsplash.com/photo-1556821840-3a63f15732ce?w=600&q=80' },
  { id: 'track-jacket', label: 'Track Jacket', img: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&q=80' },
  { id: 'graphic-tee', label: 'Graphic Tee', img: 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&q=80' },
];

export default function TryOnPage() {
  const navigate = useNavigate();
  const { addItem } = useCart();
  const toast = useToast();
  const flashRef = useRef(null);

  const [opacity, setOpacity] = useState(0.85);
  const [selectedGarment, setSelectedGarment] = useState(GARMENTS[0]);
  const [selectedSize, setSelectedSize] = useState('M');
  const [captured, setCaptured] = useState(false);

  const handleCapture = () => {
    if (flashRef.current) {
      flashRef.current.style.opacity = '1';
      setTimeout(() => { if (flashRef.current) flashRef.current.style.opacity = '0'; }, 200);
    }
    setCaptured(true);
    setTimeout(() => setCaptured(false), 3000);
  };

  const handleAddToCart = () => {
    const product = products.find(p => p.id === selectedGarment.id);
    if (product) {
      addItem({
        id: product.id,
        name: product.name,
        price: product.price,
        size: selectedSize,
        color: product.colors[0],
        image: product.images[0],
      });
      toast.success(`${product.name} (${selectedSize}) added to cart`);
    }
  };

  return (
    <div className="h-screen w-screen overflow-hidden relative bg-black">
      {/* Flash overlay */}
      <div
        ref={flashRef}
        className="absolute inset-0 bg-white pointer-events-none z-50"
        style={{ opacity: 0, transition: 'opacity 0.05s ease' }}
      />

      {/* Camera background */}
      <img
        src="https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1920&q=80"
        alt="Background"
        className="absolute inset-0 w-full h-full object-cover"
        style={{ filter: 'brightness(0.7)' }}
      />

      {/* Garment overlay */}
      <div
        className="absolute inset-0 flex items-center justify-center pointer-events-none"
        style={{ opacity }}
      >
        <img
          src={selectedGarment.img}
          alt={selectedGarment.label}
          className="h-[75vh] w-auto object-contain"
          style={{
            filter: 'drop-shadow(0 20px 60px rgba(0,0,0,0.5)) saturate(1.2)',
            mixBlendMode: 'luminosity',
          }}
        />
      </div>

      {/* Top bar */}
      <div className="absolute top-0 inset-x-0 flex items-center justify-between px-6 py-4 bg-gradient-to-b from-black/60 to-transparent z-10">
        <div className="font-['Anton'] text-2xl tracking-widest text-white">VESTA</div>
        <div className="text-center">
          <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/60">Virtual Try-On Studio</p>
          {captured && (
            <p className="text-[11px] font-bold text-[#F5C842] animate-pulse">📸 Captured!</p>
          )}
        </div>
        <button
          onClick={() => navigate(-1)}
          className="text-[11px] font-bold tracking-[0.15em] uppercase text-white/70 hover:text-white border border-white/30 px-4 py-2 hover:border-white/70 transition-all flex items-center gap-2"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          Exit Studio
        </button>
      </div>

      {/* Left panel — Garment selector */}
      <div className="absolute left-4 top-1/2 -translate-y-1/2 z-10 flex flex-col gap-2">
        {GARMENTS.map((g) => (
          <button
            key={g.id}
            onClick={() => setSelectedGarment(g)}
            className={`w-16 h-20 overflow-hidden border-2 transition-all ${
              selectedGarment.id === g.id
                ? 'border-[#E83354] scale-105'
                : 'border-white/20 opacity-60 hover:opacity-100 hover:border-white/60'
            }`}
          >
            <img src={g.img} alt={g.label} className="w-full h-full object-cover" />
          </button>
        ))}
      </div>

      {/* Right panel — Controls */}
      <div className="absolute right-4 top-1/2 -translate-y-1/2 z-10 w-52 space-y-4">
        {/* Garment name */}
        <div className="bg-black/60 backdrop-blur-md p-4 border border-white/10">
          <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/50 mb-1">Now Trying</p>
          <p className="font-['Anton'] text-lg text-white uppercase tracking-wide">{selectedGarment.label}</p>
        </div>

        {/* Size selector */}
        <div className="bg-black/60 backdrop-blur-md p-4 border border-white/10">
          <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/50 mb-3">Size</p>
          <div className="grid grid-cols-3 gap-1.5">
            {['XS', 'S', 'M', 'L', 'XL'].map((s) => (
              <button
                key={s}
                onClick={() => setSelectedSize(s)}
                className={`py-1.5 text-[11px] font-bold border transition-all ${
                  selectedSize === s
                    ? 'bg-[#E83354] text-white border-[#E83354]'
                    : 'bg-white/10 text-white border-white/20 hover:border-white/50'
                }`}
              >
                {s}
              </button>
            ))}
          </div>
        </div>

        {/* Opacity slider */}
        <div className="bg-black/60 backdrop-blur-md p-4 border border-white/10">
          <div className="flex justify-between items-center mb-3">
            <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/50">Opacity</p>
            <span className="text-[11px] text-white/70 font-bold">{Math.round(opacity * 100)}%</span>
          </div>
          <input
            type="range"
            min="0.2"
            max="1"
            step="0.05"
            value={opacity}
            onChange={(e) => setOpacity(parseFloat(e.target.value))}
            className="w-full accent-[#E83354] cursor-pointer"
          />
        </div>
      </div>

      {/* Bottom controls */}
      <div className="absolute bottom-0 inset-x-0 z-10 flex items-end justify-center gap-4 pb-8 bg-gradient-to-t from-black/60 via-black/20 to-transparent pt-16">
        {/* Add to Cart */}
        <button
          onClick={handleAddToCart}
          className="bg-white text-black text-[11px] font-bold tracking-[0.15em] uppercase px-6 py-3 hover:bg-[#E83354] hover:text-white transition-all"
        >
          + Add to Cart
        </button>

        {/* Capture */}
        <button
          onClick={handleCapture}
          className="w-16 h-16 rounded-full bg-white border-4 border-white/50 hover:scale-110 transition-transform flex items-center justify-center shadow-2xl"
          aria-label="Capture"
        >
          <div className="w-10 h-10 rounded-full bg-white border-2 border-black/10" />
        </button>

        {/* Share */}
        <button className="bg-white/10 backdrop-blur text-white text-[11px] font-bold tracking-[0.15em] uppercase px-6 py-3 border border-white/20 hover:bg-white/20 transition-all">
          Share
        </button>
      </div>

      {/* Scan lines overlay for realism */}
      <div
        className="absolute inset-0 pointer-events-none z-0"
        style={{
          background: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,0,0,0.015) 2px, rgba(0,0,0,0.015) 4px)',
        }}
      />

      {/* Corner guides */}
      {[
        'top-16 left-6',
        'top-16 right-6',
        'bottom-32 left-6',
        'bottom-32 right-6',
      ].map((pos, i) => (
        <div
          key={i}
          className={`absolute ${pos} w-8 h-8 pointer-events-none z-10`}
          style={{
            borderTop: i < 2 ? '2px solid rgba(255,255,255,0.3)' : 'none',
            borderBottom: i >= 2 ? '2px solid rgba(255,255,255,0.3)' : 'none',
            borderLeft: i % 2 === 0 ? '2px solid rgba(255,255,255,0.3)' : 'none',
            borderRight: i % 2 === 1 ? '2px solid rgba(255,255,255,0.3)' : 'none',
          }}
        />
      ))}
    </div>
  );
}
