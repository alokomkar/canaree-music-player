package dev.olog.msc.presentation.player

import android.os.Bundle
import android.support.v4.math.MathUtils
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import dev.olog.msc.R
import dev.olog.msc.constants.AppConstants
import dev.olog.msc.constants.AppConstants.PROGRESS_BAR_INTERVAL
import dev.olog.msc.constants.PlaylistConstants
import dev.olog.msc.presentation.base.BaseFragment
import dev.olog.msc.presentation.base.adapter.TouchHelperAdapterCallback
import dev.olog.msc.presentation.base.music.service.MediaProvider
import dev.olog.msc.presentation.model.DisplayableItem
import dev.olog.msc.presentation.navigator.Navigator
import dev.olog.msc.presentation.widget.SwipeableView
import dev.olog.msc.utils.MediaId
import dev.olog.msc.utils.isMarshmallow
import dev.olog.msc.utils.k.extension.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.android.synthetic.main.fragment_player.view.*
import kotlinx.android.synthetic.main.player_controls.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

class PlayerFragment : BaseFragment(), SlidingUpPanelLayout.PanelSlideListener {

    @Inject lateinit var viewModel: PlayerFragmentViewModel
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var adapter : PlayerFragmentAdapter
    private lateinit var layoutManager : LinearLayoutManager

    private lateinit var mediaProvider : MediaProvider

    private var seekBarDisposable : Disposable? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mediaProvider = (activity as MediaProvider)

        mediaProvider.onQueueChanged()
                .distinctUntilChanged()
                .mapToList { it.toDisplayableItem() }
                .map { queue ->
                    val copy = queue.toMutableList()
                    if (copy.size > PlaylistConstants.MINI_QUEUE_SIZE - 1){
                        copy.add(viewModel.footerLoadMore)
                    }
                    copy.add(0, viewModel.playerControls())
                    copy
                }
                .asLiveData()
                .subscribe(this, viewModel::updateQueue)

        viewModel.observeMiniQueue()
                .subscribe(this, adapter::updateDataSet)

        mediaProvider.onStateChanged()
                .asLiveData()
                .subscribe(this, {
                    val bookmark = it.extractBookmark()
                    viewModel.updateProgress(bookmark)
                    handleSeekBar(bookmark, it.state == PlaybackStateCompat.STATE_PLAYING)
                })

