package com.kexdev.andlibs.aop.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile

class AopPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)
        //只有application和library支持此插件
        if (!hasApp && !hasLib) {
            throw new IllegalStateException("'com.android.application' or 'com.android.library' plugin required.")
        }

        final def log = project.logger

        final def variants
        if (hasApp) {
            variants = project.android.applicationVariants
        } else {
            variants = project.android.libraryVariants
        }

        //添加参数
        project.extensions.create("aopPlugin", AopPluginExtension)

        variants.all { variant ->
            //判断aop开关
            if (!project.aopPlugin.enabled) {
                log.warn("AopPlugin is disabled.")
                return
            }

            //判断编译release时的开关
            if (project.aopPlugin.onlyDebug && !variant.buildType.isDebuggable()) {
                log.warn("Skipping non-debuggable build type '${variant.buildType.name}'.")
                return
            }

            //处理注入
            extAop(project, variant)
        }
    }

    static void extAop(Project project, def variant) {
        //JavaCompile javaCompile = variant.javaCompiler
        TaskProvider<JavaCompile> taskProvider = variant.javaCompileProvider
        JavaCompile javaCompile = taskProvider.get()
        javaCompile.doLast {
            //注入参数
            String[] args = [
                    "-showWeaveInfo",
                    "-1.5",
                    "-inpath", javaCompile.destinationDir.toString(),
                    "-aspectpath", javaCompile.classpath.asPath,
                    "-d", javaCompile.destinationDir.toString(),
                    "-classpath", javaCompile.classpath.asPath,
                    "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)
            ]

            def log = project.logger
            log.debug "ajc args: " + Arrays.toString(args)

            //调用ajc进行注入
            MessageHandler handler = new MessageHandler(true)
            new Main().run(args, handler)

            //显示日志
            for (IMessage message : handler.getMessages(null, true)) {
                switch (message.getKind()) {
                    case IMessage.ABORT:
                    case IMessage.ERROR:
                    case IMessage.FAIL:
                        log.error message.message, message.thrown
                        break
                    case IMessage.WARNING:
                    case IMessage.INFO:
                        log.info message.message, message.thrown
                        break
                    case IMessage.DEBUG:
                        log.debug message.message, message.thrown
                        break
                }
            }
        }
    }

}
