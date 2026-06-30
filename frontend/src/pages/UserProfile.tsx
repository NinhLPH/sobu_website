import {Award, ChevronRight, Mail, Phone, UserRound} from 'lucide-react';
import {Link} from 'react-router-dom';
import {useAuthStore} from '../store/useAuthStore';

const emptyValue = 'Chưa cập nhật';

export default function UserProfile() {
    const user = useAuthStore((state) => state.user);

    const profileItems = [
        {
            label: 'Tên',
            value: user?.fullName || emptyValue,
            icon: UserRound
        },
        {
            label: 'Email',
            value: user?.email || emptyValue,
            icon: Mail
        },
        {
            label: 'Số điện thoại',
            value: user?.phone || emptyValue,
            icon: Phone
        },
        {
            label: 'Hạng thành viên',
            value: 'Triển khai trong tương lai',
            icon: Award
        }
    ];

    return (
        <main className="w-full min-w-0 bg-surface px-4 pb-24 pt-28 sm:px-6 sm:pt-32">
            <nav className="mb-6 flex items-center gap-2 text-xs font-bold text-on-surface-variant">
                <Link to="/" className="transition-colors hover:text-primary">Trang chủ</Link>
                <ChevronRight className="h-3.5 w-3.5"/>
                <span className="text-primary">Hồ sơ cá nhân</span>
            </nav>

            <section className="mx-auto max-w-4xl">
                <div className="mb-8">
                    <span className="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
                        Tài khoản SOBU
                    </span>
                    <h1 className="mt-3 text-3xl font-black uppercase tracking-tight text-on-surface">
                        Hồ sơ cá nhân
                    </h1>
                    <p className="mt-2 text-xs font-bold text-outline">
                        Thông tin cơ bản đang được dùng cho đơn hàng, yêu cầu dịch vụ và quyền lợi thành viên.
                    </p>
                </div>

                <div className="overflow-hidden rounded-2xl border border-outline-variant/30 bg-surface-container-lowest shadow-sm">
                    <div className="flex flex-col gap-4 border-b border-outline-variant/20 p-5 sm:flex-row sm:items-center sm:justify-between">
                        <div className="flex min-w-0 items-center gap-4">
                            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-primary text-lg font-black uppercase text-on-primary shadow-md shadow-primary/20">
                                {user?.fullName?.charAt(0) || 'U'}
                            </div>
                            <div className="min-w-0">
                                <h2 className="truncate text-lg font-black text-on-surface">
                                    {user?.fullName || emptyValue}
                                </h2>
                                <p className="mt-1 truncate text-xs font-semibold text-outline">
                                    {user?.email || emptyValue}
                                </p>
                            </div>
                        </div>
                        <span className="inline-flex w-fit rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
                            {user?.role?.name === 'ADMIN' ? 'Quản trị viên' : 'Thành viên'}
                        </span>
                    </div>

                    <div className="grid grid-cols-1 divide-y divide-outline-variant/20 sm:grid-cols-2 sm:divide-x sm:divide-y-0">
                        {profileItems.map((item) => {
                            const Icon = item.icon;

                            return (
                                <div key={item.label} className="flex items-start gap-4 p-5">
                                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-surface-container text-primary">
                                        <Icon className="h-5 w-5"/>
                                    </div>
                                    <div className="min-w-0">
                                        <p className="text-[10px] font-black uppercase tracking-widest text-outline">
                                            {item.label}
                                        </p>
                                        <p className="mt-1 break-words text-sm font-black text-on-surface">
                                            {item.value}
                                        </p>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            </section>
        </main>
    );
}
