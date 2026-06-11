import { Link } from 'react-router-dom';

export default function AuthCard({ eyebrow, title, children }) {
  return (
    <div className="min-h-screen bg-[#E8E8E8] flex items-center justify-center px-6 py-16">
      <div className="max-w-md w-full bg-white border border-black/10 p-8">
        <Link to="/" className="font-['Anton'] text-2xl tracking-widest text-black block mb-7">
          VESTA
        </Link>
        {eyebrow && (
          <p className="text-[10px] font-bold tracking-[0.3em] uppercase text-[#E83354] mb-2">{eyebrow}</p>
        )}
        <h1 className="font-['Anton'] text-4xl uppercase tracking-tight mb-5">{title}</h1>
        {children}
      </div>
    </div>
  );
}
