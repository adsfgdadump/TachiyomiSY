package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.source.model.Source
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun SourcesFilterScreen(
    navigateUp: () -> Unit,
    state: SourcesFilterState.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
    // SY -->
    onClickSources: (Boolean, List<Source>) -> Unit,
    // SY <--
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(R.string.label_sources),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        if (state.isEmpty) {
            EmptyScreen(
                textResource = R.string.source_filter_empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }
        SourcesFilterContent(
            contentPadding = contentPadding,
            state = state,
            onClickLanguage = onClickLanguage,
            onClickSource = onClickSource,
            // SY -->
            onClickSources = onClickSources,
            // SY <--
        )
    }
}

@Composable
private fun SourcesFilterContent(
    contentPadding: PaddingValues,
    state: SourcesFilterState.Success,
    onClickLanguage: (String) -> Unit,
    onClickSource: (Source) -> Unit,
    // SY -->
    onClickSources: (Boolean, List<Source>) -> Unit,
    // SY <--
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        state.items.forEach { (language, sources) ->
            val enabled = language in state.enabledLanguages
            item(
                key = language.hashCode(),
                contentType = "source-filter-header",
            ) {
                SourcesFilterHeader(
                    modifier = Modifier.animateItemPlacement(),
                    language = language,
                    enabled = enabled,
                    onClickItem = onClickLanguage,
                )
            }
            if (!enabled) return@forEach
            // SY -->
            item(
                key = "toggle-$language",
                contentType = "source-filter-toggle",
            ) {
                val toggleEnabled = remember(state.disabledSources) {
                    sources.none { it.id.toString() in state.disabledSources }
                }
                SourcesFilterToggle(
                    modifier = Modifier.animateItemPlacement(),
                    isEnabled = toggleEnabled,
                    onClickItem = {
                        onClickSources(!toggleEnabled, sources)
                    },
                )
            }
            // SY <--
            items(
                items = sources,
                key = { "source-filter-${it.key()}" },
                contentType = { "source-filter-item" },
            ) { source ->
                SourcesFilterItem(
                    modifier = Modifier.animateItemPlacement(),
                    source = source,
                    enabled = "${source.id}" !in state.disabledSources,
                    onClickItem = onClickSource,
                )
            }
        }
    }
}

@Composable
private fun SourcesFilterHeader(
    modifier: Modifier,
    language: String,
    enabled: Boolean,
    onClickItem: (String) -> Unit,
) {
    SwitchPreferenceWidget(
        modifier = modifier,
        title = LocaleHelper.getSourceDisplayName(language, LocalContext.current),
        checked = enabled,
        onCheckedChanged = { onClickItem(language) },
    )
}

// SY -->
@Composable
fun SourcesFilterToggle(
    modifier: Modifier,
    isEnabled: Boolean,
    onClickItem: () -> Unit,
) {
    SwitchPreferenceWidget(
        modifier = modifier,
        title = stringResource(R.string.pref_category_all_sources),
        checked = isEnabled,
        onCheckedChanged = { onClickItem() },
    )
}

// SY <--

@Composable
private fun SourcesFilterItem(
    modifier: Modifier,
    source: Source,
    enabled: Boolean,
    onClickItem: (Source) -> Unit,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        showLanguageInContent = false,
        onClickItem = { onClickItem(source) },
        action = {
            Checkbox(checked = enabled, onCheckedChange = null)
        },
    )
}
