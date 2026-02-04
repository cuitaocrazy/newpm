package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.SecondaryRegionMapper;
import com.ruoyi.project.domain.SecondaryRegion;
import com.ruoyi.project.service.ISecondaryRegionService;

/**
 * 省级区域Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-02-04
 */
@Service
public class SecondaryRegionServiceImpl implements ISecondaryRegionService 
{
    @Autowired
    private SecondaryRegionMapper secondaryRegionMapper;

    /**
     * 查询省级区域
     * 
     * @param provinceId 省级区域主键
     * @return 省级区域
     */
    @Override
    public SecondaryRegion selectSecondaryRegionByProvinceId(Long provinceId)
    {
        return secondaryRegionMapper.selectSecondaryRegionByProvinceId(provinceId);
    }

    /**
     * 查询省级区域列表
     * 
     * @param secondaryRegion 省级区域
     * @return 省级区域
     */
    @Override
    public List<SecondaryRegion> selectSecondaryRegionList(SecondaryRegion secondaryRegion)
    {
        return secondaryRegionMapper.selectSecondaryRegionList(secondaryRegion);
    }

    /**
     * 新增省级区域
     * 
     * @param secondaryRegion 省级区域
     * @return 结果
     */
    @Override
    public int insertSecondaryRegion(SecondaryRegion secondaryRegion)
    {
        secondaryRegion.setCreateTime(DateUtils.getNowDate());
        return secondaryRegionMapper.insertSecondaryRegion(secondaryRegion);
    }

    /**
     * 修改省级区域
     * 
     * @param secondaryRegion 省级区域
     * @return 结果
     */
    @Override
    public int updateSecondaryRegion(SecondaryRegion secondaryRegion)
    {
        secondaryRegion.setUpdateTime(DateUtils.getNowDate());
        return secondaryRegionMapper.updateSecondaryRegion(secondaryRegion);
    }

    /**
     * 批量删除省级区域
     * 
     * @param provinceIds 需要删除的省级区域主键
     * @return 结果
     */
    @Override
    public int deleteSecondaryRegionByProvinceIds(Long[] provinceIds)
    {
        return secondaryRegionMapper.deleteSecondaryRegionByProvinceIds(provinceIds);
    }

    /**
     * 删除省级区域信息
     * 
     * @param provinceId 省级区域主键
     * @return 结果
     */
    @Override
    public int deleteSecondaryRegionByProvinceId(Long provinceId)
    {
        return secondaryRegionMapper.deleteSecondaryRegionByProvinceId(provinceId);
    }
}
