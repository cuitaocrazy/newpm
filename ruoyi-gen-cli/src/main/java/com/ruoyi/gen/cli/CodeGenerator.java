package com.ruoyi.gen.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;
import com.ruoyi.generator.util.VelocityInitializer;
import com.ruoyi.generator.util.VelocityUtils;

/**
 * 代码生成器
 * <p>
 * 复用现有 Velocity 模板渲染逻辑，将 GenTable 渲染为代码文件并输出到 ZIP。
 */
public class CodeGenerator
{
    private static final Logger log = LoggerFactory.getLogger(CodeGenerator.class);

    /**
     * 为多张表生成代码并写入 ZIP 文件
     *
     * @param tables 已解析的 GenTable 列表
     * @param outputPath ZIP 输出路径
     */
    public static void generateToZip(List<GenTable> tables, String outputPath) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bos);

        for (GenTable table : tables)
        {
            generateTable(table, zip);
        }

        IOUtils.closeQuietly(zip);

        // 写入文件
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists())
        {
            parentDir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(outputFile))
        {
            fos.write(bos.toByteArray());
        }
    }

    /**
     * 为单张表生成代码写入 ZIP
     */
    private static void generateTable(GenTable table, ZipOutputStream zip)
    {
        // 设置主键列
        setPkColumn(table);

        // 初始化 Velocity
        VelocityInitializer.initVelocity();

        // 构建模板上下文
        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table.getTplCategory(), table.getTplWebType());

        for (String template : templates)
        {
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            try
            {
                String fileName = VelocityUtils.getFileName(template, table);
                zip.putNextEntry(new ZipEntry(fileName));
                IOUtils.write(sw.toString(), zip, Constants.UTF8);
                IOUtils.closeQuietly(sw);
                zip.flush();
                zip.closeEntry();
            }
            catch (IOException e)
            {
                log.error("渲染模板失败，表名：" + table.getTableName(), e);
            }
        }
    }

    /**
     * 设置主键列信息（复用 GenTableServiceImpl 中的逻辑）
     */
    private static void setPkColumn(GenTable table)
    {
        for (GenTableColumn column : table.getColumns())
        {
            if (column.isPk())
            {
                table.setPkColumn(column);
                break;
            }
        }
        if (table.getPkColumn() == null && !table.getColumns().isEmpty())
        {
            table.setPkColumn(table.getColumns().get(0));
        }
    }
}
