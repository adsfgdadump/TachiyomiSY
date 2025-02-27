package eu.kanade.presentation.more.settings.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.category.genre.SortTagScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.UnsortedPreferences
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_BATTERY_NOT_LOW
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.presentation.core.components.WheelPicker
import tachiyomi.presentation.core.components.WheelPickerDefaults
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsLibraryScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    @StringRes
    override fun getTitleRes() = R.string.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = runBlocking { getCategories.await() })
        // SY -->
        val unsortedPreferences = remember { Injekt.get<UnsortedPreferences>() }
        // SY <--

        return mutableListOf(
            getDisplayGroup(libraryPreferences),
            getCategoriesGroup(LocalNavigator.currentOrThrow, allCategories, libraryPreferences),
            getGlobalUpdateGroup(allCategories, libraryPreferences),
            // SY -->
            getSortingCategory(LocalNavigator.currentOrThrow, libraryPreferences),
            getMigrationCategory(unsortedPreferences),
            // SY <--
        )
    }

    @Composable
    private fun getDisplayGroup(libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val portraitColumns by libraryPreferences.portraitColumns().stateIn(scope).collectAsState()
        val landscapeColumns by libraryPreferences.landscapeColumns().stateIn(scope).collectAsState()

        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            LibraryColumnsDialog(
                initialPortrait = portraitColumns,
                initialLandscape = landscapeColumns,
                onDismissRequest = { showDialog = false },
                onValueChanged = { portrait, landscape ->
                    libraryPreferences.portraitColumns().set(portrait)
                    libraryPreferences.landscapeColumns().set(landscape)
                    showDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_library_columns),
                    subtitle = "${stringResource(R.string.portrait)}: ${getColumnValue(portraitColumns)}, " +
                        "${stringResource(R.string.landscape)}: ${getColumnValue(landscapeColumns)}",
                    onClick = { showDialog = true },
                ),
            ),
        )
    }

    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val userCategoriesCount = allCategories.filterNot(Category::isSystemCategory).size

        val defaultCategory by libraryPreferences.defaultCategory().collectAsState()
        val selectedCategory = allCategories.find { it.id == defaultCategory.toLong() }

        // For default category
        val ids = listOf(libraryPreferences.defaultCategory().defaultValue()) +
            allCategories.fastMap { it.id.toInt() }
        val labels = listOf(stringResource(R.string.default_category_summary)) +
            allCategories.fastMap { it.visualName(context) }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.categories),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.action_edit_categories),
                    subtitle = pluralStringResource(
                        id = R.plurals.num_categories,
                        count = userCategoriesCount,
                        userCategoriesCount,
                    ),
                    onClick = { navigator.push(CategoryScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.defaultCategory(),
                    title = stringResource(R.string.default_category),
                    subtitle = selectedCategory?.visualName ?: stringResource(R.string.default_category_summary),
                    entries = ids.zip(labels).toMap(),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.categorizedDisplaySettings(),
                    title = stringResource(R.string.categorized_display_settings),
                    onValueChanged = {
                        if (!it) {
                            scope.launch {
                                Injekt.get<ResetCategoryFlags>().await()
                            }
                        }
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getGlobalUpdateGroup(
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val libraryUpdateIntervalPref = libraryPreferences.libraryUpdateInterval()
        val libraryUpdateDeviceRestrictionPref = libraryPreferences.libraryUpdateDeviceRestriction()
        val libraryUpdateMangaRestrictionPref = libraryPreferences.libraryUpdateMangaRestriction()
        val libraryUpdateCategoriesPref = libraryPreferences.libraryUpdateCategories()
        val libraryUpdateCategoriesExcludePref = libraryPreferences.libraryUpdateCategoriesExclude()

        val libraryUpdateInterval by libraryUpdateIntervalPref.collectAsState()

        val included by libraryUpdateCategoriesPref.collectAsState()
        val excluded by libraryUpdateCategoriesExcludePref.collectAsState()
        var showDialog by rememberSaveable { mutableStateOf(false) }
        if (showDialog) {
            TriStateListDialog(
                title = stringResource(R.string.categories),
                message = stringResource(R.string.pref_library_update_categories_details),
                items = allCategories,
                initialChecked = included.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                initialInversed = excluded.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    libraryUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    libraryUpdateCategoriesExcludePref.set(newExcluded.map { it.id.toString() }.toSet())
                    showDialog = false
                },
            )
        }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.pref_category_library_update),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    pref = libraryUpdateIntervalPref,
                    title = stringResource(R.string.pref_library_update_interval),
                    entries = mapOf(
                        0 to stringResource(R.string.update_never),
                        12 to stringResource(R.string.update_12hour),
                        24 to stringResource(R.string.update_24hour),
                        48 to stringResource(R.string.update_48hour),
                        72 to stringResource(R.string.update_72hour),
                        168 to stringResource(R.string.update_weekly),
                    ),
                    onValueChanged = {
                        LibraryUpdateJob.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    pref = libraryUpdateDeviceRestrictionPref,
                    enabled = libraryUpdateInterval > 0,
                    title = stringResource(R.string.pref_library_update_restriction),
                    subtitle = stringResource(R.string.restrictions),
                    entries = mapOf(
                        DEVICE_ONLY_ON_WIFI to stringResource(R.string.connected_to_wifi),
                        DEVICE_NETWORK_NOT_METERED to stringResource(R.string.network_not_metered),
                        DEVICE_CHARGING to stringResource(R.string.charging),
                        DEVICE_BATTERY_NOT_LOW to stringResource(R.string.battery_not_low),
                    ),
                    onValueChanged = {
                        // Post to event looper to allow the preference to be updated.
                        ContextCompat.getMainExecutor(context).execute { LibraryUpdateJob.setupTask(context) }
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    pref = libraryUpdateMangaRestrictionPref,
                    title = stringResource(R.string.pref_library_update_manga_restriction),
                    entries = mapOf(
                        MANGA_HAS_UNREAD to stringResource(R.string.pref_update_only_completely_read),
                        MANGA_NON_READ to stringResource(R.string.pref_update_only_started),
                        MANGA_NON_COMPLETED to stringResource(R.string.pref_update_only_non_completed),
                    ),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allCategories,
                        included = included,
                        excluded = excluded,
                    ),
                    onClick = { showDialog = true },
                ),
                // SY -->
                Preference.PreferenceItem.ListPreference(
                    pref = libraryPreferences.groupLibraryUpdateType(),
                    title = stringResource(R.string.library_group_updates),
                    entries = mapOf(
                        GroupLibraryMode.GLOBAL to stringResource(R.string.library_group_updates_global),
                        GroupLibraryMode.ALL_BUT_UNGROUPED to stringResource(R.string.library_group_updates_all_but_ungrouped),
                        GroupLibraryMode.ALL to stringResource(R.string.library_group_updates_all),
                    ),
                ),
                // SY <--
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.autoUpdateMetadata(),
                    title = stringResource(R.string.pref_library_update_refresh_metadata),
                    subtitle = stringResource(R.string.pref_library_update_refresh_metadata_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = libraryPreferences.autoUpdateTrackers(),
                    enabled = Injekt.get<TrackManager>().hasLoggedServices(),
                    title = stringResource(R.string.pref_library_update_refresh_trackers),
                    subtitle = stringResource(R.string.pref_library_update_refresh_trackers_summary),
                ),
            ),
        )
    }

    @Composable
    private fun LibraryColumnsDialog(
        initialPortrait: Int,
        initialLandscape: Int,
        onDismissRequest: () -> Unit,
        onValueChanged: (portrait: Int, landscape: Int) -> Unit,
    ) {
        var portraitValue by rememberSaveable { mutableStateOf(initialPortrait) }
        var landscapeValue by rememberSaveable { mutableStateOf(initialLandscape) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(R.string.pref_library_columns)) },
            text = {
                Column {
                    Row {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.portrait),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.landscape),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    LibraryColumnsPicker(
                        modifier = Modifier.fillMaxWidth(),
                        portraitValue = portraitValue,
                        onPortraitChange = { portraitValue = it },
                        landscapeValue = landscapeValue,
                        onLandscapeChange = { landscapeValue = it },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = portraitValue != initialPortrait || landscapeValue != initialLandscape,
                    onClick = { onValueChanged(portraitValue, landscapeValue) },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    @Composable
    private fun LibraryColumnsPicker(
        modifier: Modifier = Modifier,
        portraitValue: Int,
        onPortraitChange: (Int) -> Unit,
        landscapeValue: Int,
        onLandscapeChange: (Int) -> Unit,
    ) {
        BoxWithConstraints(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            WheelPickerDefaults.Background(size = DpSize(maxWidth, maxHeight))

            val size = DpSize(width = maxWidth / 2, height = 128.dp)
            Row {
                WheelPicker(
                    size = size,
                    count = 11,
                    startIndex = portraitValue,
                    onSelectionChanged = onPortraitChange,
                    backgroundContent = null,
                ) { index ->
                    WheelPickerDefaults.Item(text = getColumnValue(value = index))
                }
                WheelPicker(
                    size = size,
                    count = 11,
                    startIndex = landscapeValue,
                    onSelectionChanged = onLandscapeChange,
                    backgroundContent = null,
                ) { index ->
                    WheelPickerDefaults.Item(text = getColumnValue(value = index))
                }
            }
        }
    }

    @Composable
    @ReadOnlyComposable
    private fun getColumnValue(value: Int): String {
        return if (value == 0) {
            stringResource(R.string.label_default)
        } else {
            value.toString()
        }
    }

    // SY -->
    @Composable
    fun getSortingCategory(navigator: Navigator, libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        val tagCount by libraryPreferences.sortTagsForLibrary().collectAsState()
        return Preference.PreferenceGroup(
            stringResource(R.string.pref_sorting_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(R.string.pref_tag_sorting),
                    subtitle = pluralStringResource(R.plurals.pref_tag_sorting_desc, tagCount.size, tagCount.size),
                    onClick = {
                        navigator.push(SortTagScreen())
                    },
                ),
            ),
        )
    }

    @Composable
    fun getMigrationCategory(unsortedPreferences: UnsortedPreferences): Preference.PreferenceGroup {
        val skipPreMigration by unsortedPreferences.skipPreMigration().collectAsState()
        val migrationSources by unsortedPreferences.migrationSources().collectAsState()
        return Preference.PreferenceGroup(
            stringResource(R.string.migration),
            enabled = skipPreMigration || migrationSources.isNotEmpty(),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = unsortedPreferences.skipPreMigration(),
                    title = stringResource(R.string.skip_pre_migration),
                    subtitle = stringResource(R.string.pref_skip_pre_migration_summary),
                ),
            ),
        )
    }
    // SY <--
}
