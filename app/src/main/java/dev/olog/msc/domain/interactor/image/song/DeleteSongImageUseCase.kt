package dev.olog.msc.domain.interactor.image.song

import dev.olog.msc.domain.entity.Song
import dev.olog.msc.domain.executors.IoScheduler
import dev.olog.msc.domain.gateway.LastFmGateway
import dev.olog.msc.domain.interactor.base.CompletableUseCaseWithParam
import io.reactivex.Completable
import javax.inject.Inject

class DeleteSongImageUseCase @Inject constructor(
        schedulers: IoScheduler,
        private val gateway: LastFmGateway

) : CompletableUseCaseWithParam<Song>(schedulers) {

    override fun buildUseCaseObservable(param: Song): Completable {
        val albumId = param.albumId
        val songId = param.id

        if (param.album.isNotBlank()){
            return gateway.deleteAlbumImage(albumId)
                    .andThen(gateway.deleteTrackImage(songId))
        }

        return gateway.deleteTrackImage(songId)
    }
}