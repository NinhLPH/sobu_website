export const ProductCard = ({ image, title, price, category, tag }) => (
  <div className="group bg-white rounded-lg p-6 transition-all hover:shadow-ambient flex flex-col h-full">
    <div className="relative aspect-square mb-6 overflow-hidden rounded-[1.5rem] bg-surface-container-low">
      <img src={image} alt={title} className="w-full h-full object-cover group-hover:scale-110 transition-duration-500" />
      {tag && <span className="absolute top-4 right-4 bg-tertiary-container text-tertiary text-[10px] font-bold px-3 py-1 rounded-full uppercase tracking-blueprint">{tag}</span>}
    </div>
    <p className="text-[10px] font-bold text-outline uppercase tracking-blueprint mb-2">{category}</p>
    <h3 className="text-lg font-bold text-on-surface mb-4 leading-tight flex-grow">{title}</h3>
    <div className="flex justify-between items-center">
      <span className="text-xl font-black text-primary">{price}đ</span>
      <button className="p-3 bg-surface-container rounded-full text-primary hover:bg-primary hover:text-white transition-colors">
        🛒
      </button>
    </div>
  </div>
);