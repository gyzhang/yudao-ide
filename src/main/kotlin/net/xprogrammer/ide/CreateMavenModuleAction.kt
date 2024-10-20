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
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import net.xprogrammer.ide.settings.PluginSettings
import org.jetbrains.idea.maven.model.MavenConstants
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenUtil
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime

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
    private val YUDAO_MODULE_PREFIX = "yudao-module-"

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

        val moduleNameShort = Messages.showInputDialog(
            project,
            "请输入模块名称：\n(无需带yudao-module-前缀，如demo，生成的模块为【yudao-module-demo】)",
            "创建[芋道]项目的 Maven 模块",
            Messages.getQuestionIcon()
        )

        if (!moduleNameShort.isNullOrEmpty()) {
            val moduleName = YUDAO_MODULE_PREFIX + moduleNameShort
            var yudaoModule: VirtualFile? = null
            var created: Boolean = false
            try {
                val apiSubModuleName = moduleName + "-api"
                val bizSubModuleName = moduleName + "-biz"

                val replacements = mapOf(
                    "moduleNameShort" to moduleNameShort,
                    "moduleName" to moduleName,
                    "apiSubModuleName" to apiSubModuleName,
                    "bizSubModuleName" to bizSubModuleName,
                    "moduleCreateTime" to LocalDateTime.now().toString()
                )
                val readme = loadTemplate("yudao/ide/template/readme.md", replacements)
                val pom = loadTemplate("yudao/ide/template/pom/module.pom.xml", replacements)
                val pomApi = loadTemplate("yudao/ide/template/pom/sub-module-api.pom.xml", replacements)
                val pomBiz = loadTemplate("yudao/ide/template/pom/sub-module-biz.pom.xml", replacements)
                val errorCodeJava = loadTemplate("yudao/ide/template/java/ErrorCodeConstants.java", replacements)

                val moduleFiles = mapOf(
                    "readme.md" to readme,
                    MavenConstants.POM_XML to pom
                )
                val apiModuleFiles = mapOf(
                    "src/main/java/cn/iocoder/yudao/module/$moduleNameShort/enums/ErrorCodeConstants.java" to errorCodeJava,
                    MavenConstants.POM_XML to pomApi
                )
                val bizModuleFiles = mapOf(
                    MavenConstants.POM_XML to pomBiz
                )

                WriteAction.run<Exception> {
                    yudaoModule = createMavenModule(virtualFile, moduleName, MavenConstants.TYPE_POM, moduleFiles)
                    yudaoModule?.let {
                        createMavenModule(it, apiSubModuleName, MavenConstants.TYPE_JAR, apiModuleFiles)
                        createMavenModule(it, bizSubModuleName, MavenConstants.TYPE_JAR, bizModuleFiles)
                    } ?: logger.warn("yudaoModule is null")

                    created = true
                }
            } catch (e: Exception) {
                logger.error("Error creating Maven module", e)
            }

            if (created) {
                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.runWriteCommandAction(project) {
                        val subPomFile = getPsiFile(project, yudaoModule!!.findChild(MavenConstants.POM_XML))
                        val pomFile = getPsiFile(project, virtualFile.findChild(MavenConstants.POM_XML))

                        if (pomFile != null && subPomFile != null) {
                            logger.info("格式化父模块和新创建模块的 pom.xml 文件。")
                            CodeStyleManager.getInstance(project).reformat(subPomFile)
                            CodeStyleManager.getInstance(project).reformat(pomFile)
                        } else {
                            logger.warn("PsiFile is null when trying to reformat.")
                        }
                    }

                    logger.info("刷新 Maven 项目。")
                    MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles()
                }

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
        files: Map<String, String>
    ): VirtualFile? {
        try {
            val moduleDir = parentDir.createChildDirectory(this, moduleName)

            if (MavenConstants.TYPE_POM == type) { // 创建模块
                // 如果需要处理 POM 类型的逻辑可以在这里添加
            }
            if (MavenConstants.TYPE_JAR == type) {
                VfsUtil.createDirectories(moduleDir.path + "/src/main/java")
                VfsUtil.createDirectories(moduleDir.path + "/src/main/resources")
                VfsUtil.createDirectories(moduleDir.path + "/src/test/java")
                VfsUtil.createDirectories(moduleDir.path + "/src/test/resources")

                var packagePaths = arrayOf("")
                val packagePrefix = YUDAO_MODULE + moduleName.split('-').getOrNull(2) + "/"
                val settings: PluginSettings = service()

                if (moduleName.contains("api")) { // api 子模块默认的包
                    val packages = settings.getApiModulePackages()
                    packagePaths = packages.split(",")
                        .map { "$packagePrefix$it" }
                        .toTypedArray()
                }
                if (moduleName.contains("biz")) { // biz 子模块默认的包
                    val packages = settings.getBizModulePackages()
                    packagePaths = packages.split(",")
                        .map { "$packagePrefix$it" }
                        .toTypedArray()
                }

                packagePaths.forEach { path ->
                    VfsUtil.createDirectoryIfMissing(moduleDir.path + "/src/main/java/" + path)
                }
            }

            for ((fileName, fileContent) in files) {
                val absoluteFilePath = moduleDir.path + "/" + fileName
                val fileParentDir = File(absoluteFilePath).parentFile
                if (!fileParentDir.exists()) {
                    fileParentDir.mkdirs()
                }
                val file = moduleDir.findFileByRelativePath(fileName)
                if (file == null) {
                    val dir = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(fileParentDir.absolutePath))
                    val createdFile = dir?.createChildData(this, getFileNameFromPath(fileName))
                    createdFile?.setBinaryContent(fileContent.toByteArray())
                } else {
                    file.setBinaryContent(fileContent.toByteArray())
                }
            }

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

    private fun loadTemplate(templatePath: String, replacements: Map<String, String>): String {
        val inputStream = this::class.java.classLoader.getResourceAsStream(templatePath)
        var result = inputStream?.bufferedReader()?.use { it.readText() } ?: return ""
        replacements.forEach { (key, value) ->
            result = result.replace("\$$key", value)
        }
        return result
    }

    /**
     * 提取文件名，不要路径
     */
    private fun getFileNameFromPath(filePath: String): String {
        // 使用标准库中的 lastIndexOf 函数找到最后一个斜杠的位置
        val lastSlashIndex = filePath.lastIndexOf('/')

        // 如果找不到斜杠，说明整个字符串就是文件名
        return if (lastSlashIndex == -1) {
            filePath
        } else {
            // 从最后一个斜杠后面的部分提取文件名
            filePath.substring(lastSlashIndex + 1)
        }
    }

}