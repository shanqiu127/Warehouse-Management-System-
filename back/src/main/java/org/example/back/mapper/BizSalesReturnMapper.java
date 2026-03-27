package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.back.entity.BizSalesReturn;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 销售退货记录 Mapper 接口,数据库操作接口
 */
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

		@Select("""
						<script>
						SELECT COALESCE(SUM(total_price), 0)
						FROM biz_sales_return
						WHERE is_deleted = 0
							AND biz_status = 1
							AND operation_time <![CDATA[>=]]> #{startTime}
							AND operation_time <![CDATA[<]]> #{endTime}
						</script>
						""")
		BigDecimal sumValidReturnAmount(@Param("startTime") LocalDateTime startTime,
												 @Param("endTime") LocalDateTime endTime);

		@Select("""
						<script>
						SELECT COALESCE(SUM(quantity), 0)
						FROM biz_sales_return
						WHERE is_deleted = 0
							AND biz_status = 1
							AND operation_time <![CDATA[>=]]> #{startTime}
							AND operation_time <![CDATA[<]]> #{endTime}
						</script>
						""")
		Long sumValidReturnQuantity(@Param("startTime") LocalDateTime startTime,
										  @Param("endTime") LocalDateTime endTime);

		@Select("""
						<script>
						SELECT COALESCE(SUM(COALESCE(r.cost_total_price, 0)), 0)
						FROM biz_sales_return r
						WHERE r.is_deleted = 0
							AND r.biz_status = 1
							AND r.operation_time <![CDATA[>=]]> #{startTime}
							AND r.operation_time <![CDATA[<]]> #{endTime}
						</script>
						""")
		BigDecimal sumEstimatedReturnCost(@Param("startTime") LocalDateTime startTime,
											  @Param("endTime") LocalDateTime endTime);

		@Select("""
						<script>
						SELECT COALESCE(NULLIF(TRIM(bg.brand), ''), '未标注品牌') AS name,
						       SUM(-r.total_price + COALESCE(r.cost_total_price, 0)) AS amount
						FROM biz_sales_return r
						LEFT JOIN base_goods bg ON r.goods_id = bg.id
						WHERE r.is_deleted = 0
							AND r.biz_status = 1
							AND r.operation_time <![CDATA[>=]]> #{startTime}
							AND r.operation_time <![CDATA[<]]> #{endTime}
						GROUP BY COALESCE(NULLIF(TRIM(bg.brand), ''), '未标注品牌')
						</script>
						""")
		List<BizSalesMapper.BrandAmountAgg> brandGrossProfitPart(@Param("startTime") LocalDateTime startTime,
														 @Param("endTime") LocalDateTime endTime);

		@Select("""
						<script>
						SELECT DATE(r.operation_time) AS stat_date,
						       SUM(r.total_price) AS amount
						FROM biz_sales_return r
						WHERE r.is_deleted = 0
							AND r.biz_status = 1
							AND r.operation_time <![CDATA[>=]]> #{startTime}
							AND r.operation_time <![CDATA[<]]> #{endTime}
						GROUP BY DATE(r.operation_time)
						ORDER BY DATE(r.operation_time)
						</script>
						""")
		List<BizSalesMapper.DailyAmountAgg> dailyValidReturnAmount(@Param("startTime") LocalDateTime startTime,
															@Param("endTime") LocalDateTime endTime);

		@Select("""
						<script>
						SELECT DATE(r.operation_time) AS stat_date,
						       SUM(COALESCE(r.cost_total_price, 0)) AS amount
						FROM biz_sales_return r
						WHERE r.is_deleted = 0
							AND r.biz_status = 1
							AND r.operation_time <![CDATA[>=]]> #{startTime}
							AND r.operation_time <![CDATA[<]]> #{endTime}
						GROUP BY DATE(r.operation_time)
						ORDER BY DATE(r.operation_time)
						</script>
						""")
		List<BizSalesMapper.DailyAmountAgg> dailyEstimatedReturnCost(@Param("startTime") LocalDateTime startTime,
															 @Param("endTime") LocalDateTime endTime);
}
