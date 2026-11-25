package cn.iocoder.yudao.module.fms.controller.admin.cash;

import cn.iocoder.yudao.module.fms.controller.admin.cash.vo.FmsBankStatementImportVO;
import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.excel.core.util.PoiExcelUtils.getCellText;
import static cn.iocoder.yudao.framework.excel.core.util.PoiExcelUtils.getDecimal;
import static cn.iocoder.yudao.framework.excel.core.util.PoiExcelUtils.isEmptyRow;

/**
 * FMS 银行对账单导入 Excel 辅助类
 * <p>
 * 标准格式（第 1 行为表头，从第 2 行开始）：日期、摘要、收入、支出、余额、流水号
 *
 * @author 芋道源码
 */
@UtilityClass
public final class FmsBankStatementImportExcelHelper {

    public static List<FmsBankStatementImportVO> read(MultipartFile file) throws IOException {
        List<FmsBankStatementImportVO> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, evaluator, Arrays.asList(0, 1, 2, 3, 4, 5))) {
                    continue;
                }
                FmsBankStatementImportVO vo = new FmsBankStatementImportVO();
                vo.setStatementDate(parseDate(getCellText(row, 0, evaluator)));
                vo.setSummary(getCellText(row, 1, evaluator));
                vo.setDebitAmount(zeroIfNullDecimal(getDecimal(row, 2, evaluator)));
                vo.setCreditAmount(zeroIfNullDecimal(getDecimal(row, 3, evaluator)));
                vo.setBalance(getDecimal(row, 4, evaluator));
                vo.setReferenceNo(getCellText(row, 5, evaluator));
                result.add(vo);
            }
        }
        return result;
    }

    private static BigDecimal zeroIfNullDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim().replace('/', '-').substring(0, 10));
    }

}
