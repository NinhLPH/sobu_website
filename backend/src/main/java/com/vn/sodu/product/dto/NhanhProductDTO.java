package com.vn.sodu.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhProductDTO {

    private Long id;
    private Long parentId;
    private String code;
    private String barcode;
    private String name;
    private String otherName;
    private String status;
    private Integer vat;

    private Category category;
    private Category internalCategory;
    private Brand brand;
    private Type type;

    private Prices prices;
    private Images images;
    private Warranty warranty;
    private Shipping shipping;

    private String countryName;

    private Units units;
    private List<Combo> combos;
    private Inventory inventory;
    private List<Attribute> attributes;

    private Long updatedAt;
    private Long createdAt;

    private String description;
    private String content;

    private Supplier suppliers;
    private List<Video> videos;
    private List<BranchPrice> branchPrices;

    private List<NhanhProductDTO> childs;

    // ===== INNER CLASSES =====

    @Getter @Setter
    public static class Category {
        private Long id;
        private String code;
        private String name;
    }

    @Getter @Setter
    public static class Brand {
        private Long id;
        private String name;
    }

    @Getter @Setter
    public static class Type {
        private Long id;
        private String name;
    }

    @Getter @Setter
    public static class Prices {
        private Double retail;
        private Double wholesale;
        private Double importPrice;
        private Double old;
        private Double avgCost;
    }

    @Getter @Setter
    public static class Images {
        private String avatar;
        private List<String> others;
    }

    @Getter @Setter
    public static class Warranty {
        private Integer month;
        private String phone;
        private String address;
    }

    @Getter @Setter
    public static class Shipping {
        private Integer length;
        private Integer width;
        private Integer height;
        private Integer weight;
    }

    @Getter @Setter
    public static class Units {
        private String name;
        private List<UnitItem> list;
    }

    @Getter @Setter
    public static class UnitItem {
        private Long id;
        private String name;
        private Double quantity;
        private UnitPrice price;
    }

    @Getter @Setter
    public static class UnitPrice {
        private Double retail;
        private Double importPrice;
        private Double wholesale;
    }

    @Getter @Setter
    public static class Combo {
        private Long id;
        private String code;
        private String name;
        private Integer quantity;
    }

    @Getter @Setter
    public static class Inventory {
        private Double remain;
        private Double shipping;
        private Double damaged;
        private Double holding;
        private Double transfering;
        private Double available;

        private WarrantyInventory warranty;
        private List<Depot> depots;
    }

    @Getter @Setter
    public static class WarrantyInventory {
        private Double remain;
        private Double holding;
    }

    @Getter @Setter
    public static class Depot {
        private Long id;
        private Double remain;
        private Double shipping;
        private Double damaged;
        private Double holding;
        private Double transfering;
        private Double available;
        private WarrantyInventory warranty;
    }

    @Getter @Setter
    public static class Attribute {
        private Long id;
        private String name;
        private String value;
    }

    @Getter @Setter
    public static class Supplier {
        private Long id;
        private String name;
        private String mobile;
    }

    @Getter @Setter
    public static class Video {
        private String title;
        private String src;
    }

    @Getter @Setter
    public static class BranchPrice {
        private Branch branch;
        private Double price;
        private Double wholesalePrice;
    }

    @Getter @Setter
    public static class Branch {
        private Long id;
        private String name;
    }
}
