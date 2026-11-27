package cn.iocoder.yudao.module.fms.controller.admin.settle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - FMS 往来科目 Response VO")
@Data
public class FmsSettleSubjectRespVO {

    @Schema(description = "科目编号", example = "6")
    private Long id;

    @Schema(description = "科目编码", example = "1122")
    private String code;

    @Schema(description = "科目名称", example = "应收账款")
    private String name;

    @Schema(description = "余额方向：1 借 2 贷", example = "1")
    private Integer balanceDirection;

    @Schema(description = "可以做往来核销的辅助类别编号（客户 / 供应商 / 职员）")
    private List<Long> auxiliaryTypeIds;

}
