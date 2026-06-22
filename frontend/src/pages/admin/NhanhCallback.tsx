import {useEffect, useState} from 'react';
import {useSearchParams, useNavigate} from 'react-router-dom';
import {Loader2, CheckCircle2, AlertCircle, ArrowLeft} from 'lucide-react';
import {AdminSyncService} from '../../service/sync.service';

export default function AdminNhanhCallback() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
    const [errorMsg, setErrorMsg] = useState<string | null>(null);

    useEffect(() => {
        const accessCode = searchParams.get('accessCode');

        if (!accessCode) {
            setStatus('error');
            setErrorMsg('Không tìm thấy mã ủy quyền (accessCode) từ Nhanh.vn.');
            return;
        }

        const handleOAuthCallback = async () => {
            try {
                const response = await AdminSyncService.handleNhanhCallback(accessCode);

                if (response === 'Connected' || response?.includes('Connected')) {
                    setStatus('success');
                    if (window.opener) {
                        window.opener.postMessage('NHANH_AUTH_SUCCESS', '*');
                    }
                    window.close();
                } else {
                    throw new Error(response || 'Xác thực OAuth thất bại từ hệ thống.');
                }
            } catch (err: any) {
                console.error('Error handling Nhanh OAuth Callback:', err);
                setStatus('error');
                setErrorMsg(
                    err?.response?.data?.message ||
                    err?.message ||
                    'Có lỗi xảy ra trong quá trình xác thực OAuth với Nhanh.vn.'
                );
            }
        };

        handleOAuthCallback();
    }, [searchParams, navigate]);

    return (
        <div className="min-h-[70vh] flex items-center justify-center p-4">
            <div
                className="bg-white rounded-3xl p-8 border border-outline-variant/30 shadow-lg max-w-md w-full text-center space-y-6">

                {status === 'loading' && (
                    <div className="space-y-4 py-8 flex flex-col items-center">
                        <Loader2 className="w-12 h-12 text-primary animate-spin"/>
                        <h2 className="text-lg font-black text-on-surface uppercase tracking-wide">Xác thực kết nối
                            ERP</h2>
                        <p className="text-xs text-outline font-semibold leading-relaxed">
                            Hệ thống đang tiến hành bắt tay OAuth và xác thực mã ủy quyền với Nhanh.vn. Vui lòng giữ
                            nguyên trang web...
                        </p>
                    </div>
                )}

                {status === 'success' && (
                    <div className="space-y-4 py-8 flex flex-col items-center">
                        <div
                            className="w-16 h-16 bg-green-50 rounded-full flex items-center justify-center border border-green-200">
                            <CheckCircle2 className="w-10 h-10 text-green-600 animate-bounce"/>
                        </div>
                        <h2 className="text-lg font-black text-green-700 uppercase tracking-wide">Kết nối thành
                            công!</h2>
                        <p className="text-xs text-outline font-semibold leading-relaxed">
                            Đã kết nối thành công tài khoản ERP Nhanh.vn. Bạn đang được chuyển hướng quay lại Trung tâm
                            Đồng bộ...
                        </p>
                        <Loader2 className="w-5 h-5 text-green-600 animate-spin mt-2"/>
                    </div>
                )}

                {status === 'error' && (
                    <div className="space-y-6 py-4 flex flex-col items-center">
                        <div
                            className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center border border-red-200">
                            <AlertCircle className="w-10 h-10 text-error animate-pulse"/>
                        </div>
                        <h2 className="text-lg font-black text-error uppercase tracking-wide">Kết nối thất bại</h2>
                        <p className="text-xs text-outline font-semibold leading-relaxed">
                            {errorMsg}
                        </p>

                        <div className="pt-4 border-t border-surface-container w-full">
                            <button
                                onClick={() => navigate('/admin/sync?oauth=error&error=' + encodeURIComponent(errorMsg || ''))}
                                className="w-full py-3 bg-surface-container text-on-surface hover:bg-surface-variant rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-sm hover:scale-102 transition-transform cursor-pointer flex items-center justify-center gap-1.5"
                            >
                                <ArrowLeft className="w-4 h-4"/>
                                <span>Quay lại trang đồng bộ</span>
                            </button>
                        </div>
                    </div>
                )}

            </div>
        </div>
    );
}
