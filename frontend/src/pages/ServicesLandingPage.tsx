import {Link} from 'react-router-dom';
import {
    ArrowRight,
    BadgeCheck,
    Check,
    ClipboardList,
    Clock3,
    HandCoins,
    MessageSquareText,
    PackageOpen,
    Paintbrush,
    ShieldCheck,
    Sparkles
} from 'lucide-react';

const services = [
    {
        icon: Paintbrush,
        label: 'Custom',
        title: 'Custom mô hình theo dấu ấn riêng',
        description: 'Biến ý tưởng của bạn thành một phiên bản độc bản với giải pháp sơn, độ LED, làm base và hoàn thiện chi tiết.',
        features: ['Tư vấn concept theo nhu cầu', 'Báo giá theo từng hạng mục', 'Cập nhật tiến độ trong quá trình thực hiện'],
        accentClass: 'bg-primary text-on-primary'
    },
    {
        icon: PackageOpen,
        label: 'Pre-order',
        title: 'Đặt trước sản phẩm bạn đang tìm kiếm',
        description: 'Gửi thông tin sản phẩm, phiên bản hoặc đường dẫn tham khảo. SOBU sẽ hỗ trợ tìm nguồn và đề xuất phương án đặt trước phù hợp.',
        features: ['Hỗ trợ tìm đúng phiên bản', 'Thông tin chi phí rõ ràng', 'Theo dõi trạng thái yêu cầu thuận tiện'],
        accentClass: 'bg-secondary text-on-secondary'
    },
    {
        icon: HandCoins,
        label: 'Thu mua',
        title: 'Định giá bộ sưu tập nhanh chóng',
        description: 'Cung cấp hình ảnh và tình trạng sản phẩm để nhận tư vấn định giá khi bạn muốn bán lại hoặc nâng cấp bộ sưu tập.',
        features: ['Thẩm định dựa trên tình trạng thực tế', 'Trao đổi mức giá minh bạch', 'Quy trình tiếp nhận gọn nhẹ'],
        accentClass: 'bg-tertiary text-on-tertiary'
    }
];

const processSteps = [
    {
        icon: ClipboardList,
        number: '01',
        title: 'Gửi yêu cầu',
        description: 'Chọn loại dịch vụ và cung cấp thông tin, hình ảnh cần thiết.'
    },
    {
        icon: MessageSquareText,
        number: '02',
        title: 'Trao đổi chi tiết',
        description: 'Đội ngũ SOBU liên hệ để làm rõ nhu cầu và phương án thực hiện.'
    },
    {
        icon: BadgeCheck,
        number: '03',
        title: 'Xác nhận báo giá',
        description: 'Bạn xem lại phạm vi công việc, chi phí và thời gian dự kiến.'
    },
    {
        icon: Sparkles,
        number: '04',
        title: 'Hoàn tất dịch vụ',
        description: 'Yêu cầu được xử lý và cập nhật theo từng giai đoạn đến khi bàn giao.'
    }
];

