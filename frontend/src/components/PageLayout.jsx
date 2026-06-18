import AnnouncementBar from './AnnouncementBar';
import NavbarGlass from './NavbarGlass';
import FooterFull from './FooterFull';

export default function PageLayout({ children }) {
  return (
    <div className="min-h-screen bg-[#E8E8E8] flex flex-col">
      <AnnouncementBar />
      <NavbarGlass />
      <main className="flex-1">{children}</main>
      <FooterFull />
    </div>
  );
}

export function PageHero({ kicker, title, subtitle, image }) {
  return (
    <section className="relative bg-[#0A0A0A] text-white overflow-hidden">
      {image && (
        <img
          src={image}
          alt=""
          className="absolute inset-0 w-full h-full object-cover opacity-40"
          loading="eager"
        />
      )}
      <div className="relative max-w-[1440px] mx-auto px-6 py-24 md:py-32">
        {kicker && (
          <p className="text-[11px] font-bold tracking-[0.3em] uppercase text-[#E83354] mb-4">{kicker}</p>
        )}
        <h1 className="font-['Anton'] text-5xl md:text-7xl uppercase tracking-tight leading-[0.95]">{title}</h1>
        {subtitle && (
          <p className="mt-5 max-w-xl text-sm md:text-base text-white/60 leading-relaxed">{subtitle}</p>
        )}
      </div>
    </section>
  );
}

export function Section({ title, kicker, children, className = '' }) {
  return (
    <section className={`max-w-[1440px] mx-auto px-6 py-16 md:py-20 ${className}`}>
      {kicker && (
        <p className="text-[11px] font-bold tracking-[0.25em] uppercase text-black/40 mb-2">{kicker}</p>
      )}
      {title && (
        <h2 className="font-['Anton'] text-3xl md:text-4xl uppercase tracking-tight mb-8">{title}</h2>
      )}
      {children}
    </section>
  );
}
