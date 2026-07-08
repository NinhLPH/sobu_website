import {FormEvent, useEffect, useState} from 'react';
import {Award, ChevronRight, Mail, Phone, UserRound} from 'lucide-react';
import {Link} from 'react-router-dom';
import {useAuthStore} from '../store/useAuthStore';

const emptyValue = 'Chưa cập nhật';

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

export default function UserProfile() {
    const user = useAuthStore((state) => state.user);
    const updatePhoneAction = useAuthStore((state) => state.updatePhoneAction);
    const [phoneValue, setPhoneValue] = useState(user?.phone || '');
    const [isSavingPhone, setIsSavingPhone] = useState(false);
    const [phoneError, setPhoneError] = useState<string | null>(null);
    const [phoneSuccess, setPhoneSuccess] = useState<string | null>(null);

    useEffect(() => {
        setPhoneValue(user?.phone || '');
    }, [user?.phone]);

    const currentPhone = user?.phone?.trim() || '';
    const normalizedPhone = phoneValue.trim();
    const canSavePhone = normalizedPhone.length > 0 && normalizedPhone !== currentPhone && !isSavingPhone;

    const handlePhoneSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!canSavePhone) {
            return;
        }

        setIsSavingPhone(true);
        setPhoneError(null);
        setPhoneSuccess(null);

        try {
            await updatePhoneAction(normalizedPhone);
            setPhoneSuccess('Số điện thoại đã được cập nhật.');
        } catch (error) {
            setPhoneError(getErrorMessage(error, 'Không thể cập nhật số điện thoại. Vui lòng thử lại.'));
        } finally {
            setIsSavingPhone(false);
        }
    };

    const profileItems = [
        {
            key: 'name',
            label: 'Tên',
            value: user?.fullName || emptyValue,
            icon: UserRound
        },
        {
            key: 'email',
            label: 'Email',
            value: user?.email || emptyValue,
            icon: Mail
        },
        {
            key: 'phone',
            label: 'Số điện thoại',
            value: user?.phone || emptyValue,
            icon: Phone
        },
        {
            key: 'membership',
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
                                <div key={item.key} className="flex items-start gap-4 p-5">
                                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-surface-container text-primary">
                                        <Icon className="h-5 w-5"/>
                                    </div>
                                    <div className="min-w-0 flex-1">
                                        <p className="text-[10px] font-black uppercase tracking-widest text-outline">
                                            {item.label}
                                        </p>
                                        <p className="mt-1 break-words text-sm font-black text-on-surface">
                                            {item.value}
                                        </p>

                                        {item.key === 'phone' && (
                                            <form className="mt-4 space-y-3" onSubmit={handlePhoneSubmit}>
                                                <label htmlFor="profile-phone" className="block text-[10px] font-black uppercase tracking-widest text-primary">
                                                    Thay đổi số điện thoại
                                                </label>
                                                <div className="flex flex-col gap-2 sm:flex-row">
                                                    <input
                                                        id="profile-phone"
                                                        type="tel"
                                                        value={phoneValue}
                                                        onChange={(event) => {
                                                            setPhoneValue(event.target.value);
                                                            setPhoneError(null);
                                                            setPhoneSuccess(null);
                                                        }}
                                                        className="min-w-0 flex-1 rounded-xl border border-outline-variant/50 bg-surface-container-lowest px-3 py-2 text-sm font-semibold text-on-surface outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                                                        placeholder="Nhập số điện thoại mới"
                                                    />
                                                    <button
                                                        type="submit"
                                                        disabled={!canSavePhone}
                                                        className="rounded-xl bg-primary px-4 py-2 text-xs font-black uppercase tracking-widest text-on-primary transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:bg-outline-variant disabled:text-outline"
                                                    >
                                                        {isSavingPhone ? 'Đang lưu...' : 'Lưu'}
                                                    </button>
                                                </div>
                                                {phoneError && (
                                                    <p className="text-xs font-bold text-error">{phoneError}</p>
                                                )}
                                                {phoneSuccess && (
                                                    <p className="text-xs font-bold text-primary">{phoneSuccess}</p>
                                                )}
                                            </form>
                                        )}
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
