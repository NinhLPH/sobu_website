import {Shield, Star, Crown, Check, ShoppingBag, Award, Zap, ArrowRight} from 'lucide-react';
import {Link} from 'react-router-dom';

export default function Membership() {
    //Quy trình tích lũy
    const steps = [
        {
            icon: ShoppingBag,
            title: '1. Tích lũy chi tiêu',
            desc: 'Mọi hóa đơn mua mô hình sẵn có hoặc chi phí sử dụng dịch vụ Custom, Pre-order đều được hệ thống tự động ghi nhận qua tài khoản.'
        },
        {
            icon: Award,
            title: '2. Thăng hạng tự động',
            desc: 'Hệ thống POS đồng bộ thời gian thực với nhanh.vn sẽ lập tức kích hoạt cấp thẻ mới ngay khi bạn chạm mốc chi tiêu tích lũy.'
        },
        {
            icon: Zap,
            title: '3. Hưởng trọn đặc quyền',
            desc: 'Hệ thống tự động áp dụng mức chiết khấu trực tiếp vào giỏ hàng, ưu tiên xếp lịch độ xe và nhận đặc quyền Private Sale.'
        }
    ];

    const tiers = [
        {
            name: 'Silver Member',
            icon: Shield,
            price: 'Tích lũy từ 5.000.000 đ',
            cardBg: 'bg-surface-container-lowest border border-surface-container-high shadow-[0_10px_30px_rgba(0,0,0,0.02)]',
            iconStyle: 'text-slate-400 bg-slate-50 border border-slate-200/60',
            badgeStyle: 'bg-slate-100 text-slate-600',
            textColor: 'text-on-surface',
            perkCheckColor: 'text-slate-500',
            btnStyle: 'bg-surface-container-high text-on-surface hover:bg-surface-container-highest',
            perks: ['Tích điểm 1% mỗi đơn hàng', 'Theo dõi tiến độ đơn hàng Custom/Pre-order', 'Hỗ trợ kỹ thuật tiêu chuẩn tại Workshop']
        },
        {
            name: 'Gold Collector',
            icon: Star,
            price: 'Tích lũy từ 20.000.000 đ',
            cardBg: 'bg-gradient-to-b from-amber-50/40 to-surface-container-lowest border-2 border-amber-400 shadow-[0_20px_50px_-10px_rgba(245,158,11,0.15)] md:-mt-6 relative z-10',
            iconStyle: 'text-amber-500 bg-amber-50 border border-amber-200 shadow-sm shadow-amber-500/10',
            badgeStyle: 'bg-amber-500 text-white tracking-widest uppercase text-[9px] font-black',
            textColor: 'text-on-surface',
            perkCheckColor: 'text-amber-500',
            btnStyle: 'bg-gradient-to-br from-amber-500 to-amber-600 text-white shadow-md shadow-amber-500/20 hover:scale-[1.02]',
            perks: ['Tích điểm 3% mỗi đơn hàng', 'Miễn phí vận chuyển toàn quốc cho mọi đơn', 'Ưu tiên xếp lịch duyệt đơn Custom phôi độ', 'Nhận quà tặng sinh nhật bản Collector giới hạn']
        },
        {
            name: 'Platinum Elite',
            icon: Crown,
            price: 'Tích lũy từ 50.000.000 đ',
            cardBg: 'bg-gradient-to-b from-[#1C2127] to-[#0E1114] text-white shadow-[0_30px_60px_-15px_rgba(0,0,0,0.35)] border border-neutral-800',
            iconStyle: 'text-zinc-300 bg-white/5 border border-white/10 shadow-inner',
            badgeStyle: 'bg-white/10 text-zinc-300 border border-white/10',
            textColor: 'text-white',
            perkCheckColor: 'text-zinc-400',
            btnStyle: 'bg-surface-container-lowest text-on-surface hover:bg-surface-container-low shadow-lg shadow-white/5 hover:scale-[1.02]',
            perks: ['Tích điểm 5% mỗi đơn hàng', 'Mức giá thẩm định Thu mua (Trade-in) tốt nhất', 'Vé mời tham gia Private Sale săn mô hình hiếm', 'Trợ lý cá nhân hỗ trợ 1-1 qua Zalo 24/7', 'Miễn phí 100% công độ hệ thống LED cơ bản']
        }
    ];

    return (
        <main className="min-h-screen w-full min-w-0 bg-surface px-4 pb-16 pt-24 sm:px-6">
            <div className="mx-auto flex w-full flex-col gap-12">
                <div className="text-center">
                    <span
                        className="text-[10px] font-black uppercase tracking-widest text-primary bg-primary-container/20 px-3 py-1 rounded-full">
                        Hệ thống ưu đãi đặc quyền
                    </span>
                    <h1 className="text-3xl md:text-4xl font-black uppercase tracking-tight text-on-surface mt-3 mb-2">
                        SOBU COLLECTOR CLUB
                    </h1>
                    <p className="text-xs font-medium text-on-surface-variant max-w-xl mx-auto leading-relaxed">
                        Trở thành hội viên chính thức để kích hoạt công cụ theo dõi tiến độ Custom, nhận hạn mức cọc
                        Pre-order và tận hưởng mức chiết khấu trọn đời.
                    </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-stretch pt-4">
                    {tiers.map((tier, idx) => (
                        <div
                            key={idx}
                            className={`rounded-3xl p-6 transition-all duration-500 flex flex-col justify-between ${tier.cardBg}`}
                        >
                            {/* Header Card */}
                            <div>
                                <div className="flex items-center justify-between mb-4">
                                    <div
                                        className={`w-10 h-10 rounded-xl flex items-center justify-center ${tier.iconStyle}`}>
                                        <tier.icon className="w-5 h-5"/>
                                    </div>
                                    {tier.name.includes('Gold') ? (
                                        <div
                                            className={`px-2.5 py-0.5 rounded-md text-[9px] font-black ${tier.badgeStyle}`}>
                                            Phổ biến nhất
                                        </div>
                                    ) : (
                                        <div
                                            className={`px-2 py-0.5 rounded-md text-[9px] font-bold ${tier.badgeStyle}`}>
                                            Tier {idx + 1}
                                        </div>
                                    )}
                                </div>

                                <h2 className={`text-xl font-black uppercase tracking-tight mb-0.5 ${tier.textColor}`}>
                                    {tier.name}
                                </h2>
                                <p className={`text-[11px] font-black uppercase tracking-wider pb-4 mb-4 border-b border-surface-container-high/40 opacity-80 ${tier.textColor}`}>
                                    {tier.price}
                                </p>

                                <ul className="space-y-3 mb-6">
                                    {tier.perks.map((perk, i) => (
                                        <li key={i}
                                            className={`flex items-start gap-2.5 font-semibold text-xs leading-tight ${tier.textColor}`}>
                                            <Check className={`w-4 h-4 flex-shrink-0 mt-0.5 ${tier.perkCheckColor}`}/>
                                            <span>{perk}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                            <button
                                className={`w-full py-2.5 rounded-xl font-black uppercase tracking-wider text-xs transition-all duration-300 ${tier.btnStyle}`}>
                                Kích hoạt quyền lợi
                            </button>
                        </div>
                    ))}
                </div>

                <div className="mt-10 bg-surface-container-low rounded-3xl p-6 border border-surface-container/60">
                    <div
                        className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-surface-container-high pb-4 mb-6">
                        <div>
                            <h3 className="text-sm font-black uppercase tracking-tight text-on-surface">
                                Cơ chế tích lũy điểm thành viên
                            </h3>
                            <p className="text-[11px] font-medium text-on-surface-variant">
                                Quy trình hoàn toàn tự động dựa trên mã hệ thống của SOBU Ecosystem.
                            </p>
                        </div>
                        <Link to="/products"
                              className="text-xs font-bold text-primary flex items-center gap-1 hover:underline">
                            Đến cửa hàng <ArrowRight className="w-3.5 h-3.5"/>
                        </Link>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        {steps.map((step, index) => (
                            <div key={index} className="flex gap-3.5">
                                <div
                                    className="w-8 h-8 rounded-lg bg-surface-container-highest flex items-center justify-center text-primary shrink-0">
                                    <step.icon className="w-4 h-4"/>
                                </div>
                                <div className="space-y-1">
                                    <h4 className="text-xs font-black text-on-surface">{step.title}</h4>
                                    <p className="text-[11px] text-on-surface-variant font-medium leading-relaxed text-justify">
                                        {step.desc}
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

            </div>
        </main>
    );
}
