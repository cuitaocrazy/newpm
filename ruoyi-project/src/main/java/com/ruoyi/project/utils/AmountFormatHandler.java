package com.ruoyi.project.utils;

import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Workbook;
import com.ruoyi.common.utils.poi.ExcelHandlerAdapter;

/**
 * 数值格式化处理器（导出 Excel 用）
 *
 * 默认（无 args）：格式 #,##0.00，千分位 + 两位小数
 * args = {"0.000"}：格式 0.000，无千分位 + 三位小数（适用于人天）
 * args = {"0.00"} ：格式 0.00， 无千分位 + 两位小数
 *
 * 使用字符串格式而非内置 ID，避免中文版 Excel 将 numFmtId=4 映射为货币分类。
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
            // 确定格式：args[0] 优先，默认为千分位两位小数
            // args = {"id:N"}：使用内置格式 ID N（如 id:1 = 整数数值分类）
            // args = {"0.000"}：自定义格式字符串
            String arg0 = (args != null && args.length > 0 && !args[0].isEmpty()) ? args[0] : "#,##0.00";
            // 写入数值
            cell.setCellValue(amount.doubleValue());
            // 克隆现有样式并应用格式
            CellStyle newStyle = wb.createCellStyle();
            newStyle.cloneStyleFrom(cell.getCellStyle());
            if (arg0.startsWith("id:"))
            {
                // 使用内置格式 ID，Excel 能将其识别为标准分类（数值/货币等）
                short builtinId = Short.parseShort(arg0.substring(3));
                newStyle.setDataFormat(builtinId);
            }
            else
            {
                DataFormat dataFormat = wb.createDataFormat();
                newStyle.setDataFormat(dataFormat.getFormat(arg0));
            }
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
