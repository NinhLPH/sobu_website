import React from 'react';

const Sidebar = () => {
  return (
    <aside className="w-full md:w-64 flex flex-col gap-10 bg-white rounded-lg p-8">
      
      <div>
        <h3 className="text-sm font-bold uppercase tracking-blueprint mb-6 text-on-surface">Danh mục</h3>
        <div className="flex flex-col gap-4">
          {['LEGO Technic', 'Action Figures', 'Model Kits 1:24', 'Die-cast Cars'].map((cat) => (
            <label key={cat} className="flex items-center gap-3 cursor-pointer group">
              <input type="checkbox" className="w-5 h-5 bg-surface-container-low rounded-md border-none checked:bg-primary transition-all cursor-pointer" />
              <span className="text-sm text-on-surface/80 group-hover:text-primary transition-colors">{cat}</span>
            </label>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-sm font-bold uppercase tracking-blueprint mb-6 text-on-surface">Giá bán</h3>
        <input type="range" className="w-full h-1.5 bg-surface rounded-full appearance-none cursor-pointer accent-primary" />
        <div className="flex justify-between mt-4 text-[10px] font-bold text-outline uppercase tracking-blueprint">
          <span className="bg-surface p-2 rounded-lg">100k</span>
          <span className="bg-surface-container p-2 rounded-lg">5.000k</span>
        </div>
      </div>

      <div>
        <h3 className="text-sm font-bold uppercase tracking-blueprint mb-6 text-on-surface">Độ tuổi</h3>
        <div className="grid grid-cols-2 gap-2">
          {['14+', '18+', '8+', 'Tất cả'].map((age) => (
            <button key={age} className="py-2 rounded-md bg-surface-container-low text-sm font-bold text-on-surface hover:bg-tertiary-container hover:text-tertiary transition-all">
              {age}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6 p-6 rounded-lg bg-primary-container/10 backdrop-blur-[20px] relative overflow-hidden border border-white/20">
        <p className="text-[10px] font-bold text-primary uppercase tracking-blueprint mb-2">Thành viên mới</p>
        <h4 className="text-lg font-bold text-on-surface leading-tight mb-4">Miễn phí vận chuyển cho đơn từ 500k</h4>
        <button className="text-primary font-bold text-xs flex items-center gap-2">Đăng ký ngay →</button>
      </div>
    </aside>
  );
};

export default Sidebar;