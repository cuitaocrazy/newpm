package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.Attachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 附件Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@Mapper
public interface AttachmentMapper
{
    /**
     * 查询附件
     *
     * @param attachmentId 附件主键
     * @return 附件
     */
    public Attachment selectAttachmentByAttachmentId(Long attachmentId);

    /**
     * 查询附件列表
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 附件集合
     */
    public List<Attachment> selectAttachmentList(String businessType, Long businessId);

    /**
     * 新增附件
     *
     * @param attachment 附件
     * @return 结果
     */
    public int insertAttachment(Attachment attachment);

    /**
     * 修改附件
     *
     * @param attachment 附件
     * @return 结果
     */
    public int updateAttachment(Attachment attachment);

    /**
     * 删除附件
     *
     * @param attachmentId 附件主键
     * @return 结果
     */
    public int deleteAttachmentByAttachmentId(Long attachmentId);

    /**
     * 批量删除附件
     *
     * @param attachmentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAttachmentByAttachmentIds(Long[] attachmentIds);

    /**
     * 根据业务类型和业务ID逻辑删除附件
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 结果
     */
    public int deleteAttachmentByBusinessId(String businessType, Long businessId);

    /**
     * 统计业务附件数量
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 附件数量
     */
    public int countByBusinessTypeAndId(String businessType, Long businessId);

    /**
     * 统计业务附件数量（别名方法）
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 附件数量
     */
    default int countAttachmentByBusiness(String businessType, Long businessId) {
        return countByBusinessTypeAndId(businessType, businessId);
    }
}
