package com.smnm.englishtrackingai

data class VocabWord(
    val word: String,
    val meaning: String,
    val sentence: String,
    var isVisible: Boolean = false
)
