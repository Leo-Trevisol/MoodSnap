package com.br.leo.moodsnap.service.model

import java.util.Date

data class MoodFirebaseModel(
    var date: Date = Date(),
    var moodType: Int = 0,
    var description: String? = null,
    var imagePath: String? = null
)
