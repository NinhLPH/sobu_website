package com.vn.sodu.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhProductDTO {

    // ================== ObjectMapper ==================
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ================== Basic Fields ==================
    private Long id;
    private Long parentId;
    private String code;
    private String barcode;
    private String name;
    private String otherName;
    private String status;
    private Integer vat;

    // ================== Nested Objects ==================
    private Category category;
    private Category internalCategory;
    private Brand brand;
    private Type type;
    private Prices prices;
    private Images images;
    private Warranty warranty;
    private Shipping shipping;
    private Units units;
    private Inventory inventory;
    private Supplier suppliers;

    // ================== Collections & Simple Fields ==================
    private String countryName;
    private List<Combo> combos;
    private List<Attribute> attributes;
    private Long updatedAt;
    private Long createdAt;
    private String description;
    private String content;
    private List<Video> videos;
    private List<BranchPrice> branchPrices;
    private List<NhanhProductDTO> childs;

    // ================== Custom Setters (null-safe object mapping) ==================
    @JsonSetter("category")
    public void setCategory(JsonNode node) {
        this.category = convert(node, Category.class);
    }

    @JsonSetter("internalCategory")
    public void setInternalCategory(JsonNode node) {
        this.internalCategory = convert(node, Category.class);
    }

    @JsonSetter("brand")
    public void setBrand(JsonNode node) {
        this.brand = convert(node, Brand.class);
    }

    @JsonSetter("type")
    public void setType(JsonNode node) {
        this.type = convert(node, Type.class);
    }

    @JsonSetter("prices")
    public void setPrices(JsonNode node) {
        this.prices = convert(node, Prices.class);
    }

    @JsonSetter("images")
    public void setImages(JsonNode node) {
        this.images = convert(node, Images.class);
    }

    @JsonSetter("warranty")
    public void setWarranty(JsonNode node) {
        this.warranty = convert(node, Warranty.class);
    }

    @JsonSetter("shipping")
    public void setShipping(JsonNode node) {
        this.shipping = convert(node, Shipping.class);
    }

    @JsonSetter("units")
    public void setUnits(JsonNode node) {
        this.units = convert(node, Units.class);
    }

    @JsonSetter("inventory")
    public void setInventory(JsonNode node) {
        this.inventory = convert(node, Inventory.class);
    }

    @JsonSetter("suppliers")
    public void setSuppliers(JsonNode node) {
        this.suppliers = convert(node, Supplier.class);
    }

    // ================== Helper ==================
    private <T> T convert(JsonNode node, Class<T> clazz) {
        if (node != null && node.isObject()) {
            return MAPPER.convertValue(node, clazz);
        }
        return null;
    }

    // ================== INNER CLASSES ==================

    @Getter
    @Setter
    public static class Category {
        private Long id;
        private String code;
        private String name;
    }

    @Getter
    @Setter
    public static class Brand {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    public static class Type {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    public static class Prices {
        private Double retail;
        private Double wholesale;
        private Double importPrice;
        private Double old;
        private Double avgCost;
    }

    @Getter
    @Setter
    public static class Images {
        private String avatar;
        private List<String> others;
    }

    @Getter
    @Setter
    public static class Warranty {
        private Integer month;
        private String phone;
        private String address;
    }

    @Getter
    @Setter
    public static class Shipping {
        private Integer length;
        private Integer width;
        private Integer height;
        private Integer weight;
    }

    @Getter
    @Setter
    public static class Units {
        private String name;
        private List<UnitItem> list;
    }

    @Getter
    @Setter
    public static class UnitItem {
        private Long id;
        private String name;
        private Double quantity;
        private UnitPrice price;
    }

    @Getter
    @Setter
    public static class UnitPrice {
        private Double retail;
        private Double importPrice;
        private Double wholesale;
    }

    @Getter
    @Setter
    public static class Combo {
        private Long id;
        private String code;
        private String name;
        private Integer quantity;
    }

    @Getter
    @Setter
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

    @Getter
    @Setter
    public static class WarrantyInventory {
        private Double remain;
        private Double holding;
    }

    @Getter
    @Setter
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

    @Getter
    @Setter
    public static class Attribute {
        private Long id;
        private String name;
        private String value;
    }

    @Getter
    @Setter
    public static class Supplier {
        private Long id;
        private String name;
        private String mobile;
    }

    @Getter
    @Setter
    public static class Video {
        private String title;
        private String src;
    }

    @Getter
    @Setter
    public static class BranchPrice {
        private Branch branch;
        private Double price;
        private Double wholesalePrice;
    }

    @Getter
    @Setter
    public static class Branch {
        private Long id;
        private String name;
    }
}