package com.flashsale.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Update("""
            UPDATE inventory
            SET available_stock = available_stock - 1,
                version = version + 1
            WHERE product_id = #{productId}
              AND available_stock > 0
              AND version = #{version}
            """)
    int deductStockWithVersion(@Param("productId") Long productId, @Param("version") Integer version);
}
