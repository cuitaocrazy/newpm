package com.ruoyi.project.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.ProjectContractRel;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.mapper.ProjectContractRelMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.mapper.ContractMapper;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.service.IContractService;

/**
 * 合同管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-02-03
 */
@Service
public class ContractServiceImpl implements IContractService
{
    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);

    /** 合同编号中表示「未填写」的字面值（存量数据里有人这么填） */
    private static final String CONTRACT_CODE_NONE = "无";

    /**
     * 合同编号唯一索引名，见 pm-sql/init/00_tables_ddl.sql 与
     * pm-sql/fix_contract_code_unique_20260806.sql。
     * 用于从 DuplicateKeyException 中辨认出「确实是编号撞了」，改名时两处必须同步。
     */
    private static final String UK_CONTRACT_CODE_NORM = "uk_contract_code_norm";

    /**
     * 合同编号重复的统一提示前缀。
     *
     * <p>新增拦截、编辑拦截、唯一索引 1062 兜底三处共用同一句话——用户无需（也无法）
     * 区分自己撞的是哪一层。提示只带被占用的编号本身，<b>不带冲突方的合同名称</b>：
     * 判重刻意绕过 {@code @DataScope}，带出对方合同名等于跨部门信息泄露。
     */
    private static final String CONTRACT_CODE_DUPLICATE_PREFIX = "合同编号已存在：";

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ProjectContractRelMapper projectContractRelMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private com.ruoyi.project.mapper.PaymentMapper paymentMapper;

    @Autowired
    private com.ruoyi.project.mapper.AttachmentMapper attachmentMapper;

    /**
     * 查询合同管理
     *
     * @param contractId 合同管理主键
     * @return 合同管理
     */
    @Override
    public Contract selectContractByContractId(Long contractId)
    {
        Contract contract = contractMapper.selectContractByContractId(contractId);
        if (contract != null) {
            // 查询关联项目列表
            List<Project> projectList = projectMapper.selectProjectListByContractId(contractId);
            contract.setProjectList(projectList);

            // 提取项目ID列表（用于编辑页面回显）
            if (projectList != null && !projectList.isEmpty()) {
                List<Long> projectIds = new java.util.ArrayList<>();
                for (Project project : projectList) {
                    projectIds.add(project.getProjectId());
                }
                contract.setProjectIds(projectIds);

                // 附加团队收入确认明细
                List<Map<String, Object>> details = projectMapper.selectTeamConfirmDetailsByIds(projectIds);
                Map<Long, List<Map<String, Object>>> detailMap = details.stream()
                    .collect(Collectors.groupingBy(d -> Long.parseLong(d.get("projectId").toString())));
                for (Project project : projectList) {
                    if (project.getProjectId() != null) {
                        List<Map<String, Object>> confirmList = detailMap.getOrDefault(project.getProjectId(), Collections.emptyList());
                        project.setTeamConfirmList(confirmList);
                        String depts = confirmList.stream()
                            .map(d -> (String) d.get("deptName"))
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .collect(Collectors.joining("、"));
                        project.setTeamConfirmDepts(depts.isEmpty() ? null : depts);
                    }
                }
            }

            // 查询创建人和更新人姓名
            if (contract.getCreateBy() != null) {
                SysUser createUser = userMapper.selectUserByUserName(contract.getCreateBy());
                if (createUser != null) {
                    contract.setCreateByName(createUser.getNickName());
                }
            }
            if (contract.getUpdateBy() != null) {
                SysUser updateUser = userMapper.selectUserByUserName(contract.getUpdateBy());
                if (updateUser != null) {
                    contract.setUpdateByName(updateUser.getNickName());
                }
            }
        }
        return contract;
    }

    /**
     * 查询合同管理列表
     *
     * @param contract 合同管理
     * @return 合同管理
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u1")
    public List<Contract> selectContractList(Contract contract)
    {
        List<Contract> contractList = contractMapper.selectContractList(contract);
        // 为每个合同查询关联项目列表
        for (Contract c : contractList) {
            List<Project> projectList = projectMapper.selectProjectListByContractId(c.getContractId());
            c.setProjectList(projectList);
        }
        return contractList;
    }

    /**
     * 查询合同金额总计
     *
     * @param contract 查询条件
     * @return 总计数据
     */
    @Override
    public Map<String, BigDecimal> selectContractSummary(Contract contract)
    {
        return contractMapper.selectContractSummary(contract);
    }

    /**
     * 新增合同管理
     *
     * @param contract 合同管理
     * @return 结果
     */
    @Override
    @Transactional
    public int insertContract(Contract contract)
    {
        // 检查项目是否已有关联合同（一个项目只能关联一个合同）
        if (contract.getProjectIds() != null && !contract.getProjectIds().isEmpty()) {
            for (Long projectId : contract.getProjectIds()) {
                Long existingContractId = contractMapper.selectContractIdByProjectId(projectId);
                if (existingContractId != null) {
                    throw new ServiceException("项目已关联合同，无法重复添加");
                }
            }
        }

        // 合同编号归一化 + 判重（必须排在「项目已关联合同」校验之后，见 plan.md D5）
        normalizeAndCheckContractCode(contract, null);

        calculateTaxAmounts(contract);

        String username = SecurityUtils.getUsername();
        java.util.Date now = DateUtils.getNowDate();
        contract.setDelFlag("0");
        contract.setCreateBy(username);
        contract.setCreateTime(now);
        contract.setUpdateBy(username);
        contract.setUpdateTime(now);
        int rows;
        try {
            rows = contractMapper.insertContract(contract);
        } catch (DuplicateKeyException e) {
            // 并发兜底：两个请求同时通过上面的判重后先后落库，由唯一索引 uk_contract_code_norm 拦下（1062）
            throw translateDuplicateKey(e, contract);
        }

        // 保存关联项目
        insertProjectContractRel(contract);

        return rows;
    }

    /**
     * 新增项目合同关联信息
     */
    private void insertProjectContractRel(Contract contract)
    {
        List<Long> projectIds = contract.getProjectIds();
        if (projectIds != null && !projectIds.isEmpty()) {
            for (Long projectId : projectIds) {
                ProjectContractRel rel = new ProjectContractRel();
                rel.setProjectId(projectId.toString());
                rel.setContractId(contract.getContractId());
                rel.setRelStatus("有效");
                rel.setBindDate(new Date());
                rel.setDelFlag("0");
                rel.setCreateBy(SecurityUtils.getUsername());
                rel.setCreateTime(DateUtils.getNowDate());
                projectContractRelMapper.insertProjectContractRel(rel);
            }
        }
    }

    /**
     * 修改合同管理
     *
     * @param contract 合同管理
     * @return 结果
     */
    @Override
    @Transactional
    public int updateContract(Contract contract)
    {
        // 检查新绑定的项目是否已关联其他合同（排除自身）
        if (contract.getProjectIds() != null && !contract.getProjectIds().isEmpty()) {
            for (Long projectId : contract.getProjectIds()) {
                Long existingContractId = contractMapper.selectContractIdByProjectId(projectId);
                if (existingContractId != null && !existingContractId.equals(contract.getContractId())) {
                    throw new ServiceException("项目已关联其他合同，无法重复添加");
                }
            }
        }

        // 合同编号归一化 + 判重（必须排在「项目已关联合同」校验之后，见 plan.md D5）。
        // 排除自身是硬性要求：漏掉它，全部存量合同都将无法编辑保存
        normalizeAndCheckContractCode(contract, contract.getContractId());

        calculateTaxAmounts(contract);

        contract.setUpdateBy(SecurityUtils.getUsername());
        contract.setUpdateTime(DateUtils.getNowDate());
        int rows;
        try {
            rows = contractMapper.updateContract(contract);
        } catch (DuplicateKeyException e) {
            // 并发兜底：唯一索引 uk_contract_code_norm 拦下（1062）
            throw translateDuplicateKey(e, contract);
        }

        // 删除旧的关联项目
        projectContractRelMapper.deleteProjectContractRelByContractId(contract.getContractId());

        // 保存新的关联项目
        insertProjectContractRel(contract);

        return rows;
    }

    /**
     * 批量删除合同管理
     *
     * @param contractIds 需要删除的合同管理主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteContractByContractIds(Long[] contractIds)
    {
        for (Long contractId : contractIds) {
            Contract contract = contractMapper.selectContractByContractId(contractId);
            if (contract == null) {
                throw new ServiceException("合同不存在");
            }

            String contractName = contract.getContractName();

            // 检查是否有关联款项
            int paymentCount = paymentMapper.countPaymentByContractId(contractId);
            if (paymentCount > 0) {
                throw new ServiceException("【" + contractName + "】下已有款项里程碑信息，不可进行删除操作！");
            }

            // 检查是否有关联附件
            int attachmentCount = attachmentMapper.countAttachmentByBusiness("contract", contractId);
            if (attachmentCount > 0) {
                throw new ServiceException("【" + contractName + "】已上传附件信息，不可进行删除操作！");
            }

            // 物理删除项目关联关系
            projectContractRelMapper.deleteProjectContractRelByContractId(contractId);

            // 逻辑删除合同
            contract.setDelFlag("1");
            contract.setUpdateBy(SecurityUtils.getUsername());
            contract.setUpdateTime(DateUtils.getNowDate());
            contractMapper.updateContract(contract);
        }
        return contractIds.length;
    }

    /**
     * 删除合同管理信息
     *
     * @param contractId 合同管理主键
     * @return 结果
     */
    @Override
    public int deleteContractByContractId(Long contractId)
    {
        return contractMapper.deleteContractByContractId(contractId);
    }

    /**
     * 搜索合同（用于下拉选择）
     *
     * @param keyword 搜索关键词（合同名称或合同编号）
     * @return 合同列表
     */
    @Override
    public List<Contract> searchContracts(String keyword)
    {
        return contractMapper.searchContracts(keyword);
    }

    /**
     * 按数据权限搜索合同编号/合同名称（autoComplete）。
     * 与合同列表一致：按合同部门(d) + 创建人(u1) 注入数据权限。
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u1")
    public List<Map<String, Object>> searchContractsForFilter(Contract contract)
    {
        return contractMapper.searchContractsForFilter(contract);
    }

    @Override
    public List<Map<String, Object>> listContractsByDept(Long deptId, String keyword)
    {
        return contractMapper.listContractsByDept(deptId, keyword);
    }
    /**
     * 检查合同名称是否唯一
     *
     * @param contractName 合同名称
     * @param contractId 合同ID（编辑时传入，新增时为null）
     * @return true-唯一，false-不唯一
     */
    @Override
    public boolean checkContractNameUnique(String contractName, Long contractId)
    {
        if (contractName == null || contractName.trim().isEmpty()) {
            return false;
        }

        Contract contract = new Contract();
        contract.setContractName(contractName.trim());
        List<Contract> list = contractMapper.selectContractList(contract);

        // 如果没有找到同名合同，则唯一
        if (list == null || list.isEmpty()) {
            return true;
        }

        // 如果是编辑模式，排除自己
        if (contractId != null) {
            for (Contract c : list) {
                if (!contractId.equals(c.getContractId())) {
                    // 找到其他同名合同，不唯一
                    return false;
                }
            }
            // 只找到自己，唯一
            return true;
        }

        // 新增模式，找到同名合同，不唯一
        return false;
    }

    /**
     * 检查合同编号是否唯一。
     *
     * <p>与合同名称校验的语义<b>不同</b>：合同编号是可选字段，未填写（空、纯空白、字面「无」）
     * 一律视为唯一，多条空编号合同可以并存；只有归一化后非空的编号才参与判重。
     *
     * <p>判重直调 {@code contractMapper}（而非 {@code this.selectContractList}），
     * 刻意绕开 {@code @DataScope}：唯一性是全局属性，按部门收窄会漏判。
     * 「是否相同」的判断全部交给 SQL 与唯一索引，Java 侧不做二次字符串比对——
     * 库表排序规则是 utf8mb4_0900_ai_ci（大小写不敏感），Java 的 equals 大小写敏感，
     * 在 Java 侧再比一次会产生「应用放行、数据库 1062」的裂缝。
     *
     * @param contractCode 合同编号（原始输入，内部会归一化）
     * @param contractId 合同ID（编辑时传入用于排除自身，新增时为null）
     * @return true-唯一（含未填写），false-不唯一
     */
    @Override
    public boolean checkContractCodeUnique(String contractCode, Long contractId)
    {
        String normalizedCode = normalizeContractCode(contractCode);
        // 未填写不参与判重，直接判为唯一且不查库
        if (normalizedCode == null) {
            return true;
        }
        return contractMapper.countByContractCode(normalizedCode, contractId) == 0;
    }

    /**
     * 合同编号归一化并回写实体，随后判重；重复则抛业务异常。
     *
     * <p>归一化值必须回写实体，保证落库的是干净值（避免再产生末尾带 TAB 那类脏数据），
     * 并保证「清空编号」以 NULL 落库——这依赖 ContractMapper.xml 的 updateContract
     * 对 contract_code 无条件更新（不得加 {@code <if>} 守卫）。
     *
     * @param contract 待落库的合同实体（编号会被就地归一化）
     * @param excludeContractId 判重时需排除的合同ID（编辑传自身ID，新增传 null）
     */
    private void normalizeAndCheckContractCode(Contract contract, Long excludeContractId)
    {
        String normalizedCode = normalizeContractCode(contract.getContractCode());
        contract.setContractCode(normalizedCode);
        // 未填写的编号不参与判重（多条空编号合同合法共存）
        if (normalizedCode == null) {
            return;
        }
        if (contractMapper.countByContractCode(normalizedCode, excludeContractId) > 0) {
            throw new ServiceException(duplicateContractCodeMessage(normalizedCode));
        }
    }

    /**
     * 把<b>合同编号</b>唯一索引的冲突（1062 → DuplicateKeyException）转译成与主动拦截同一句人话。
     *
     * <p>必须同时满足两个条件才报「编号已存在」，缺一不可：
     * <ol>
     *   <li>本次提交的编号非空——编号为空时冲突必然与编号无关；</li>
     *   <li>异常信息指向 {@value #UK_CONTRACT_CODE_NORM}——{@code DuplicateKeyException}
     *       只说明「某个唯一键撞了」，不判索引名的话，将来给本表加任何新的唯一约束，
     *       它的冲突都会被张冠李戴地报成「合同编号已存在」。</li>
     * </ol>
     *
     * <p><b>不满足时也绝不能原样抛出</b>（安全审查 2026-08-07）：
     * {@code GlobalExceptionHandler.handleRuntimeException} 对 {@code RuntimeException}
     * 的处理是 {@code AjaxResult.error(e.getMessage())}，而 MyBatis 拼装的 1062 消息里含
     * 表名、全部列名、完整 INSERT 语句、服务器上的 jar 绝对路径与构件版本号 ——
     * 原样抛出等于把这些直接渲染到用户浏览器里（已实测）。
     * 故一律转成不含底层信息的业务异常，原始堆栈只写服务端日志。
     */
    private RuntimeException translateDuplicateKey(DuplicateKeyException e, Contract contract)
    {
        String normalizedCode = contract.getContractCode();
        String message = e.getMessage();
        if (normalizedCode != null && message != null && message.contains(UK_CONTRACT_CODE_NORM)) {
            return new ServiceException(duplicateContractCodeMessage(normalizedCode));
        }
        // 与合同编号无关的唯一约束冲突：细节只进日志，不出接口
        log.error("合同保存遭遇非合同编号的唯一约束冲突，contractId={}", contract.getContractId(), e);
        return new ServiceException("保存失败，请稍后重试；若持续失败请联系管理员");
    }

    /** 合同编号重复的统一提示文案：只带编号本身，不带冲突方合同名、不带异常类名或 SQL */
    private static String duplicateContractCodeMessage(String normalizedCode)
    {
        // 后缀与前端 add.vue / edit.vue 的 validateContractCode 逐字一致：
        // 同一个冲突走前端校验或后端拦截，用户必须看到同一句话，否则会以为是两个不同的问题
        return CONTRACT_CODE_DUPLICATE_PREFIX + normalizedCode + "，请使用其他编号";
    }

    /**
     * 合同编号归一化：删除全部 TAB/CR/LF → 去首尾半角空格 → 空串或字面「无」视为未填写(null)。
     *
     * <p>口径必须与 ContractMapper.xml 的 countByContractCode、
     * pm_contract.contract_code_norm 生成列<b>逐字一致</b>。
     *
     * <p>此处刻意不使用 {@link String#trim()}：它会去掉全部 {@code <= U+0020} 的字符
     * （含 \f \v \0），而 MySQL 的 TRIM() 只去半角空格，用 trim() 会让 Java 与 SQL
     * 在这些字符上产生分歧，出现「应用放行、数据库 1062」的裂缝。
     *
     * <p>只剥首尾空白，<b>中间的空格必须保留</b>——「ABC 001」与「ABC001」是两个不同的编号。
     *
     * @param contractCode 原始编号
     * @return 归一化后的编号；视为未填写时返回 null
     */
    private static String normalizeContractCode(String contractCode)
    {
        if (contractCode == null) {
            return null;
        }
        String s = contractCode.replace("\t", "").replace("\r", "").replace("\n", "");
        int begin = 0;
        int end = s.length();
        while (begin < end && s.charAt(begin) == ' ') {
            begin++;
        }
        while (end > begin && s.charAt(end - 1) == ' ') {
            end--;
        }
        s = s.substring(begin, end);
        return (s.isEmpty() || CONTRACT_CODE_NONE.equals(s)) ? null : s;
    }

    /**
     * 查询合同及其付款里程碑列表（用于付款里程碑查询页面）
     *
     * @param contract 查询条件
     * @return 合同及付款里程碑列表
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u1")
    public List<Contract> selectContractWithPaymentsList(Contract contract)
    {
        return contractMapper.selectContractWithPaymentsList(contract);
    }

    /**
     * 统计付款里程碑总金额（用于付款里程碑查询页面）
     *
     * @param contract 查询条件
     * @return 付款总金额
     */
    @Override
    public BigDecimal sumPaymentAmount(Contract contract)
    {
        return contractMapper.sumPaymentAmount(contract);
    }

    /**
     * 根据合同金额和税率计算不含税金额和税金
     * 公式：不含税金额 = 合同金额 / (1 + 税率/100)，税金 = 合同金额 - 不含税金额
     */
    void calculateTaxAmounts(Contract contract) {
        if (contract.getContractAmount() == null || contract.getTaxRate() == null) {
            return;
        }
        BigDecimal taxRate = contract.getTaxRate().divide(new BigDecimal(100), 10, RoundingMode.HALF_UP);
        BigDecimal amountNoTax = contract.getContractAmount().divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = contract.getContractAmount().subtract(amountNoTax);
        contract.setAmountNoTax(amountNoTax);
        contract.setTaxAmount(taxAmount);
    }
}
