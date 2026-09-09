@file:Suppress("FunctionName")

package com.sedsoftware.bagcue.compose.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.session_ad_label
import com.sedsoftware.bagcue.catalog.CatalogComponent
import com.sedsoftware.bagcue.catalog.integration.CatalogComponentPreview
import com.sedsoftware.bagcue.compose.App
import com.sedsoftware.bagcue.domain.apa.AdvertisingConsentChoice
import com.sedsoftware.bagcue.history.HistoryComponent
import com.sedsoftware.bagcue.history.integration.HistoryComponentPreview
import com.sedsoftware.bagcue.root.RootComponent
import com.sedsoftware.bagcue.root.integration.RootComponentPreview
import com.sedsoftware.bagcue.session.SessionComponent
import com.sedsoftware.bagcue.session.integration.SessionComponentPreview
import com.sedsoftware.bagcue.settings.SettingsComponent
import com.sedsoftware.bagcue.settings.integration.SettingsComponentPreview
import com.sedsoftware.bagcue.templates.TemplateComponent
import com.sedsoftware.bagcue.templates.integration.TemplateComponentPreview
import org.jetbrains.compose.resources.stringResource

@Composable
private fun previewApp(
    session: SessionComponent = SessionComponentPreview(BagCuePreviewFixtures.today()),
    history: HistoryComponent = HistoryComponentPreview(BagCuePreviewFixtures.history(empty = true)),
    templates: TemplateComponent = TemplateComponentPreview(BagCuePreviewFixtures.templates()),
    catalog: CatalogComponent = CatalogComponentPreview(BagCuePreviewFixtures.catalog()),
    settings: SettingsComponent = SettingsComponentPreview(BagCuePreviewFixtures.settings()),
    destination: RootComponent.PrimaryDestination = RootComponent.PrimaryDestination.Today,
    showCatalog: Boolean = false,
    showTestAd: Boolean = false,
) {
    App(
        rootComponent = RootComponentPreview(
            session,
            history,
            templates,
            catalog,
            settings,
            destination,
            initialCatalog = showCatalog,
        ),
        inlineResultAd = if (showTestAd) ({ PreviewInlineAdSurface() }) else ({}),
    )
}

@Composable
private fun PreviewInlineAdSurface() {
    androidx.compose.material3.Surface(
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
    ) {
        androidx.compose.material3.Text(
            text = stringResource(Res.string.session_ad_label),
            modifier = Modifier.padding(16.dp),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        )
    }
}

// SCREEN-001 — Today
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_001_FirstRunStarter() = previewApp()
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_001_ActivePartial() =
    previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.today(BagCuePreviewFixtures.todaySummary())))
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_001_NoCurrentNextFuture() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.today()))
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_001_ReadFailure_RU200() =
    previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.today(error = SessionComponent.ErrorKey.LoadFailed)))
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_001_ActiveAdaptive() =
    previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.today(BagCuePreviewFixtures.todaySummary())))

// SCREEN-002 — Create packing
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_002_DefaultsOffReminders() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.create()))
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_002_SelectedKitsLong_RU200() =
    previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.create(selected = true)))
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_002_ExistingDateChoice() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.create(selected = true,
             occupied = true)))
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_002_EmptyMerge() =
    previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.create(validation = SessionComponent.ValidationError.EmptySession)))
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_002_SaveErrorIme_RU200() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.create(selected = true,
             error = SessionComponent.ErrorKey.CreateFailed)))
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_002_SupportingPane() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.create(selected = true)))

// SCREEN-003 — Active packing session
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_003_NewOfficePool() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.active()))
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_003_PartialSourceHints_RU200() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.active()))
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_003_UnresolvedBagConflict() =
    previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.active(conflict = true)))
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_003_AllPacked() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.active(allPacked = true)))
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_003_RemoveUndo_RU200() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.active(undo = true)))
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_003_SaveError() =
    previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.active(error = SessionComponent.ErrorKey.RevisionConflict)))
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_003_SupportingPane() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.active()))

