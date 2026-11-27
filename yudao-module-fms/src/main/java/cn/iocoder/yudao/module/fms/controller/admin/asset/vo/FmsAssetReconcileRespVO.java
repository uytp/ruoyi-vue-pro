package cn.iocoder.yudao.module.fms.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - FMS 固定资产与总账对账 Response VO")
@Data
public class FmsAssetReconcileRespVO {

    @Schema(description = "对账月份", example = "2027-01")
    private String month;

    @Schema(description = "是否全部一致", example = "true")
    private Boolean balanced;

    @Schema(description = "每个科目一行：固定资产科目对原值，累计折旧科目对累计折旧")
    private List<Row> rows = new ArrayList<>();

    @Schema(description = "一个科目的对账结果")
    @Data
    public static class Row {

        @Schema(description = "科目", example = "1601 固定资产")
        private String subjectName;

        @Schema(description = "对账内容：1 原值 2 累计折旧", example = "1")
        private Integer type;

        @Schema(description = "卡片合计（月末仍在用的资产）")
        private BigDecimal cardAmount = BigDecimal.ZERO;

        @Schema(description = "总账期末余额（累计折旧按贷方余额取正数）")
        private BigDecimal ledgerAmount = BigDecimal.ZERO;

        @Schema(description = "差额 = 卡片 - 总账")
        private BigDecimal diff = BigDecimal.ZERO;

    }

}
