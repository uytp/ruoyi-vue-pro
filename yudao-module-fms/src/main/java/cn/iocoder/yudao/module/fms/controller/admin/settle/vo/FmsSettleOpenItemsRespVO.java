package cn.iocoder.yudao.module.fms.controller.admin.settle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - FMS 往来未核销项目列表 Response VO")
@Data
public class FmsSettleOpenItemsRespVO {

    @Schema(description = "借方未核销项目")
    private List<FmsSettleOpenItemRespVO> debitItems = new ArrayList<>();

    @Schema(description = "贷方未核销项目")
    private List<FmsSettleOpenItemRespVO> creditItems = new ArrayList<>();

    @Schema(description = "借方未核销合计", example = "3000.00")
    private BigDecimal debitRemain = BigDecimal.ZERO;

    @Schema(description = "贷方未核销合计", example = "1000.00")
    private BigDecimal creditRemain = BigDecimal.ZERO;

}
