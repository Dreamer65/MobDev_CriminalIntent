package com.bignerdranch.android.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time
import java.util.*

@Entity
data class Crime(@PrimaryKey val id: UUID = UUID.randomUUID(),
                 var title: String = "",
                 var date: Date = Date(),
                 var time: Time = Time(0,0,0),
                 var isSolved: Boolean = false,
                 var requiresPolice: Boolean = false,
                 var suspect: String = "",
                 var uri: String = "") {

    val photoFileName
        get() = "IMG_$id.jpg"
}