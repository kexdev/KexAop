package com.kexdev.andlibs.aop.plugin

class AopPluginExtension {

    def enabled = true
    def onlyDebug = true

    def setEnabled(boolean enabled) {
        this.enabled = enabled
    }

    def getEnabled() {
        return enabled
    }

    def getOnlyDebug() {
        return onlyDebug
    }

    void setOnlyDebug(boolean onlyDebug) {
        this.onlyDebug = onlyDebug
    }
}
