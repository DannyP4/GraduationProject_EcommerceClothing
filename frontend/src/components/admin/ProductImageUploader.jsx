import { useRef, useState } from 'react';
import { uploadImageEndToEnd } from '../../services/adminUploadService';
import * as imageSvc from '../../services/adminProductImageService';

const MAX_IMAGES = 8;
const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const MAX_FILE_BYTES = 5 * 1024 * 1024;

export default function ProductImageUploader({ productId, images, productSlug, onChange, readOnly = false }) {
  const inputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState({ done: 0, total: 0 });
  const [error, setError] = useState(null);
  const [dragOver, setDragOver] = useState(false);

  const remaining = MAX_IMAGES - images.length;

  const handleFiles = async (files) => {
    setError(null);
    const fileList = Array.from(files || []).filter(Boolean);
    if (fileList.length === 0) return;

    const accepted = [];
    for (const f of fileList) {
      if (!ACCEPTED_TYPES.includes(f.type)) {
        setError(`Unsupported file type: ${f.name}. Use JPG, PNG, WEBP, or GIF.`);
        return;
      }
      if (f.size > MAX_FILE_BYTES) {
        setError(`File too large: ${f.name}. Max 5MB per image.`);
        return;
      }
      accepted.push(f);
    }
    if (accepted.length > remaining) {
      setError(`Only ${remaining} more image slot${remaining === 1 ? '' : 's'} left (max ${MAX_IMAGES}).`);
      return;
    }

    setUploading(true);
    setProgress({ done: 0, total: accepted.length });
    try {
      let done = 0;
      for (const file of accepted) {
        const hint = productSlug ? `${productSlug}-${Date.now()}` : `product-${Date.now()}`;
        const uploaded = await uploadImageEndToEnd(file, { folder: 'uniform/products', filenameHint: hint });
        await imageSvc.createImage(productId, {
          url: uploaded.url,
          publicId: uploaded.publicId,
          altText: file.name.replace(/\.[^.]+$/, ''),
          isPrimary: false,
        });
        done++;
        setProgress({ done, total: accepted.length });
      }
      await onChange();
    } catch (err) {
      setError(err.message || 'Upload failed.');
    } finally {
      setUploading(false);
      setProgress({ done: 0, total: 0 });
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  const onDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    if (uploading) return;
    handleFiles(e.dataTransfer.files);
  };

  const setPrimary = async (image) => {
    setError(null);
    try {
      await imageSvc.updateImage(image.id, { isPrimary: true });
      await onChange();
    } catch (err) {
      setError(err.message || 'Could not update.');
    }
  };

  const deleteImage = async (image) => {
    if (!window.confirm('Delete this image? The Cloudinary asset will be removed too.')) return;
    setError(null);
    try {
      await imageSvc.deleteImage(image.id);
      await onChange();
    } catch (err) {
      setError(err.message || 'Could not delete.');
    }
  };

  return (
    <section className="bg-white border border-black/10 p-6 space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h2 className="font-['Anton'] text-2xl uppercase tracking-tight">Images</h2>
        <span className="text-xs text-black/50">{images.length} / {MAX_IMAGES}</span>
      </div>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-3 py-2 text-xs">{error}</div>
      )}

      {remaining > 0 && !readOnly && (
        <div
          onDragOver={(e) => { e.preventDefault(); if (!uploading) setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={onDrop}
          onClick={() => !uploading && inputRef.current?.click()}
          className={`border-2 border-dashed transition-colors px-6 py-10 text-center cursor-pointer ${
            dragOver ? 'border-black bg-black/[0.03]' : 'border-black/20 hover:border-black/40'
          } ${uploading ? 'opacity-50 cursor-wait' : ''}`}
        >
          <p className="font-['Anton'] text-xl uppercase tracking-tight mb-1">
            {uploading ? `Uploading ${progress.done} / ${progress.total}...` : 'Drop images or click to browse'}
          </p>
          <p className="text-xs text-black/50">JPG / PNG / WEBP / GIF up to 5MB each. {remaining} slot{remaining === 1 ? '' : 's'} left.</p>
          <input
            ref={inputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            multiple
            className="hidden"
            disabled={uploading}
            onChange={(e) => handleFiles(e.target.files)}
          />
        </div>
      )}

      {images.length === 0 ? (
        <p className="text-xs text-black/40 text-center py-6">
          {readOnly ? 'No images.' : 'No images yet. Drop the first one above.'}
        </p>
      ) : (
        <ul className="grid gap-3 grid-cols-2 md:grid-cols-4">
          {images.map((img) => (
            <li key={img.id} className="relative group border border-black/10">
              <img src={img.url} alt={img.altText ?? ''} className="w-full aspect-square object-cover" />
              {img.isPrimary && (
                <span className="absolute top-2 left-2 text-[9px] font-bold tracking-[0.15em] uppercase bg-emerald-600 text-white px-2 py-0.5">
                  Primary
                </span>
              )}
              {!readOnly && (
                <div className="absolute inset-x-0 bottom-0 bg-black/70 text-white text-[10px] flex divide-x divide-white/20 opacity-0 group-hover:opacity-100 transition-opacity">
                  {!img.isPrimary && (
                    <button
                      type="button"
                      onClick={() => setPrimary(img)}
                      className="flex-1 font-bold tracking-[0.15em] uppercase py-2 hover:bg-emerald-600"
                    >
                      Set primary
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => deleteImage(img)}
                    className="flex-1 font-bold tracking-[0.15em] uppercase py-2 hover:bg-[#E83354]"
                  >
                    Delete
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
