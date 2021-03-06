/*
 * Copyright 2014 Fidesmo AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.fidesmo.gradle.javacard

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile


class ConvertJavacardTask extends DefaultTask {

    @Input String aid
    @Input String fullyQualifiedPackageName
    @Input String version
    @Input Map<String, String> applets
    @Input String sdkVersion

    String getJavacardHome() {
        JavacardPlugin.getJavacardHome(project)
    }


    def javacardDirectory = new File(project.getBuildDir(), 'javacard')

    private def getPackageFilepath() {
        getFullyQualifiedPackageName().replace('.', File.separator)
    }

    private def getPackageName() {
        getFullyQualifiedPackageName().split('\\.').last()
    }

    @OutputFile
    def getCapFile() {
        new File(javacardDirectory, "${getPackageFilepath()}/javacard/${getPackageName()}.cap")
    }

    @OutputFile
    def getExpFile() {
        new File(javacardDirectory, "${getPackageFilepath()}/javacard/${getPackageName()}.exp")
    }

    @InputDirectory
    def getClassesDir() {
        new File(project.sourceSets.main.output.classesDir, getPackageFilepath())
    }

    @TaskAction
    def convert() {
        project.dependencies {
            if (sdkVersion ==~ /3.0.[0-4]/) {
                javacardTools project.files("${getJavacardHome()}/lib/jctasks.jar")
                javacardTools project.files("${getJavacardHome()}/lib/tools.jar")
            } else {
                javacardTools project.files("${getJavacardHome()}/ant-tasks/lib/jctasks.jar")
                javacardTools project.files("${getJavacardHome()}/lib/converter.jar")
                javacardTools project.files("${getJavacardHome()}/lib/offcardverifier.jar")
            }

            javacardExport project.files("${getJavacardHome()}/api_export_files")
        }

        ant.taskdef(name: 'convert',
                    classname: 'com.sun.javacard.ant.tasks.ConverterTask',
                    classpath: project.configurations.javacardTools.asPath)

        ant.convert(CAP: true,
                    EXP: true,
                    packagename: getFullyQualifiedPackageName(),
                    packageaid: getAid(),
                    majorminorversion: getVersion(),
                    debug: true,
                    classdir: project.sourceSets.main.output.classesDir,
                    outputdirectory: new File(project.getBuildDir(), 'javacard'),
                    exportpath: project.configurations.javacardExport.asPath,
                    classpath: project.configurations.javacardTools.asPath) {

            getApplets().each() { aid, className ->
                AppletNameAID(appletname: "${getFullyQualifiedPackageName()}.${className}" , aid: aid )
            }
        }
    }
}
