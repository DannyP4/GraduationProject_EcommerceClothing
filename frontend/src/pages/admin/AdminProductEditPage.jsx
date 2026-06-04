import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useToast } from '../../components/Toast';
import { goBack } from '../../lib/historyBack';
import * as productSvc from '../../services/adminProductService';
import * as brandSvc from '../../services/adminBrandService';
import * as catSvc from '../../services/adminCategoryService';
import ProductBasicsSection from '../../components/admin/ProductBasicsSection';
import VariantTableEditor from '../../components/admin/VariantTableEditor';
import ProductImageUploader from '../../components/admin/ProductImageUploader';

const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;

const EMPTY = {
  slug: '',
  name: '',
  description: '',
  brandId: '',
  categoryId: '',
  gender: 'UNISEX',
  basePrice: '',
  isActive: true,
};

export default function AdminProductEditPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { id } = useParams();
  const location = useLocation();
  const backTo = location.state?.backTo || '/admin/products';
  const [searchParams, setSearchParams] = useSearchParams();
  const isCreate = !id;
  const viewMode = !isCreate && searchParams.get('view') === '1';

  const [values, setValues] = useState(EMPTY);
  const [product, setProduct] = useState(null);
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(!isCreate);
  const [saving, setSaving] = useState(false);
  const [basicsError, setBasicsError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([brandSvc.listBrands(), catSvc.listCategories()])
      .then(([b, c]) => { if (!cancelled) { setBrands(b ?? []); setCategories(c ?? []); } })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const loadProduct = useCallback(async () => {
    if (isCreate) return;
    setLoading(true);
    try {
      const data = await productSvc.getProduct(id);
      setProduct(data);
      setValues({
        slug: data.slug,
        name: data.name,
        description: data.description ?? '',
        brandId: data.brand?.id ?? '',
        categoryId: data.category?.id ?? '',
        gender: data.gender,
        basePrice: data.basePrice,
        isActive: data.isActive,
      });
    } catch (err) {
      setBasicsError(err.message || 'Could not load product.');
    } finally {
      setLoading(false);
    }
  }, [id, isCreate]);

  useEffect(() => { loadProduct(); }, [loadProduct]);

  const handleSave = async () => {
    setBasicsError(null);
    if (!values.name.trim()) { setBasicsError('Name is required.'); return; }
    if (!values.brandId) { setBasicsError('Brand is required.'); return; }
    if (!values.categoryId) { setBasicsError('Category is required.'); return; }
    if (values.basePrice === '' || Number(values.basePrice) < 0) { setBasicsError('Base price must be 0 or greater.'); return; }
    if (isCreate && !SLUG_PATTERN.test(values.slug)) { setBasicsError('Slug must be lowercase kebab-case.'); return; }

    setSaving(true);
    try {
      if (isCreate) {
        const created = await productSvc.createProduct({
          slug: values.slug,
          name: values.name.trim(),
          description: values.description.trim() || null,
          brandId: Number(values.brandId),
          categoryId: Number(values.categoryId),
          gender: values.gender,
          basePrice: Number(values.basePrice),
          isActive: values.isActive,
        });
        toast.success(`Created "${created.name}" — add variants and images below`);
        navigate(`/admin/products/${created.id}`, { replace: true });
      } else {
        const updated = await productSvc.updateProduct(id, {
          name: values.name.trim(),
          description: values.description.trim() || '',
          brandId: Number(values.brandId),
          categoryId: Number(values.categoryId),
          gender: values.gender,
          basePrice: Number(values.basePrice),
          isActive: values.isActive,
        });
        setProduct(updated);
        toast.success(`Saved "${updated.name}"`);
      }
    } catch (err) {
      setBasicsError(err.message || 'Save failed.');
      toast.error(err.message || 'Save failed.');
    } finally {
      setSaving(false);
    }
  };

  const exitViewMode = () => {
    const next = new URLSearchParams(searchParams);
    next.delete('view');
    setSearchParams(next, { replace: true });
  };

  if (!isCreate && loading) {
    return <div className="px-6 py-10 text-center text-sm text-black/40">Loading product...</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <button
          type="button"
          onClick={() => goBack(navigate, location, '/admin/products')}
          className="inline-flex items-center gap-2 text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-3 py-2 hover:bg-black hover:text-white transition-colors"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          Back to products
        </button>
      </div>

      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">
              {isCreate ? 'New product' : product?.name || 'Edit product'}
            </h1>
            {viewMode && (
              <span className="text-[10px] font-bold tracking-[0.15em] uppercase bg-black text-white px-2 py-1">View only</span>
            )}
          </div>
          {!isCreate && product && (
            <p className="text-xs text-black/50 mt-1 font-mono">{product.slug}</p>
          )}
        </div>
        {viewMode ? (
          <button
            type="button"
            onClick={exitViewMode}
            className="bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase px-5 py-3 hover:bg-[#E83354] transition-colors"
          >
            Switch to edit
          </button>
        ) : (
          <button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase px-5 py-3 hover:bg-[#E83354] transition-colors disabled:opacity-40"
          >
            {saving ? 'Saving...' : isCreate ? 'Create product' : 'Save changes'}
          </button>
        )}
      </div>

      <ProductBasicsSection
        isCreate={isCreate}
        readOnly={viewMode}
        values={values}
        setValues={setValues}
        brands={brands}
        categories={categories}
        error={basicsError}
      />

      {!isCreate && product && (
        <>
          <VariantTableEditor
            productId={Number(id)}
            variants={product.variants ?? []}
            onChange={loadProduct}
            readOnly={viewMode}
          />
          <ProductImageUploader
            productId={Number(id)}
            productSlug={product.slug}
            images={product.images ?? []}
            onChange={loadProduct}
            readOnly={viewMode}
          />
        </>
      )}

      {isCreate && (
        <div className="bg-black/[0.02] border border-dashed border-black/20 p-6 text-center text-xs text-black/50">
          Create the product first, then variants and images become editable.
        </div>
      )}
    </div>
  );
}
