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
public class CreateBannerRequest {
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer displayOrder;
    private Banner.BannerPosition position;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Banner.DeviceType deviceType;
}
