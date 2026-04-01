package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.MessageQueryDTO;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysMessage;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysMessageMapper;
import org.example.back.mapper.SysUserMapper;
import org.example.back.vo.MessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class MessageService {

    private static final int MESSAGE_UNREAD = 0;
    private static final int MESSAGE_READ = 1;
    private static final int USER_STATUS_ENABLED = 1;
    private static final String ROLE_ADMIN = "admin";

    @Autowired
    private SysMessageMapper sysMessageMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private AuthzService authzService;

    public PageResult<MessageVO> page(MessageQueryDTO queryDTO) {
        requireMessageAccess();
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getRecipientUserId, currentUser.getId())
                .eq(queryDTO.getRead() != null, SysMessage::getIsRead, Boolean.TRUE.equals(queryDTO.getRead()) ? MESSAGE_READ : MESSAGE_UNREAD)
                .orderByDesc(SysMessage::getCreateTime)
                .orderByDesc(SysMessage::getId);

        Page<SysMessage> page = sysMessageMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<MessageVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public Long unreadCount() {
        requireMessageAccess();
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getRecipientUserId, currentUser.getId())
                .eq(SysMessage::getIsRead, MESSAGE_UNREAD);
        return sysMessageMapper.selectCount(wrapper);
    }

    public void markRead(Long id) {
        requireMessageAccess();
        SysMessage message = requireOwnedMessage(id);
        if (Integer.valueOf(MESSAGE_READ).equals(message.getIsRead())) {
            return;
        }
        message.setIsRead(MESSAGE_READ);
        message.setReadTime(LocalDateTime.now());
        sysMessageMapper.updateById(message);
    }

    public void markAllRead() {
        requireMessageAccess();
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getRecipientUserId, currentUser.getId())
                .eq(SysMessage::getIsRead, MESSAGE_UNREAD);

        SysMessage updateEntity = new SysMessage();
        updateEntity.setIsRead(MESSAGE_READ);
        updateEntity.setReadTime(LocalDateTime.now());
        sysMessageMapper.update(updateEntity, wrapper);
    }

    public void deleteAllRead() {
        requireMessageAccess();
        LoginResponse.UserInfoVO currentUser = authzService.currentUser();
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getRecipientUserId, currentUser.getId())
                .eq(SysMessage::getIsRead, MESSAGE_READ);
        sysMessageMapper.delete(wrapper);
    }

    public void sendNewEmployeePasswordReminder(String employeeName, Long deptId, String operatorLabel) {
        sendToDeptAdmins(
                deptId,
                "新员工密码设置提醒",
                String.format(
                        Locale.ROOT,
                        "您有新员工%s加入，需要设置新密码（若不设置，将使用默认密码123456），操作人：%s。",
                        safeEmployeeName(employeeName),
                        safeOperatorLabel(operatorLabel)
                )
        );
    }

    public void sendEmployeePasswordChangedReminder(String employeeName, Long deptId, String operatorLabel) {
        sendToDeptAdmins(
                deptId,
                "员工密码变更提醒",
                String.format(
                        Locale.ROOT,
                        "您的员工%s的密码已被修改，如有疑问，请联系操作人，操作人：%s。",
                        safeEmployeeName(employeeName),
                        safeOperatorLabel(operatorLabel)
                )
        );
    }

    public void sendEmployeeTransferReminders(String employeeName, Long oldDeptId, Long newDeptId, String operatorLabel) {
        if (oldDeptId == null || newDeptId == null || oldDeptId.equals(newDeptId)) {
            return;
        }
        SysDept oldDept = sysDeptMapper.selectById(oldDeptId);
        SysDept newDept = sysDeptMapper.selectById(newDeptId);
        if (oldDept != null && newDept != null) {
            sendToDeptAdmins(
                    oldDeptId,
                    "员工调离提醒",
                    String.format(
                            Locale.ROOT,
                            "您的员工%s已调离并将前往%s，操作人：%s。",
                            safeEmployeeName(employeeName),
                            newDept.getDeptName(),
                            safeOperatorLabel(operatorLabel)
                    )
            );
            sendToDeptAdmins(
                    newDeptId,
                    "员工调入提醒",
                    String.format(
                            Locale.ROOT,
                            "您已新增从%s调入的员工%s，操作人：%s。",
                            oldDept.getDeptName(),
                            safeEmployeeName(employeeName),
                            safeOperatorLabel(operatorLabel)
                    )
            );
        }
    }

    public void sendEmployeeLeftReminder(String employeeName, Long deptId, String operatorLabel) {
        sendToDeptAdmins(
                deptId,
                "员工离职提醒",
                String.format(
                        Locale.ROOT,
                        "您的员工%s已离职，请及时处理相关事宜，操作人：%s。",
                        safeEmployeeName(employeeName),
                        safeOperatorLabel(operatorLabel)
                )
        );
    }

    public void sendEmployeeDisabledReminder(String employeeName, Long deptId, String operatorLabel) {
        sendToDeptAdmins(
                deptId,
                "员工账号禁用提醒",
                String.format(
                        Locale.ROOT,
                        "您的员工%s已被禁用系统账户，请及时处理相关事宜，操作人：%s。",
                        safeEmployeeName(employeeName),
                        safeOperatorLabel(operatorLabel)
                )
        );
    }

    public void sendEmployeeDeletedReminder(String employeeName, Long deptId, String operatorLabel) {
        sendToDeptAdmins(
                deptId,
                "员工账号删除提醒",
                String.format(
                        Locale.ROOT,
                        "您的员工%s已被删除系统账户，请及时处理相关事宜，操作人：%s。",
                        safeEmployeeName(employeeName),
                        safeOperatorLabel(operatorLabel)
                )
        );
    }

    private void sendToDeptAdmins(Long deptId, String title, String content) {
        if (deptId == null || !StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return;
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, ROLE_ADMIN)
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getStatus, USER_STATUS_ENABLED)
                .orderByAsc(SysUser::getId);

        List<SysUser> recipients = sysUserMapper.selectList(wrapper);
        for (SysUser recipient : recipients) {
            SysMessage message = new SysMessage();
            message.setRecipientUserId(recipient.getId());
            message.setRecipientDeptId(deptId);
            message.setTitle(title.trim());
            message.setContent(content.trim());
            message.setIsRead(MESSAGE_UNREAD);
            sysMessageMapper.insert(message);
        }
    }

    private void requireMessageAccess() {
        if (!authzService.isAdmin()) {
            throw BusinessException.forbidden("仅部门管理员可访问消息中心");
        }
    }

    private SysMessage requireOwnedMessage(Long id) {
        SysMessage message = sysMessageMapper.selectById(id);
        if (message == null) {
            throw BusinessException.notFound("消息不存在");
        }
        Long currentUserId = authzService.currentUser().getId();
        if (!currentUserId.equals(message.getRecipientUserId())) {
            throw BusinessException.forbidden("无权限操作该消息");
        }
        return message;
    }

    private MessageVO toVO(SysMessage message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setRead(Integer.valueOf(MESSAGE_READ).equals(message.getIsRead()));
        vo.setReadTime(message.getReadTime());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private String safeEmployeeName(String employeeName) {
        return StringUtils.hasText(employeeName) ? employeeName.trim() : "员工";
    }

    private String safeOperatorLabel(String operatorLabel) {
        return StringUtils.hasText(operatorLabel) ? operatorLabel.trim() : "系统";
    }
}