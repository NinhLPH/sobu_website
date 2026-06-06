import {useState, useEffect} from 'react';
import {useSearchParams} from 'react-router-dom';
import {RefreshCw, CheckCircle2, AlertCircle, Loader2, Package, ListTree, Award} from 'lucide-react';
import {AdminSyncService} from '../../service/sync.service';
import {ToastService} from '../../service/toast.service';

export default function AdminSync() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [isConnecting, setIsConnecting] = useState(false);
    const [oauthStatus, setOauthStatus] = useState<'success' | 'error' | null>(null);
    const [oauthErrorMsg, setOauthErrorMsg] = useState<string | null>(null);

    const [isSyncing, setIsSyncing] = useState<{ [key: string]: boolean }>({
        products: false,
        categories: false,
        brands: false
    });

    const [syncMessages, setSyncMessages] = useState<{ [key: string]: string | null }>({
        products: null,
        categories: null,
        brands: null
    });

    const [syncErrors, setSyncErrors] = useState<{ [key: string]: string | null }>({
        products: null,
        categories: null,
        brands: null
    });

    useEffect(() => {
        const oauth = searchParams.get('oauth');
        const errMsg = searchParams.get('error');
        if (oauth === 'success') {
            setOauthStatus('success');
            setSearchParams({}, {replace: true});
        } else if (oauth === 'error') {
            setOauthStatus('error');
            setOauthErrorMsg(errMsg || 'Đã có lỗi xảy ra trong quá trình xác thực OAuth.');
            setSearchParams({}, {replace: true});
        }
    }, [searchParams, setSearchParams]);

    useEffect(() => {
        const handleMessage = (event: MessageEvent) => {
            if (event.data === 'NHANH_AUTH_SUCCESS') {
                setOauthStatus('success');
                console.log('NHANH_AUTH_SUCCESS');
                ToastService.success('Kết nối tài khoản ERP Nhanh.vn thành công!');
            }
        };
        window.addEventListener('message', handleMessage);
        return () => window.removeEventListener('message', handleMessage);
    }, []);

    const handleNhanhConnect = async () => {
        setIsConnecting(true);
        setOauthStatus(null);
        setOauthErrorMsg(null);
        try {
            const url = await AdminSyncService.getNhanhLoginUrl();
            if (url) {
                window.open(url, 'NhanhAuth', 'width=600,height=700,left=200,top=100');
            } else {
                throw new Error("Không lấy được URL đăng nhập từ hệ thống.");
            }
        } catch (err: any) {
            console.error("Lỗi lấy URL kết nối Nhanh.vn:", err);
            setOauthStatus('error');
            setOauthErrorMsg(err?.response?.data?.message || err?.message || 'Không thể lấy URL đăng nhập Nhanh.vn.');
        } finally {
            setIsConnecting(false);
        }
    };

    const handleSync = async (type: 'products' | 'categories' | 'brands') => {
        setIsSyncing(prev => ({...prev, [type]: true}));
        setSyncMessages(prev => ({...prev, [type]: null}));
        setSyncErrors(prev => ({...prev, [type]: null}));

        try {
            let res: { message: string };
            if (type === 'products') {
                res = await AdminSyncService.syncProducts();
            } else if (type === 'categories') {
                res = await AdminSyncService.syncCategories();
            } else {
                res = await AdminSyncService.syncBrands();
            }

            setSyncMessages(prev => ({...prev, [type]: res?.message || 'Đồng bộ dữ liệu thành công!'}));
        } catch (err: any) {
            const errorMsg = 'Có lỗi xảy ra trong quá trình đồng bộ ERP!';
            setSyncErrors(prev => ({...prev, [type]: errorMsg}));
        } finally {
            setIsSyncing(prev => ({...prev, [type]: false}));
        }
    };

    return (
        <div className="pt-6 space-y-6">
            <div>
                <h1 className="text-2xl font-black text-on-surface uppercase tracking-tight">Trung tâm đồng bộ ERP
                    Nhanh.vn</h1>
                <p className="text-xs text-outline font-bold mt-1">Đồng bộ dữ liệu thời gian thực từ phần mềm quản lý
                    bán hàng Nhanh.vn về cơ sở dữ liệu local.</p>
            </div>

            {/* OAuth Nhanh.vn Integration Section */}
            <div
                className="bg-white rounded-3xl p-6 border border-outline-variant/30 shadow-sm flex flex-col md:flex-row justify-between items-center gap-6">
                <div className="space-y-2 max-w-2xl font-bold">
                    <h2 className="text-sm font-black text-on-surface uppercase tracking-wide">Tích hợp kết nối ERP
                        Nhanh.vn</h2>
                    <p className="text-xs text-outline leading-relaxed font-semibold">
                        Kết nối hệ thống bán hàng SOBU với tài khoản ERP Nhanh.vn qua giao thức OAuth2. Hệ thống sẽ tự
                        động ủy quyền để đồng bộ sản phẩm, danh mục, thương hiệu và đơn hàng.
                    </p>
                </div>
                <button
                    onClick={handleNhanhConnect}
                    disabled={isConnecting}
                    className="w-full md:w-auto px-6 py-3.5 bg-primary text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center gap-2 cursor-pointer shrink-0"
                >
                    {isConnecting ? (
                        <>
                            <Loader2 className="w-4 h-4 animate-spin"/>
                            <span>Đang kết nối...</span>
                        </>
                    ) : (
                        <>
                            <RefreshCw className="w-4 h-4"/>
                            <span>Kết nối Nhanh.vn</span>
                        </>
                    )}
                </button>
            </div>

            {/* Connection Status Banner */}
            {oauthStatus === 'success' && (
                <div
                    className="p-4 bg-green-50 border border-green-200 rounded-2xl text-green-800 text-xs font-bold flex gap-3 items-center">
                    <CheckCircle2 className="w-5 h-5 text-green-600 shrink-0"/>
                    <span>Kết nối tài khoản ERP Nhanh.vn thành công! Hệ thống sẵn sàng đồng bộ.</span>
                </div>
            )}

            {oauthStatus === 'error' && (
                <div
                    className="p-4 bg-error/10 border border-error/20 rounded-2xl text-error text-xs font-bold flex gap-3 items-start">
                    <AlertCircle className="w-5 h-5 text-error shrink-0 mt-0.5"/>
                    <div className="space-y-1">
                        <span className="block uppercase tracking-wide">Lỗi kết nối Nhanh.vn OAuth:</span>
                        <p className="font-semibold text-outline-variant">{oauthErrorMsg}</p>
                    </div>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

                {/* 1. PRODUCTS SYNC */}
                <div
                    className="bg-white rounded-3xl p-6 border border-outline-variant/30 shadow-sm flex flex-col justify-between min-h-[300px]">
                    <div className="space-y-4">
                        <div
                            className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
                            <Package className="w-6 h-6"/>
                        </div>
                        <div>
                            <h3 className="text-base font-black text-on-surface uppercase">Sản phẩm (Products)</h3>
                            <p className="text-xs text-outline leading-relaxed mt-1 font-semibold">
                                Đồng bộ tất cả sản phẩm mô hình, kiểm tra SKU sản phẩm, giá bán, giá cũ và số lượng tồn
                                kho khả dụng từ Nhanh.vn.
                            </p>
                        </div>
                    </div>

                    <div className="space-y-4 pt-4 border-t border-surface-container">
                        {syncMessages.products && (
                            <div
                                className="p-3 rounded-xl bg-green-50 text-green-700 text-[10px] font-bold flex gap-2 items-center">
                                <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0"/>
                                <span>{syncMessages.products}</span>
                            </div>
                        )}
                        {syncErrors.products && (
                            <div
                                className="p-3 rounded-xl bg-error/10 text-error text-[10px] font-bold flex gap-2 items-start">
                                <AlertCircle className="w-4 h-4 text-error shrink-0 mt-0.5"/>
                                <span className="text-red-600">{syncErrors.products}</span>
                            </div>
                        )}

                        <button
                            onClick={() => handleSync('products')}
                            disabled={isSyncing.products}
                            className="w-full py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                            {isSyncing.products ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin"/>
                                    <span>Đang đồng bộ, vui lòng không tắt trang...</span>
                                </>
                            ) : (
                                <>
                                    <RefreshCw className="w-4 h-4"/>
                                    <span>Đồng bộ Sản phẩm</span>
                                </>
                            )}
                        </button>
                    </div>
                </div>

                {/* 2. CATEGORIES SYNC */}
                <div
                    className="bg-white rounded-3xl p-6 border border-outline-variant/30 shadow-sm flex flex-col justify-between min-h-[300px]">
                    <div className="space-y-4">
                        <div
                            className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
                            <ListTree className="w-6 h-6"/>
                        </div>
                        <div>
                            <h3 className="text-base font-black text-on-surface uppercase">Danh mục (Categories)</h3>
                            <p className="text-xs text-outline leading-relaxed mt-1 font-semibold">
                                Đồng bộ toàn bộ cây danh mục sản phẩm từ ERP để cập nhật cấu trúc phân mục chính và danh
                                mục nhỏ trên trang cửa hàng SOBU.
                            </p>
                        </div>
                    </div>

                    <div className="space-y-4 pt-4 border-t border-surface-container">
                        {syncMessages.categories && (
                            <div
                                className="p-3 rounded-xl bg-green-50 text-green-700 text-[10px] font-bold flex gap-2 items-center">
                                <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0"/>
                                <span>{syncMessages.categories}</span>
                            </div>
                        )}
                        {syncErrors.categories && (
                            <div
                                className="p-3 rounded-xl bg-error/10 text-error text-[10px] font-bold flex gap-2 items-start">
                                <AlertCircle className="w-4 h-4 text-error shrink-0 mt-0.5"/>
                                <span className="text-red-600">{syncErrors.categories}</span>
                            </div>
                        )}

                        <button
                            onClick={() => handleSync('categories')}
                            disabled={isSyncing.categories}
                            className="w-full py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                            {isSyncing.categories ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin"/>
                                    <span>Đang đồng bộ, vui lòng không tắt trang...</span>
                                </>
                            ) : (
                                <>
                                    <RefreshCw className="w-4 h-4"/>
                                    <span>Đồng bộ Danh mục</span>
                                </>
                            )}
                        </button>
                    </div>
                </div>

                {/* 3. BRANDS SYNC */}
                <div
                    className="bg-white rounded-3xl p-6 border border-outline-variant/30 shadow-sm flex flex-col justify-between min-h-[300px]">
                    <div className="space-y-4">
                        <div
                            className="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
                            <Award className="w-6 h-6"/>
                        </div>
                        <div>
                            <h3 className="text-base font-black text-on-surface uppercase">Thương hiệu (Brands)</h3>
                            <p className="text-xs text-outline leading-relaxed mt-1 font-semibold">
                                Đồng bộ danh sách thương hiệu đồ chơi mô hình chính hãng (Bandai, Mattel, Hot Toys,
                                LEGO, Tamiya) liên kết để hiển thị danh mục.
                            </p>
                        </div>
                    </div>

                    <div className="space-y-4 pt-4 border-t border-surface-container">
                        {syncMessages.brands && (
                            <div
                                className="p-3 rounded-xl bg-green-50 text-green-700 text-[10px] font-bold flex gap-2 items-center">
                                <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0"/>
                                <span>{syncMessages.brands}</span>
                            </div>
                        )}
                        {syncErrors.brands && (
                            <div
                                className="p-3 rounded-xl bg-error/10 text-error text-[10px] font-bold flex gap-2 items-start">
                                <AlertCircle className="w-4 h-4 text-error shrink-0 mt-0.5"/>
                                <span className="text-red-600">{syncErrors.brands}</span>
                            </div>
                        )}

                        <button
                            onClick={() => handleSync('brands')}
                            disabled={isSyncing.brands}
                            className="w-full py-3 bg-gradient-to-br from-primary to-primary-container text-white rounded-2xl text-xs font-black uppercase tracking-widest text-center shadow-md hover:scale-102 transition-transform disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                            {isSyncing.brands ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin"/>
                                    <span>Đang đồng bộ, vui lòng không tắt trang...</span>
                                </>
                            ) : (
                                <>
                                    <RefreshCw className="w-4 h-4"/>
                                    <span>Đồng bộ Thương hiệu</span>
                                </>
                            )}
                        </button>
                    </div>
                </div>

            </div>
        </div>
    );
}
