import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';

export default function NavbarGlass() {
  const { cartCount } = useCart();
  const navigate = useNavigate();

  return (
    <nav className="sticky top-0 z-50 backdrop-blur-md bg-white/80 border-b border-black/10">
      <div className="max-w-[1440px] mx-auto px-6 flex items-center justify-between h-14">
        {/* Logo */}
        <Link
          to="/"
          className="font-['Anton'] text-2xl tracking-widest text-black hover:text-[#E83354] transition-colors"
        >
          UNIFORM
        </Link>

        {/* Nav links */}
        <div className="hidden md:flex items-center gap-8">
          {['Shop', 'Collections', 'Lookbook', 'About'].map((item) => (
            <Link
              key={item}
              to={item === 'Shop' ? '/shop' : '#'}
              className="text-[11px] font-bold tracking-[0.12em] uppercase text-black/70 hover:text-black transition-colors"
            >
              {item}
            </Link>
          ))}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-4">
          {/* Search */}
          <button className="text-black/60 hover:text-black transition-colors" aria-label="Search">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
            </svg>
          </button>

          {/* Login */}
          <Link
            to="/login"
            className="text-[11px] font-bold tracking-[0.1em] uppercase text-black/70 hover:text-black transition-colors hidden sm:block"
          >
            Login
          </Link>

          {/* Cart */}
          <Link to="/cart" className="relative text-black/70 hover:text-black transition-colors" aria-label="Cart">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <path d="M16 10a4 4 0 0 1-8 0" />
            </svg>
            {cartCount > 0 && (
              <span className="absolute -top-2 -right-2 bg-[#E83354] text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
                {cartCount}
              </span>
            )}
          </Link>
        </div>
      </div>
    </nav>
  );
}
