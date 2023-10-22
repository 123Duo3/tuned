package ink.duo3.tuned.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "RecentlyUpdatedPodcast")
data class RecUpdPodEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "podId")
    val podId: Int,
    @ColumnInfo(name = "enclosure")
    val enclosure: String,
    @ColumnInfo(name = "podName")
    val podName: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "imageUrl")
    val imageUrl: String,
    @ColumnInfo(name = "pubDate")
    val pubDateInMs: Long,
    @ColumnInfo(name = "length")
    val length: String
)
