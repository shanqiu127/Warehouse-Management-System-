package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.back.entity.BizSalesReturn;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface BizSalesReturnMapper extends BaseMapper<BizSalesReturn> {

		@Select("""
						<script>
						SELECT COALESCE(SUM(total_price), 0)
						FROM biz_sales_return
						WHERE is_deleted = 0
							AND operation_time <![CDATA[>=]]> #{startTime}
							AND operation_time <![CDATA[<]]> #{endTime}
						</script>
						""")
		BigDecimal sumReturnAmount(@Param("startTime") LocalDateTime startTime,
															 @Param("endTime") LocalDateTime endTime);

		@Select("""
						<script>
						SELECT COALESCE(SUM(quantity), 0)
						FROM biz_sales_return
						WHERE is_deleted = 0
							AND operation_time <![CDATA[>=]]> #{startTime}
							AND operation_time <![CDATA[<]]> #{endTime}
						</script>
						""")
		Long sumReturnQuantity(@Param("startTime") LocalDateTime startTime,
													 @Param("endTime") LocalDateTime endTime);
}
