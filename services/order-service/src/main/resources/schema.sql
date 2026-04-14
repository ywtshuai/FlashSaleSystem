CREATE TABLE IF NOT EXISTS `order_info` (
  `id` BIGINT NOT NULL COMMENT '订单ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-新建未支付，1-已支付，2-已发货',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
