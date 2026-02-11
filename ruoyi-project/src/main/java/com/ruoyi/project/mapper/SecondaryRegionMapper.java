package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.SecondaryRegion;

/**
 * 二级区域Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-04
 */
public interface SecondaryRegionMapper
{
    /**
     * 查询二级区域
     *
     * @param regionId 二级区域主键
     * @return 二级区域
     */
    public SecondaryRegion selectSecondaryRegionByRegionId(Long regionId);

    /**
     * 查询二级区域列表
     *
     * @param secondaryRegion 二级区域
     * @return 二级区域集合
     */
    public List<SecondaryRegion> selectSecondaryRegionList(SecondaryRegion secondaryRegion);

    /**
     * 新增二级区域
     *
     * @param secondaryRegion 二级区域
     * @return 结果
     */
    public int insertSecondaryRegion(SecondaryRegion secondaryRegion);

    /**
     * 修改二级区域
     *
     * @param secondaryRegion 二级区域
     * @return 结果
     */
    public int updateSecondaryRegion(SecondaryRegion secondaryRegion);

    /**
     * 删除二级区域
     *
     * @param regionId 二级区域主键
     * @return 结果
     */
    public int deleteSecondaryRegionByRegionId(Long regionId);

    /**
     * 批量删除二级区域
     *
     * @param regionIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSecondaryRegionByRegionIds(Long[] regionIds);
}
