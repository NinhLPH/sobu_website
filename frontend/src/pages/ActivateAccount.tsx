import {useEffect, useRef, useState} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';
import {AlertCircle, CheckCircle2, Loader2} from 'lucide-react';
import {useAuthStore} from '../store/useAuthStore';

type ActivationStatus = 'loading' | 'success' | 'error';

export default function ActivateAccount() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const {activateAccountAction, clearError} = useAuthStore();
    const [status, setStatus] = useState<ActivationStatus>('loading');
    const [message, setMessage] = useState('Đang xác thực tài khoản...');
    const [redirectSeconds, setRedirectSeconds] = useState(3);
    const activationRequest = useRef<{
        token: string;
        promise: Promise<string>;
    } | null>(null);

    useEffect(() => {
        clearError();

        if (!token) {
            setStatus('error');
            setMessage('Liên kết kích hoạt không có token hợp lệ.');
            return;
        }

        if (activationRequest.current?.token !== token) {
            activationRequest.current = {
                token,
                promise: activateAccountAction(token)
            };
        }
        const request = activationRequest.current.promise;

        let cancelled = false;
        const activate = async () => {
            setStatus('loading');
            try {
                const successMessage = await request;
                if (!cancelled) {
                    setStatus('success');
                    setMessage(successMessage);
                }
            } catch (error: any) {
                if (!cancelled) {
                    setStatus('error');
                    setMessage(
                        error?.response?.data?.message ||
                        error?.message ||
                        'Token kích hoạt không hợp lệ hoặc đã hết hạn.'
                    );
                }
            }
        };

        activate();
        return () => {
            cancelled = true;
        };
    }, [token, activateAccountAction, clearError]);

    useEffect(() => {
        if (status !== 'success') {
            return;
        }

        const countdownTimer = window.setInterval(() => {
            setRedirectSeconds((current) => Math.max(0, current - 1));
        }, 1000);
        const redirectTimer = window.setTimeout(() => {
            navigate('/login', {replace: true});
        }, 3000);

        return () => {
            window.clearInterval(countdownTimer);
            window.clearTimeout(redirectTimer);
        };
    }, [status, navigate]);

    const icon = status === 'loading'
        ? <Loader2 className="h-10 w-10 animate-spin"/>
        : status === 'success'
            ? <CheckCircle2 className="h-10 w-10"/>
            : <AlertCircle className="h-10 w-10"/>;

    return (
        <main className="min-h-screen bg-surface px-4 pb-12 pt-24 flex items-center justify-center">
            <section
                className="w-full max-w-md overflow-hidden rounded-3xl border border-surface-container/60 bg-surface-container-lowest shadow-xl">
                <div
                    className="h-28 bg-gradient-to-r from-primary to-primary-container flex items-center justify-center">
                    <span
                        className="rounded-2xl border border-white/20 bg-white/10 px-6 py-1.5 text-3xl font-black tracking-widest text-white">
                        SOBU
                    </span>
                </div>

                <div className="p-8 text-center">
                    <div className={`mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full ${
                        status === 'error'
                            ? 'bg-error/10 text-error'
                            : status === 'success'
                                ? 'bg-green-500/10 text-green-600'
                                : 'bg-primary/10 text-primary'
                    }`}>
                        {icon}
                    </div>

                    <h1 className="mb-2 text-lg font-black text-on-surface">
                        {status === 'loading'
                            ? 'Đang kích hoạt tài khoản'
                            : status === 'success'
                                ? 'Kích hoạt thành công'
                                : 'Kích hoạt thất bại'}
                    </h1>
                    <p className={`text-sm font-semibold leading-6 ${
                        status === 'error' ? 'text-error' : 'text-outline'
                    }`}>
                        {message}
                    </p>

                    {status === 'success' && (
                        <>
                            <p className="mt-3 text-xs font-semibold text-outline">
                                Tự động chuyển tới đăng nhập sau {redirectSeconds} giây.
                            </p>
                            <Link
                                to="/login"
                                className="mt-6 inline-flex rounded-xl bg-gradient-to-br from-primary to-primary-container px-6 py-3 text-xs font-black uppercase text-white shadow-md"
                            >
                                Về trang đăng nhập
                            </Link>
                        </>
                    )}

                    {status === 'error' && (
                        <div className="mt-6 flex flex-col gap-3">
                            <Link
                                to="/verify-email"
                                className="rounded-xl bg-primary px-6 py-3 text-xs font-black uppercase text-white"
                            >
                                Gửi lại email kích hoạt
                            </Link>
                            <Link to="/login" className="text-xs font-bold text-primary hover:underline">
                                Quay lại đăng nhập
                            </Link>
                        </div>
                    )}
                </div>
            </section>
        </main>
    );
}
