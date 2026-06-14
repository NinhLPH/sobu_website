import { FormEvent, useEffect, useState } from 'react';
import { CheckCircle2, Loader2, Mail, XCircle } from 'lucide-react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';

type Feedback = {
    type: 'success' | 'error';
    message: string;
} | null;

export default function VerifyEmailPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [email, setEmail] = useState(searchParams.get('email') || '');
    const [countdown, setCountdown] = useState(0);
    const [redirectSeconds, setRedirectSeconds] = useState(3);
    const [feedback, setFeedback] = useState<Feedback>(null);
    const {
        resendActivationAction,
        isLoading,
        error,
        clearError
    } = useAuthStore();

    useEffect(() => {
        clearError();
    }, [clearError]);

    useEffect(() => {
        if (countdown <= 0) {
            return;
        }
        const timer = window.setInterval(() => {
            setCountdown((current) => Math.max(0, current - 1));
        }, 1000);
        return () => window.clearInterval(timer);
    }, [countdown]);

    useEffect(() => {
        if (feedback?.type !== 'success') {
            return;
        }

        const countdownTimer = window.setInterval(() => {
            setRedirectSeconds((current) => Math.max(0, current - 1));
        }, 1000);
        const redirectTimer = window.setTimeout(() => {
            navigate('/login', { replace: true });
        }, 3000);

        return () => {
            window.clearInterval(countdownTimer);
            window.clearTimeout(redirectTimer);
        };
    }, [feedback?.type, navigate]);

    const handleResend = async (event: FormEvent) => {
        event.preventDefault();
        if (!email.trim() || countdown > 0 || isLoading) {
            return;
        }

        clearError();
        setFeedback(null);
        try {
            const successMessage = await resendActivationAction(email.trim());
            setCountdown(60);
            setFeedback({ type: 'success', message: successMessage });
        } catch (requestError: any) {
            if (requestError?.response?.status === 429) {
                setCountdown(60);
            }
            setFeedback({
                type: 'error',
                message:
                    requestError?.response?.data?.message ||
                    requestError?.message ||
                    'Không thể gửi lại email kích hoạt lúc này.'
            });
        }
    };

    const visibleError = feedback?.type === 'error'
        ? feedback.message
        : error;

    return (
        <main className="min-h-screen bg-surface px-4 pb-12 pt-24 flex items-center justify-center">
            <section className="w-full max-w-md rounded-3xl border border-surface-container/60 bg-surface-container-lowest p-8 text-center shadow-xl">
                <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                    <Mail className="h-9 w-9" />
                </div>

                <h1 className="text-xl font-black text-on-surface">Kiểm tra email của bạn</h1>
                <p className="mt-3 text-sm font-semibold leading-6 text-outline">
                    Nhập email đã đăng ký để nhận lại liên kết kích hoạt. Mỗi email chỉ có thể gửi lại một lần trong 60 giây.
                </p>

                {feedback?.type === 'success' && (
                    <div className="mt-5 flex items-start gap-2 rounded-xl border border-green-200 bg-green-50 p-3.5 text-left text-xs font-bold text-green-700">
                        <CheckCircle2 className="h-4 w-4 shrink-0" />
                        <span>
                            {feedback.message} Chuyển tới trang đăng nhập sau {redirectSeconds} giây.
                        </span>
                    </div>
                )}

                {visibleError && (
                    <div className="mt-5 flex items-start gap-2 rounded-xl border border-error/20 bg-error/10 p-3.5 text-left text-xs font-bold text-error">
                        <XCircle className="h-4 w-4 shrink-0" />
                        <span>{visibleError}</span>
                    </div>
                )}

                <form onSubmit={handleResend} className="mt-6 space-y-3">
                    <label className="block text-left text-xs font-bold uppercase text-outline">
                        Email đăng ký
                    </label>
                    <input
                        type="email"
                        value={email}
                        onChange={(event) => {
                            setEmail(event.target.value);
                            setFeedback(null);
                            clearError();
                        }}
                        disabled={isLoading}
                        placeholder="email@example.com"
                        className="w-full rounded-xl bg-surface-container px-4 py-3.5 text-sm font-semibold text-on-surface outline-none focus:ring-2 focus:ring-primary/30"
                        required
                    />
                    <button
                        type="submit"
                        disabled={isLoading || countdown > 0 || !email.trim()}
                        className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-br from-primary to-primary-container px-6 py-3.5 text-xs font-black uppercase text-white shadow-md disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
                        {countdown > 0 ? `Gửi lại sau ${countdown}s` : 'Gửi lại email kích hoạt'}
                    </button>
                </form>

                <Link
                    to="/login"
                    className="mt-5 inline-flex text-xs font-bold text-primary hover:underline"
                >
                    Quay lại trang đăng nhập
                </Link>
            </section>
        </main>
    );
}
