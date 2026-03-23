package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.back.entity.SysNotice;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
// 系统公告 Mapper 接口,数据库操作接口
public interface SysNoticeMapper extends BaseMapper<SysNotice> {

	@Delete("DELETE FROM sys_notice WHERE id = #{id}")
	int physicalDeleteById(@Param("id") Long id);
}