package com.vn.sodu.ui.dto;

import com.vn.sodu.ui.Banner;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BannerDTO {
    private Long id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer displayOrder;
    private String position;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Banner.DeviceType deviceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
