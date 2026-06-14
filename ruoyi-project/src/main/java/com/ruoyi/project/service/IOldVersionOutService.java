package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.OldVersionOut;

/**
 * 出入库版本旧数据查询 Service（纯只读）
 *
 * @author yadapm-migrate
 */
public interface IOldVersionOutService
{
    /** 查询旧版本归档列表 */
    List<OldVersionOut> selectOldVersionOutList(OldVersionOut oldVersionOut);

    /** 投产批次号下拉 */
    List<String> selectProBatchNoOptions();

    /** 子产品下拉 */
    List<String> selectProductOptions();

    /** 版本类型下拉 */
    List<String> selectVersionTypeOptions();
}
