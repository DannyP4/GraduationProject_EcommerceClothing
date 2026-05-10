import { useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { products } from '../data/products';
import { useAuth } from '../context/AuthContext';

export default function HomePage() {
  const bgTextRef = useRef(null);
  const ribbon1Ref = useRef(null);
  const ribbon2Ref = useRef(null);
  const modelRef = useRef(null);
  const heroRef = useRef(null);
  const navigate = useNavigate();
  const { status, user } = useAuth();
  const firstName = (user?.fullName || user?.email || '').split(' ')[0];

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

  const trending = products.slice(0, 4);

  return (
    <div style={{ background: 'var(--bg-color)', minHeight: '100vh' }}>
      {/* NAV */}
      <nav className="home-nav">
        <Link to="/" className="logo">UNIFORM</Link>
        <ul className="home-nav-links">
          <li><Link to="/shop">Shop</Link></li>
          <li><a href="#">Collections</a></li>
          <li><a href="#">Lookbook</a></li>
          <li><a href="#">About</a></li>
        </ul>
        <div className="home-nav-actions">
          {status === 'authenticated' ? (
            <Link to="/account/profile" title={user?.email}>{firstName || 'Account'}</Link>
          ) : status === 'loading' ? null : (
            <Link to="/login">Login</Link>
          )}
          <Link to="/cart">Cart</Link>
        </div>
      </nav>

      {/* HERO */}
      <section className="hero" ref={heroRef}>
        {/* Background big text */}
        <div className="hero-bg-text" ref={bgTextRef}>CAMPUS LEGEND</div>

        {/* Floating shapes */}
        <div className="hero-shape hero-shape-1" />
        <div className="hero-shape hero-shape-2" />
        <div className="hero-ribbon hero-ribbon-1" ref={ribbon1Ref} />
        <div className="hero-ribbon hero-ribbon-2" ref={ribbon2Ref} />
        <div className="hero-ribbon hero-ribbon-3" />

        {/* Hero model image */}
        <img
          ref={modelRef}
          className="hero-model"
          src="https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=900&q=90"
          alt="Model"
        />

        {/* Content */}
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
            { tag: 'Campus Essentials', title: 'Back to School', h: '600px' },
            { tag: 'Streetwear', title: 'Urban Uniform', h: '290px' },
            { tag: 'Seasonal', title: 'Winter Layers', h: '290px' },
            { tag: 'Minimal', title: 'Clean Cuts', h: '290px' },
            { tag: 'Retro', title: 'Varsity Club', h: '290px' },
          ].map((lb, i) => (
            <div key={i} className="lookbook-card" style={i === 0 ? { gridRow: 'span 2', height: '600px' } : { height: '290px' }}>
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

        <div className="trending-grid">
          {trending.map((p) => (
            <div
              key={p.id}
              className="product-card-home"
              onClick={() => navigate(`/product/${p.id}`)}
            >
              <div className="product-img-wrap">
                <img src={p.images[0]} alt={p.name} />
                {p.badge && <span className="product-badge">{p.badge}</span>}
              </div>
              <div className="product-info-home">
                <p className="product-cat-home">{p.category}</p>
                <h3 className="product-name-home">{p.name}</h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span className="product-price-home">${p.price}</span>
                  {p.originalPrice && (
                    <span style={{ fontSize: '13px', color: 'rgba(10,10,10,0.4)', textDecoration: 'line-through' }}>
                      ${p.originalPrice}
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* SIMPLE FOOTER */}
      <footer className="footer-simple">
        <span className="logo">UNIFORM</span>
        <span className="copy">© 2024 UNIFORM. All rights reserved.</span>
      </footer>
    </div>
  );
}
