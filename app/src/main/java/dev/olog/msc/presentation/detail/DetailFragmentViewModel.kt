package dev.olog.msc.presentation.detail

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import dagger.Lazy
import dev.olog.msc.constants.PlaylistConstants
import dev.olog.msc.domain.entity.SortArranging
import dev.olog.msc.domain.entity.SortType
import dev.olog.msc.domain.interactor.MoveItemInPlaylistUseCase
import dev.olog.msc.domain.interactor.RemoveFromPlaylistUseCase
import dev.olog.msc.domain.interactor.detail.GetDetailTabsVisibilityUseCase
import dev.olog.msc.domain.interactor.detail.item.GetArtistFromAlbumUseCase
import dev.olog.msc.domain.interactor.detail.sorting.*
import dev.olog.msc.presentation.model.DisplayableItem
import dev.olog.msc.utils.MediaId
import dev.olog.msc.utils.MediaIdCategory
import dev.olog.msc.utils.k.extension.asLiveData
import dev.olog.msc.utils.k.extension.emitThenDebounce
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables

class DetailFragmentViewModel(
        val mediaId: MediaId,
        item: Map<MediaIdCategory, @JvmSuppressWildcards Flowable<List<DisplayableItem>>>,
        albums: Map<MediaIdCategory, @JvmSuppressWildcards Observable<List<DisplayableItem>>>,
        data: Map<String, @JvmSuppressWildcards Observable<List<DisplayableItem>>>,
        private val headers: DetailFragmentHeaders,
        private val getArtistFromAlbumUseCase: GetArtistFromAlbumUseCase,
        private val setSortOrderUseCase: SetSortOrderUseCase,
        private val observeSortOrderUseCase: GetSortOrderUseCase,
        private val setSortArrangingUseCase: SetSortArrangingUseCase,
        private val getSortArrangingUseCase: GetSortArrangingUseCase,
        private val moveItemInPlaylistUseCase: Lazy<MoveItemInPlaylistUseCase>,
        getVisibleTabsUseCase : GetDetailTabsVisibilityUseCase,
        val getDetailSortDataUseCase: GetDetailSortDataUseCase,
        private val removeFromPlaylistUseCase: RemoveFromPlaylistUseCase

) : ViewModel() {

    companion object {
        const val RECENTLY_ADDED = "RECENTLY_ADDED"
        const val MOST_PLAYED = "MOST_PLAYED"
        const val RELATED_ARTISTS = "RELATED_ARTISTS"
        const val SONGS = "SONGS"

        const val NESTED_SPAN_COUNT = 4
        const val VISIBLE_RECENTLY_ADDED_PAGES = NESTED_SPAN_COUNT * 4
        const val RELATED_ARTISTS_TO_SEE = 10
    }

    var firstAccess = true

    private val currentCategory = mediaId.category

    val itemLiveData: LiveData<List<DisplayableItem>> = item[currentCategory]!!.asLiveData()

    fun artistMediaId() : Maybe<MediaId> {
        if (mediaId.isAlbum){
            return getArtistFromAlbumUseCase
                    .execute(mediaId)
                    .firstElement()
                    .map { MediaId.artistId(it.id) }
        } else {
            return Maybe.empty()
        }

    }

    val mostPlayedLiveData: LiveData<List<DisplayableItem>> = data[MOST_PLAYED]!!
            .asLiveData()

    val relatedArtistsLiveData : LiveData<List<DisplayableItem>> = data[RELATED_ARTISTS]!!
            .map { it.take(RELATED_ARTISTS_TO_SEE) }
            .asLiveData()

    val recentlyAddedLiveData: LiveData<List<DisplayableItem>> = data[RECENTLY_ADDED]!!
            .map { it.take(VISIBLE_RECENTLY_ADDED_PAGES) }
            .asLiveData()



    val data : Observable<MutableMap<DetailFragmentDataType, MutableList<DisplayableItem>>> = Observables.combineLatest(
            item[currentCategory]!!.toObservable().doOnNext { println("next item") },
            data[MOST_PLAYED]!!.doOnNext { println("next most played") },
            data[RECENTLY_ADDED]!!.doOnNext { println("next recent") },
            albums[currentCategory]!!.doOnNext { println("next albums") },
            data[RELATED_ARTISTS]!!.doOnNext { println("next artists") },
            data[SONGS]!!.doOnNext { println("next songs") },
            getVisibleTabsUseCase.execute(),
            { item, mostPlayed, recent, albums, artists, songs, visibility ->

                mutableMapOf(
                        DetailFragmentDataType.HEADER to item.toMutableList(),
                        DetailFragmentDataType.MOST_PLAYED to handleMostPlayedHeader(mostPlayed.toMutableList(), visibility[0]),
                        DetailFragmentDataType.RECENT to handleRecentlyAddedHeader(recent.toMutableList(), visibility[1]),
                        DetailFragmentDataType.SONGS to handleSongsHeader(songs.toMutableList()),
                        DetailFragmentDataType.ARTISTS_IN to handleRelatedArtistsHeader(artists.toMutableList(), visibility[2]),
                        DetailFragmentDataType.ALBUMS to handleAlbumsHeader(albums.toMutableList(), item)
                ) }
    ).emitThenDebounce()

    private fun handleMostPlayedHeader(list: MutableList<DisplayableItem>, isEnabled: Boolean) : MutableList<DisplayableItem>{
        if (isEnabled && list.isNotEmpty()){
            list.clear()
            list.addAll(0, headers.mostPlayed)
        } else {
            list.clear()
        }
        return list
    }

    private fun handleRecentlyAddedHeader(list: MutableList<DisplayableItem>, isEnabled: Boolean) : MutableList<DisplayableItem>{
        if (isEnabled && list.isNotEmpty()){
            val size = list.size
            list.clear()
            list.addAll(0, headers.recent(size, size > VISIBLE_RECENTLY_ADDED_PAGES))
        } else {
            list.clear()
        }
        return list
    }

    private fun handleAlbumsHeader(list: MutableList<DisplayableItem>, item: List<DisplayableItem>) : MutableList<DisplayableItem>{
        val albumsList = list.toMutableList()
        if (albumsList.isNotEmpty()){
            val artist = when {
                mediaId.isAlbum -> item[1].subtitle
                else -> null
            }
            albumsList.add(0, headers.albums(artist))
        }

        return albumsList
    }

    private fun handleRelatedArtistsHeader(list: MutableList<DisplayableItem>, isEnabled: Boolean) : MutableList<DisplayableItem>{
        if (isEnabled && list.isNotEmpty()){
            val size = list.size
            list.clear()
            list.addAll(0, headers.relatedArtists(size > 10))
        } else {
            list.clear()
        }
        return list
    }

    private fun handleSongsHeader(list: MutableList<DisplayableItem>) : MutableList<DisplayableItem>{
        if (list.isNotEmpty()) {
            list.addAll(0, headers.songs)
        } else {
            list.add(headers.no_songs)
        }
        return list
    }

    fun updateSortType(sortType: SortType): Completable {
        return setSortOrderUseCase.execute(SetSortOrderRequestModel(
                mediaId, sortType))
    }

    fun toggleSortArranging(): Completable {
        return setSortArrangingUseCase.execute()
    }

    fun observeSortOrder(): Observable<SortType> {
        return observeSortOrderUseCase.execute(mediaId)
    }

    fun getSortArranging(): Observable<SortArranging> {
        return getSortArrangingUseCase.execute()
    }

    fun moveItemInPlaylist(from: Int, to: Int){
        if (!mediaId.isPlaylist){
            throw IllegalStateException("not a playlist")
        }
        val playlistId = mediaId.categoryValue.toLong()
        moveItemInPlaylistUseCase.get().execute(playlistId, from, to)
    }

    fun removeFromPlaylist(item: DisplayableItem): Completable {
        if (!mediaId.isPlaylist){
            throw IllegalStateException("not a playlist")
        }
        val playlistId = mediaId.categoryValue.toLong()
        if (playlistId == PlaylistConstants.FAVORITE_LIST_ID){
            // favorites use songId instead of idInPlaylist
            return removeFromPlaylistUseCase.execute(playlistId to item.mediaId.leaf!!)
        }
        return removeFromPlaylistUseCase.execute(playlistId to item.trackNumber.toLong())
    }

}