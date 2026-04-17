import pic1 from "../../assets/IMG_9853.JPG";
import pic2 from "../../assets/IMG_9863.JPG"
import pic3 from "../../assets/IMG_9877.JPG"
import pic4 from "../../assets/IMG_9918.JPG"
// sau này sẽ thay bằng truyền props ảnh, content

export const HotCategories = () => (
  <section className="py-24 bg-white">
    <div className="container mx-auto px-8">
      <h2 className="text-4xl font-bold mb-12 tracking-tightest">Danh mục HOT</h2>
      <div className="grid grid-cols-12 gap-6 h-[600px]">
        <div className="col-span-12 md:col-span-7 bg-surface-container rounded-lg overflow-hidden relative group">
          <img src={pic1} className="w-full h-full object-cover group-hover:scale-105 transition-transform" />
          <div className="absolute bottom-8 left-8 text-white">
            <h3 className="text-3xl font-bold mb-2">Gunpla Master</h3>
            <p className="opacity-80">Bộ sưu tập mô hình lắp ráp cao cấp</p>
          </div>
        </div>
        <div className="col-span-12 md:col-span-5 grid grid-rows-2 gap-6">
          <div className="bg-[#1a1a1a] rounded-lg overflow-hidden relative">
             <img src="" className="w-full h-full object-cover" />
             <div className="absolute inset-0 bg-black/20 p-8 flex flex-col justify-end text-white">
                <h3 className="text-2xl font-bold">Designer Toys</h3>
             </div>
          </div>
          <div className="grid grid-cols-2 gap-6">
            <div className="bg-surface-container-low rounded-lg overflow-hidden">
                <img src={pic3} className="w-full h-full object-cover" />
            </div>
            <div className="bg-black rounded-lg overflow-hidden">
                <img src={pic3} className="w-full h-full object-cover" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
);