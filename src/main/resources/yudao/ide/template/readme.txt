必须读我！必须读我！必须读我！
为了不污染当前的项目，在创建 $moduleNameShort 模块后，需要在项目中手动执行一些操作，将 $moduleNameShort 模块集成到项目中。

1、创建 $moduleNameShort 模块后，请执行如下 SQL 语句，为 $moduleNameShort 模块创建顶层菜单，以供后续代码生成的菜单接入：
    -- 在菜单表(system_menu)中插入 $moduleNameShort 模块的顶层菜单，后续生成的代码都用这个菜单作为父菜单
    INSERT INTO `ruoyi-vue-pro`.`system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `updater`, `deleted`) VALUES ('$moduleNameShort模块', '', 1, 999, 0, '/$moduleNameShort', 'fa:rocket', NULL, NULL, 0, b'1', b'1', b'1', '1', '1', b'0');

2、创建 $moduleNameShort 模块后，请将如下依赖项拷贝到 yudao-server/pom.xml，以将 $moduleNameShort 模块集成到项目中：
    <dependency>
        <groupId>cn.iocoder.boot</groupId>
        <artifactId>yudao-module-$moduleNameShort-biz</artifactId>
        <version>${revision}</version>
    </dependency>

3、在全局错误码 ServiceErrorCodeRange 类文件中添加注释，为 $moduleNameShort 模块分配全局的错误码区间范围说明：
    // 模块 $moduleNameShort 错误码区间 [1-606-000-000 ~ 1-607-000-000)

4、在 yudao-server/src/main/resources/application-local.yaml 中添加 MyBatis 查询日志输出配置信息：
    logging.level.cn.iocoder.yudao.module.$moduleNameShort.dal.mysql: debug

5、在 SystemWebConfiguration 文件中增加 $moduleNameShort 模块的 API 接口分组：
    /**
     * $moduleNameShort 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi $moduleNameShortGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("$moduleNameShort");
    }

6、请使用如下 DDL 创建 $moduleNameShort 模块的 $moduleNameShort_hello_world 表，然后使用项目中的代码生成器，生成测试验证代码：
    -- 表名前缀要和模块名称一致，比如这里的 $moduleNameShort_，原因是：代码生成器会自动解析表名的前缀，获得其所属的 Maven Module 模块，简化配置过程
    CREATE TABLE `$moduleNameShort_hello_world` (
      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
      `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
      `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '描述',
      `status` tinyint NOT NULL COMMENT '状态',
      `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
      `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
      PRIMARY KEY (`id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成验证';

7、代码生成器在导入表后，需要进一步修改其配置，有两点需要注意的地方：
    7.1 在“修改生成配置”的“生成信息”页签的“上级菜单”下拉框中，为生成代码的功能选择上级菜单（本文第1步的 insert 语句创建的上级菜单），以将本功能添加到 $moduleNameShort 模块中
    7.2 在“修改生成配置”的“字段信息”页签中，为使用字典选项的字段（如 status）选择当前项目中已经存在的字典类型（如“系统状态”，如没有你需要的字典类型，则需要在代码生成前添加后选择）

8、 解压生成代码的 zip 文件后，按照目录执行或拷贝文件：
    8.1 执行 sql 目录下的 sql.sql 文件，将根据表生成的菜单配置信息添加到数据库中
    8.2 拷贝 yudao-module-$moduleNameShort 到后端项目中，注意 ErrorCodeConstants_手动操作.java 文件中的错误码示例，并需要自行编码
    8.3 yudao-ui-admin-vue3 中的 src目录到前端项目中

9、启动前后端项目，开始你的[芋道]编程之旅。

本模块创建于：$moduleCreateTime, by xprogrammer@YuDaoIDE, Kevin Zhang

Powered by https://gitee.com/zhijiantianya