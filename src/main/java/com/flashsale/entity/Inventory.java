package com.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Integer totalStock;

    private Integer availableStock;

    private Integer version;
}
