package com.ruoyi.project.utils;

import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import com.ruoyi.common.utils.poi.ExcelHandlerAdapter;

/**
 * 金额数值格式化处理器（导出 Excel 用）
 * 写入真实数值，单元格格式 #,##0.00（千分位，保留两位小数）
 */
public class AmountFormatHandler implements ExcelHandlerAdapter
{
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb)
    {
        if (value == null)
        {
            return "";
        }
        try
        {
            BigDecimal amount;
            if (value instanceof BigDecimal)
            {
                amount = (BigDecimal) value;
            }
            else
            {
                amount = new BigDecimal(value.toString());
            }
            // 写入数值
            cell.setCellValue(amount.doubleValue());
            // 克隆现有样式，使用 Excel 内置格式索引 4（#,##0.00），确保分类显示为"数值"而非"自定义"
            CellStyle newStyle = wb.createCellStyle();
            newStyle.cloneStyleFrom(cell.getCellStyle());
            newStyle.setDataFormat((short) 4);
            cell.setCellStyle(newStyle);
            // 返回 null：通知 ExcelUtil 跳过 setCellValue，避免覆盖已写入的数值
            return null;
        }
        catch (Exception e)
        {
            return value.toString();
        }
    }
}