// SCREEN-004 — Packing result
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_004_FullSuccessAdHidden() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.result()))
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_004_FullSuccessEligibleAd_RU200() =
    previewApp(
        session = SessionComponentPreview(BagCuePreviewFixtures.result(advertising = SessionComponent.ResultAdvertisingStatus.Ready)),
     showTestAd = true)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_004_ConsentRequired() =
    previewApp(
        session = SessionComponentPreview(
            BagCuePreviewFixtures.result(advertising = SessionComponent.ResultAdvertisingStatus.ConsentRequired,
             choiceAvailable = true)))
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_004_SkippedNoAd_RU200() =
    previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.result(allPacked = false)))
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_004_AdFailureNoGap() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.result()))
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_004_FullSuccessAdaptive() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.result()))

// SCREEN-005 — Sessions
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_005_MixedCalendarCollapsed() = previewApp(history = HistoryComponentPreview(BagCuePreviewFixtures.history()),
     destination = RootComponent.PrimaryDestination.Sessions)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_005_CalendarExpandedFilter_RU200() =
    previewApp(history = HistoryComponentPreview(BagCuePreviewFixtures.history(calendar = true)),
     destination = RootComponent.PrimaryDestination.Sessions)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_005_EmptyAndFilteredEmpty() =
    previewApp(history = HistoryComponentPreview(BagCuePreviewFixtures.history(empty = true)),
     destination = RootComponent.PrimaryDestination.Sessions)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_005_DeleteConfirmationUndo_RU200() =
    previewApp(history = HistoryComponentPreview(BagCuePreviewFixtures.history(deleting = true,
             undo = true)),
     destination = RootComponent.PrimaryDestination.Sessions)
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_005_ListDetail() = previewApp(history = HistoryComponentPreview(BagCuePreviewFixtures.history()),
     destination = RootComponent.PrimaryDestination.Sessions)

// SCREEN-006 — Templates
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_006_StarterContent() = previewApp(destination = RootComponent.PrimaryDestination.Templates)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_006_LongCustomTemplates_RU200() =
    previewApp(templates = TemplateComponentPreview(BagCuePreviewFixtures.templates(long = true)),
     destination = RootComponent.PrimaryDestination.Templates)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_006_Empty() = previewApp(templates = TemplateComponentPreview(BagCuePreviewFixtures.templates(empty = true)),
     destination = RootComponent.PrimaryDestination.Templates)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_006_DeleteDuplicateFeedback_RU200() =
    previewApp(templates = TemplateComponentPreview(BagCuePreviewFixtures.templates(duplicate = true,
             deleting = true)),
     destination = RootComponent.PrimaryDestination.Templates)
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_006_ListDetail() = previewApp(destination = RootComponent.PrimaryDestination.Templates)

// SCREEN-007 — Template editor
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_007_NewEmpty() = previewApp(templates = TemplateComponentPreview(BagCuePreviewFixtures.templateEditor(new = true)),
     destination = RootComponent.PrimaryDestination.Templates)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_007_StarterOffice() = previewApp(templates = TemplateComponentPreview(BagCuePreviewFixtures.templateEditor()),
     destination = RootComponent.PrimaryDestination.Templates)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_007_LongCustomIme_RU200() =
    previewApp(templates = TemplateComponentPreview(BagCuePreviewFixtures.templateEditor(long = true,
             validation = TemplateComponent.ValidationError.QuantityOutOfRange)),
     destination = RootComponent.PrimaryDestination.Templates)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_007_SaveFailure() =
    previewApp(templates = TemplateComponentPreview(BagCuePreviewFixtures.templateEditor(error = TemplateComponent.ErrorKey.SaveFailed)),
     destination = RootComponent.PrimaryDestination.Templates)
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_007_DetailPane() = previewApp(templates = TemplateComponentPreview(BagCuePreviewFixtures.templateEditor()),
     destination = RootComponent.PrimaryDestination.Templates)

// SCREEN-008 — Item catalog and selector
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_008_StarterCatalog() = previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalog()),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_008_SearchMatchSuggestion_RU200() =
    previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalog(duplicate = true)),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_008_DependencyDeletion() =
    previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalog(deleting = true)),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_008_EmptyError_RU200() = previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalog(empty = true,
             error = CatalogComponent.ErrorKey.LoadFailed)),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_008_ListDetail() = previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalog()),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)

