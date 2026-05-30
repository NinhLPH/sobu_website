import {ProductModel} from "../interface/product.model";
import {CategoryModel} from "../interface/category.model";
import {Order, OrderItem} from "../interface/order";
import {ServiceRequest} from "../interface/service-request";

export const mockCategories: CategoryModel[] = [
    {
        id: 'CAT_VEHICLE',
        name: 'Mô hình xe',
        children: [
            { id: 'CAT_SUPERCAR', name: 'Mô hình siêu xe', parentId: 'CAT_VEHICLE' },
            { id: 'CAT_RACING', name: 'Mô hình xe đua', parentId: 'CAT_VEHICLE' },
            { id: 'CAT_CLASSIC', name: 'Xe cổ thập niên 90', parentId: 'CAT_VEHICLE' },
            { id: 'CAT_BIKE', name: 'Mô tô & Xe máy', parentId: 'CAT_VEHICLE' },
        ]
    },
    {
        id: 'CAT_MECHA',
        name: 'Mô hình lắp ráp',
        children: [
            { id: 'CAT_GUNPLA_HG', name: 'Gundam High Grade (HG)', parentId: 'CAT_MECHA' },
            { id: 'CAT_GUNPLA_MG', name: 'Gundam Master Grade (MG)', parentId: 'CAT_MECHA' },
            { id: 'CAT_MILITARY', name: 'Mô hình Quân sự / Tank / Tàu chiến', parentId: 'CAT_MECHA' },
        ]
    },
    {
        id: 'CAT_FIGURE',
        name: 'Action Figures',
        children: [
            { id: 'CAT_ANIME', name: 'Anime & Manga Figures', parentId: 'CAT_FIGURE' },
            { id: 'CAT_MARVEL_DC', name: 'Marvel / DC Comics Movie', parentId: 'CAT_FIGURE' },
            { id: 'CAT_COSPLAY', name: 'Nendoroid & Chibi', parentId: 'CAT_FIGURE' },
        ]
    },
    {
        id: 'CAT_LEGO',
        name: 'LEGO Technic',
        children: [
            { id: 'CAT_LEGO_CAR', name: 'Siêu xe Technic 1:8 / 1:12', parentId: 'CAT_LEGO' },
            { id: 'CAT_LEGO_ARCH', name: 'Architecture & Creator', parentId: 'CAT_LEGO' },
        ]
    },
    {
        id: 'CAT_ACCESSORY',
        name: 'Phụ kiện & Công cụ',
        children: [
            { id: 'CAT_TOOLS', name: 'Dụng cụ kềm / Nhíp / Dao', parentId: 'CAT_ACCESSORY' },
            { id: 'CAT_PAINTS', name: 'Sơn sơn Acrylic / Airbrush', parentId: 'CAT_ACCESSORY' },
            { id: 'CAT_BOX', name: 'Hộp mica & Đèn LED trưng bày', parentId: 'CAT_ACCESSORY' },
        ]
    }
];

