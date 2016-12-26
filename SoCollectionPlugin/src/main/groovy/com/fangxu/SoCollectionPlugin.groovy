package com.fangxu

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer

class SoCollectionPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.tasks.whenTaskAdded {
            task->
                if (task.name.startsWith('transformNative_libsWithMergeJniLibsFor')) {
                    def flavor = getFlavor(task.name, 'transformNative_libsWithMergeJniLibsFor', null)
                    def md5 = project.tasks.create("md5_" + flavor, Md5Task.class)
                    md5.dependsOn task
                }
                if (task.name.startsWith('merge') && task.name.endsWith('Assets')) {
                    def flavor = getFlavor(task.name, 'merge', 'Assets')
                    task.dependsOn "md5_" + flavor
                }
        }
    }

    def getFlavor(String name, String bSign, String eSign){
        int pos = name.indexOf(bSign) + bSign.length();
        int end;
        if (eSign == null) {
            end = name.length()
        } else {
            end = name.indexOf(eSign);
        }

        String flavor = "md5_default";
        if ( end > pos ){
            flavor = name.subSequence(pos,end);
        }

        print flavor
        return flavor;
    }
}