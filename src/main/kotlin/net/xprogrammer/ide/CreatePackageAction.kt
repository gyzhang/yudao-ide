package net.xprogrammer.ide

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

private const val YUDAO_MODULE = "cn/iocoder/yudao/module/"

private const val SOURCE_MAIN_JAVA = "/src/main/java"

private const val POM_XML = "pom.xml"

class CreatePackageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile: VirtualFile? = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)

        // 检查选中的是否是一个目录
        if (virtualFile != null && virtualFile.isDirectory) {
            // 在当前目录下查找pom.xml文件
            val pomFile = virtualFile.findChild(POM_XML)

            if (pomFile != null && pomFile.exists()) {
                val result = Messages.showOkCancelDialog(
                    project,
                    "请你确定，是否创建模块内包结构？",
                    "创建YuDao模块结构",
                    "好的，请创建",
                    "算了，放弃吧",
                    Messages.getQuestionIcon()
                )

                if (result == Messages.OK) {
                    val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT) ?: return
                    val module: Module? = ModuleUtilCore.findModuleForPsiElement(psiElement)
                    if (module == null) return
                    val sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots()
                    if (sourceRoots.isEmpty()) return
                    var selectedSourceRoot: VirtualFile = sourceRoots.first()
                    for (sourceRoot in sourceRoots) {
                        if (sourceRoot.toString().indexOf(SOURCE_MAIN_JAVA) != -1){
                            selectedSourceRoot = sourceRoot
                            break
                        }
                    }

                    val parts = virtualFile.toString().split('/').takeLast(1).get(0).split('-').takeLast(2)
                    val moduleName = parts.get(0) //模块名称，如yudao-module-demo-api中的demo
                    val subModuleName = parts.get(1) //子模块名称，如yudao-module-demo-api中的api
                    val packagePrefix = YUDAO_MODULE + moduleName //模块前缀
                    var packagePaths = arrayOf("")

                    if ("api".equals(subModuleName)){ //api子模块默认的包
                        packagePaths = arrayOf(
                            packagePrefix + "/api",
                            packagePrefix + "/enums"
                        )
                    }
                    if ("biz".equals(subModuleName)){ //biz子模块默认的包
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

                    // 使用 Application.runWriteAction() 方法来包装写操作代码。这个方法确保代码在正确的上下文中执行，并处理所有必要的线程同步问题。
                    ApplicationManager.getApplication().runWriteAction {
                        packagePaths.forEach { path ->
                            VfsUtil.createDirectoryIfMissing(selectedSourceRoot, path)
                        }
                    }
                }
            } else { // 如果没有找到pom.xml文件，则显示错误或提示信息
                Messages.showMessageDialog(
                    project,
                    "请选择一个Maven模块来创建YuDao项目结构。",
                    "错误",
                    Messages.getErrorIcon()
                )
            }
        } else { // 如果没有选中任何目录或选中的不是一个目录，则显示错误或提示信息
            Messages.showMessageDialog(
                project,
                "请选择一个Maven模块来创建YuDao项目结构。",
                "错误",
                Messages.getErrorIcon()
            )
        }
    }
}
