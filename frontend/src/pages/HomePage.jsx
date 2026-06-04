import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import NavbarGlass from '../components/NavbarGlass';
import AnnouncementBar from '../components/AnnouncementBar';
import FooterFull from '../components/FooterFull';
import { getProducts } from '../services/productService';

export default function HomePage() {
  const bgTextRef = useRef(null);
  const ribbon1Ref = useRef(null);
  const ribbon2Ref = useRef(null);
  const modelRef = useRef(null);
  const heroRef = useRef(null);
  const navigate = useNavigate();

  const [trending, setTrending] = useState([]);
  const [trendingLoading, setTrendingLoading] = useState(true);

  useEffect(() => {
    const t = setTimeout(() => heroRef.current?.classList.add('hero-entered'), 100);
    return () => clearTimeout(t);
  }, []);

  useEffect(() => {
    const handleMouseMove = (e) => {
      const cx = window.innerWidth / 2;
      const cy = window.innerHeight / 2;
      const dx = (e.clientX - cx) / cx;
      const dy = (e.clientY - cy) / cy;

      if (bgTextRef.current) {
        bgTextRef.current.style.transform = `translate(calc(-50% + ${dx * -45}px), calc(-50% + ${dy * -28}px))`;
      }
      if (ribbon1Ref.current) {
        ribbon1Ref.current.style.transform = `translate(${dx * 28}px, ${dy * 20}px)`;
      }
      if (ribbon2Ref.current) {
        ribbon2Ref.current.style.transform = `translate(${dx * -22}px, ${dy * 28}px)`;
      }
      if (modelRef.current) {
        modelRef.current.style.transform = `translateX(${dx * -18}px) translateY(${dy * -10}px)`;
      }
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  useEffect(() => {
    let cancelled = false;
    setTrendingLoading(true);
    getProducts({ size: 4, sort: 'POPULAR' })
      .then((data) => { if (!cancelled) setTrending(data?.content ?? []); })
      .catch(() => { if (!cancelled) setTrending([]); })
      .finally(() => { if (!cancelled) setTrendingLoading(false); });
    return () => { cancelled = true; };
  }, []);

  return (
    <div style={{ background: 'var(--bg-color)', minHeight: '100vh' }}>
      <AnnouncementBar />
      <NavbarGlass />

      {/* HERO */}
      <section className="hero" ref={heroRef}>
        <div className="hero-bg-text" ref={bgTextRef}>CAMPUS LEGEND</div>

        <div className="hero-shape hero-shape-1" />
        <div className="hero-shape hero-shape-2" />
        <div className="hero-ribbon hero-ribbon-1" ref={ribbon1Ref} />
        <div className="hero-ribbon hero-ribbon-2" ref={ribbon2Ref} />
        <div className="hero-ribbon hero-ribbon-3" />

        <img
          ref={modelRef}
          className="hero-model"
          src="https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=900&q=90"
          alt="Model"
        />

        <div className="hero-content">
          <p className="hero-eyebrow">SS 2026 Collection</p>
          <h1 className="hero-title">
            DRESS<br />
            LIKE A<br />
            <span>LEGEND</span>
          </h1>
          <p className="hero-subtitle">
            Student streetwear engineered for campus life and beyond. Limited drops, unlimited style.
          </p>
          <div className="hero-cta">
            <Link to="/shop" className="btn-primary">Shop The Drop</Link>
            <Link to="/shop" className="btn-secondary">Explore Looks</Link>
          </div>
        </div>
      </section>

      {/* FEATURED LOOKBOOKS */}
      <section style={{ padding: '80px 48px' }}>
        <div style={{ marginBottom: '40px' }}>
          <p className="section-label">Editorial</p>
          <h2 className="section-title">Featured<br />Lookbooks</h2>
        </div>

        <div className="lookbooks-grid">
          {[
            { tag: 'Campus Essentials', title: 'Back to School' },
            { tag: 'Streetwear', title: 'Urban Edit' },
            { tag: 'Seasonal', title: 'Winter Layers' },
            { tag: 'Minimal', title: 'Clean Cuts' },
            { tag: 'Retro', title: 'Varsity Club' },
          ].map((lb, i) => (
            <div key={i} className="lookbook-card">
              <div className="lookbook-card-inner" style={{ height: '100%' }}>
                <img
                  className="lookbook-img"
                  src={`https://images.unsplash.com/photo-${['1539109136881-3be0616acf4b', '1509631179647-0177331693ae', '1516762689617-e1cffcef479d', '1487222477894-8943e31ef7b2', '1591047139829-d91aecb6caea'][i]}?w=800&q=80`}
                  alt={lb.title}
                  style={{ height: '100%', minHeight: 'unset' }}
                />
                <div className="lookbook-overlay">
                  <p className="lookbook-tag">{lb.tag}</p>
                  <h3 className="lookbook-title">{lb.title}</h3>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* STATEMENT */}
      <section className="statement-section">
        <div
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: 'clamp(14px, 2vw, 18px)',
            fontWeight: 700,
            letterSpacing: '0.3em',
            textTransform: 'uppercase',
            color: 'rgba(255,255,255,0.4)',
            marginBottom: '24px',
          }}
        >
          The Manifesto
        </div>
        <p className="statement-text">
          WEAR YOUR <span className="highlight">AMBITION.</span> OWN THE <span className="highlight">CAMPUS.</span> SET THE STANDARD.
        </p>
        <p style={{ marginTop: '32px', color: 'rgba(255,255,255,0.5)', fontSize: '14px', maxWidth: '500px', margin: '32px auto 0', lineHeight: 1.7 }}>
          Every piece is designed with the student in mind — built for long days, late nights, and everything in between.
        </p>
      </section>

      {/* TRENDING */}
      <section style={{ padding: '80px 48px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '40px' }}>
          <div>
            <p className="section-label">What's Hot</p>
            <h2 className="section-title">Trending<br />Now</h2>
          </div>
          <Link to="/shop" className="btn-secondary" style={{ fontSize: '11px' }}>View All</Link>
        </div>

        <TrendingGrid loading={trendingLoading} products={trending} onClick={(p) => navigate(`/product/${p.slug || p.id}`)} />
      </section>

      <FooterFull />
    </div>
  );
}

function TrendingGrid({ loading, products, onClick }) {
  if (loading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="bg-white">
            <div className="bg-black/5 animate-pulse" style={{ paddingTop: '125%' }} />
            <div className="p-4 space-y-2">
              <div className="h-3 w-1/3 bg-black/10 animate-pulse" />
              <div className="h-4 w-2/3 bg-black/10 animate-pulse" />
              <div className="h-6 w-1/4 bg-black/10 animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (products.length === 0) {
    return (
      <div className="text-center py-16 text-black/40">
        <p className="text-sm">No products available yet.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
      {products.map((product) => (
        <div
          key={product.id}
          className="group bg-white cursor-pointer relative overflow-hidden transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-[0_12px_24px_-12px_rgba(0,0,0,0.25)]"
          onClick={() => onClick(product)}
        >
          <div className="relative overflow-hidden" style={{ paddingTop: '125%' }}>
            {product.primaryImageUrl ? (
              <img
                src={product.primaryImageUrl}
                alt={product.name}
                className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
              />
            ) : (
              <div className="absolute inset-0 bg-black/5 flex items-center justify-center text-black/30 text-xs">
                No image
              </div>
            )}
            <div className="absolute inset-x-0 bottom-0 bg-black/85 text-white text-center py-3 translate-y-full group-hover:translate-y-0 transition-transform duration-300 ease-out">
              <span className="text-[11px] font-bold tracking-[0.2em] uppercase">View Details →</span>
            </div>
          </div>

          <div className="p-4">
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 mb-1">
              {product.categoryName}
            </p>
            <h3 className="text-sm font-bold uppercase tracking-wider mb-2 group-hover:text-[#E83354] transition-colors">{product.name}</h3>
            <span className="font-['Anton'] text-xl">{formatPrice(product.basePrice, product.currency)}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}
