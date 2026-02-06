package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AttachmentLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 附件操作日志Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@Mapper
public interface AttachmentLogMapper
{
    /**
     * 查询附件操作日志
     *
     * @param logId 附件操作日志主键
     * @return 附件操作日志
     */
    public AttachmentLog selectAttachmentLogByLogId(Long logId);

    /**
     * 查询附件操作日志列表
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 附件操作日志集合
     */
    public List<AttachmentLog> selectAttachmentLogList(String businessType, Long businessId);

    /**
     * 新增附件操作日志
     *
     * @param attachmentLog 附件操作日志
     * @return 结果
     */
    public int insertAttachmentLog(AttachmentLog attachmentLog);

    /**
     * 修改附件操作日志
     *
     * @param attachmentLog 附件操作日志
     * @return 结果
     */
    public int updateAttachmentLog(AttachmentLog attachmentLog);

    /**
     * 删除附件操作日志
     *
     * @param logId 附件操作日志主键
     * @return 结果
     */
    public int deleteAttachmentLogByLogId(Long logId);

    /**
     * 批量删除附件操作日志
     *
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAttachmentLogByLogIds(Long[] logIds);
}
