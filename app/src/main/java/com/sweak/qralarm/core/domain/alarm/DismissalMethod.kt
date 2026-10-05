package com.sweak.qralarm.core.domain.alarm

enum class DismissalMethod {
    NONE, CODE, OBJECT;

    val requiresCamera: Boolean get() = this != NONE
}
