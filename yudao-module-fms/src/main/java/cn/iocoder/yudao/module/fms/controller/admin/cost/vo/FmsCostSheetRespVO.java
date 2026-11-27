package cn.iocoder.yudao.module.fms.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - FMS 成本计算单 Response VO")
@Data
public class FmsCostSheetRespVO {

    @Schema(description = "成本单编号；null 表示本期还没有计算过")
    private Long id;
    @Schema(description = "账套编号")
    private Long accountSetId;
    @Schema(description = "会计期间 yyyy-MM")
    private String period;
    @Schema(description = "状态：0草稿 1已生成凭证")
    private Integer status;
    @Schema(description = "本期制造费用归集总额")
    private BigDecimal overheadTotal;
    @Schema(description = "制造费用分配凭证编号")
    private Long allocVoucherId;
    @Schema(description = "完工入库凭证编号")
    private Long doneVoucherId;
    @Schema(description = "销售成本结转凭证编号")
    private Long cogsVoucherId;
    @Schema(description = "产品明细")
    private List<FmsCostSheetItemVO> items = new ArrayList<>();
    @Schema(description = "提示信息（如分录没有选产品，没有被归集）")
    private List<String> warnings = new ArrayList<>();

}
