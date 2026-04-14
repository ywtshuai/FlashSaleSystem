CREATE TABLE IF NOT EXISTS `inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
  `available_stock` INT NOT NULL DEFAULT 0 COMMENT '可用库存',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

INSERT INTO `inventory` (`product_id`, `total_stock`, `available_stock`, `version`)
SELECT 1, 20, 20, 0
WHERE NOT EXISTS (SELECT 1 FROM `inventory` WHERE `product_id` = 1);
