package com.fangxu

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer

class SoCollectionPlugin implements Plugin<Project> {
    static final String PLUGIN_NAME = "SoCheckPlugin"

    @Override
    void apply(Project project) {
        project.extensions.create(PLUGIN_NAME, SoCheckPluginExtension.class)

        def container = project.extensions.getByType(SoCheckPluginExtension.class)

        def md5 = project.tasks.create('md5', Md5Task.class)

        def tmpFlavor
        def tmpType
        def mergeAssetsTaskName
        def mergeJniTaskName
        def tmpVariant

        project.afterEvaluate {
            container.flavors.each {
                flavor ->
                    container.types.each {
                        type ->
                            tmpFlavor = flavor.substring(0, 1).toUpperCase().concat(flavor.substring(1))
                            tmpType = type.substring(0, 1).toUpperCase().concat(type.substring(1))
                            tmpVariant = tmpFlavor.concat(tmpType)
                            mergeAssetsTaskName = 'merge' + tmpVariant + 'Assets'
                            mergeJniTaskName = 'transformNative_libsWithMergeJniLibsFor' + tmpVariant
                            println mergeAssetsTaskName
                            println mergeJniTaskName
                            project.tasks.getByName mergeAssetsTaskName dependsOn md5
                            md5.dependsOn mergeJniTaskName
                    }
            }
        }
    }
}