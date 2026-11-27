package cn.iocoder.yudao.module.fms.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * FMS 错误码常量
 *
 * @author 芋道源码
 */
public interface ErrorCodeConstants {

    // ========== 账套 1-052-100-000 ==========
    ErrorCode ACCOUNT_SET_NOT_EXISTS = new ErrorCode(1_052_100_000, "账套不存在");
    ErrorCode ACCOUNT_SET_CODE_DUPLICATE = new ErrorCode(1_052_100_001, "账套编码已存在");
    ErrorCode ACCOUNT_SET_ACCESS_DENIED = new ErrorCode(1_052_100_002, "无权访问该账套");
    ErrorCode ACCOUNT_SET_ALREADY_INITIALIZED = new ErrorCode(1_052_100_003, "账套已完成初始化");
    ErrorCode ACCOUNT_SET_NOT_INITIALIZED = new ErrorCode(1_052_100_005, "账套尚未完成初始化");
    ErrorCode ACCOUNT_SET_SETTINGS_RULE_INVALID = new ErrorCode(1_052_100_006, "科目编码规则不正确");
    ErrorCode ACCOUNT_SET_SETTINGS_RULE_SHRINK = new ErrorCode(1_052_100_007, "科目级次和编码长度不能调小");
    ErrorCode FINANCE_PARAMETER_NOT_EXISTS = new ErrorCode(1_052_100_008, "财务参数不存在");

