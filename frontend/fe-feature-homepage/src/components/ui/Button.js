export const PrimaryButton = ({ children, className }) => (
  <button className={`px-8 py-4 rounded-full bg-gradient-to-br from-[#00618e] to-[#4bbafe] text-white font-bold shadow-lg hover:scale-105 transition-all text-sm tracking-wide ${className}`}>
    {children}
  </button>
);

export const TertiaryButton = ({ children }) => (
  <button className="text-primary font-bold border-b-2 border-primary/20 hover:border-primary transition-all pb-1 text-sm">
    {children}
  </button>
);