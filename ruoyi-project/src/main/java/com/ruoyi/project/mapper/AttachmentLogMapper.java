package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AttachmentLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 附件操作日志Mapper接口
 *
 * @author ruoyi
 * @date 2026-02-01
 */
@Mapper
public interface AttachmentLogMapper
{
    /**
     * 查询附件操作日志列表
     *
     * @param attachmentLog 附件操作日志
     * @return 附件操作日志集合
     */
    public List<AttachmentLog> selectAttachmentLogList(AttachmentLog attachmentLog);

    /**
     * 新增附件操作日志
     *
     * @param attachmentLog 附件操作日志
     * @return 结果
     */
    public int insertAttachmentLog(AttachmentLog attachmentLog);
}
