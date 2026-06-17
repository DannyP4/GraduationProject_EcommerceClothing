import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatPrice } from '../lib/format';
import WishlistButton from './WishlistButton';

export default function ProductCard({ product, hideWishlist = false }) {
  const { t } = useTranslation();
  const onSale = product.salePrice != null;
  return (
    <Link
      to={`/product/${product.slug || product.id}`}
      className="group block bg-white relative overflow-hidden transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-[0_12px_24px_-12px_rgba(0,0,0,0.25)]"
    >
      <div className="relative overflow-hidden" style={{ paddingTop: '125%' }}>
        {product.discountPercent != null && (
          <span className="absolute top-2 left-2 z-10 bg-[#E83354] text-white text-[10px] font-bold tracking-widest uppercase px-2 py-1">
            -{product.discountPercent}%
          </span>
        )}
        {!hideWishlist && <WishlistButton productId={product.id} productSlug={product.slug} />}
        {product.primaryImageUrl ? (
          <img
            src={product.primaryImageUrl}
            alt={product.name}
            className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
        ) : (
          <div className="absolute inset-0 bg-black/5 flex items-center justify-center text-black/30 text-xs">
            {t('common.noImage')}
          </div>
        )}
      </div>

      <div className="p-3">
        <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 mb-1 truncate">
          {product.categoryName}
        </p>
        <h3 className="text-[13px] font-bold uppercase tracking-wide mb-1.5 line-clamp-2 min-h-[2.25rem] group-hover:text-[#E83354] transition-colors">
          {product.name}
        </h3>
        <div className="flex items-baseline gap-2">
          {onSale ? (
            <>
              <span className="font-['Anton'] text-lg text-[#E83354]">{formatPrice(product.salePrice, product.currency)}</span>
              <span className="text-[11px] text-black/40 line-through">{formatPrice(product.basePrice, product.currency)}</span>
            </>
          ) : (
            <span className="font-['Anton'] text-lg">{formatPrice(product.basePrice, product.currency)}</span>
          )}
        </div>
      </div>
    </Link>
  );
}
