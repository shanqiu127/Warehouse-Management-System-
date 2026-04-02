package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.common.exception.BusinessException;
import org.example.back.dto.EmployeeContactUpdateDTO;
import org.example.back.dto.LoginResponse;
import org.example.back.entity.BaseGoods;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysEmployee;
import org.example.back.entity.SysErrorLog;
import org.example.back.entity.SysUser;
import org.example.back.mapper.BaseGoodsMapper;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysEmployeeMapper;
import org.example.back.mapper.SysErrorLogMapper;
import org.example.back.mapper.SysUserMapper;
import org.example.back.vo.EmployeeWorkbenchVO;
import org.example.back.vo.ErrorLogBriefVO;
import org.example.back.vo.HomeSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class HomeService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-()\\s]{6,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private AuthService authService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysErrorLogMapper sysErrorLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BaseGoodsMapper baseGoodsMapper;

    @Autowired
    private SysEmployeeMapper sysEmployeeMapper;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    public HomeSummaryVO summary() {
        LoginResponse.UserInfoVO userInfo = authService.getUserInfo();
        SysUser user = sysUserMapper.selectById(userInfo.getId());
        HomeSummaryVO vo = buildSummary(userInfo, user);
        String role = userInfo.getRole();
        if ("superadmin".equalsIgnoreCase(role)) {
            vo.setDbStatus(checkDbStatus());
            vo.setErrorCount24h(countErrorLogsLast24h());
            vo.setRecentErrorLogs(queryRecentErrorLogs(5));
        }

        if ("admin".equalsIgnoreCase(role) || "employee".equalsIgnoreCase(role)) {
            vo.setLowStockCount(countLowStockGoods());
            vo.setZeroStockCount(countZeroStockGoods());
        }

        return vo;
    }

    public EmployeeWorkbenchVO employeeWorkbench() {
        LoginResponse.UserInfoVO userInfo = authService.getUserInfo();
        if (!"employee".equalsIgnoreCase(userInfo.getRole())) {
            throw BusinessException.forbidden("仅员工可访问此接口");
        }

        EmployeeWorkbenchVO result = new EmployeeWorkbenchVO();
        SysUser user = sysUserMapper.selectById(userInfo.getId());
        HomeSummaryVO summary = buildSummary(userInfo, user);
        EmployeeWorkbenchVO.SummaryInfo summaryInfo = new EmployeeWorkbenchVO.SummaryInfo();
        summaryInfo.setUserId(summary.getUserId());
        summaryInfo.setUsername(summary.getUsername());
        summaryInfo.setRealName(summary.getRealName());
        summaryInfo.setDeptId(summary.getDeptId());
        summaryInfo.setDeptCode(summary.getDeptCode());
        summaryInfo.setDeptName(summary.getDeptName());
        summaryInfo.setCurrentLoginTime(summary.getCurrentLoginTime());
        summaryInfo.setLastLoginTime(summary.getLastLoginTime());
        String deptCode = userInfo.getDeptCode();
        if ("purchase".equals(deptCode) || "sales".equals(deptCode) || "warehouse".equals(deptCode)) {
            summaryInfo.setLowStockCount(countLowStockGoods());
            summaryInfo.setZeroStockCount(countZeroStockGoods());
        }
        result.setSummary(summaryInfo);

        EmployeeWorkbenchVO.ProfileInfo profileInfo = new EmployeeWorkbenchVO.ProfileInfo();
        LambdaQueryWrapper<SysEmployee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(SysEmployee::getUserId, userInfo.getId()).last("LIMIT 1");
        SysEmployee emp = sysEmployeeMapper.selectOne(empWrapper);
        if (emp != null) {
            profileInfo.setEmpCode(emp.getEmpCode());
            profileInfo.setPosition(emp.getPosition());
            profileInfo.setPhone(emp.getPhone() != null ? emp.getPhone() : (user != null ? user.getPhone() : null));
            profileInfo.setEmail(emp.getEmail() != null ? emp.getEmail() : (user != null ? user.getEmail() : null));
        } else if (user != null) {
            profileInfo.setPhone(user.getPhone());
            profileInfo.setEmail(user.getEmail());
        }
        result.setProfile(profileInfo);

        EmployeeWorkbenchVO.DeptContactInfo deptContact = new EmployeeWorkbenchVO.DeptContactInfo();
        if (userInfo.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(userInfo.getDeptId());
            if (dept != null) {
                deptContact.setDeptName(dept.getDeptName());
                deptContact.setLeader(dept.getLeader());
                deptContact.setPhone(dept.getPhone());
            }
        }
        result.setDeptContact(deptContact);

        result.setTips(buildTips(deptCode));

        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEmployeeContact(EmployeeContactUpdateDTO dto) {
        LoginResponse.UserInfoVO userInfo = authService.getUserInfo();
        if (!"employee".equalsIgnoreCase(userInfo.getRole())) {
            throw BusinessException.forbidden("仅员工可修改联系方式");
        }

        SysUser user = sysUserMapper.selectById(userInfo.getId());
        if (user == null) {
            throw BusinessException.notFound("当前用户不存在");
        }

        String phone = normalizeNullable(dto == null ? null : dto.getPhone());
        String email = normalizeNullable(dto == null ? null : dto.getEmail());
        validateContact(phone, email);

        user.setPhone(phone);
        user.setEmail(email);
        sysUserMapper.updateById(user);

        SysEmployee employee = findEmployeeByUserId(user.getId());
        if (employee != null) {
            employee.setPhone(phone);
            employee.setEmail(email);
            sysEmployeeMapper.updateById(employee);
        }
    }

    private HomeSummaryVO buildSummary(LoginResponse.UserInfoVO userInfo, SysUser user) {
        HomeSummaryVO vo = new HomeSummaryVO();
        vo.setUserId(userInfo.getId());
        vo.setUsername(userInfo.getUsername());
        vo.setRealName(userInfo.getRealName());
        vo.setRole(userInfo.getRole());
        vo.setDeptId(userInfo.getDeptId());
        vo.setDeptCode(userInfo.getDeptCode());
        vo.setDeptName(userInfo.getDeptName());
        vo.setCurrentLoginTime(user == null ? null : user.getCurrentLoginTime());
        vo.setLastLoginTime(user == null ? null : user.getLastLoginTime());
        vo.setServerTime(LocalDateTime.now());
        return vo;
    }

    private List<String> buildTips(String deptCode) {
        Map<String, List<String>> tipsMap = Map.of(
            "hr", List.of("及时提交入离职资料", "保持员工档案信息完整规范", "关注考勤材料提交时效"),
            "purchase", List.of("关注补货优先级与供应商跟进", "退货登记请在当日完成", "核对采购单据确保准确"),
            "sales", List.of("每日核对订单信息与库存", "缺货问题及时与仓储沟通", "退货登记请按流程及时提交"),
            "warehouse", List.of("优先关注低库存与零库存商品", "盘点差异需在当日完成登记", "涉及历史单据时注意审批协同"),
            "finance", List.of("关注月结节奏与对账时间", "核对对账资料确保数据准确", "配合各部门做好数据核对")
        );
        return tipsMap.getOrDefault(deptCode != null ? deptCode : "", List.of("请关注最新公告", "如有疑问请联系部门负责人"));
    }

    private SysEmployee findEmployeeByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEmployee::getUserId, userId).last("LIMIT 1");
        return sysEmployeeMapper.selectOne(wrapper);
    }

    private void validateContact(String phone, String email) {
        if (StringUtils.hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw BusinessException.validateFail("手机号格式不正确");
        }
        if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email).matches()) {
            throw BusinessException.validateFail("邮箱格式不正确");
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // 检查数据库连接状态
    private String checkDbStatus() {
        try {
            Integer v = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return v != null && v == 1 ? "正常" : "异常";
        } catch (Exception e) {
            return "异常";
        }
    }

    private Long countErrorLogsLast24h() {
        LocalDateTime begin = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<SysErrorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SysErrorLog::getCreateTime, begin)
                .ge(SysErrorLog::getStatusCode, 500);
        return sysErrorLogMapper.selectCount(wrapper);
    }

    private Long countLowStockGoods() {
        LambdaQueryWrapper<BaseGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseGoods::getStatus, 1)
                .apply("stock <= warning_stock");
        return baseGoodsMapper.selectCount(wrapper);
    }

    private Long countZeroStockGoods() {
        LambdaQueryWrapper<BaseGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseGoods::getStatus, 1)
                .eq(BaseGoods::getStock, 0);
        return baseGoodsMapper.selectCount(wrapper);
    }
    // 查询最近的错误日志列表
    private List<ErrorLogBriefVO> queryRecentErrorLogs(int limit) {
        LambdaQueryWrapper<SysErrorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SysErrorLog::getStatusCode, 500)
                .orderByDesc(SysErrorLog::getCreateTime)
                .last("LIMIT " + Math.max(limit, 1));
        // 将 SysErrorLog 实体转换为 ErrorLogBriefVO 对象，并截取消息内容以适合首页展示
        return sysErrorLogMapper.selectList(wrapper).stream().map(item -> {
            ErrorLogBriefVO vo = new ErrorLogBriefVO();
            vo.setRequestUri(item.getRequestUri());
            vo.setMethod(item.getMethod());
            vo.setStatusCode(item.getStatusCode());
            vo.setErrorType(item.getErrorType());
            vo.setMessage(safeMessageForHome(item.getMessage()));
            vo.setCreateTime(item.getCreateTime());
            return vo;
        }).toList();
    }
    // 截取错误消息内容以适合首页展示，避免过长文本影响布局
    private String safeMessageForHome(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 120 ? message.substring(0, 120) + "..." : message;
    }
}
