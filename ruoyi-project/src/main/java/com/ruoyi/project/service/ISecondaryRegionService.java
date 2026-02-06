package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.SecondaryRegion;

/**
 * 省级区域Service接口
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
public interface ISecondaryRegionService 
{
    /**
     * 查询省级区域
     * 
     * @param provinceId 省级区域主键
     * @return 省级区域
     */
    public SecondaryRegion selectSecondaryRegionByProvinceId(Long provinceId);

    /**
     * 查询省级区域列表
     * 
     * @param secondaryRegion 省级区域
     * @return 省级区域集合
     */
    public List<SecondaryRegion> selectSecondaryRegionList(SecondaryRegion secondaryRegion);

    /**
     * 新增省级区域
     * 
     * @param secondaryRegion 省级区域
     * @return 结果
     */
    public int insertSecondaryRegion(SecondaryRegion secondaryRegion);

    /**
     * 修改省级区域
     * 
     * @param secondaryRegion 省级区域
     * @return 结果
     */
    public int updateSecondaryRegion(SecondaryRegion secondaryRegion);

    /**
     * 批量删除省级区域
     * 
     * @param provinceIds 需要删除的省级区域主键集合
     * @return 结果
     */
    public int deleteSecondaryRegionByProvinceIds(Long[] provinceIds);

    /**
     * 删除省级区域信息
     * 
     * @param provinceId 省级区域主键
     * @return 结果
     */
    public int deleteSecondaryRegionByProvinceId(Long provinceId);
}
