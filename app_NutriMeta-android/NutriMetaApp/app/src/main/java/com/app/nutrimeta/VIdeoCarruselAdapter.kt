package com.app.nutrimeta

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView

class VideoCarouselAdapter(
    private val videoList: List<Int>,
    private val onVideoEnded: () -> Unit
) : RecyclerView.Adapter<VideoCarouselAdapter.VideoViewHolder>() {

    private val holders = mutableMapOf<Int, VideoViewHolder>()
    private var currentPlayingPosition = 0

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoView: VideoView = itemView.findViewById(R.id.videoViewCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.items_carrusel_cad, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holders[position] = holder
        val videoResId = videoList[position]
        val context = holder.itemView.context

        val videoUri = Uri.parse("android.resource://${context.packageName}/$videoResId")
        holder.videoView.setVideoURI(videoUri)

        // Cuando el video finaliza, pasa a la siguiente
        holder.videoView.setOnCompletionListener {
            if (position == currentPlayingPosition) {
                onVideoEnded()
            }
        }

        holder.videoView.setOnPreparedListener {
            if (position == currentPlayingPosition) {
                holder.videoView.seekTo(0)
                holder.videoView.start()
            }
        }
    }

    // Reproduce el video actual y pausa los demás
    fun playVideoAt(position: Int) {
        currentPlayingPosition = position
        holders.forEach { (pos, holder) ->
            if (pos == position) {
                holder.videoView.seekTo(0)
                holder.videoView.start()
            } else {
                holder.videoView.pause()
            }
        }
    }

    override fun getItemCount(): Int = videoList.size
}