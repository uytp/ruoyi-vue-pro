package cn.iocoder.yudao.module.fms.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - FMS 成本报表 Response VO")
@Data
public class FmsCostReportRespVO {

    @Schema(description = "开始月份 yyyy-MM")
    private String startPeriod;
    @Schema(description = "结束月份 yyyy-MM")
    private String endPeriod;
    @Schema(description = "范围内的成本单数量（0 表示这段时间没有算过成本）")
    private Integer sheetCount;
    @Schema(description = "范围内有多少个月还是草稿（没生成凭证）")
    private Integer draftCount;
    @Schema(description = "产品成本汇总表：每个产品一行")
    private List<FmsCostReportRowVO> summary = new ArrayList<>();
    @Schema(description = "汇总表的合计行")
    private FmsCostReportRowVO total;
    @Schema(description = "成本明细表：产品 × 月份")
    private List<FmsCostReportRowVO> detail = new ArrayList<>();

}
