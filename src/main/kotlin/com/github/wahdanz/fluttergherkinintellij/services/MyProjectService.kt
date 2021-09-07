package com.github.wahdanz.fluttergherkinintellij.services

import com.intellij.openapi.project.Project
import com.github.wahdanz.fluttergherkinintellij.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
