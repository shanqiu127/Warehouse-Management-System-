package org.example.back.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.NoticeQueryDTO;
import org.example.back.dto.NoticeSaveDTO;
import org.example.back.entity.SysNotice;
import org.example.back.mapper.SysNoticeMapper;
import org.example.back.vo.NoticeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {

    @Autowired
    private SysNoticeMapper sysNoticeMapper;

    public PageResult<NoticeVO> page(NoticeQueryDTO queryDTO) {
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getTitle()), SysNotice::getTitle, queryDTO.getTitle())
                .eq(queryDTO.getStatus() != null, SysNotice::getStatus, queryDTO.getStatus())
                .orderByDesc(SysNotice::getPublishTime)
                .orderByDesc(SysNotice::getId);

        Page<SysNotice> page = sysNoticeMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<NoticeVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public NoticeVO getById(Long id) {
        SysNotice notice = requireNotice(id);
        return toVO(notice);
    }

    public void create(NoticeSaveDTO dto) {
        SysNotice notice = new SysNotice();
        BeanUtils.copyProperties(dto, notice);
        notice.setPublisher(currentRealName());
        if (notice.getStatus() != null && notice.getStatus() == 1 && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        sysNoticeMapper.insert(notice);
    }

    public void update(Long id, NoticeSaveDTO dto) {
        SysNotice notice = requireNotice(id);
        BeanUtils.copyProperties(dto, notice);
        notice.setId(id);
        notice.setPublisher(currentRealName());
        if (notice.getStatus() != null && notice.getStatus() == 1 && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        sysNoticeMapper.updateById(notice);
    }

    public void delete(Long id) {
        requireNotice(id);
        sysNoticeMapper.physicalDeleteById(id);
    }

    private SysNotice requireNotice(Long id) {
        SysNotice notice = sysNoticeMapper.selectById(id);
        if (notice == null) {
            throw BusinessException.notFound("公告不存在");
        }
        return notice;
    }

    private NoticeVO toVO(SysNotice notice) {
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(notice, vo);
        vo.setAuthor(notice.getPublisher());
        vo.setDate(notice.getPublishTime());
        return vo;
    }

    private String currentRealName() {
        Object userInfo = StpUtil.getSession().get("userInfo");
        if (userInfo instanceof LoginResponse.UserInfoVO loginUser) {
            return loginUser.getRealName();
        }
        return "系统";
    }
}