package com.vn.sodu.order.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportOrdersRequestDto {
    private List<Long> ids;
    private String status;
    private String query;
}