        if (act.isLandscape && !AppConstants.THEME.isFullscreen()){
            mediaProvider.onMetadataChanged()
                    .asLiveData()
                    .subscribe(this, cover::loadImage)

            mediaProvider.onStateChanged()
                    .asLiveData()
                    .subscribe(this, cover::toggleElevation)

            mediaProvider.onRepeatModeChanged()
                    .asLiveData()
                    .subscribe(this, repeat::cycle)

            mediaProvider.onShuffleModeChanged()
                    .asLiveData()
                    .subscribe(this, shuffle::cycle)

            mediaProvider.onStateChanged()
                    .map { it.state }
                    .filter { state -> state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT ||
                            state == PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS }
                    .map { state -> state == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT }
                    .asLiveData()
                    .subscribe(this, this::animateSkipTo)

            mediaProvider.onStateChanged()
                    .map { it.state }
                    .filter { it == PlaybackStateCompat.STATE_PLAYING ||
                            it == PlaybackStateCompat.STATE_PAUSED
                    }.distinctUntilChanged()
                    .asLiveData()
                    .subscribe(this, { state ->

                        if (state == PlaybackStateCompat.STATE_PLAYING){
                            playAnimation(true)
                        } else {
                            pauseAnimation(true)
                        }
                    })

            RxView.clicks(next)
                    .asLiveData()
                    .subscribe(this, { mediaProvider.skipToNext() })

            RxView.clicks(playPause)
                    .asLiveData()
                    .subscribe(this, { mediaProvider.playPause() })

            RxView.clicks(previous)
                    .asLiveData()
                    .subscribe(this, { mediaProvider.skipToPrevious() })

            viewModel.observePlayerControlsVisibility()
                    .filter { !AppConstants.THEME.isFullscreen() }
                    .asLiveData()
                    .subscribe(this, {
                        previous.toggleVisibility(it, true)
                        playPause.toggleVisibility(it, true)
                        next.toggleVisibility(it, true)
                    })

            viewModel.skipToNextVisibility.asLiveData()
                    .subscribe(this, next::updateVisibility)

            viewModel.skipToPreviousVisibility.asLiveData()
                    .subscribe(this, previous::updateVisibility)
        }
    }

    override fun onViewBound(view: View, savedInstanceState: Bundle?) {
        layoutManager = LinearLayoutManager(context)
        view.list.adapter = adapter
        view.list.layoutManager = layoutManager
        view.list.setHasFixedSize(true)
        val callback = TouchHelperAdapterCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(view.list)
        adapter.touchHelper = touchHelper

        val statusBarAlpha = if (!isMarshmallow()) 1f else 0f
        view.statusBar?.alpha = statusBarAlpha
    }

    private fun animateSkipTo(toNext: Boolean) {
        if (getSlidingPanel().isCollapsed()) return

        if (toNext) {
            next.playAnimation()
        } else {
            previous.playAnimation()
        }
    }

    private fun playAnimation(animate: Boolean) {
        playPause.animationPlay(getSlidingPanel().isExpanded() && animate)
    }

    private fun pauseAnimation(animate: Boolean) {
        playPause.animationPause(getSlidingPanel().isExpanded() && animate)
    }

    private fun handleSeekBar(bookmark: Int, isPlaying: Boolean){
        seekBarDisposable.unsubscribe()

        if (isPlaying){
            seekBarDisposable = Observable.interval(PROGRESS_BAR_INTERVAL.toLong(), TimeUnit.MILLISECONDS, Schedulers.computation())
                    .map { (it + 1) * PROGRESS_BAR_INTERVAL + bookmark }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ viewModel.updateProgress(it.toInt()) }, Throwable::printStackTrace)
        }
    }

    override fun onResume() {
        super.onResume()
        activity!!.slidingPanel.setScrollableView(list)
        swipeableView?.setOnSwipeListener(onSwipeListener)
        shuffle?.setOnClickListener { mediaProvider.toggleShuffleMode() }
        repeat?.setOnClickListener { mediaProvider.toggleRepeatMode() }
        getSlidingPanel()!!.addPanelSlideListener(this)
    }

    override fun onPause() {
        super.onPause()
        swipeableView?.setOnSwipeListener(null)
        shuffle?.setOnClickListener(null)
        repeat?.setOnClickListener(null)
        getSlidingPanel()!!.removePanelSlideListener(this)
    }

    override fun onStop() {
        super.onStop()
        seekBarDisposable.unsubscribe()
    }

    private val onSwipeListener = object : SwipeableView.SwipeListener {
        override fun onSwipedLeft() {
            mediaProvider.skipToNext()
        }

        override fun onSwipedRight() {
            mediaProvider.skipToPrevious()
        }

        override fun onClick() {
            mediaProvider.playPause()
        }

        override fun onLeftEdgeClick() {
            mediaProvider.skipToPrevious()
        }

        override fun onRightEdgeClick() {
            mediaProvider.skipToNext()
        }
    }

    override fun onPanelSlide(panel: View?, slideOffset: Float) {
        if (!isMarshmallow() && slideOffset in .9f..1f){
            val alpha = (1 - slideOffset) * 10
            statusBar?.alpha = MathUtils.clamp(abs(1 - alpha), 0f, 1f)
        }
    }

    override fun onPanelStateChanged(panel: View, previousState: SlidingUpPanelLayout.PanelState, newState: SlidingUpPanelLayout.PanelState) {
    }

    private fun MediaSessionCompat.QueueItem.toDisplayableItem(): DisplayableItem {
        val description = this.description

        return DisplayableItem(
                R.layout.item_mini_queue,
                MediaId.fromString(description.mediaId!!),
                description.title!!.toString(),
                DisplayableItem.adjustArtist(description.subtitle!!.toString()),
                description.mediaUri!!.toString(),
                isPlayable = true,
                trackNumber = "${this.queueId}"
        )
    }

    override fun provideLayoutId(): Int {
        return if(AppConstants.THEME.isFullscreen()){
            R.layout.fragment_player_fullscreen
        } else R.layout.fragment_player
    }
}