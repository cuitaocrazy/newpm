package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.Attachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 附件管理Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-01
 */
@Mapper
public interface AttachmentMapper
{
    /**
     * 查询附件管理列表
     *
     * @param attachment 附件管理
     * @return 附件管理集合
     */
    public List<Attachment> selectAttachmentList(Attachment attachment);

    /**
     * 查询附件管理
     *
     * @param attachmentId 附件管理主键
     * @return 附件管理
     */
    public Attachment selectAttachmentByAttachmentId(Long attachmentId);

    /**
     * 新增附件管理
     *
     * @param attachment 附件管理
     * @return 结果
     */
    public int insertAttachment(Attachment attachment);

    /**
     * 修改附件管理
     *
     * @param attachment 附件管理
     * @return 结果
     */
    public int updateAttachment(Attachment attachment);

    /**
     * 删除附件管理
     *
     * @param attachmentId 附件管理主键
     * @return 结果
     */
    public int deleteAttachmentByAttachmentId(Long attachmentId);
}
