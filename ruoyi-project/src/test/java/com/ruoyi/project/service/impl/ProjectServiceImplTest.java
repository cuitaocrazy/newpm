package com.ruoyi.project.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.Contract;
import com.ruoyi.project.domain.Project;
import com.ruoyi.project.mapper.ContractMapper;
import com.ruoyi.project.mapper.ProjectContractRelMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.service.IProjectMemberService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ProjectServiceImpl 行为锁定测试（Characterization Test）
 * 目的：锁定现有正确行为，后续重构时防止回归
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @InjectMocks
    private ProjectServiceImpl service;

    @Mock private ProjectMapper projectMapper;
    @Mock private ContractMapper contractMapper;
    @Mock private ProjectContractRelMapper projectContractRelMapper;
    @Mock private IProjectMemberService projectMemberService;

    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setUp() {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUsername).thenReturn("testuser");
    }

    @AfterEach
    void tearDown() {
        securityMock.close();
    }

    // ========== generateProjectCode 行为锁定 ==========

    @Test
    @DisplayName("项目编号生成：完整5段格式 {行业}-{区域}-{省份}-{简称}-{年份}")
    void generateProjectCode_fullFormat() {
        String code = service.generateProjectCode("JR", "HB", "BJ", "测试项目", "2026");
        assertEquals("JR-HB-BJ-测试项目-2026", code);
    }

    @Test
    @DisplayName("项目编号生成：缺少部分字段时跳过对应段")
    void generateProjectCode_partialFields() {
        assertEquals("JR-HB-2026", service.generateProjectCode("JR", "HB", null, null, "2026"));
        assertEquals("JR-HB-2026", service.generateProjectCode("JR", "HB", "", "", "2026"));
    }

    @Test
    @DisplayName("项目编号生成：只有行业代码")
    void generateProjectCode_industryOnly() {
        assertEquals("JR", service.generateProjectCode("JR", null, null, null, null));
    }

    @Test
    @DisplayName("项目编号生成：全部为空返回空字符串")
    void generateProjectCode_allEmpty() {
        assertEquals("", service.generateProjectCode(null, null, null, null, null));
        assertEquals("", service.generateProjectCode("", "", "", "", ""));
    }

    @Test
    @DisplayName("项目编号生成：只有中间字段时无前导分隔符")
    void generateProjectCode_middleFieldOnly() {
        assertEquals("测试", service.generateProjectCode(null, null, null, "测试", null));
    }

    @ParameterizedTest(name = "行业={0}, 区域={1}, 省={2}, 简称={3}, 年={4} → {5}")
    @DisplayName("项目编号生成：各种组合")
    @CsvSource({
        "'JR', 'HB', 'BJ', '项目A', '2026', 'JR-HB-BJ-项目A-2026'",
        "'JR', '',   '',   '项目B', '2026', 'JR-项目B-2026'",
        "'',   'HB', '',   '',      '2026', 'HB-2026'",
    })
    void generateProjectCode_combinations(String ind, String reg, String prov,
                                           String name, String year, String expected) {
        assertEquals(expected, service.generateProjectCode(ind, reg, prov, name, year));
    }

    // ========== checkProjectCode 行为锁定 ==========

    @Test
    @DisplayName("编号冲突检测：无冲突时返回 exists=false，suggestedCode=原编号")
    void checkProjectCode_noConflict() {
        when(projectMapper.selectProjectsByCodePrefix("JR-HB-BJ-项目-2026", null))
            .thenReturn(Collections.emptyList());

        Map<String, Object> result = service.checkProjectCode("JR-HB-BJ-项目-2026", null);

        assertEquals(false, result.get("exists"));
        assertEquals("JR-HB-BJ-项目-2026", result.get("suggestedCode"));
    }

    @Test
    @DisplayName("编号冲突检测：精确匹配冲突，无后缀编号时建议 -01")
    void checkProjectCode_exactConflict_noSuffix() {
        String code = "JR-HB-项目-2026";
        Map<String, Object> existing = new HashMap<>();
        existing.put("projectCode", code);
        existing.put("projectName", "已有项目");

        when(projectMapper.selectProjectsByCodePrefix(code, null))
            .thenReturn(List.of(existing));

        Map<String, Object> result = service.checkProjectCode(code, null);

        assertEquals(true, result.get("exists"));
        assertEquals("JR-HB-项目-2026-01", result.get("suggestedCode"));
        assertNotNull(result.get("existingProject"));
    }

    @Test
    @DisplayName("编号冲突检测：已有后缀编号时递增")
    void checkProjectCode_incrementSuffix() {
        String code = "JR-HB-项目-2026";
        Map<String, Object> exact = new HashMap<>();
        exact.put("projectCode", code);
        Map<String, Object> suffixed = new HashMap<>();
        suffixed.put("projectCode", code + "-03");

        when(projectMapper.selectProjectsByCodePrefix(code, null))
            .thenReturn(List.of(exact, suffixed));

        Map<String, Object> result = service.checkProjectCode(code, null);

        assertEquals(true, result.get("exists"));
        assertEquals("JR-HB-项目-2026-04", result.get("suggestedCode"));
    }

    @Test
    @DisplayName("编号冲突检测：后缀必须是两位数字才参与计算")
    void checkProjectCode_onlyTwoDigitSuffix() {
        String code = "JR-项目-2026";
        Map<String, Object> exact = new HashMap<>();
        exact.put("projectCode", code);
        // This has a 3-digit suffix, should NOT match the -\\d{2} regex
        Map<String, Object> threedigit = new HashMap<>();
        threedigit.put("projectCode", code + "-123");

        when(projectMapper.selectProjectsByCodePrefix(code, null))
            .thenReturn(List.of(exact, threedigit));

        Map<String, Object> result = service.checkProjectCode(code, null);

        // 3-digit suffix is ignored, so max is 0, suggested = -01
        assertEquals("JR-项目-2026-01", result.get("suggestedCode"));
    }

    @Test
    @DisplayName("编号冲突检测：编辑模式排除自身项目ID")
    void checkProjectCode_excludeSelf() {
        when(projectMapper.selectProjectsByCodePrefix("CODE", 5L))
            .thenReturn(Collections.emptyList());

        Map<String, Object> result = service.checkProjectCode("CODE", 5L);

        assertEquals(false, result.get("exists"));
        verify(projectMapper).selectProjectsByCodePrefix("CODE", 5L);
    }

    @Test
    @DisplayName("编号冲突检测：前缀返回但无精确匹配时 exists=false")
    void checkProjectCode_prefixOnlyNoExactMatch() {
        String code = "JR-项目-2026";
        // Returned by prefix query but code doesn't exactly match
        Map<String, Object> similar = new HashMap<>();
        similar.put("projectCode", code + "-01");

        when(projectMapper.selectProjectsByCodePrefix(code, null))
            .thenReturn(List.of(similar));

        Map<String, Object> result = service.checkProjectCode(code, null);

        assertEquals(false, result.get("exists"));
        assertEquals(code, result.get("suggestedCode"));
    }

    // ========== syncProjectMembers (via insertProject/updateProject) 行为锁定 ==========

    @Test
    @DisplayName("新增项目：同步成员 - 收集5个来源的用户ID")
    void insertProject_syncMembers_allSources() {
        Project project = new Project();
        project.setProjectId(1L);
        project.setProjectManagerId(101L);
        project.setMarketManagerId(102L);
        project.setSalesManagerId(103L);
        project.setTeamLeaderId(104L);
        project.setParticipants("105,106,107");
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        verify(projectMemberService).syncMembers(eq(1L), captor.capture());
        Set<Long> synced = captor.getValue();
        assertEquals(Set.of(101L, 102L, 103L, 104L, 105L, 106L, 107L), synced);
    }

    @Test
    @DisplayName("新增项目：同步成员 - 重复ID去重")
    void insertProject_syncMembers_dedup() {
        Project project = new Project();
        project.setProjectId(1L);
        project.setProjectManagerId(101L);
        project.setMarketManagerId(101L); // same as PM
        project.setParticipants("101,102");
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        verify(projectMemberService).syncMembers(eq(1L), captor.capture());
        Set<Long> synced = captor.getValue();
        assertEquals(2, synced.size());
        assertTrue(synced.containsAll(Set.of(101L, 102L)));
    }

    @Test
    @DisplayName("新增项目：同步成员 - 参与人含空格和无效值时忽略")
    void insertProject_syncMembers_trimAndIgnoreInvalid() {
        Project project = new Project();
        project.setProjectId(1L);
        project.setParticipants(" 101 , , abc, 102 ");
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        verify(projectMemberService).syncMembers(eq(1L), captor.capture());
        Set<Long> synced = captor.getValue();
        assertEquals(Set.of(101L, 102L), synced);
    }

    @Test
    @DisplayName("新增项目：projectId为null时不调用syncMembers")
    void insertProject_nullProjectId_skipSync() {
        Project project = new Project();
        // projectId is null (not set by mapper in test)
        project.setProjectManagerId(101L);
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        verify(projectMemberService, never()).syncMembers(anyLong(), any());
    }

    @Test
    @DisplayName("新增项目：所有角色字段均为null，参与人为空时传空集合")
    void insertProject_syncMembers_emptySet() {
        Project project = new Project();
        project.setProjectId(1L);
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        verify(projectMemberService).syncMembers(eq(1L), captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    @DisplayName("修改项目：也触发成员同步")
    void updateProject_alsoSyncsMembers() {
        Project project = new Project();
        project.setProjectId(1L);
        project.setProjectManagerId(201L);
        when(projectMapper.updateProject(any())).thenReturn(1);

        service.updateProject(project);

        verify(projectMemberService).syncMembers(eq(1L), any());
    }

    // ========== insertProject 其他行为锁定 ==========

    @Test
    @DisplayName("新增项目：自动设置 createBy/updateBy 为当前用户")
    void insertProject_setsAuditFields() {
        Project project = new Project();
        project.setProjectId(1L);
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        assertEquals("testuser", project.getCreateBy());
        assertEquals("testuser", project.getUpdateBy());
        assertNotNull(project.getCreateTime());
        assertNotNull(project.getUpdateTime());
    }

    @Test
    @DisplayName("新增项目：收入确认年度默认为 dd(待定)")
    void insertProject_defaultRevenueConfirmYear() {
        Project project = new Project();
        project.setProjectId(1L);
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        assertEquals("dd", project.getRevenueConfirmYear());
    }

    @Test
    @DisplayName("新增项目：收入确认状态默认为 0(待定)")
    void insertProject_defaultRevenueConfirmStatus() {
        Project project = new Project();
        project.setProjectId(1L);
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        assertEquals("0", project.getRevenueConfirmStatus());
    }

    @Test
    @DisplayName("新增项目：已有收入确认年度时不覆盖")
    void insertProject_preserveExistingRevenueYear() {
        Project project = new Project();
        project.setProjectId(1L);
        project.setRevenueConfirmYear("2026");
        project.setRevenueConfirmStatus("1");
        when(projectMapper.insertProject(any())).thenReturn(1);

        service.insertProject(project);

        assertEquals("2026", project.getRevenueConfirmYear());
        assertEquals("1", project.getRevenueConfirmStatus());
    }

    @Test
    @DisplayName("新增项目：编号超500字符时抛异常")
    void insertProject_codeTooLong_throws() {
        Project project = new Project();
        project.setProjectCode("A".repeat(501));

        assertThrows(ServiceException.class, () -> service.insertProject(project));
        verify(projectMapper, never()).insertProject(any());
    }

    @Test
    @DisplayName("修改项目：编号超500字符时抛异常")
    void updateProject_codeTooLong_throws() {
        Project project = new Project();
        project.setProjectCode("A".repeat(501));

        assertThrows(ServiceException.class, () -> service.updateProject(project));
        verify(projectMapper, never()).updateProject(any());
    }

    // ========== enrichForExport 行为锁定 ==========

    @Test
    @DisplayName("导出充实：null或空列表不报错")
    void enrichForExport_nullOrEmpty() {
        assertDoesNotThrow(() -> service.enrichForExport(null));
        assertDoesNotThrow(() -> service.enrichForExport(Collections.emptyList()));
    }

    @Test
    @DisplayName("导出充实：部门路径跳过前两级(根节点+一级)")
    void enrichForExport_deptPath_skipTopLevels() {
        // dept 100: ancestors="0,1", deptName="二级部门"
        // dept 200: ancestors="0,1,100", deptName="三级部门"
        List<Map<String, Object>> allDepts = new ArrayList<>();
        allDepts.add(makeDept(0L, "根节点", ""));
        allDepts.add(makeDept(1L, "一级", "0"));
        allDepts.add(makeDept(100L, "二级部门", "0,1"));
        allDepts.add(makeDept(200L, "三级部门", "0,1,100"));
        when(projectMapper.selectAllDeptsForPath()).thenReturn(allDepts);

        Project p = new Project();
        p.setProjectDept("200");

        service.enrichForExport(List.of(p));

        assertEquals("二级部门 - 三级部门", p.getDeptPathDisplay());
        assertEquals("二级部门", p.getDeptOrgLevel2());
        assertEquals("三级部门", p.getDeptOrgLevel3());
        assertNull(p.getDeptOrgLevel4());
    }

    @Test
    @DisplayName("导出充实：部门层级只有两级时直接显示(不跳过)")
    void enrichForExport_deptPath_shallowDept() {
        // dept 5: ancestors="0", deptName="顶级部门"  (fullPath=[0,5], size=2, not > 2)
        List<Map<String, Object>> allDepts = new ArrayList<>();
        allDepts.add(makeDept(0L, "根", ""));
        allDepts.add(makeDept(5L, "顶级部门", "0"));
        when(projectMapper.selectAllDeptsForPath()).thenReturn(allDepts);

        Project p = new Project();
        p.setProjectDept("5");

        service.enrichForExport(List.of(p));

        // fullPath = [0, 5], size=2, not > 2, so displayIds = fullPath itself = [0, 5]
        assertEquals("根 - 顶级部门", p.getDeptPathDisplay());
    }

    @Test
    @DisplayName("导出充实：参与人名称逗号分隔ID解析为顿号分隔名称")
    void enrichForExport_participantsNames() {
        when(projectMapper.selectAllDeptsForPath()).thenReturn(Collections.emptyList());

        List<Map<String, Object>> users = new ArrayList<>();
        users.add(makeUser(10L, "张三"));
        users.add(makeUser(20L, "李四"));
        when(projectMapper.selectUserNickNamesByIds(any())).thenReturn(users);

        Project p = new Project();
        p.setParticipants("10,20");

        service.enrichForExport(List.of(p));

        assertEquals("张三、李四", p.getParticipantsNames());
    }

    @Test
    @DisplayName("导出充实：参与人ID无效值被忽略")
    void enrichForExport_invalidParticipantIds() {
        when(projectMapper.selectAllDeptsForPath()).thenReturn(Collections.emptyList());

        List<Map<String, Object>> users = new ArrayList<>();
        users.add(makeUser(10L, "张三"));
        when(projectMapper.selectUserNickNamesByIds(any())).thenReturn(users);

        Project p = new Project();
        p.setParticipants("10, abc, ,20");

        service.enrichForExport(List.of(p));

        // user 20 not found in map, so only 张三 is included
        assertEquals("张三", p.getParticipantsNames());
    }

    @Test
    @DisplayName("导出充实：无参与人时不查询用户表")
    void enrichForExport_noParticipants_noUserQuery() {
        when(projectMapper.selectAllDeptsForPath()).thenReturn(Collections.emptyList());

        Project p = new Project();
        // no participants set

        service.enrichForExport(List.of(p));

        verify(projectMapper, never()).selectUserNickNamesByIds(any());
    }

    @Test
    @DisplayName("导出充实：updateTimeDisplay 设置为 updateTime 的值")
    void enrichForExport_updateTimeDisplay() {
        when(projectMapper.selectAllDeptsForPath()).thenReturn(Collections.emptyList());

        Project p = new Project();
        Date now = new Date();
        p.setUpdateTime(now);

        service.enrichForExport(List.of(p));

        assertEquals(now, p.getUpdateTimeDisplay());
    }

    @Test
    @DisplayName("导出充实：多项目批量处理")
    void enrichForExport_multipleProjects() {
        List<Map<String, Object>> allDepts = new ArrayList<>();
        allDepts.add(makeDept(0L, "根", ""));
        allDepts.add(makeDept(1L, "一级", "0"));
        allDepts.add(makeDept(100L, "开发部", "0,1"));
        when(projectMapper.selectAllDeptsForPath()).thenReturn(allDepts);

        List<Map<String, Object>> users = new ArrayList<>();
        users.add(makeUser(10L, "张三"));
        when(projectMapper.selectUserNickNamesByIds(any())).thenReturn(users);

        Project p1 = new Project();
        p1.setProjectDept("100");
        p1.setParticipants("10");

        Project p2 = new Project();
        p2.setProjectDept("100");

        service.enrichForExport(List.of(p1, p2));

        assertEquals("开发部", p1.getDeptPathDisplay());
        assertEquals("张三", p1.getParticipantsNames());
        assertEquals("开发部", p2.getDeptPathDisplay());
    }

    // ========== bindContractToProject / unbindContractFromProject 行为锁定 ==========

    @Test
    @DisplayName("绑定合同：先失效旧关联再插入新关联")
    void bindContractToProject_invalidatesOldThenInserts() {
        service.bindContractToProject(1L, 100L);

        var order = inOrder(projectContractRelMapper);
        order.verify(projectContractRelMapper).invalidateByProjectId(1L);
        order.verify(projectContractRelMapper).insertProjectContractRel(any());
    }

    @Test
    @DisplayName("解绑合同：调用失效方法")
    void unbindContractFromProject_invalidates() {
        service.unbindContractFromProject(1L);

        verify(projectContractRelMapper).invalidateByProjectId(1L);
    }

    // ========== selectContractByProjectId 行为锁定 ==========

    @Test
    @DisplayName("查询项目合同：无关联时返回null")
    void selectContractByProjectId_noRel_returnsNull() {
        when(contractMapper.selectContractIdByProjectId(1L)).thenReturn(null);

        assertNull(service.selectContractByProjectId(1L));
        verify(contractMapper, never()).selectContractByContractId(any());
    }

    @Test
    @DisplayName("查询项目合同：有关联时查询合同详情")
    void selectContractByProjectId_hasRel_returnsContract() {
        when(contractMapper.selectContractIdByProjectId(1L)).thenReturn(100L);
        Contract contract = new Contract();
        contract.setContractId(100L);
        when(contractMapper.selectContractByContractId(100L)).thenReturn(contract);

        Contract result = service.selectContractByProjectId(1L);

        assertNotNull(result);
        assertEquals(100L, result.getContractId());
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> makeDept(Long deptId, String deptName, String ancestors) {
        Map<String, Object> dept = new HashMap<>();
        dept.put("deptId", deptId);
        dept.put("deptName", deptName);
        dept.put("ancestors", ancestors);
        return dept;
    }

    private Map<String, Object> makeUser(Long userId, String nickName) {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("nickName", nickName);
        return user;
    }
}
