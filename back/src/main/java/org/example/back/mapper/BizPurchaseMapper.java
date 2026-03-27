package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.back.entity.BizPurchase;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface BizPurchaseMapper extends BaseMapper<BizPurchase> {

		@Select("""
						<script>
						SELECT p.unit_price
						FROM biz_purchase p
						WHERE p.goods_id = #{goodsId}
							AND p.is_deleted = 0
							AND p.biz_status = 1
							AND p.operation_time <![CDATA[<=]]> #{bizTime}
						ORDER BY p.operation_time DESC, p.id DESC
						LIMIT 1
						</script>
						""")
		BigDecimal latestValidUnitPrice(@Param("goodsId") Long goodsId,
																		@Param("bizTime") LocalDateTime bizTime);
}
