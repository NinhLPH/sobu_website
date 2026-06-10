import React, {useEffect, useState} from 'react';
import {CheckCircle2, Loader2, Mail, XCircle} from 'lucide-react';
import {Link, useSearchParams} from 'react-router-dom';
import {AuthService} from '../service/auth.service';

type VerifyStatus = 'idle' | 'loading' | 'success' | 'error';

export default function VerifyEmailPage() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const email = searchParams.get('email');
    const [status, setStatus] = useState<VerifyStatus>(token ? 'loading' : 'idle');
    const [message, setMessage] = useState<string | null>(null);
    const [resendSeconds, setResendSeconds] = useState(0);
    const [isResending, setIsResending] = useState(false);

    useEffect(() => {
        if (!token) {
            return;
        }

        let cancelled = false;

        const verify = async () => {
            try {
                await AuthService.activate(token);
                if (!cancelled) {
                    setStatus('success');
                    setMessage('Email da duoc xac thuc. Ban co the dang nhap ngay.');
                }
            } catch (err: any) {
                if (!cancelled) {
                    setStatus('error');
                    setMessage(err?.response?.data?.message || err?.message || 'Lien ket xac thuc khong hop le hoac da het han.');
                }
            }
        };

        verify();

        return () => {
            cancelled = true;
        };
    }, [token]);

    useEffect(() => {
        if (resendSeconds <= 0) {
            return;
        }

        const timer = window.setTimeout(() => {
            setResendSeconds((current) => Math.max(current - 1, 0));
        }, 1000);

        return () => window.clearTimeout(timer);
    }, [resendSeconds]);

    const handleResend = async () => {
        if (!email || resendSeconds > 0 || isResending) {
            return;
        }

        setIsResending(true);
        setMessage(null);

        try {
            await AuthService.resendActivationEmail(email);
            setResendSeconds(60);
            setMessage('Email xac thuc moi da duoc gui. Vui long kiem tra hop thu cua ban.');
        } catch (err: any) {
            setMessage(err?.response?.data?.message || err?.message || 'Khong the gui lai email xac thuc luc nay.');
        } finally {
            setIsResending(false);
        }
    };

    const icon = status === 'loading'
        ? <Loader2 className="w-10 h-10 animate-spin"/>
        : status === 'success'
            ? <CheckCircle2 className="w-10 h-10"/>
            : status === 'error'
                ? <XCircle className="w-10 h-10"/>
                : <Mail className="w-10 h-10"/>;

    return (
        <div className="min-h-screen pt-24 pb-12 flex items-center justify-center bg-surface px-4">
            <div className="w-full max-w-md bg-surface-container-lowest rounded-3xl shadow-xl border border-surface-container/60 p-8 text-center">
                <div className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center mb-5 ${
                    status === 'error' ? 'bg-error/10 text-error' : 'bg-primary-container/20 text-primary'
                }`}>
                    {icon}
                </div>

                <h1 className="text-xl font-black text-on-surface mb-3">
                    {status === 'loading'
                        ? 'Dang xac thuc email...'
                        : status === 'success'
                            ? 'Xac thuc thanh cong'
                            : status === 'error'
                                ? 'Xac thuc that bai'
                                : 'Kiem tra email cua ban'}
                </h1>

                <p className="text-sm font-semibold text-outline leading-6">
                    {message || (
                        email
                            ? <>Chung toi da gui email xac thuc den <span className="text-on-surface">{email}</span>. Vui long mo email va bam lien ket xac thuc.</>
                            : 'Vui long mo email dang ky va bam lien ket xac thuc de kich hoat tai khoan.'
                    )}
                </p>

                <Link
                    to="/login"
                    className="mt-6 inline-flex items-center justify-center px-6 py-3 bg-primary text-white rounded-2xl text-xs font-black uppercase shadow-md"
                >
                    Ve trang dang nhap
                </Link>

                {status === 'idle' && email && (
                    <button
                        type="button"
                        onClick={handleResend}
                        disabled={isResending || resendSeconds > 0}
                        className="mt-3 w-full inline-flex items-center justify-center gap-2 px-6 py-3 bg-surface-container text-on-surface rounded-2xl text-xs font-black uppercase disabled:opacity-60"
                    >
                        {isResending && <Loader2 className="w-4 h-4 animate-spin"/>}
                        {resendSeconds > 0 ? `Gui lai sau ${resendSeconds}s` : 'Gui lai email'}
                    </button>
                )}
            </div>
        </div>
    );
}
