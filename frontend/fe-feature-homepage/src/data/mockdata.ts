import {Product} from "../interface/product";
import {Category} from "../interface/category";
import {Order, OrderItem} from "../interface/order";
import {ServiceRequest} from "../interface/service-request";

export const mockCategories: Category[] = [
    {
        id: 'CAT_VEHICLE',
        name: 'Mô hình xe',
        children: [
            { id: 'CAT_SUPERCAR', name: 'Mô hình siêu xe', parentId: 'CAT_VEHICLE' },
            { id: 'CAT_RACING', name: 'Mô hình xe đua', parentId: 'CAT_VEHICLE' },
        ]
    },
    {
        id: 'CAT_MECHA',
        name: 'Mô hình lắp ráp',
    },
    {
        id: 'CAT_FIGURE',
        name: 'Action Figures',
    },
    {
        id: 'CAT_LEGO',
        name: 'LEGO Technic',
    },
    {
        id: 'CAT_ACCESSORY',
        name: 'Phụ kiện',
    }
];

export const mockProducts: Product[] = [
    {
        id: 'SB-FER-082',
        name: 'Mẫu Xe Ferrari F8 Tributo Red',
        price: 2450000,
        originalPrice: 2900000,
        category: 'Mô hình siêu xe',
        scale: '1:24',
        brand: 'N/A',
        description: 'Mỗi chi tiết nhỏ nhất trên mô hình Ferrari F8 Tributo này đều được tinh chỉnh thủ công để đảm bảo độ chính xác tuyệt đối so với nguyên bản. Phiên bản giới hạn 500 chiếc toàn thế giới có số series riêng.',
        stock: 10,
        imageUrl: 'https://images.unsplash.com/photo-1583121274602-3e2820c69888?ixlib=rb-4.0.3&auto=format&fit=crop&w=1000&q=80',
        thumbnailUrls: [
            'https://images.unsplash.com/photo-1583121274602-3e2820c69888?ixlib=rb-4.0.3&auto=format&fit=crop&w=1000&q=80',
            'https://images.unsplash.com/photo-1614200187524-dc4b892acf16?ixlib=rb-4.0.3&auto=format&fit=crop&w=1000&q=80',
            'https://images.unsplash.com/photo-1730110206448-10297c1902bd?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
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
        brand: 'N/A',
        description: 'Mô hình xe đua Ferrari 488 GTE.',
        stock: 10,
        imageUrl: 'https://images.unsplash.com/photo-1629661414961-62b0d03007ab?q=80&w=1171&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D',
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
        description: 'Perfect Grade Unleashed RX-78-2. Siêu phẩm mô hình lắp ráp.',
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
        description: 'LEGO Technic Ferrari Daytona SP3.',
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
        description: 'Roronoa Zoro Wano Country Figure.',
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