    // ========== 科目 1-052-101-000 ==========
    ErrorCode SUBJECT_NOT_EXISTS = new ErrorCode(1_052_101_000, "科目不存在");
    ErrorCode SUBJECT_CODE_DUPLICATE = new ErrorCode(1_052_101_001, "科目编码已存在");
    ErrorCode SUBJECT_CATEGORY_INVALID = new ErrorCode(1_052_101_002, "科目类别与科目类型不匹配");
    ErrorCode SUBJECT_PARENT_INVALID = new ErrorCode(1_052_101_003,
            "上级科目不属于当前账套，或科目类型、类别不一致");
    ErrorCode SUBJECT_CODE_RULE_INVALID = new ErrorCode(1_052_101_004, "科目编码不符合科目级次规则");
    ErrorCode SUBJECT_HAS_CHILDREN = new ErrorCode(1_052_101_005, "科目下有子级科目，不能删除或修改编码");
    ErrorCode SUBJECT_QUANTITY_UNIT_REQUIRED = new ErrorCode(1_052_101_006, "启用数量核算时数量单位不能为空");
    ErrorCode SUBJECT_CURRENCY_INVALID = new ErrorCode(1_052_101_007, "外币核算不能选择本位币");
    ErrorCode SUBJECT_VOUCHER_ENTRY_IN_USE = new ErrorCode(1_052_101_008,
            "科目已被 {} 条凭证分录使用，不能删除");
    ErrorCode SUBJECT_VOUCHER_TEMPLATE_IN_USE = new ErrorCode(1_052_101_009,
            "科目已被 {} 个凭证模板使用，不能删除");
    ErrorCode SUBJECT_INITIAL_BALANCE_IN_USE = new ErrorCode(1_052_101_010,
            "科目已被 {} 条初始余额使用，不能删除");
    ErrorCode SUBJECT_AUXILIARY_COMBINATION_IN_USE = new ErrorCode(1_052_101_011,
            "科目已被 {} 个辅助核算组合使用，不能删除");
    ErrorCode SUBJECT_CLOSING_SCHEME_IN_USE = new ErrorCode(1_052_101_012,
            "科目已被 {} 个结账方案使用，不能删除");
    ErrorCode SUBJECT_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_052_101_013, "导入科目数据不能为空");
    ErrorCode SUBJECT_USED_BALANCE_DIRECTION_IMMUTABLE = new ErrorCode(1_052_101_014,
            "科目已有业务数据，不能修改余额方向");
    ErrorCode SUBJECT_USED_AUXILIARY_IMMUTABLE = new ErrorCode(1_052_101_015,
            "科目已有业务数据，不能直接修改辅助核算");
    ErrorCode SUBJECT_NON_LEAF_CONFIG_IMMUTABLE = new ErrorCode(1_052_101_016,
            "非末级科目不能修改科目类型、类别或辅助核算");
    ErrorCode SUBJECT_QUANTITY_ACCOUNTING_IN_USE = new ErrorCode(1_052_101_017,
            "科目已有数量数据，不能关闭数量核算");
    ErrorCode SUBJECT_PARENT_IN_USE = new ErrorCode(1_052_101_018,
            "上级科目已有业务数据，新增下级前需要迁移历史数据");
    ErrorCode SUBJECT_PARENT_CONFIG_INCOMPATIBLE = new ErrorCode(1_052_101_019,
            "迁移历史数据时，下级科目的余额方向和核算配置必须与上级科目一致");
    ErrorCode SUBJECT_AUXILIARY_MIGRATION_INVALID = new ErrorCode(1_052_101_020,
            "科目已有业务数据，新增辅助核算时必须为每个类别选择有效的迁移项目");
    ErrorCode SUBJECT_CLOSING_TEMPLATE_IN_USE = new ErrorCode(1_052_101_021,
            "科目已被 {} 个结账模板使用，不能删除");

    // ========== 辅助核算 1-052-102-000 ==========
    ErrorCode AUXILIARY_TYPE_NOT_EXISTS = new ErrorCode(1_052_102_000, "辅助核算类别不存在");
    ErrorCode AUXILIARY_TYPE_NAME_DUPLICATE = new ErrorCode(1_052_102_001, "辅助核算类别名称已存在");
    ErrorCode AUXILIARY_TYPE_SYSTEM_NOT_EDITABLE = new ErrorCode(1_052_102_002, "系统预置辅助核算类别不能编辑或删除");
    ErrorCode AUXILIARY_TYPE_HAS_ITEM = new ErrorCode(1_052_102_003,
            "辅助核算类别下有 {} 个项目，不能删除");
    ErrorCode AUXILIARY_ITEM_NOT_EXISTS = new ErrorCode(1_052_102_004, "辅助核算项目不存在");
    ErrorCode AUXILIARY_ITEM_CODE_DUPLICATE = new ErrorCode(1_052_102_005, "辅助核算项目编码已存在");
    ErrorCode AUXILIARY_TYPE_SUBJECT_IN_USE = new ErrorCode(1_052_102_006,
            "辅助核算类别已被 {} 个科目使用，不能删除");
    ErrorCode AUXILIARY_ITEM_VOUCHER_IN_USE = new ErrorCode(1_052_102_007,
            "辅助核算项目已被 {} 条凭证分录使用，不能删除");
    ErrorCode AUXILIARY_ITEM_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_052_102_008,
            "导入辅助核算项目数据不能为空");
    ErrorCode AUXILIARY_ITEM_VOUCHER_TEMPLATE_IN_USE = new ErrorCode(1_052_102_009,
            "辅助核算项目已被 {} 个凭证模板使用，不能删除");
    ErrorCode AUXILIARY_ITEM_INITIAL_BALANCE_IN_USE = new ErrorCode(1_052_102_010,
            "辅助核算项目已被 {} 条初始余额使用，不能删除");
    ErrorCode AUXILIARY_TYPE_VOUCHER_IN_USE = new ErrorCode(1_052_102_012,
            "辅助核算类别已被 {} 条凭证分录使用，不能删除");
    ErrorCode AUXILIARY_TYPE_VOUCHER_TEMPLATE_IN_USE = new ErrorCode(1_052_102_013,
            "辅助核算类别已被 {} 个凭证模板使用，不能删除");
    ErrorCode AUXILIARY_TYPE_INITIAL_BALANCE_IN_USE = new ErrorCode(1_052_102_014,
            "辅助核算类别已被 {} 条初始余额使用，不能删除");

    // ========== 凭证 1-052-103-000 ==========
    ErrorCode VOUCHER_NOT_EXISTS = new ErrorCode(1_052_103_000, "凭证不存在");
    ErrorCode VOUCHER_WORD_NOT_EXISTS = new ErrorCode(1_052_103_001, "凭证字不存在");
    ErrorCode VOUCHER_NUMBER_DUPLICATE = new ErrorCode(1_052_103_002, "该期间的凭证号已存在");
    ErrorCode VOUCHER_ENTRY_REQUIRED = new ErrorCode(1_052_103_003, "凭证至少需要两条有效分录");
    ErrorCode VOUCHER_ENTRY_AMOUNT_INVALID = new ErrorCode(1_052_103_004, "凭证分录只能填写借方或贷方金额");
    ErrorCode VOUCHER_AMOUNT_UNBALANCED = new ErrorCode(1_052_103_005, "凭证借贷金额不平衡");
    ErrorCode VOUCHER_APPROVED_NOT_EDITABLE = new ErrorCode(1_052_103_006, "已审核凭证不能编辑或删除");
    ErrorCode VOUCHER_STATUS_INVALID = new ErrorCode(1_052_103_007, "凭证状态流转不正确");
    ErrorCode VOUCHER_SUBJECT_DISABLED = new ErrorCode(1_052_103_008, "凭证分录使用了已停用科目");
    ErrorCode VOUCHER_SUBJECT_HAS_CHILDREN = new ErrorCode(1_052_103_009, "凭证分录不能使用非末级科目");
    ErrorCode VOUCHER_AUXILIARY_REQUIRED = new ErrorCode(1_052_103_010, "凭证分录的辅助核算项目不完整");
    ErrorCode VOUCHER_QUANTITY_INVALID = new ErrorCode(1_052_103_011, "凭证分录的数量核算信息不正确");
    ErrorCode VOUCHER_WORD_NAME_DUPLICATE = new ErrorCode(1_052_103_012, "凭证字已存在");
    ErrorCode VOUCHER_WORD_DEFAULT_REQUIRED = new ErrorCode(1_052_103_013, "请确保至少有一个默认凭证字");
    ErrorCode VOUCHER_WORD_DEFAULT_NOT_DELETABLE = new ErrorCode(1_052_103_014, "默认凭证字不允许删除");
    ErrorCode VOUCHER_WORD_IN_USE_NOT_EDITABLE = new ErrorCode(1_052_103_015, "使用中的凭证字不能编辑");
    ErrorCode VOUCHER_WORD_IN_USE_NOT_DELETABLE = new ErrorCode(1_052_103_016,
            "该凭证字已被 {} 张凭证使用，不能删除");
    ErrorCode VOUCHER_TEMPLATE_CATEGORY_NOT_EXISTS = new ErrorCode(1_052_103_017, "凭证模板分类不存在");
    ErrorCode VOUCHER_TEMPLATE_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_052_103_018, "凭证模板分类已存在");
    ErrorCode VOUCHER_TEMPLATE_CATEGORY_IN_USE = new ErrorCode(1_052_103_019, "分类下存在凭证模板，不能删除");
    ErrorCode VOUCHER_TEMPLATE_NOT_EXISTS = new ErrorCode(1_052_103_020, "凭证模板不存在");
    ErrorCode DIGEST_NOT_EXISTS = new ErrorCode(1_052_103_021, "常用摘要不存在");
    ErrorCode VOUCHER_STATISTICS_RANGE_INVALID = new ErrorCode(1_052_103_022, "凭证汇总查询范围不正确");
    ErrorCode VOUCHER_MOVE_RANGE_INVALID = new ErrorCode(1_052_103_023, "移动到的凭证号必须小于原凭证号");
    ErrorCode VOUCHER_MOVE_SOURCE_NOT_EXISTS = new ErrorCode(1_052_103_024, "要移动的凭证不存在");
    ErrorCode VOUCHER_IMPORT_FILE_EMPTY = new ErrorCode(1_052_103_025, "请选择要导入的凭证文件");
    ErrorCode VOUCHER_IMPORT_FILE_TOO_LARGE = new ErrorCode(1_052_103_026, "导入文件不能超过 2MB");
    ErrorCode VOUCHER_IMPORT_TEMPLATE_INVALID = new ErrorCode(1_052_103_027, "凭证导入模板格式不正确");
    ErrorCode VOUCHER_CLOSING_GENERATED_NOT_EDITABLE = new ErrorCode(1_052_103_029, "结账生成凭证不能编辑、审核或删除");
    ErrorCode VOUCHER_POST_NOT_APPROVED = new ErrorCode(1_052_103_031, "只有已审核的凭证才能过账");
    ErrorCode VOUCHER_POST_STATUS_INVALID = new ErrorCode(1_052_103_032, "凭证过账状态流转不正确");
    ErrorCode VOUCHER_POSTED_NOT_UNREVIEWABLE = new ErrorCode(1_052_103_033, "已过账凭证不能反审核，请先反过账");
    ErrorCode VOUCHER_VOID_STATUS_INVALID = new ErrorCode(1_052_103_034, "只有待审核的凭证才能作废，只有已作废的凭证才能恢复");
    ErrorCode VOUCHER_VOIDED_NOT_EDITABLE = new ErrorCode(1_052_103_035, "已作废凭证不能修改，请先恢复");
    ErrorCode VOUCHER_REVERSE_NOT_APPROVED = new ErrorCode(1_052_103_036, "只有已审核的凭证才能红字冲销");
    ErrorCode VOUCHER_REVERSE_SOURCE_INVALID = new ErrorCode(1_052_103_037, "红字冲销生成的凭证不能再冲销");
    ErrorCode VOUCHER_ALREADY_REVERSED = new ErrorCode(1_052_103_038, "该凭证已经冲销过，不能重复冲销");
    ErrorCode VOUCHER_WORD_IN_USE_BY_CLOSING_SCHEME_NOT_DELETABLE = new ErrorCode(1_052_103_030,
            "该凭证字已被 {} 个结账方案使用，不能删除");

    // ========== 账簿 1-052-104-000 ==========
    ErrorCode LEDGER_PERIOD_INVALID = new ErrorCode(1_052_104_000, "开始会计期间不能晚于结束会计期间");
    ErrorCode INITIAL_BALANCE_SUBJECT_NOT_LEAF = new ErrorCode(1_052_104_001, "初始余额只能录入末级科目");
    ErrorCode INITIAL_BALANCE_AUXILIARY_INVALID = new ErrorCode(1_052_104_002, "辅助核算项目与科目配置不一致");
    ErrorCode INITIAL_BALANCE_PROFIT_LOSS_YEAR_OPENING_INVALID = new ErrorCode(1_052_104_003,
            "损益类科目的年初余额必须为 0");
    ErrorCode INITIAL_BALANCE_IMPORT_TEMPLATE_INVALID = new ErrorCode(1_052_104_006,
            "初始余额导入模板格式不正确");
    ErrorCode INITIAL_BALANCE_IMPORT_ROW_INVALID = new ErrorCode(1_052_104_008,
            "第 {} 行导入失败：{}");
    ErrorCode INITIAL_BALANCE_PERIOD_CLOSED = new ErrorCode(1_052_104_009,
            "账套已结账，不能修改初始余额");
    ErrorCode LEDGER_PERIOD_BEFORE_ACCOUNT_START = new ErrorCode(1_052_104_010,
            "开始会计期间不能早于账套启用期间");

    // ========== 结账 1-052-105-000 ==========
    ErrorCode CLOSING_PERIOD_CLOSED = new ErrorCode(1_052_105_000, "该会计期间已结账");
    ErrorCode CLOSING_PERIOD_NOT_CLOSED = new ErrorCode(1_052_105_001, "该会计期间尚未结账");
    ErrorCode CLOSING_NOT_LATEST_PERIOD = new ErrorCode(1_052_105_002, "只能反结账最近的已结账期间");
    ErrorCode CLOSING_VOUCHER_PENDING_REVIEW = new ErrorCode(1_052_105_003, "存在未审核凭证，不能结账");
    ErrorCode CLOSING_PROFIT_LOSS_NOT_TRANSFERRED = new ErrorCode(1_052_105_004, "损益类科目尚有余额，请先结转损益");
    ErrorCode CLOSING_BALANCE_SHEET_UNBALANCED = new ErrorCode(1_052_105_005, "资产负债表不平衡，不能结账");
    ErrorCode CLOSING_PROFIT_SUBJECT_INVALID = new ErrorCode(1_052_105_006, "本年利润科目不正确");
    ErrorCode CLOSING_NO_PROFIT_LOSS_BALANCE = new ErrorCode(1_052_105_007, "当前期间无需结转损益");
    ErrorCode CLOSING_SCHEME_NOT_EXISTS = new ErrorCode(1_052_105_008, "结账方案不存在");
    ErrorCode CLOSING_SCHEME_SUBJECT_INVALID = new ErrorCode(1_052_105_009, "结账方案科目不正确");
    ErrorCode CLOSING_SCHEME_RATIO_INVALID = new ErrorCode(1_052_105_010, "结账方案借贷金额比例必须分别等于 100%");
    ErrorCode CLOSING_SCHEME_NO_BALANCE = new ErrorCode(1_052_105_011, "当前期间无需生成该结转凭证");
    ErrorCode CLOSING_SCHEME_IN_USE = new ErrorCode(1_052_105_012, "结账方案已生成凭证，不能删除");
    ErrorCode CLOSING_NOT_CURRENT_PERIOD = new ErrorCode(1_052_105_013, "只能操作当前会计期间");
    ErrorCode CLOSING_PROFIT_LOSS_SETTINGS_INVALID = new ErrorCode(1_052_105_014, "结转损益科目设置不正确");
    ErrorCode CLOSING_PROFIT_LOSS_SETTINGS_NOT_EXISTS = new ErrorCode(1_052_105_015, "请先完成结转损益参数设置");
    ErrorCode CLOSING_SPECIAL_SETTINGS_INVALID = new ErrorCode(1_052_105_016, "专用结转设置不正确");
    ErrorCode CLOSING_SPECIAL_SUBJECT_NOT_EXISTS = new ErrorCode(1_052_105_017, "专用结转所需的预置科目不存在");
    ErrorCode CLOSING_INITIAL_BALANCE_UNBALANCED = new ErrorCode(1_052_105_018, "初始余额试算不平衡，不能结账");
    ErrorCode CLOSING_VOUCHER_NUMBER_DISCONTINUOUS = new ErrorCode(1_052_105_019,
            "当前期间存在断号凭证，请先整理凭证号");
    ErrorCode CLOSING_PROFIT_LOSS_VOUCHER_MISSING = new ErrorCode(1_052_105_020,
            "当前期间存在损益发生额，请先生成结转损益凭证");
    ErrorCode CLOSING_INCOME_STATEMENT_UNBALANCED = new ErrorCode(1_052_105_021,
            "利润表勾稽不平衡，不能结账");
    ErrorCode CLOSING_REPORT_SUBJECT_UNMAPPED = new ErrorCode(1_052_105_022,
            "存在未纳入报表公式的科目，不能结账");
    ErrorCode CLOSING_TEMPLATE_NOT_EXISTS = new ErrorCode(1_052_105_023, "结账模板不存在");
    ErrorCode CLOSING_TEMPLATE_PRESET_INVALID = new ErrorCode(1_052_105_024, "结账模板预置数据不正确");
    ErrorCode CLOSING_TEMPLATE_PRESET_SUBJECT_NOT_EXISTS = new ErrorCode(1_052_105_025,
            "结账模板【{}】所需科目【{}】不存在");
    ErrorCode CLOSING_SCHEME_PRESET_SUBJECT_NOT_EXISTS = new ErrorCode(1_052_105_026,
            "结账方案【{}】所需科目【{}】不存在");
    ErrorCode CLOSING_SCHEME_NOT_PERIOD_END = new ErrorCode(1_052_105_027,
            "该结账方案未启用期末结转");
    ErrorCode CLOSING_VOUCHER_NOT_POSTED = new ErrorCode(1_052_105_028, "存在未过账凭证，不能结账");
    ErrorCode CLOSING_YEAR_END_PROFIT_NOT_TRANSFERRED = new ErrorCode(1_052_105_029,
            "12 月结账前，请先在【期末结转方案】里生成“结转本年利润（年末）”凭证，把本年利润转入未分配利润");

    // ========== 报表 1-052-106-000 ==========
    ErrorCode REPORT_CONFIG_NOT_EXISTS = new ErrorCode(1_052_106_000, "报表项目不存在");
    ErrorCode REPORT_CONFIG_NOT_EDITABLE = new ErrorCode(1_052_106_001, "该报表项目不允许编辑公式");
    ErrorCode REPORT_FORMULA_SUBJECT_DUPLICATE = new ErrorCode(1_052_106_002, "报表公式科目不能重复");
    ErrorCode REPORT_FORMULA_RULE_INVALID = new ErrorCode(1_052_106_003, "报表公式取数规则不正确");
    ErrorCode REPORT_FORMULA_OPERATOR_INVALID = new ErrorCode(1_052_106_004, "报表公式运算符不正确");
    ErrorCode REPORT_ADJUSTMENT_NOT_EXISTS = new ErrorCode(1_052_106_005, "现金流量辅助数据不存在");
    ErrorCode REPORT_AMOUNT_NOT_ADJUSTABLE = new ErrorCode(1_052_106_006, "该现金流量表项目不允许调整金额");

    // ========== 币种 1-052-107-000 ==========
    ErrorCode CURRENCY_NOT_EXISTS = new ErrorCode(1_052_107_000, "币别不存在");
    ErrorCode CURRENCY_CODE_DUPLICATE = new ErrorCode(1_052_107_001, "币别编码已存在");
    ErrorCode CURRENCY_STANDARD_NOT_DELETABLE = new ErrorCode(1_052_107_002, "本位币不允许删除");
    ErrorCode CURRENCY_IN_USE = new ErrorCode(1_052_107_003, "币别已被科目使用，不能删除");
    ErrorCode CURRENCY_STANDARD_RATE_NOT_EDITABLE = new ErrorCode(1_052_107_004, "本位币汇率固定为 1，不需要设置汇率");
    ErrorCode CURRENCY_RATE_NOT_EXISTS = new ErrorCode(1_052_107_005, "汇率记录不存在");
    ErrorCode CURRENCY_IN_VOUCHER_USE = new ErrorCode(1_052_107_006, "币别已在凭证或外币期初中使用，不能删除");
    ErrorCode INITIAL_CURRENCY_BALANCE_SUBJECT_INVALID = new ErrorCode(1_052_107_007,
            "科目【{}】不是末级科目或未启用该币别的外币核算");
    ErrorCode EXCHANGE_ADJUST_NOTHING = new ErrorCode(1_052_107_008, "当前期间没有需要调汇的外币余额（汇兑差额都为 0）");
    ErrorCode EXCHANGE_ADJUST_SUBJECT_INVALID = new ErrorCode(1_052_107_009,
            "汇兑损益科目不存在、已停用或不是末级科目，请重新选择");
    ErrorCode EXCHANGE_ADJUST_RATE_INVALID = new ErrorCode(1_052_107_010, "请填写币别【{}】大于 0 的期末汇率");
    ErrorCode VOUCHER_CURRENCY_REQUIRED = new ErrorCode(1_052_107_011,
            "科目【{}】启用了外币核算，请选择币别并填写汇率和原币金额");
    ErrorCode VOUCHER_CURRENCY_NOT_ALLOWED = new ErrorCode(1_052_107_012, "科目【{}】未启用该币别的外币核算");
    ErrorCode VOUCHER_EXCHANGE_RATE_INVALID = new ErrorCode(1_052_107_013, "科目【{}】的汇率必须大于 0");
    ErrorCode VOUCHER_ORIGINAL_AMOUNT_MISMATCH = new ErrorCode(1_052_107_014,
            "科目【{}】的本位币金额应等于 原币 × 汇率（允许 0.01 尾差）");

    // ========== 首页 1-052-108-000 ==========
    ErrorCode HOME_METRIC_INVALID = new ErrorCode(1_052_108_001, "首页财务指标不正确");

    // ========== 出纳与银行对账 1-052-109-000 ==========
    ErrorCode CASH_BANK_SUBJECT_INVALID = new ErrorCode(1_052_109_000, "银行存款科目不存在或不是末级科目");
    ErrorCode CASH_SUBJECT_INVALID = new ErrorCode(1_052_109_001, "库存现金科目不存在或不是末级科目");
    ErrorCode BANK_STATEMENT_IMPORT_EMPTY = new ErrorCode(1_052_109_002, "导入的银行对账单没有有效数据");
    ErrorCode BANK_STATEMENT_IMPORT_ROW_INVALID = new ErrorCode(1_052_109_003, "第 {} 行数据格式不正确：{}");
    ErrorCode BANK_STATEMENT_NOT_EXISTS = new ErrorCode(1_052_109_004, "银行对账单不存在");
    ErrorCode CASH_ENTRY_NOT_EXISTS = new ErrorCode(1_052_109_005, "凭证分录不存在或不属于该账套");
    ErrorCode CASH_ENTRY_SUBJECT_INVALID = new ErrorCode(1_052_109_006, "该分录科目不是库存现金或银行存款，不能出纳勾对");
    ErrorCode BANK_RECONCILE_PERIOD_CLOSED = new ErrorCode(1_052_109_007, "该会计期间已结账，不能修改对账状态");
    ErrorCode BANK_RECONCILE_NOT_BALANCED = new ErrorCode(1_052_109_008, "银行余额调节表不平衡，调节后余额两边相差 {} 元");
    ErrorCode BANK_STATEMENT_DUPLICATE = new ErrorCode(1_052_109_009, "银行流水号 {} 已导入，不能重复导入");

    // ========== 成本核算 1-052-110-000 ==========
    ErrorCode COST_SETTING_NOT_CONFIGURED = new ErrorCode(1_052_110_000, "请先在【成本参数】里设置成本科目");
    ErrorCode COST_SUBJECT_INVALID = new ErrorCode(1_052_110_001, "成本参数中的科目【{}】不存在、已停用或不是末级科目");
    ErrorCode COST_PERIOD_CLOSED = new ErrorCode(1_052_110_002, "该会计期间已结账，不能计算成本或生成成本凭证");
    ErrorCode COST_SHEET_VOUCHER_GENERATED = new ErrorCode(1_052_110_003, "本期成本凭证已生成，请先撤销凭证再重新计算");
    ErrorCode COST_SHEET_NOT_EXISTS = new ErrorCode(1_052_110_004, "本期还没有成本计算单，请先计算成本");
    ErrorCode COST_OVERHEAD_NOT_ALLOCATED = new ErrorCode(1_052_110_005,
            "本期制造费用 {} 元还没有分摊完（已分摊 {} 元），请先填写各产品的分摊依据后重新计算成本");
    ErrorCode COST_SALES_QTY_EXCEED = new ErrorCode(1_052_110_006, "产品【{}】销售数量 {} 超过可售数量 {}");
    ErrorCode COST_NOTHING_TO_GENERATE = new ErrorCode(1_052_110_007, "本期没有可生成凭证的成本数据");
    ErrorCode COST_PERIOD_INVALID = new ErrorCode(1_052_110_008, "会计期间格式不正确，应为 yyyy-MM");
    ErrorCode COST_VOUCHER_NOT_GENERATED = new ErrorCode(1_052_110_009, "本期还没有生成成本凭证");
    ErrorCode COST_PRODUCT_AUX_TYPE_INVALID = new ErrorCode(1_052_110_010, "产品辅助核算类别不存在，请在成本参数中重新选择");
    ErrorCode COST_SUBJECT_AUX_REQUIRED = new ErrorCode(1_052_110_011,
            "科目【{}】需要先启用【{}】辅助核算，才能按产品归集成本");
    ErrorCode COST_REPORT_RANGE_INVALID = new ErrorCode(1_052_110_012, "开始月份不能晚于结束月份");
    ErrorCode COST_REPORT_RANGE_TOO_LONG = new ErrorCode(1_052_110_013, "成本报表一次最多查询 36 个月");

    // ========== 往来核销与账龄 1-052-111-000 ==========
    ErrorCode SETTLE_AUX_TYPE_INVALID = new ErrorCode(1_052_111_000, "只有客户、供应商、职员类的辅助核算可以做往来核销和账龄分析");
    ErrorCode SETTLE_SUBJECT_INVALID = new ErrorCode(1_052_111_001, "科目【{}】没有启用对应的客户、供应商或职员辅助核算，不能做往来核销");
    ErrorCode SETTLE_ITEM_EMPTY = new ErrorCode(1_052_111_002, "请至少选择一笔借方和一笔贷方");
    ErrorCode SETTLE_AMOUNT_NOT_EQUAL = new ErrorCode(1_052_111_003, "借方核销合计 {} 元与贷方核销合计 {} 元不相等");
    ErrorCode SETTLE_AMOUNT_INVALID = new ErrorCode(1_052_111_004, "核销金额必须大于 0");
    ErrorCode SETTLE_AMOUNT_EXCEED = new ErrorCode(1_052_111_005, "【{}】可核销余额只有 {} 元，本次核销 {} 元超出");
    ErrorCode SETTLE_REF_NOT_FOUND = new ErrorCode(1_052_111_006, "核销项目 {} 不存在、方向不对或已经没有可核销余额，请刷新后重试");
    ErrorCode SETTLE_BATCH_NOT_EXISTS = new ErrorCode(1_052_111_007, "核销记录不存在或已被撤销");
    ErrorCode SETTLE_MONTH_INVALID = new ErrorCode(1_052_111_008, "截止月份格式不正确，应为 yyyy-MM");
    ErrorCode SETTLE_BUCKET_INVALID = new ErrorCode(1_052_111_009, "账龄区间格式不正确，应为从小到大的天数，用逗号隔开，例如 30,60,90,180,365");
    ErrorCode SETTLE_ITEM_NOT_EXISTS = new ErrorCode(1_052_111_010, "往来单位不存在，请重新选择");
    ErrorCode SETTLE_ENTRY_SETTLED = new ErrorCode(1_052_111_011, "这张凭证里有分录已经做过往来核销，请先到【往来核销】撤销核销，再修改、删除或作废");
    ErrorCode SETTLE_DATE_INVALID = new ErrorCode(1_052_111_012, "核销日期不能早于所选凭证中最晚的日期 {}");

    // ========== 固定资产 1-052-112-000 ==========
    ErrorCode ASSET_CATEGORY_CODE_DUPLICATE = new ErrorCode(1_052_112_000, "类别编码 {} 已存在");
    ErrorCode ASSET_CATEGORY_NOT_EXISTS = new ErrorCode(1_052_112_001, "资产类别不存在");
    ErrorCode ASSET_CATEGORY_USED = new ErrorCode(1_052_112_002, "类别【{}】下还有资产卡片，不能删除");
    ErrorCode ASSET_CATEGORY_SUBJECT_INVALID = new ErrorCode(1_052_112_003, "【{}】科目不存在、已停用或不是末级科目");
    ErrorCode ASSET_CODE_DUPLICATE = new ErrorCode(1_052_112_004, "资产编号 {} 已存在");
    ErrorCode ASSET_NOT_EXISTS = new ErrorCode(1_052_112_005, "资产卡片不存在");
    ErrorCode ASSET_VALUE_INVALID = new ErrorCode(1_052_112_006, "原值必须大于 0，净残值率必须在 0 到 100 之间，折旧月数必须大于 0");
    ErrorCode ASSET_INIT_INVALID = new ErrorCode(1_052_112_007, "已折旧月数不能超过折旧月数，已提累计折旧不能超过“原值 - 净残值”");
    ErrorCode ASSET_DISPOSED = new ErrorCode(1_052_112_008, "资产【{}】已处置，不能再修改、删除或计提折旧");
    ErrorCode ASSET_HAS_DEPRECIATION = new ErrorCode(1_052_112_009, "资产【{}】已经计提过折旧，类别、启用日期、原值、净残值率、折旧月数、已折旧情况不能再修改");
    ErrorCode ASSET_DELETE_HAS_DEPRECIATION = new ErrorCode(1_052_112_010, "资产【{}】已有折旧记录，不能删除");
    ErrorCode ASSET_PERIOD_INVALID = new ErrorCode(1_052_112_011, "会计期间格式不正确，应为 yyyy-MM");
    ErrorCode ASSET_PERIOD_BEFORE_START = new ErrorCode(1_052_112_012, "{} 早于账套启用月份 {}，不能计提折旧");
    ErrorCode ASSET_DEPRECIATION_GENERATED = new ErrorCode(1_052_112_013, "{} 的折旧已经计提，请先撤销再重新计提");
    ErrorCode ASSET_DEPRECIATION_PREV_MISSING = new ErrorCode(1_052_112_014, "请先计提 {} 的折旧（折旧要一个月一个月往后做）");
    ErrorCode ASSET_DEPRECIATION_NOTHING = new ErrorCode(1_052_112_015, "{} 没有需要计提折旧的资产");
    ErrorCode ASSET_DEPRECIATION_NOT_GENERATED = new ErrorCode(1_052_112_016, "{} 还没有计提折旧");
    ErrorCode ASSET_DEPRECIATION_REVOKE_DISPOSED = new ErrorCode(1_052_112_017, "资产【{}】已经处置，请先撤销处置，再撤销这个月的折旧");
    ErrorCode ASSET_DEPRECIATION_REVOKE_NEWER = new ErrorCode(1_052_112_018, "只能从最近计提的月份往前撤销，请先撤销 {} 的折旧");
    ErrorCode ASSET_DISPOSE_DEPR_MISSING = new ErrorCode(1_052_112_019, "请先计提 {} 的折旧再处置资产（当月减少的资产当月仍计提折旧）");
    ErrorCode ASSET_DISPOSE_DATE_INVALID = new ErrorCode(1_052_112_020, "处置日期不能早于启用日期 {}");
    ErrorCode ASSET_DISPOSE_LATER_DEPR = new ErrorCode(1_052_112_021, "资产在 {} 之后还有折旧记录，请先撤销之后月份的折旧再处置");
    ErrorCode ASSET_CLEARING_SUBJECT_MISSING = new ErrorCode(1_052_112_022, "没有找到“固定资产清理”科目（编码 1606），请先在科目设置里添加");
    ErrorCode ASSET_NOT_DISPOSED = new ErrorCode(1_052_112_023, "资产【{}】还没有处置");
    ErrorCode ASSET_DEPRECIATION_NO_EXPENSE = new ErrorCode(1_052_112_024, "类别【{}】的折旧科目设置不完整，请先到类别管理里补全");
    ErrorCode ASSET_START_TOO_EARLY = new ErrorCode(1_052_112_025, "这个资产应当从 {} 开始折旧，但 {} 的折旧已经计提过了。请先撤销 {} 及之后月份的折旧再保存；旧资产请把启用日期照实填，用“进入系统前已折旧月数 / 累计折旧”录入已提部分");

    // ========== 发票与增值税 1-052-113-000 ==========
    ErrorCode INVOICE_NOT_EXISTS = new ErrorCode(1_052_113_000, "发票不存在");
    ErrorCode INVOICE_DUPLICATE = new ErrorCode(1_052_113_001, "同一方向下发票代码 {} 号码 {} 已经登记过了");
    ErrorCode INVOICE_VALUE_INVALID = new ErrorCode(1_052_113_002, "不含税金额不能为 0，税率必须在 0 到 100 之间");
    ErrorCode INVOICE_TAX_MISMATCH = new ErrorCode(1_052_113_003, "税额和“金额 × 税率”相差超过 0.10 元，按税率算应为 {}，请核对");
    ErrorCode INVOICE_VOIDED = new ErrorCode(1_052_113_004, "发票 {} 已作废，不能修改，请先恢复");
    ErrorCode INVOICE_CERTIFY_NOT_APPLICABLE = new ErrorCode(1_052_113_005, "只有正常状态的进项增值税专用发票才需要认证抵扣：{}");
    ErrorCode INVOICE_CERTIFY_PERIOD_INVALID = new ErrorCode(1_052_113_006, "认证所属期格式应为 yyyy-MM，且不能早于开票月份 {}");
    ErrorCode INVOICE_PERIOD_LOCKED = new ErrorCode(1_052_113_007, "这张发票影响 {} 的增值税，而 {} 已经生成了税务凭证，请先到“增值税申报”撤销它");
    ErrorCode INVOICE_NOT_VOIDED = new ErrorCode(1_052_113_008, "发票 {} 没有作废，不需要恢复");
    ErrorCode TAX_PERIOD_INVALID = new ErrorCode(1_052_113_009, "税款所属期格式不正确，应为 yyyy-MM");
    ErrorCode TAX_SETTING_INVALID = new ErrorCode(1_052_113_010, "税率必须在 0 到 100 之间，留抵税额不能为负数，开始月份格式应为 yyyy-MM");
    ErrorCode TAX_SUBJECT_MISSING = new ErrorCode(1_052_113_011, "没有找到科目 {}（{}），请先在科目设置里添加");
    ErrorCode TAX_GENERATED = new ErrorCode(1_052_113_012, "{} 的税务凭证已经生成，请先撤销再重新生成");
    ErrorCode TAX_NOT_GENERATED = new ErrorCode(1_052_113_013, "{} 还没有生成税务凭证");
    ErrorCode TAX_NOTHING_TO_GENERATE = new ErrorCode(1_052_113_014, "{} 没有需要结转的增值税和附加税（应纳税额为 0，可能是留抵或没有发票）");
    ErrorCode TAX_PERIOD_BEFORE_START = new ErrorCode(1_052_113_015, "{} 早于开始月份 {}，请先到“税务参数”里调整开始月份");
    ErrorCode TAX_PERIOD_TOO_FAR = new ErrorCode(1_052_113_016, "开始月份到 {} 超过 120 个月，请把税务参数里的开始月份调近一点");

}
