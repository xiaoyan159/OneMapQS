package com.navinfo.omqs.ui.fragment.evaluationresult

import android.graphics.drawable.AnimationDrawable
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ChatMsgEntity
import com.navinfo.omqs.databinding.AdapterSoundListBinding
import com.navinfo.omqs.ui.other.BaseRecyclerViewAdapter
import com.navinfo.omqs.ui.other.BaseViewHolder
import java.io.*

/**
 * 语音 RecyclerView 适配器
 *
 * 在 RecycleView 的 ViewHolder 中监听 ViewModel 的 LiveData，然后此时传递的 lifecycleOwner 是对应的 Fragment。由于 ViewHolder 的生命周期是比 Fragment 短的，所以当 ViewHolder 销毁时，由于 Fragment 的 Lifecycle 还没有结束，此时 ViewHolder 会发生内存泄露（监听的 LiveData 没有解绑）
 *   这种场景下有两种解决办法：
 *使用 LiveData 的 observeForever 然后在 ViewHolder 销毁前手动调用 removeObserver
 *使用 LifecycleRegistry 给 ViewHolder 分发生命周期(这里使用了这个)
 */
class SoundtListAdapter(
) : BaseRecyclerViewAdapter<ChatMsgEntity>() {

    //媒体播放器
    private val mMediaPlayer = MediaPlayer()

    //媒体播放器
    private var md: MediaPlayer? = null

    private var mAudioTrack: AudioTrack? = null

    //录音结束后，右侧显示图片
    private var animView: View? = null

    //录音时动画效果
    private var animaV: AnimationDrawable? = null

    private var itemClick: OnItemClickListner? = null

    //最大宽度
    private val maxWidth = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val viewBinding =
            AdapterSoundListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(viewBinding)
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)

    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val binding: AdapterSoundListBinding =
            holder.viewBinding as AdapterSoundListBinding
        val entity = data[position]
        //tag 方便onclick里拿到数据
        holder.tag = entity.name.toString()
        holder.viewBinding.tvTime.isSelected = entity.isDelete
        holder.viewBinding.rlSoundContent.isSelected = entity.isDelete
        holder.viewBinding.ivSoundAnim.setBackgroundResource(R.drawable.icon_sound_03)
/*        if (itemClick != null) {
            holder.viewBinding.rlSoundContent.setOnClickListener {
                itemClick!!.onItemClick(it.findViewById<View>(R.id.rl_sound_content), position)
            }
        }*/
        //mixWidth
        if (!TextUtils.isEmpty(entity.name)) {
/*            if (entity.name.indexOf(".pcm") > 0) {
                val file: File = File(entity.voiceUri + entity.name)
                if (file != null) {
                    val time = (file.length() / 16000).toInt()
                    val layoutParams: ViewGroup.LayoutParams =
                        holder.viewBinding.rlSoundContent.getLayoutParams()
                    layoutParams.width = 115 + time * 10
                    layoutParams.width =
                        if (layoutParams.width > layoutParams.width) maxWidth else layoutParams.width
                    holder.viewBinding.rlSoundContent.setLayoutParams(layoutParams)
                    holder.viewBinding.tvTime.text = time.toString() + "\""
                }
            } else {
                try {
                    md = MediaPlayer()
                    md!!.reset()
                    md!!.setDataSource(entity.voiceUri+entity.name)
                    md!!.prepare()
                } catch (e: Exception) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
                var time =
                    if (entity.voiceTimeLong == null) md!!.duration.toString() + "" else entity.voiceTimeLong
                        .toString() + ""
                if (!TextUtils.isEmpty(time)) {
                    val i = md!!.duration / 1000
                    time = i.toString() + "\""
                    val layoutParams: ViewGroup.LayoutParams =
                        holder.viewBinding.rlSoundContent.getLayoutParams()
                    layoutParams.width = 115 + i * 10
                    layoutParams.width =
                        if (layoutParams.width > layoutParams.width) maxWidth else layoutParams.width
                    holder.viewBinding.rlSoundContent.layoutParams = layoutParams
                }
                holder.viewBinding.tvTime.text = time
                md!!.release()
            }*/
        }
    }

    /**
     * 播放某段录音
     *
     * @param view  显示动画
     * @param index 录音在集合中索引
     */
    fun setPlayerIndex(view: View, index: Int) {
        val imageV = view.findViewById<View>(R.id.iv_sound_anim) as ImageView
        val width = view.width
        if (animView != null) {
            animaV?.stop()
            animView!!.setBackgroundResource(R.drawable.icon_sound_03)
        }
        animView = imageV
        val entity: ChatMsgEntity = data.get(index)
        playMusic(entity.voiceUri + entity.name, imageV)
    }

    /**
     * 播放录音
     *
     * @param name 录音名称
     * @Description
     */
    fun playMusic(name: String, imageV: ImageView) {
        imageV.setBackgroundResource(R.drawable.sound_anim)
        animaV = imageV.background as AnimationDrawable
        animaV!!.start()
/*        if (name.index(".pcm") > 0) {
            audioTrackPlay(name, imageV)
        } else {
            mediaPlayer(name, imageV)
        }*/
    }

    fun mediaPlayer(name: String, imageV: ImageView) {
        try {
            if (mMediaPlayer.isPlaying) {
                mMediaPlayer.stop()
            }
            mMediaPlayer.reset()
            mMediaPlayer.setDataSource(name)
            mMediaPlayer.prepare()
            mMediaPlayer.start()
            //播放结束
            mMediaPlayer.setOnCompletionListener {
                animaV!!.stop()
                imageV.setBackgroundResource(R.drawable.icon_sound_03)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun audioTrackPlay(name: String, imageV: ImageView) {
        var dis: DataInputStream? = null
        try {
            //从音频文件中读取声音
            dis = DataInputStream(BufferedInputStream(FileInputStream(name)))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        //最小缓存区
        val bufferSizeInBytes = AudioTrack.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        //创建AudioTrack对象   依次传入 :流类型、采样率（与采集的要一致）、音频通道（采集是IN 播放时OUT）、量化位数、最小缓冲区、模式
        if (mAudioTrack != null) {
            mAudioTrack!!.stop()
            mAudioTrack!!.release()
            mAudioTrack = null
        }
        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes,
            AudioTrack.MODE_STREAM
        )
        val data = ByteArray(bufferSizeInBytes)
        mAudioTrack!!.play() //开始播放
        while (true) {
            var i = 0
            try {
                while (dis!!.available() > 0 && i < data.size) {
                    data[i] = dis.readByte() //录音时write Byte 那么读取时就该为readByte要相互对应
                    i++
                }
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            mAudioTrack!!.write(data, 0, data.size)
            if (i != bufferSizeInBytes) //表示读取完了
            {
                break
            }
        }
        mAudioTrack!!.stop() //停止播放
        mAudioTrack!!.release() //释放资源
        mAudioTrack = null
        imageV.post {
            animaV?.stop()
            imageV.setBackgroundResource(R.drawable.icon_sound_03)
        }
    }

    fun setOnItemClickListener(clickListner: OnItemClickListner) {
        itemClick = clickListner
    }

    interface OnItemClickListner {
        fun onItemClick(view: View?, postion: Int)
    }

    override fun getItemViewRes(position: Int): Int {
        return R.layout.adapter_sound_list
    }
}


