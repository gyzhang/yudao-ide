package net.xprogrammer.ide

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import net.xprogrammer.ide.settings.RevisionSettings
import org.jetbrains.idea.maven.model.MavenConstants
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenUtil
import java.io.File

/**
 * 参考了：
 * 1、ChatGPT生成的代码
 * 2、https://github.com/JetBrains/intellij-community/plugins/maven/src/main/java/org/jetbrains/idea/maven/wizards/MavenModuleBuilderHelper.kt
 * @author Kevin Zhang
 *
 */
class CreateMavenModuleAction : AnAction() {
    private val logger = Logger.getInstance(CreateMavenModuleAction::class.java)
    private val YUDAO_MODULE = "cn/iocoder/yudao/module/"

    // 芋道源码的当前版本，应该从上下文中获取的，简化为一个常量（不想写了）
    var revision = "2.2.0-snapshot"

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.getData(CommonDataKeys.PROJECT)
        val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (project == null || virtualFile == null) {
            return
        }

        val module: Module? = ModuleUtil.findModuleForFile(virtualFile, project)

        if (module != null && MavenUtil.isMavenModule(module)) {
            // 弹出输入对话框，获取模块名
            val moduleName = Messages.showInputDialog(
                project,
                "请输入模块名称：",
                "创建[芋道]项目的 Maven 模块",
                Messages.getQuestionIcon()
            )
            if (!moduleName.isNullOrEmpty()) {
                // 示例代码：从插件的配置信息中获取修订版本号，后续可使用到生成的 pom.xml 文件中
                val settings: RevisionSettings = service()
                val storedRevision = settings.getRevision()
                println("Stored Revision: $storedRevision")

                var yudaoModule: VirtualFile? = null
                var created: Boolean = false
                try {
                    val apiSubModuleName = moduleName + "-api";
                    val bizSubModuleName = moduleName + "-biz";

                    // 读取 pom.xml 文件
                    val inputStreamModulePom =
                        this::class.java.classLoader.getResourceAsStream("yudao/ide/template/module.pom.xml")
                    val pom = inputStreamModulePom?.bufferedReader().use { it?.readText() }.toString()
                        .replace("\$moduleName", moduleName)

                    val inputStreamSubModuleApiPom =
                        this::class.java.classLoader.getResourceAsStream("yudao/ide/template/sub-module-api.pom.xml")
                    val pomApi = inputStreamSubModuleApiPom?.bufferedReader().use { it?.readText() }.toString()
                        .replace("\$moduleName", moduleName).replace("\$apiSubModuleName", apiSubModuleName)

                    val inputStreamSubModuleBizPom =
                        this::class.java.classLoader.getResourceAsStream("yudao/ide/template/sub-module-biz.pom.xml")
                    val pomBiz = inputStreamSubModuleBizPom?.bufferedReader().use { it?.readText() }.toString()
                        .replace("\$moduleName", moduleName).replace("\$bizSubModuleName", bizSubModuleName).replace("\$apiSubModuleName", apiSubModuleName)

                    WriteAction.run<Exception> {
                        yudaoModule = createMavenModule(virtualFile, moduleName, MavenConstants.TYPE_POM, pom)
                        yudaoModule!!.let {
                            createMavenModule(it, apiSubModuleName, MavenConstants.TYPE_JAR, pomApi)
                            createMavenModule(it, bizSubModuleName, MavenConstants.TYPE_JAR, pomBiz)
                        }
                        created = true
                    }
                } catch (e: Exception) {
                    logger.error("Error creating Maven module", e)
                }

                if (created) {
                    ApplicationManager.getApplication().invokeLater({
                        val subPomFile = getPsiFile(project, yudaoModule!!.findChild(MavenConstants.POM_XML))
                        val pomFile = getPsiFile(project, virtualFile!!.findChild(MavenConstants.POM_XML))

                        if (pomFile != null && subPomFile != null) {
                            logger.info("格式化父模块和新创建模块的 pom.xml 文件。")
                            WriteCommandAction.runWriteCommandAction(project) {
                                CodeStyleManager.getInstance(project).reformat(subPomFile)
                                CodeStyleManager.getInstance(project).reformat(pomFile)
                            }
                        } else {

                            logger.warn("PsiFile is null when trying to reformat.")
                        }
                        logger.info("刷新 Maven 项目。")
                        MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles()
                    }, ModalityState.nonModal())

                    logger.info("成功：在 '${virtualFile.path + File.separator + module.name}' 位置下创建了[芋道]模块['$moduleName']。")
                    Messages.showMessageDialog(
                        project,
                        "在 '${virtualFile.path + File.separator + module.name}' 位置下创建了[芋道]模块['$moduleName']。",
                        "模块创建成功",
                        Messages.getInformationIcon()
                    )
                }
            }
        } else {
            Messages.showErrorDialog(project, "请右键单击 Maven 模块。", "错误")
        }
    }

    private fun createMavenModule(
        parentDir: VirtualFile,
        moduleName: String,
        type: String,
        pom: String
    ): VirtualFile? {
        try {
            val moduleDir = parentDir.createChildDirectory(this, moduleName)
            if (MavenConstants.TYPE_JAR == type) {
                VfsUtil.createDirectories(moduleDir.path + "/src/main/java")
                VfsUtil.createDirectories(moduleDir.path + "/src/main/resources")
                VfsUtil.createDirectories(moduleDir.path + "/src/test/java")
                VfsUtil.createDirectories(moduleDir.path + "/src/test/resources")

                var packagePaths = arrayOf("")
                val packagePrefix = YUDAO_MODULE + moduleName.split('-').getOrNull(2)

                if (moduleName.contains("api")){ //api子模块默认的包
                    packagePaths = arrayOf(
                        packagePrefix + "/api",
                        packagePrefix + "/enums"
                    )
                }
                if (moduleName.contains("biz")){ //biz子模块默认的包
                    packagePaths = arrayOf(
                        packagePrefix + "/controller/admin",
                        packagePrefix + "/controller/user",
                        packagePrefix + "/convert",
                        packagePrefix + "/dal",
                        packagePrefix + "/job",
                        packagePrefix + "/mq",
                        packagePrefix + "/service"
                    )
                }

                packagePaths.forEach { path ->
                    VfsUtil.createDirectoryIfMissing(moduleDir.path + "/src/main/java/" + path)
                }
            }
            // 创建 pom.xml 文件并写入内容
            val pomFile = moduleDir.createChildData(this, MavenConstants.POM_XML)
            pomFile.setBinaryContent(pom.toByteArray())
            // 更新父模块的 pom.xml 文件
            val parentPomFile = parentDir.findChild(MavenConstants.POM_XML)
            if (parentPomFile != null) {
                val parentPomContent = parentPomFile.contentsToByteArray().toString(Charsets.UTF_8)
                val updatedParentPomContent = updateParentPom(parentPomContent, moduleName)
                parentPomFile.setBinaryContent(updatedParentPomContent.toByteArray())
            }
            return moduleDir
        } catch (e: Exception) {
            logger.error("Failed to create Maven module: ${e.message}")
            return null
        }
    }

    /**
     * 更新父 pom.xml 文件以添加新模块。
     * 为了简化代码，pom.xml文件中必须存在<modules></modules>标签，提前在模板文件中提供这个标签。
     */
    private fun updateParentPom(pomContent: String, moduleName: String): String {
        val modulesEndTag = "</modules>"
        return pomContent.replace(modulesEndTag, "    <module>$moduleName</module>\n$modulesEndTag")
    }

    private fun getPsiFile(project: Project?, pom: VirtualFile?): PsiFile? {
        return PsiManager.getInstance(project!!).findFile(pom!!)
    }
}