export const mockProducts: ProductModel[] = [
    {
        id: 'SB-FER-082',
        name: 'Mẫu Xe Ferrari F8 Tributo Red',
        price: 2450000,
        originalPrice: 2900000,
        category: 'Mô hình siêu xe',
        scale: '1:24',
        brand: 'Bburago',
        description: 'Mẫu mô hình tĩnh Ferrari F8 Tributo tỷ lệ 1:24 được chế tác tinh xảo với thân xe bằng kim loại hợp kim (Diecast) sơn tĩnh điện cao cấp, đảm bảo độ bền màu vượt thời gian. Các chi tiết như khoang động cơ phía sau, nội thất vô lăng, ghế ngồi được mô phỏng chính xác theo nguyên bản xe thật của Ferrari. Hai cửa xe và nắp capo hoàn toàn có thể đóng mở linh hoạt. Sản phẩm đi kèm đế trưng bày sang trọng, phù hợp cho việc decor bàn làm việc hoặc bổ sung vào bộ sưu tập siêu xe của bạn.',
        stock: 10,
        imageUrl: 'https://images.unsplash.com/photo-1583121274602-3e2820c69888?ixlib=rb-4.0.3&auto=format&fit=crop&w=1000&q=80',
        thumbnailUrls: [
            'https://images.unsplash.com/photo-1583121274602-3e2820c69888?ixlib=rb-4.0.3&auto=format&fit=crop&w=1000&q=80',
            'https://images.unsplash.com/photo-1614200187524-dc4b892acf16?ixlib=rb-4.0.3&auto=format&fit=crop&w=1000&q=80',
            'https://images.unsplash.com/photo-1730110206448-10297c1902bd?q=80&w=687&auto=format&fit=crop'
        ],
        isNew: true,
        rating: 4.9,
        reviewsCount: 120,
    },
    {
        id: 'MM-F488',
        name: 'Ferrari 488 GTE "AF Corse"',
        price: 2450000,
        category: 'Mô hình xe đua',
        scale: '1:24',
        brand: 'Maisto',
        description: 'Mô hình xe đua bản quyền chính thức Ferrari 488 GTE mang số hiệu "AF Corse" nổi tiếng trên các đường đua Le Mans. Khung vỏ xe làm bằng kim loại đặc nguyên khối mang lại cảm giác cầm nắm rất đầm tay. Bộ lốp xe được làm bằng cao su tự nhiên có vân bám đường như thật, hệ thống giảm xóc bánh xe có lò xo hoạt động mượt mà. Toàn bộ tem xe và hoa văn tài trợ (livery) được in chuyển nhiệt độ phân giải cao, không bong tróc, mô phỏng chính xác 100% chiếc xe đua thực tế.',
        stock: 10,
        imageUrl: 'https://images.unsplash.com/photo-1629661414961-62b0d03007ab?q=80&w=1171&auto=format&fit=crop',
        rating: 4.5,
        reviewsCount: 45
    },
    {
        id: 'GUN-PG-01',
        name: 'PG Unleashed RX-78-2 Gundam',
        price: 5890000,
        category: 'Mô hình lắp ráp',
        scale: '1:60',
        brand: 'Bandai',
        description: 'Perfect Grade Unleashed (PGU) RX-78-2 Gundam là đỉnh cao công nghệ đúc nhựa của Bandai kỷ niệm 40 năm dòng phim Gunpla. Với chứng chỉ lắp ráp đa tầng (Multi-Layer Frame), mô hình sở hữu hệ thống khung xương chuyển động phức tạp nhất từ trước đến nay, kết hợp giữa các chi tiết nhựa mạ chrome, chi tiết kim loại etching dán và hệ thống đèn LED đa điểm chạy khắp thân xác cụ tổ Gundam. Sản phẩm mang tới trải nghiệm lắp ráp đỉnh cao, không cần sơn vẫn đạt độ chi tiết tuyệt đối.',
        stock: 10,
        imageUrl: 'https://product.hstatic.net/1000231532/product/gundam_shop_ban_rx-78-2_gundam_pg_unleashed_445279d9bbe74732ad1d1e8c47e509c1_master.jpg',
        thumbnailUrls: [
            'https://product.hstatic.net/1000231532/product/gundam_shop_ban_rx-78-2_gundam_pg_unleashed_445279d9bbe74732ad1d1e8c47e509c1_master.jpg',
            'https://product.hstatic.net/1000231532/product/gunpla_shop_ban_rx-78-2_gundam_pg_unleashed_777312e5e58b4a048e36d180c48dca66_master.jpg'
        ]
    },
    {
        id: 'LT-DAYTONA',
        name: 'Ferrari Daytona SP3',
        price: 8500000,
        originalPrice: 9200000,
        category: 'LEGO Technic',
        scale: '1:8',
        brand: 'LEGO',
        description: 'Thuộc bộ sưu tập cao cấp LEGO Technic Ultimate Car Concept, mô hình siêu xe Ferrari Daytona SP3 tỷ lệ 1:8 gồm 3,778 mảnh ghép là một thử thách cơ khí đỉnh cao dành cho người chơi. Mô hình tái hiện hoàn hảo hộp số tuần tự 8 cấp chuyển động bằng lẫy chuyển số trên vô lăng, động cơ V12 có piston chuyển động thụt thò khi xe lăn bánh, cùng hệ thống cửa mở cánh bướm cánh chim (butterfly doors) mở mượt mà. Mỗi hộp sản phẩm chứa một mã code độc quyền để mở khóa các nội dung online cao cấp từ Ferrari.',
        stock: 10,
        imageUrl: 'https://www.mykingdom.com.vn/cdn/shop/files/42143_14974512-209e-4be9-a830-11504fe351c5.jpg?v=1725530410',
        isHot: true,
    },
    {
        id: 'FIG-ZORO',
        name: 'Roronoa Zoro - Wano Country Figure',
        price: 1250000,
        category: 'Action Figures',
        brand: 'Bandai Spirits',
        description: 'Mô hình tĩnh nhân vật Kiếm sĩ Roronoa Zoro trong trang phục võ sĩ tại đảo quốc Wano (Arc Wano Quốc từ anime One Piece). Figure đạt độ chi tiết cực cao ở phần tạo hình cơ bắp bộc phát, các thớ vải áo choàng động mượt mà như đang bay trong gió. Cả 3 thanh bảo kiếm Wado Ichimonji, Shusui và Enma được đúc riêng biệt với các đường vân kiếm (Hamon) sắc nét. Chất liệu nhựa PVC/ABS cao cấp an toàn, màu sơn đổ bóng (shading) thủ công tạo chiều sâu ấn tượng dưới ánh đèn trưng bày.',
        stock: 10,
        imageUrl: 'https://down-vn.img.susercontent.com/file/vn-11134201-7ras8-mccp9anpf1hu68@resize_w450_nl.webp',
    },
    {
        id: 'TAM-GTR',
        name: 'Nissan Skyline GT-R R34 Model Kit',
        price: 850000,
        category: 'Model Kits 1:24',
        brand: 'Tamiya',
        description: 'Nissan Skyline GT-R R34 Model Kit by Tamiya.',
        stock: 10,
        imageUrl: 'https://d7z22c0gz59ng.cloudfront.net/japan_contents/img/usr/item/pkg/24210_p1.jpg',
    },
    {
        id: 'TLS-KEM-PRO',
        name: 'Kềm cắt Precision Pro',
        price: 450000,
        category: 'Phụ kiện',
        brand: 'SOBU',
        description: 'Kềm cắt chi tiết.',
        stock: 10,
        imageUrl: 'https://down-vn.img.susercontent.com/file/vn-11134207-81ztc-mm00wy89e7t079.webp',
    },
    {
        id: 'TLS-SON-12',
        name: 'Set sơn Acrylic 12 màu',
        price: 820000,
        category: 'Phụ kiện',
        brand: 'SOBU',
        description: 'Set 12 màu sơn acrylic.',
        stock: 10,
        imageUrl: 'https://down-vn.img.susercontent.com/file/9f736b18c9989ea4fd00868ef2c6ebf0.webp',
    }
];

