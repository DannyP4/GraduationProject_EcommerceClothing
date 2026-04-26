import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import { products } from '../data/products';
import { useCart } from '../context/CartContext';

const COLLECTIONS = ['All', 'Tops', 'Bottoms', 'Outerwear', 'Accessories'];
const SIZES = ['XS', 'S', 'M', 'L', 'XL', 'XXL'];
const SORTS = ['Featured', 'Price: Low to High', 'Price: High to Low', 'Newest'];

export default function ShopPage() {
  const [activeCollection, setActiveCollection] = useState('All');
  const [activeSizes, setActiveSizes] = useState([]);
  const [sort, setSort] = useState('Featured');
  const navigate = useNavigate();
  const { addItem } = useCart();

  const toggleSize = (s) =>
    setActiveSizes((prev) => (prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s]));

  let filtered = products.filter(
    (p) => activeCollection === 'All' || p.category === activeCollection
  );

  if (activeSizes.length > 0) {
    filtered = filtered.filter((p) => p.sizes.some((s) => activeSizes.includes(s)));
  }

  if (sort === 'Price: Low to High') filtered = [...filtered].sort((a, b) => a.price - b.price);
  if (sort === 'Price: High to Low') filtered = [...filtered].sort((a, b) => b.price - a.price);

  const handleAddToCart = (e, product) => {
    e.stopPropagation();
    addItem({
      id: product.id,
      name: product.name,
      price: product.price,
      size: product.sizes[1] || product.sizes[0],
      color: product.colors[0],
      image: product.images[0],
    });
  };

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />

      {/* Page header */}
      <div className="bg-[#0A0A0A] text-white py-12 px-6">
        <div className="max-w-[1440px] mx-auto">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-white/40 mb-2">
            SS 2024 · {filtered.length} items
          </p>
          <h1 className="font-['Anton'] text-5xl md:text-7xl tracking-tight uppercase">
            Shop All
          </h1>
        </div>
      </div>

      <div className="max-w-[1440px] mx-auto px-6 py-10 flex gap-8">
        {/* SIDEBAR */}
        <aside className="w-56 flex-shrink-0">
          <div className="sticky top-20 space-y-8">
            {/* Collections */}
            <div>
              <h3 className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-4">Collections</h3>
              <ul className="space-y-1">
                {COLLECTIONS.map((c) => (
                  <li key={c}>
                    <button
                      onClick={() => setActiveCollection(c)}
                      className={`w-full text-left text-sm py-1.5 px-2 transition-all font-medium ${
                        activeCollection === c
                          ? 'bg-black text-white'
                          : 'text-black/60 hover:text-black hover:bg-black/5'
                      }`}
                    >
                      {c}
                    </button>
                  </li>
                ))}
              </ul>
            </div>

            {/* Sizes */}
            <div>
              <h3 className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-4">Size</h3>
              <div className="grid grid-cols-3 gap-1.5">
                {SIZES.map((s) => (
                  <button
                    key={s}
                    onClick={() => toggleSize(s)}
                    className={`text-[11px] font-bold py-2 border transition-all ${
                      activeSizes.includes(s)
                        ? 'bg-black text-white border-black'
                        : 'bg-white text-black border-black/15 hover:border-black'
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>

            {/* Sort */}
            <div>
              <h3 className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-4">Sort By</h3>
              <select
                value={sort}
                onChange={(e) => setSort(e.target.value)}
                className="w-full border border-black/15 bg-white text-sm px-3 py-2.5 focus:outline-none focus:border-black appearance-none cursor-pointer"
              >
                {SORTS.map((s) => <option key={s}>{s}</option>)}
              </select>
            </div>

            {/* Clear filters */}
            {(activeCollection !== 'All' || activeSizes.length > 0) && (
              <button
                onClick={() => { setActiveCollection('All'); setActiveSizes([]); }}
                className="text-[11px] font-bold tracking-[0.1em] uppercase text-[#E83354] hover:underline"
              >
                Clear Filters
              </button>
            )}
          </div>
        </aside>

        {/* PRODUCT GRID */}
        <main className="flex-1">
          {filtered.length === 0 ? (
            <div className="text-center py-24 text-black/40">
              <p className="text-xl font-bold mb-2">No products found</p>
              <p className="text-sm">Try adjusting your filters</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
              {filtered.map((product) => (
                <div
                  key={product.id}
                  className="group bg-white cursor-pointer relative overflow-hidden"
                  onClick={() => navigate(`/product/${product.id}`)}
                >
                  {/* Image */}
                  <div className="relative overflow-hidden" style={{ paddingTop: '125%' }}>
                    <img
                      src={product.images[0]}
                      alt={product.name}
                      className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                    />
                    {product.badge && (
                      <span className="absolute top-3 left-3 bg-[#E83354] text-white text-[9px] font-bold tracking-widest uppercase px-2 py-1">
                        {product.badge}
                      </span>
                    )}
                    {/* Hover: Add to Cart */}
                    <div className="absolute bottom-0 inset-x-0 translate-y-full group-hover:translate-y-0 transition-transform duration-300">
                      <button
                        onClick={(e) => handleAddToCart(e, product)}
                        className="w-full bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase py-3.5 hover:bg-[#E83354] transition-colors"
                      >
                        + Add to Cart
                      </button>
                    </div>
                  </div>

                  {/* Info */}
                  <div className="p-4">
                    <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 mb-1">
                      {product.category}
                    </p>
                    <h3 className="text-sm font-bold uppercase tracking-wider mb-2">{product.name}</h3>
                    <div className="flex items-center gap-2">
                      <span className="font-['Anton'] text-xl">${product.price}</span>
                      {product.originalPrice && (
                        <span className="text-sm text-black/35 line-through">${product.originalPrice}</span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Pagination */}
          <div className="flex justify-center items-center gap-2 mt-12">
            {[1, 2, 3, '...', 8].map((p, i) => (
              <button
                key={i}
                className={`w-9 h-9 text-sm font-bold transition-all ${
                  p === 1
                    ? 'bg-black text-white'
                    : 'bg-white text-black/60 border border-black/15 hover:border-black hover:text-black'
                }`}
              >
                {p}
              </button>
            ))}
          </div>
        </main>
      </div>

      <FooterFull />
    </div>
  );
}
