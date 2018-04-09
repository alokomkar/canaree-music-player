package dev.olog.msc.presentation.edit.artist

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import dev.olog.msc.domain.entity.Artist
import dev.olog.msc.domain.entity.Song
import dev.olog.msc.presentation.edit.UpdateResult
import dev.olog.msc.utils.k.extension.context
import dev.olog.msc.utils.k.extension.unsubscribe
import dev.olog.msc.utils.media.store.notifyMediaStore
import io.reactivex.disposables.Disposable
import org.jaudiotagger.tag.TagOptionSingleton

class EditArtistFragmentViewModel(
        application: Application,
        private val presenter: EditArtistFragmentPresenter

) : AndroidViewModel(application){

    private val songList = MutableLiveData<List<Song>>()

    private val displayedArtist = MutableLiveData<DisplayableArtist>()

    private var songListDisposable: Disposable? = null
    private var artistDisposable: Disposable? = null

    init {
        TagOptionSingleton.getInstance().isAndroid = true

        artistDisposable = presenter.getArtist()
                .subscribe({
                    this.displayedArtist.postValue(it.toDisplayableArtist())
                }, Throwable::printStackTrace)

        songListDisposable = presenter.getSongList()
                .subscribe({
                    songList.postValue(it)
                }, Throwable::printStackTrace)
    }

    override fun onCleared() {
        songListDisposable.unsubscribe()
    }

    fun observeData(): LiveData<DisplayableArtist> = displayedArtist
    fun observeSongList(): LiveData<List<Song>> = songList

    fun updateMetadata(
            artist: String
    ) : UpdateResult {
        if (artist.isBlank()) return UpdateResult.EMPTY_TITLE

        try {
            presenter.deleteLastFmEntry()
            presenter.updateSongList(artist)
            presenter.getSongList()
            for (song in presenter.songList) {
                notifyMediaStore(context, song.path)
            }

            return UpdateResult.OK
        } catch (ex: Exception){
            ex.printStackTrace()
            return UpdateResult.ERROR
        }
    }

    private fun Artist.toDisplayableArtist(): DisplayableArtist {
        return DisplayableArtist(
                this.id,
                this.name
        )
    }

}