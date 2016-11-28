package com.fangxu.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class SoCheckPlugin implements Plugin<Project> {
    static final String PLUGIN_NAME = "SoCheckPlugin"

    @Override
    void apply(Project project) {
        project.extensions.create(PLUGIN_NAME, SoCheckPluginExtension.class)
        def md5 = project.tasks.create('md5', Md5Task.class)

        project.afterEvaluate {
            project.tasks.getByName project.extensions.getByType(SoCheckPluginExtension.class).mergeAssetsTaskName dependsOn md5
            md5.dependsOn project.extensions.getByType(SoCheckPluginExtension.class).mergeJniTaskName
        }
    }
}