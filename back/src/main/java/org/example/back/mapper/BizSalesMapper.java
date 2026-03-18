package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.back.entity.BizSales;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BizSalesMapper extends BaseMapper<BizSales> {

	@Select("SELECT MIN(operation_time) FROM biz_sales WHERE is_deleted = 0")
	LocalDateTime minOperationTime();

	@Select("SELECT MAX(operation_time) FROM biz_sales WHERE is_deleted = 0")
	LocalDateTime maxOperationTime();

	@Select("""
			<script>
			SELECT COALESCE(SUM(total_price), 0)
			FROM biz_sales
			WHERE is_deleted = 0
			  AND operation_time <![CDATA[>=]]> #{startTime}
			  AND operation_time <![CDATA[<]]> #{endTime}
			</script>
			""")
	BigDecimal sumSalesAmount(@Param("startTime") LocalDateTime startTime,
							  @Param("endTime") LocalDateTime endTime);

	@Select("""
			<script>
			SELECT COALESCE(SUM(quantity), 0)
			FROM biz_sales
			WHERE is_deleted = 0
			  AND operation_time <![CDATA[>=]]> #{startTime}
			  AND operation_time <![CDATA[<]]> #{endTime}
			</script>
			""")
	Long sumSalesQuantity(@Param("startTime") LocalDateTime startTime,
						  @Param("endTime") LocalDateTime endTime);

	@Select("""
			<script>
			SELECT goods_name AS name, SUM(quantity) AS quantity
			FROM biz_sales
			WHERE is_deleted = 0
			  AND operation_time <![CDATA[>=]]> #{startTime}
			  AND operation_time <![CDATA[<]]> #{endTime}
			GROUP BY goods_id, goods_name
			ORDER BY quantity DESC
			LIMIT 5
			</script>
			""")
	List<TopGoodsAgg> topGoods(@Param("startTime") LocalDateTime startTime,
							   @Param("endTime") LocalDateTime endTime);

	@Select("""
			<script>
			SELECT COALESCE(NULLIF(TRIM(bg.brand), ''), '未标注品牌') AS name,
				   SUM(s.total_price) AS amount
			FROM biz_sales s
			LEFT JOIN base_goods bg ON s.goods_id = bg.id
			WHERE s.is_deleted = 0
			  AND (bg.is_deleted = 0 OR bg.id IS NULL)
			  AND s.operation_time <![CDATA[>=]]> #{startTime}
			  AND s.operation_time <![CDATA[<]]> #{endTime}
			GROUP BY COALESCE(NULLIF(TRIM(bg.brand), ''), '未标注品牌')
			ORDER BY amount DESC
			</script>
			""")
	List<BrandAmountAgg> brandSalesAmount(@Param("startTime") LocalDateTime startTime,
										  @Param("endTime") LocalDateTime endTime);

	@Select("""
			<script>
			SELECT DATE(operation_time) AS stat_date,
				   SUM(total_price) AS amount
			FROM biz_sales
			WHERE is_deleted = 0
			  AND operation_time <![CDATA[>=]]> #{startTime}
			  AND operation_time <![CDATA[<]]> #{endTime}
			GROUP BY DATE(operation_time)
			ORDER BY DATE(operation_time)
			</script>
			""")
	List<DailyAmountAgg> dailySalesAmount(@Param("startTime") LocalDateTime startTime,
										  @Param("endTime") LocalDateTime endTime);

	class TopGoodsAgg {
		private String name;
		private Long quantity;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getQuantity() {
			return quantity;
		}

		public void setQuantity(Long quantity) {
			this.quantity = quantity;
		}
	}

	class BrandAmountAgg {
		private String name;
		private BigDecimal amount;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
	}

	class DailyAmountAgg {
		private String statDate;
		private BigDecimal amount;

		public String getStatDate() {
			return statDate;
		}

		public void setStatDate(String statDate) {
			this.statDate = statDate;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
	}
}