// SCREEN-009 — Item and position editor
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_009_NewCatalogItem() = previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalogEditor()),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_009_SeedOverrideMatch_RU200() =
    previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalogEditor(existing = true,
             long = true,
             duplicate = true)),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_009_SessionPosition() = previewApp(session = SessionComponentPreview(BagCuePreviewFixtures.active(editing = true)),
     destination = RootComponent.PrimaryDestination.Today)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_009_ValidationSaveErrorIme_RU200() =
    previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalogEditor(long = true,
             invalid = true,
             error = CatalogComponent.ErrorKey.SaveFailed)),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_009_DetailPane() =
    previewApp(catalog = CatalogComponentPreview(BagCuePreviewFixtures.catalogEditor(existing = true)),
     destination = RootComponent.PrimaryDestination.Templates, showCatalog = true)

// SCREEN-010 — Settings
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_010_AllDefaultsOff() = previewApp(destination = RootComponent.PrimaryDestination.Settings)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_010_RemindersOnPermissionDenied_RU200() =
    previewApp(settings = SettingsComponentPreview(BagCuePreviewFixtures.settings(reminders = true,
             capability = SettingsComponent.NotificationCapability.Denied)),
     destination = RootComponent.PrimaryDestination.Settings)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_010_AnalyticsOnOff() =
    previewApp(settings = SettingsComponentPreview(BagCuePreviewFixtures.settings(analytics = true)),
     destination = RootComponent.PrimaryDestination.Settings)
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_010_ProtectedPrivacyChoice_RU200() =
    previewApp(
        settings = SettingsComponentPreview(
            BagCuePreviewFixtures.settings(privacy = SettingsComponent.AdvertisingPrivacyStatus.ConsentRequired,
             choice = AdvertisingConsentChoice.Declined,
             choiceAvailable = true)),
     destination = RootComponent.PrimaryDestination.Settings)
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_010_EndpointUnresolved() =
    previewApp(
        settings = SettingsComponentPreview(BagCuePreviewFixtures.settings(privacy = SettingsComponent.AdvertisingPrivacyStatus.Unresolved,
             error = SettingsComponent.Error.PrivacyRefreshFailed)),
     destination = RootComponent.PrimaryDestination.Settings)
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_010_AdaptiveGroups() = previewApp(destination = RootComponent.PrimaryDestination.Settings)

// SCREEN-011 — Advertising consent, rendered by the production result UI.
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_011_RequiredChoice() =
    previewApp(
        session = SessionComponentPreview(
            BagCuePreviewFixtures.result(advertising = SessionComponent.ResultAdvertisingStatus.ConsentRequired,
             choiceAvailable = true)))
@Preview(name = "light_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, showBackground = true)
@Preview(name = "dark_compact_ru-200", widthDp = 390, heightDp = 1260, locale = "ru", fontScale = 2f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_011_TextStress_RU200() =
    previewApp(
        session = SessionComponentPreview(
            BagCuePreviewFixtures.result(advertising = SessionComponent.ResultAdvertisingStatus.ConsentRequired,
             choiceAvailable = true)))
@Preview(name = "light_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_compact_en-100", widthDp = 390, heightDp = 844, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_011_SaveFailure() =
    previewApp(
        session = SessionComponentPreview(
            BagCuePreviewFixtures.result(advertising = SessionComponent.ResultAdvertisingStatus.ConsentRequired,
             choiceAvailable = true,
             error = SessionComponent.ErrorKey.SaveFailed)))
@Preview(name = "light_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, showBackground = true)
@Preview(name = "dark_expanded_en-100", widthDp = 960, heightDp = 900, locale = "en", fontScale = 1f, uiMode = 0x20, showBackground = true)
@Composable fun SCREEN_011_BoundedModal() =
    previewApp(
        session = SessionComponentPreview(
            BagCuePreviewFixtures.result(advertising = SessionComponent.ResultAdvertisingStatus.ConsentRequired,
             choiceAvailable = true)))
