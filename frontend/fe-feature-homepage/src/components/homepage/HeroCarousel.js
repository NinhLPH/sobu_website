import { useState } from "react";
import { PrimaryButton } from "../ui/Button";
import pic1 from "../../assets/IMG_9827.JPG";

export const HeroCarousel = () => {
  const [activeTab, setActiveTab] = useState(0);
  
  const slides = [
    {
      id: 1,
      title: "The Precision Masterpiece.",
      desc: "Khám phá bộ sưu tập mô hình đạt độ chính xác đến từng milimet, nơi kỹ thuật gặp gỡ nghệ thuật.",
      image: pic1,
      tag: "MỚI RA MẮT"
    },
  ];

  return (
    <section className="container mx-auto px-8 py-10">      
      <div className="relative h-[800px] w-full overflow-hidden rounded-[2rem] shadow-ambient">
        <div className="absolute inset-0 bg-cover bg-center transition-all duration-1000 ease-in-out"
          style={{ backgroundImage: `url(${slides[activeTab].image})` }}>
          <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/20 to-transparent" />
        </div>
        <div className="relative z-10 h-full flex flex-col justify-end items-start pb-20 px-20 max-w-3xl">
          <span className="bg-primary-container text-white text-[11px] font-bold px-5 py-1.5 rounded-full w-fit mb-8 tracking-blueprint uppercase">
            {slides[activeTab].tag}
          </span>
          <h1 className="text-7xl font-black text-white mb-8 tracking-tightest leading-[0.9]">
            {slides[activeTab].title}
          </h1>
          <p className="text-xl text-white/90 mb-12 leading-relaxed font-light max-w-xl">
            {slides[activeTab].desc}
          </p>

          <PrimaryButton className="w-fit shadow-lg">Khám phá ngay</PrimaryButton>
        </div>
        <div className="absolute bottom-12 right-20 flex gap-4 items-center">
          {slides.map((_, index) => (
            <button
              key={index}
              onClick={() => setActiveTab(index)}
              className={`h-1.5 rounded-full transition-all duration-500 ${
                activeTab === index 
                ? "w-16 bg-primary-container shadow-[0_0_15px_rgba(75,186,254,0.6)]" 
                : "w-8 bg-white/40 hover:bg-white/60"
              }`}
            />
          ))}
        </div>
      </div>
    </section>
  );
};