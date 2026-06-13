package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.OldVersionOut;

/**
 * 出入库版本旧数据查询 Mapper（纯只读）
 *
 * @author yadapm-migrate
 */
public interface OldVersionOutMapper
{
    /**
     * 查询旧版本归档列表（任务编号模糊 + 投产批次号/子产品/版本类型精确）
     */
    List<OldVersionOut> selectOldVersionOutList(OldVersionOut oldVersionOut);

    /** 投产批次号下拉（distinct，非空） */
    List<String> selectProBatchNoOptions();

    /** 子产品下拉（distinct，非空） */
    List<String> selectProductOptions();

    /** 版本类型下拉（distinct，非空） */
    List<String> selectVersionTypeOptions();
}
