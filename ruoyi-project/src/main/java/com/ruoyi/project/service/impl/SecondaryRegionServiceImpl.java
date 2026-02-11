package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.SecondaryRegionMapper;
import com.ruoyi.project.domain.SecondaryRegion;
import com.ruoyi.project.service.ISecondaryRegionService;

/**
 * 二级区域Service业务层处理
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
     * 查询二级区域
     *
     * @param regionId 二级区域主键
     * @return 二级区域
     */
    @Override
    public SecondaryRegion selectSecondaryRegionByRegionId(Long regionId)
    {
        return secondaryRegionMapper.selectSecondaryRegionByRegionId(regionId);
    }

    /**
     * 查询二级区域列表
     *
     * @param secondaryRegion 二级区域
     * @return 二级区域
     */
    @Override
    public List<SecondaryRegion> selectSecondaryRegionList(SecondaryRegion secondaryRegion)
    {
        return secondaryRegionMapper.selectSecondaryRegionList(secondaryRegion);
    }

    /**
     * 新增二级区域
     *
     * @param secondaryRegion 二级区域
     * @return 结果
     */
    @Override
    public int insertSecondaryRegion(SecondaryRegion secondaryRegion)
    {
        secondaryRegion.setCreateTime(DateUtils.getNowDate());
        return secondaryRegionMapper.insertSecondaryRegion(secondaryRegion);
    }

    /**
     * 修改二级区域
     *
     * @param secondaryRegion 二级区域
     * @return 结果
     */
    @Override
    public int updateSecondaryRegion(SecondaryRegion secondaryRegion)
    {
        secondaryRegion.setUpdateTime(DateUtils.getNowDate());
        return secondaryRegionMapper.updateSecondaryRegion(secondaryRegion);
    }

    /**
     * 批量删除二级区域
     *
     * @param regionIds 需要删除的二级区域主键
     * @return 结果
     */
    @Override
    public int deleteSecondaryRegionByRegionIds(Long[] regionIds)
    {
        return secondaryRegionMapper.deleteSecondaryRegionByRegionIds(regionIds);
    }

    /**
     * 删除二级区域信息
     *
     * @param regionId 二级区域主键
     * @return 结果
     */
    @Override
    public int deleteSecondaryRegionByRegionId(Long regionId)
    {
        return secondaryRegionMapper.deleteSecondaryRegionByRegionId(regionId);
    }
}
