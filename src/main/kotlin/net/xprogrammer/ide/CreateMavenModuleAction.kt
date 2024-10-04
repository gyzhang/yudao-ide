package net.xprogrammer.ide

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
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
    // 芋道源码的当前版本，应该从上下文中获取的，简化为一个常量（不想写了）
    val revision = "2.2.0-snapshot"

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
                var created = false

                try {
                    WriteAction.run<Exception> {
                        created = createMavenModule(project, virtualFile, moduleName, revision, MavenConstants.TYPE_POM)
                    }
                } catch (e: Exception) {
                    logger.error("Error creating Maven module", e)
                }

                if (created) {
                    ApplicationManager.getApplication().invokeLater({
                        val psiFile = getPsiFile(project, virtualFile.findChild(MavenConstants.POM_XML))
                        if (psiFile != null) {
                            logger.info("格式化父模块的 pom.xml 文件。")
                            WriteCommandAction.runWriteCommandAction(project) {
                                CodeStyleManager.getInstance(project).reformat(psiFile)
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
        project: Project,
        parentDir: VirtualFile,
        moduleName: String,
        revision: String,
        type: String
    ): Boolean {
        try {
            val moduleDir = parentDir.createChildDirectory(this, moduleName)
            if (MavenConstants.TYPE_JAR == type) {
                VfsUtil.createDirectories(moduleDir.path + "/src/main/java")
                VfsUtil.createDirectories(moduleDir.path + "/src/main/resources")
                VfsUtil.createDirectories(moduleDir.path + "/src/test/java")
                VfsUtil.createDirectories(moduleDir.path + "/src/test/resources")
            }

            val pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>cn.iocoder.boot</groupId>
                    <artifactId>yudao</artifactId>
                    <version>$revision</version>
                </parent>
            
                <artifactId>$moduleName</artifactId>
                <packaging>pom</packaging>
                <name>$moduleName</name>
            
                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
            
            </project>
        """.trimIndent()

            // 创建 pom.xml 文件并写入内容
            val pomFile = moduleDir.createChildData(this, MavenConstants.POM_XML)
            pomFile.setBinaryContent(pomContent.toByteArray())
            // 更新父模块的 pom.xml 文件
            val parentPomFile = parentDir.findChild(MavenConstants.POM_XML)
            if (parentPomFile != null) {
                val parentPomContent = parentPomFile.contentsToByteArray().toString(Charsets.UTF_8)
                val updatedParentPomContent = updateParentPom(parentPomContent, moduleName)
                parentPomFile.setBinaryContent(updatedParentPomContent.toByteArray())
            }
            return true
        } catch (e: Exception) {
            logger.error("Failed to create Maven module: ${e.message}")
            return false
        }
    }

    /**
     * 更新父 pom.xml 文件以添加新模块
     */
    private fun updateParentPom(pomContent: String, moduleName: String): String {
        // 找到 <modules> 标签的位置
        val modulesStartTag = "<modules>"
        val modulesEndTag = "</modules>"

        if (pomContent.contains(modulesStartTag)) {
            // 如果已经存在 <modules> 标签，直接添加新模块
            return pomContent.replace(modulesEndTag, "    <module>$moduleName</module>\n$modulesEndTag")
        } else {
            // 如果不存在 <modules> 标签，创建一个
            return pomContent.replace(
                "<project",
                "<project>\n    <modules>\n        <module>$moduleName</module>\n    </modules>",
                ignoreCase = true
            )
        }
    }

    private fun getPsiFile(project: Project?, pom: VirtualFile?): PsiFile? {
        return PsiManager.getInstance(project!!).findFile(pom!!)
    }
}