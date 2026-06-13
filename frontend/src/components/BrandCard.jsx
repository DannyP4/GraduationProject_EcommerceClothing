import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import StarRating from './StarRating';
import { getBrandSummary } from '../services/productService';

export default function BrandCard({ brandId, brandName }) {
  const { t, i18n } = useTranslation();
  const [data, setData] = useState(null);

  useEffect(() => {
    if (!brandId) return undefined;
    let cancelled = false;
    setData(null);
    getBrandSummary(brandId)
      .then((d) => { if (!cancelled) setData(d); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [brandId, i18n.language]);

  if (!brandId) return null;

  const name = data?.name || brandName || t('brand.fallbackName');
  const initial = name.trim().charAt(0).toUpperCase() || '?';
  const rating = data?.averageRating;
  const reviewCount = data?.reviewCount ?? 0;
  const sold = data?.soldCount ?? 0;
  const products = data?.productCount ?? 0;

  return (
    <section className="mt-10 flex items-center gap-4">
      <div className="w-14 h-14 sm:w-16 sm:h-16 flex-shrink-0 rounded-full overflow-hidden bg-[#0A0A0A] flex items-center justify-center">
        {data?.logoUrl ? (
          <img src={data.logoUrl} alt={name} className="w-full h-full object-cover" />
        ) : (
          <span className="font-['Anton'] text-2xl sm:text-3xl text-white leading-none">{initial}</span>
        )}
      </div>

      <div className="min-w-0">
        <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-0.5">{t('brand.label')}</p>
        <div className="flex items-center gap-3 flex-wrap">
          <h3 className="font-['Anton'] text-xl sm:text-2xl uppercase tracking-tight truncate">{name}</h3>
          <Link
            to={`/shop?brand=${brandId}`}
            className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black px-3 py-1.5 hover:bg-black hover:text-white transition-colors whitespace-nowrap"
          >
            {t('brand.viewStore')}
          </Link>
        </div>
        {data?.description && (
          <p className="text-[12px] text-black/55 mt-1.5 leading-relaxed line-clamp-2">{data.description}</p>
        )}
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-1.5 text-[12px] text-black/60">
          <span><span className="font-bold text-black">{products.toLocaleString('vi-VN')}</span> {t('brand.products')}</span>
          <span className="text-black/20">·</span>
          <span><span className="font-bold text-black">{sold.toLocaleString('vi-VN')}</span> {t('brand.sold')}</span>
          <span className="text-black/20">·</span>
          {reviewCount > 0 ? (
            <span className="inline-flex items-center gap-1.5">
              <StarRating value={rating ?? 0} size={14} />
              <span className="font-bold text-black">{Number(rating ?? 0).toFixed(1)}</span>
              <span className="text-black/50">({reviewCount})</span>
            </span>
          ) : (
            <span className="text-black/40">{t('brand.noRatings')}</span>
          )}
        </div>
      </div>
    </section>
  );
}
