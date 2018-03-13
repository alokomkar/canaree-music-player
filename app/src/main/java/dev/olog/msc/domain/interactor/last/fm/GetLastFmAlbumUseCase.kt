package dev.olog.msc.domain.interactor.last.fm

import com.github.dmstocking.optional.java.util.Optional
import dev.olog.msc.app.IoSchedulers
import dev.olog.msc.domain.entity.LastFmAlbum
import dev.olog.msc.domain.gateway.LastFmGateway
import dev.olog.msc.domain.interactor.base.SingleUseCaseWithParam
import io.reactivex.Single
import javax.inject.Inject

class GetLastFmAlbumUseCase @Inject constructor(
        schedulers: IoSchedulers,
        private val gateway: LastFmGateway

): SingleUseCaseWithParam<Optional<out LastFmAlbum?>, LastFmAlbumRequest>(schedulers) {

    override fun buildUseCaseObservable(param: LastFmAlbumRequest): Single<Optional<out LastFmAlbum?>> {
        val (id, title, artist) = param
        return gateway.getAlbum(id, title, artist)
    }
}

data class LastFmAlbumRequest(
        val id: Long,
        val title: String,
        val artist: String
)