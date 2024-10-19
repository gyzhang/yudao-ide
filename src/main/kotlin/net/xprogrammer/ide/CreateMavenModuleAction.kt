package net.xprogrammer.ide

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
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
import net.xprogrammer.ide.settings.PluginSettings
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

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.getData(CommonDataKeys.PROJECT)
        val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        project ?: return
        virtualFile ?: return

        val module: Module? = ModuleUtil.findModuleForFile(virtualFile, project)
        module ?: return
        if (!MavenUtil.isMavenModule(module)) {
            Messages.showErrorDialog(project, "请右键单击 Maven 模块。", "错误")
            return
        }

        // 以下都是在满足maven module的基础上进行
        val moduleName = Messages.showInputDialog(
            project,
            "请输入模块名称：",
            "创建[芋道]项目的 Maven 模块",
            Messages.getQuestionIcon()
        )
        if (!moduleName.isNullOrEmpty()) {
            var yudaoModule: VirtualFile? = null
            var created: Boolean = false
            try {
                val apiSubModuleName = moduleName + "-api";
                val bizSubModuleName = moduleName + "-biz";

                val replacements = mapOf(
                    "moduleName" to moduleName,
                    "apiSubModuleName" to apiSubModuleName,
                    "bizSubModuleName" to bizSubModuleName
                )
                val pom = loadPomTemplate("yudao/ide/template/pom/module.pom.xml", replacements)
                val pomApi = loadPomTemplate("yudao/ide/template/pom/sub-module-api.pom.xml", replacements)
                val pomBiz = loadPomTemplate("yudao/ide/template/pom/sub-module-biz.pom.xml", replacements)

                WriteAction.run<Exception> {
                    yudaoModule = createMavenModule(virtualFile, moduleName, MavenConstants.TYPE_POM, pom)
                    yudaoModule?.let {
                        createMavenModule(it, apiSubModuleName, MavenConstants.TYPE_JAR, pomApi)
                        createMavenModule(it, bizSubModuleName, MavenConstants.TYPE_JAR, pomBiz)
                    } ?: logger.warn("yudaoModule is null")

                    created = true
                }
            } catch (e: Exception) {
                logger.error("Error creating Maven module", e)
            }

            if (created) {
                ApplicationManager.getApplication().invokeLater({
                    val subPomFile = getPsiFile(project, yudaoModule!!.findChild(MavenConstants.POM_XML))
                    val pomFile = getPsiFile(project, virtualFile.findChild(MavenConstants.POM_XML))

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
                })


                logger.info("成功：在 '${virtualFile.path + File.separator + module.name}' 位置下创建了[芋道]模块['$moduleName']。")
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("listener")
                    .createNotification(
                        "YuDao IDE",
                        "[芋道]模块【'$moduleName'】创建成功。",
                        NotificationType.INFORMATION
                    )
                    .notify(e.project)
                Messages.showMessageDialog(
                    project,
                    "在 '${virtualFile.path + File.separator + module.name}' 位置下创建了[芋道]模块['$moduleName']。",
                    "模块创建成功",
                    Messages.getInformationIcon()
                )
            }
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
                val packagePrefix = YUDAO_MODULE + moduleName.split('-').getOrNull(2) + "/"
                val settings: PluginSettings = service()

                if (moduleName.contains("api")) { //api子模块默认的包
                    val packages = settings.getApiModulePackages()
                    packagePaths = packages.split(",")
                        .map { "$packagePrefix$it" }
                        .toTypedArray()
                }
                if (moduleName.contains("biz")) { //biz子模块默认的包
                    val packages = settings.getBizModulePackages()
                    packagePaths = packages.split(",")
                        .map { "$packagePrefix$it" }
                        .toTypedArray()
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

    private fun loadPomTemplate(templatePath: String, replacements: Map<String, String>): String {
        val inputStream = this::class.java.classLoader.getResourceAsStream(templatePath)
        val pom = inputStream?.bufferedReader()?.use { it.readText() } ?: return ""
        var result = pom
        replacements.forEach { (key, value) ->
            result = result.replace("\$$key", value)
        }
        return result
    }

}