export const megaMenuBrands = [
    'AutoArt', 'Bburago', 'Maisto', 'Welly', 'Kyosho', 'Minichamps',
    'Bandai', 'Hot Toys', 'LEGO', 'Tamiya', 'Aoshima', 'Hasegawa',
    'Ferrari', 'Lamborghini', 'Porsche', 'McLaren', 'Bugatti', 'Khác...'
];

export const mockOrders: Order[] = [
    {
        id: 'ORD-001',
        customerName: 'Nguyễn Văn A',
        customerPhone: '0901234567',
        customerEmail: 'nguyenvana@example.com',
        shippingAddress: '123 Cầu Giấy, Hà Nội',
        items: [
            { productId: 'SB-FER-082', productName: 'Mẫu Xe Ferrari F8 Tributo Red', quantity: 1, price: 2450000 }
        ],
        subtotal: 2450000,
        tax: 245000,
        shippingFee: 0,
        total: 2695000,
        status: 'DELIVERED',
        createdAt: '2026-04-20T10:00:00Z',
    },
    {
        id: 'ORD-002',
        customerName: 'Trần Thị B',
        customerPhone: '0987654321',
        customerEmail: 'tranthib@example.com',
        shippingAddress: '456 Lê Lợi, TP.HCM',
        items: [
            { productId: 'GUN-PG-01', productName: 'PG Unleashed RX-78-2 Gundam', quantity: 2, price: 5890000 },
            { productId: 'TLS-KEM-PRO', productName: 'Kềm cắt Precision Pro', quantity: 1, price: 450000 }
        ],
        subtotal: 12230000,
        tax: 1223000,
        shippingFee: 0,
        total: 13453000,
        status: 'PENDING',
        createdAt: '2026-04-27T08:30:00Z',
    }
];

export const mockRequests: ServiceRequest[] = [
    {
        id: 'REQ-PO-001',
        type: 'PRE_ORDER',
        customerName: 'Lê Văn C',
        customerPhone: '0912345678',
        customerEmail: 'levanc@example.com',
        productName: 'Hot Toys Iron Man Mark LXXXV',
        description: 'Mình muốn order bản Diecast của Hot Toys, báo giá giúp mình nhé.',
        status: 'PENDING',
        createdAt: '2026-04-26T14:20:00Z',
    },
    {
        id: 'REQ-CU-001',
        type: 'CUSTOM',
        customerName: 'Phạm Thị D',
        customerPhone: '0933445566',
        customerEmail: 'phamthid@example.com',
        description: 'Sơn lại mô hình Gundam màu hồng thay vì màu xanh đỏ cơ bản, có thêm hoa văn decal tự thiết kế.',
        budget: 3000000,
        status: 'ACCEPTED',
        adminNotes: 'Đã liên hệ báo giá 3.5m, khách đồng ý.',
        createdAt: '2026-04-25T09:15:00Z',
    }
];

