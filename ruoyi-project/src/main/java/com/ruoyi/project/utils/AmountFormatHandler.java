package com.ruoyi.project.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import com.ruoyi.common.utils.poi.ExcelHandlerAdapter;

/**
 * 金额千分位格式化处理器（导出 Excel 用）
 * 输出格式: 1,234,567.89
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
            DecimalFormat df = new DecimalFormat("#,##0.00");
            return df.format(amount);
        }
        catch (Exception e)
        {
            return value.toString();
        }
    }
}
