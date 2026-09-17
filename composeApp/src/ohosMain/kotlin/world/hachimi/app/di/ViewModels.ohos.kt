package world.hachimi.app.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import world.hachimi.app.model.*

/** CPF bindings replace Koin compiler-generated registrations. */
fun Module.applyViewModels() {
    viewModel { ArtworkDetailViewModel(get(), get()) }
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { CategorySongsViewModel(get(), get()) }
    viewModel { ChangelogViewModel(get(), get()) }
    viewModel { ContributorEntryViewModel(get(), get()) }
    viewModel { CreatePostViewModel(get(), get()) }
    viewModel { DeviceManagementViewModel(get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
    viewModel { EventDetailViewModel(get(), get()) }
    viewModel { EventsListViewModel(get(), get()) }
    viewModel { parameters -> FollowViewModel(parameters.get(), get(), get()) }
    viewModel { ForgetPasswordViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { MyPRViewModel(get(), get()) }
    viewModel { PlayerViewModel(get(), get()) }
    viewModel { PlaylistDetailViewModel(get(), get()) }
    viewModel { PlaylistViewModel(get(), get()) }
    viewModel { PublicPlaylistViewModel(get(), get()) }
    viewModel { PublishViewModel(get(), get()) }
    viewModel { PublishedTabViewModel(get(), get()) }
    viewModel { RecentLikeViewModel(get(), get()) }
    viewModel { RecentPlayViewModel(get(), get()) }
    viewModel { RecentPublishViewModel(get(), get()) }
    viewModel { RecommendViewModel(get(), get()) }
    viewModel { ReviewDetailViewModel(get(), get()) }
    viewModel { ReviewHistoryViewModel(get(), get()) }
    viewModel { ReviewViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { UserSpaceViewModel(get(), get()) }
    viewModel { WeeklyHotViewModel(get(), get()) }
}

