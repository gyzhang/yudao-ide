必须读我！必须读我！必须读我！

1、创建模块后，请执行如下 SQL 语句，为模块创建顶层菜单，以供后续代码生成的菜单接入；
    -- 在菜单表中插入 $moduleNameShort 模块的顶层菜单，后续生成的代码都用这个菜单作为父菜单
    INSERT INTO `ruoyi-vue-pro`.`system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `updater`, `deleted`) VALUES ('$moduleNameShort模块', '', 1, 999, 0, '/$moduleNameShort', 'fa:rocket', NULL, NULL, 0, b'1', b'1', b'1', '1', '1', b'0');

本模块创建于：$moduleCreateTime，by xprogrammer@YuDaoIDE

Powered by https://gitee.com/zhijiantianya