export const placeholderImages = {
    hero: 'https://images.unsplash.com/photo-1614200187524-dc4b892acf16?q=80&w=2000&auto=format&fit=crop',
    custom1: 'https://images.unsplash.com/photo-1730110206448-10297c1902bd?q=80&w=800&auto=format&fit=crop',
    custom2: 'https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop', // Gundam/Workshop
    hotwheels: 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg',
    marvel: 'https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?q=80&w=1000&auto=format&fit=crop',
    dc: 'https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?q=80&w=1000&auto=format&fit=crop',
    trans: 'https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?q=80&w=1000&auto=format&fit=crop',
    pacificrim: 'https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?q=80&w=1000&auto=format&fit=crop',
    naruto: 'https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?q=80&w=1000&auto=format&fit=crop',
    classic: 'https://images.unsplash.com/photo-1551522435-a13afa10f103?q=80&w=1000&auto=format&fit=crop',
    gallery: [
        'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?q=80&w=600&auto=format&fit=crop',
        'https://www.zervtek.com/_next/image?url=https%3A%2F%2Fstrapi-zervtek.s3.ap-southeast-1.amazonaws.com%2FDSC_01467_2_7d3d2fff3a.jpg&w=3840&q=75',
        'https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?q=80&w=600&auto=format&fit=crop',
        'https://images.unsplash.com/photo-1599839619722-39751411ea63?q=80&w=600&auto=format&fit=crop',
        'https://cdn.mohinhcaocap.com/wp-content/uploads/2025/08/18193223/491812046_1003457155226798_8966527836694548731_n-Medium.jpg',
        'https://www.mykingdom.com.vn/cdn/shop/files/mo-hinh-marvel-bien-hinh-iron-man-mark-3-morstorm-zc8824.jpg?v=1741766340',
    ]
};

export const HERO_SLIDES = [
    {
        title: "SOBU STUDIO",
        subtitle: "ĐỈNH CAO CUSTOM MÔ HÌNH SỐ 1 VIỆT NAM",
        image: placeholderImages.hero,
    },
    {
        title: "HOT WHEELS",
        subtitle: "KHÁM PHÁ CÁC SIÊU PHẨM TỐC ĐỘ GIỚI HẠN",
        image: placeholderImages.hotwheels,
    },
    {
        title: "MECHA & GUNDAM",
        subtitle: "XƯỞNG ĐỘ LED VÀ SƠN PHIM CHUYÊN NGHIỆP",
        image: placeholderImages.custom2,
    },
];

export const mockBlogs = [
    {
        id: 'blog-1',
        title: 'Cách giữ Led mô hình lâu hơn',
        excerpt: 'Mẹo vệ sinh và bảo quản hệ thống đèn LED trên các mô hình Custom...',
        content: 'Nội dung chi tiết bài viết về cách bảo quản LED. Bạn nên sử dụng pin chuẩn, tránh để nơi ẩm ướt và tắt khi không sử dụng...',
        image: 'https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?q=80&w=800&auto=format&fit=crop',
        date: '18/05/2026'
    },
    {
        id: 'blog-2',
        title: 'Review: PG Unleashed RX-78-2',
        excerpt: 'Đánh giá chi tiết siêu phẩm Perfect Grade mới nhất từ Bandai.',
        content: 'Với cấu trúc khung xương nhiều lớp và chi tiết kim loại, đây là mô hình đáng tiền nhất...',
        image: 'https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop',
        date: '15/05/2026'
    },
    {
        id: 'blog-3',
        title: 'Thú chơi xe Diecast tỷ lệ 1:24',
        excerpt: 'Vì sao tỷ lệ 1:24 lại được giới sưu tầm yêu thích đến vậy?',
        content: 'Tỷ lệ 1:24 mang lại sự cân bằng hoàn hảo giữa kích thước trưng bày và độ chi tiết...',
        image: 'https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?q=80&w=800&auto=format&fit=crop',
        date: '10/05/2026'
    },
    {
        id: 'blog-4',
        title: 'Kinh nghiệm Custom màu sơn',
        excerpt: 'Hướng dẫn pha màu và sử dụng airbrush cho người mới bắt đầu.',
        content: 'Airbrush là công cụ không thể thiếu. Hãy bắt đầu với áp suất 15-20 psi...',
        image: 'https://images.unsplash.com/photo-1599839619722-39751411ea63?q=80&w=800&auto=format&fit=crop',
        date: '05/05/2026'
    },
    {
        id: 'blog-5',
        title: 'Top 5 mô hình Marvel Hot Toys 2026',
        excerpt: 'Điểm mặt những gương mặt siêu anh hùng cháy hàng nhất năm nay.',
        content: 'Iron Man Mark LXXXV vẫn giữ vững ngôi vương, theo sau là Spider-Man...',
        image: 'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?q=80&w=800&auto=format&fit=crop',
        date: '01/05/2026'
    }
];