export default function ServicesLandingPage() {
    return (
        <main className="min-h-screen overflow-hidden bg-surface pt-24 text-on-surface">
            <section className="relative px-6 pb-20 pt-14 md:pb-28 md:pt-20">
                <div className="absolute -left-32 top-8 h-80 w-80 rounded-full bg-primary-container/20 blur-3xl"/>
                <div className="absolute -right-32 bottom-0 h-96 w-96 rounded-full bg-tertiary-container/20 blur-3xl"/>

                <div className="relative mx-auto grid w-full items-center gap-8 lg:grid-cols-12 lg:gap-12">
                    <div className="lg:col-span-7">
                        <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/10 px-4 py-2 text-xs font-black uppercase tracking-widest text-primary">
                            <Sparkles className="h-4 w-4"/>
                            Dịch vụ dành cho collector
                        </div>

                        <h1 className="max-w-4xl text-3xl font-black leading-tight tracking-tight text-on-surface sm:text-5xl lg:text-6xl">
                            Biến ý tưởng sưu tầm thành
                            <span className="block text-primary">hiện thực cùng SOBU.</span>
                        </h1>

                        <p className="mt-6 max-w-2xl text-base font-medium leading-8 text-on-surface-variant md:text-lg">
                            Từ custom mô hình, tìm sản phẩm pre-order đến thu mua bộ sưu tập, SOBU đồng hành cùng bạn
                            bằng một quy trình rõ ràng và chuyên nghiệp.
                        </p>

                        <div className="mt-9 flex flex-col gap-3 sm:flex-row">
                            <Link
                                to="/requests/new"
                                className="inline-flex items-center justify-center gap-2 rounded-full bg-primary px-7 py-3.5 text-sm font-black text-on-primary shadow-lg shadow-primary/20 transition-all hover:-translate-y-0.5 hover:bg-secondary"
                            >
                                Tạo yêu cầu dịch vụ
                                <ArrowRight className="h-4 w-4"/>
                            </Link>
                            <a
                                href="#service-list"
                                className="inline-flex items-center justify-center rounded-full border border-outline-variant/60 bg-surface-container-lowest px-7 py-3.5 text-sm font-black text-on-surface transition-colors hover:border-primary/40 hover:text-primary"
                            >
                                Khám phá dịch vụ
                            </a>
                        </div>
                    </div>

                    <div className="relative lg:col-span-5">
                        <div className="rounded-[2rem] border border-surface-container-high bg-surface-container-lowest p-6 shadow-xl md:p-8">
                            <div className="flex items-start justify-between gap-4 border-b border-surface-container pb-6">
                                <div>
                                    <p className="text-xs font-black uppercase tracking-widest text-primary">SOBU Service</p>
                                    <h2 className="mt-2 text-2xl font-black text-on-surface">Một yêu cầu, trọn quy trình</h2>
                                </div>
                                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary text-on-primary">
                                    <ClipboardList className="h-6 w-6"/>
                                </div>
                            </div>

                            <div className="space-y-4 py-6">
                                {['Tiếp nhận đúng nhu cầu', 'Tư vấn và báo giá minh bạch', 'Theo dõi yêu cầu tập trung'].map((item) => (
                                    <div key={item} className="flex items-center gap-3">
                                        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                                            <Check className="h-4 w-4" strokeWidth={3}/>
                                        </span>
                                        <span className="text-sm font-bold text-on-surface">{item}</span>
                                    </div>
                                ))}
                            </div>

                            <div className="rounded-2xl bg-surface-container-low p-5">
                                <div className="flex items-center gap-3 text-primary">
                                    <Clock3 className="h-5 w-5"/>
                                    <span className="text-xs font-black uppercase tracking-wider">Phản hồi nhanh chóng</span>
                                </div>
                                <p className="mt-2 text-sm font-medium leading-6 text-on-surface-variant">
                                    Thông tin đầy đủ giúp đội ngũ SOBU tư vấn và xử lý yêu cầu của bạn hiệu quả hơn.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            <section id="service-list" className="scroll-mt-28 bg-surface-container-low px-6 py-20 md:py-24">
                <div className="mx-auto w-full">
                    <div className="mx-auto mb-12 max-w-3xl text-center">
                        <p className="text-xs font-black uppercase tracking-[0.25em] text-primary">Dịch vụ của chúng tôi</p>
                        <h2 className="mt-3 whitespace-nowrap text-base font-black tracking-tight text-on-surface min-[360px]:text-lg sm:text-3xl md:text-5xl">
                            Chọn giải pháp phù hợp với bạn
                        </h2>
                        <p className="mt-4 font-medium leading-7 text-on-surface-variant">
                            Mỗi nhu cầu đều bắt đầu bằng một yêu cầu đơn giản. SOBU sẽ cùng bạn làm rõ phần còn lại.
                        </p>
                    </div>

                    <div className="grid gap-6 lg:grid-cols-3">
                        {services.map((service) => {
                            const Icon = service.icon;

                            return (
                                <article
                                    key={service.label}
                                    className="group flex h-full flex-col rounded-[2rem] border border-surface-container-high bg-surface-container-lowest p-7 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-xl md:p-8"
                                >
                                    <div className={`flex h-14 w-14 items-center justify-center rounded-2xl ${service.accentClass}`}>
                                        <Icon className="h-7 w-7"/>
                                    </div>

                                    <p className="mt-7 text-xs font-black uppercase tracking-[0.2em] text-primary">
                                        {service.label}
                                    </p>
                                    <h3 className="mt-2 text-2xl font-black leading-tight text-on-surface">
                                        {service.title}
                                    </h3>
                                    <p className="mt-4 text-sm font-medium leading-7 text-on-surface-variant">
                                        {service.description}
                                    </p>

                                    <ul className="mt-6 space-y-3">
                                        {service.features.map((feature) => (
                                            <li key={feature} className="flex items-start gap-3 text-sm font-bold text-on-surface">
                                                <Check className="mt-0.5 h-4 w-4 shrink-0 text-primary" strokeWidth={3}/>
                                                {feature}
                                            </li>
                                        ))}
                                    </ul>

                                    <Link
                                        to="/requests/new"
                                        className="mt-8 inline-flex items-center justify-between rounded-2xl bg-surface-container px-5 py-4 text-sm font-black text-on-surface transition-colors group-hover:bg-primary group-hover:text-on-primary"
                                    >
                                        Gửi yêu cầu
                                        <ArrowRight className="h-4 w-4"/>
                                    </Link>
                                </article>
                            );
                        })}
                    </div>
                </div>
            </section>

            <section className="px-6 py-20 md:py-24">
                <div className="mx-auto w-full">
                    <div className="grid gap-12 lg:grid-cols-12">
                        <div className="lg:col-span-4">
                            <p className="text-xs font-black uppercase tracking-[0.25em] text-primary">Cách hoạt động</p>
                            <h2 className="mt-3 text-3xl font-black tracking-tight text-on-surface md:text-4xl">
                                Một quy trình dễ hiểu từ đầu đến cuối
                            </h2>
                            <p className="mt-5 font-medium leading-7 text-on-surface-variant">
                                Bạn tập trung mô tả điều mình cần. SOBU sẽ hỗ trợ từng bước để yêu cầu được xử lý đúng
                                hướng.
                            </p>
                        </div>

                        <div className="grid gap-4 sm:grid-cols-2 lg:col-span-8">
                            {processSteps.map((step) => {
                                const Icon = step.icon;

                                return (
                                    <div
                                        key={step.number}
                                        className="rounded-3xl border border-surface-container-high bg-surface-container-lowest p-6"
                                    >
                                        <div className="flex items-center justify-between">
                                            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                                                <Icon className="h-5 w-5"/>
                                            </div>
                                            <span className="text-3xl font-black text-surface-container-highest">{step.number}</span>
                                        </div>
                                        <h3 className="mt-5 text-lg font-black text-on-surface">{step.title}</h3>
                                        <p className="mt-2 text-sm font-medium leading-6 text-on-surface-variant">
                                            {step.description}
                                        </p>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>
            </section>

            <section className="px-6 pb-24">
                <div className="relative mx-auto w-full overflow-hidden rounded-[2rem] bg-inverse-surface px-6 py-10 text-inverse-on-surface sm:rounded-[2.5rem] md:px-12 lg:px-16 lg:py-16">
                    <div className="absolute -right-20 -top-32 h-80 w-80 rounded-full bg-primary/30 blur-3xl"/>
                    <div className="relative grid items-center gap-10 lg:grid-cols-12">
                        <div className="lg:col-span-7">
                            <div className="mb-5 flex items-center gap-3 text-inverse-primary">
                                <ShieldCheck className="h-6 w-6"/>
                                <span className="text-xs font-black uppercase tracking-[0.2em]">Bắt đầu cùng SOBU</span>
                            </div>
                            <h2 className="text-3xl font-black leading-tight text-on-primary md:text-5xl">
                                Bạn đã có ý tưởng cho bộ sưu tập của mình?
                            </h2>
                            <p className="mt-4 max-w-2xl font-medium leading-7 text-inverse-on-surface">
                                Hãy gửi thông tin cho chúng tôi. Đội ngũ SOBU sẽ đồng hành để biến nhu cầu của bạn
                                thành một kế hoạch rõ ràng.
                            </p>
                        </div>

                        <div className="flex lg:col-span-5 lg:justify-end">
                            <Link
                                to="/requests/new"
                                className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-inverse-primary px-8 py-4 text-sm font-black text-on-primary-container shadow-lg transition-transform hover:-translate-y-0.5 sm:w-auto"
                            >
                                Tạo yêu cầu ngay
                                <ArrowRight className="h-4 w-4"/>
                            </Link>
                        </div>
                    </div>
                </div>
            </section>
        </main>
    );
}
