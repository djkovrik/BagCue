package com.sedsoftware.bagcue.compose.catalog

import androidx.compose.runtime.Composable
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.starter_bag_backpack
import bagcue.shared.compose.generated.resources.starter_bag_sports
import bagcue.shared.compose.generated.resources.starter_item_badge
import bagcue.shared.compose.generated.resources.starter_item_charger
import bagcue.shared.compose.generated.resources.starter_item_flip_flops
import bagcue.shared.compose.generated.resources.starter_item_goggles
import bagcue.shared.compose.generated.resources.starter_item_headphones
import bagcue.shared.compose.generated.resources.starter_item_keys
import bagcue.shared.compose.generated.resources.starter_item_laptop
import bagcue.shared.compose.generated.resources.starter_item_lunch_snack
import bagcue.shared.compose.generated.resources.starter_item_shower_gel
import bagcue.shared.compose.generated.resources.starter_item_swim_cap
import bagcue.shared.compose.generated.resources.starter_item_swimsuit
import bagcue.shared.compose.generated.resources.starter_item_towel
import bagcue.shared.compose.generated.resources.starter_item_water_bottle
import bagcue.shared.compose.generated.resources.starter_item_wet_bag
import bagcue.shared.compose.generated.resources.starter_template_office
import bagcue.shared.compose.generated.resources.starter_template_pool
import com.sedsoftware.bagcue.catalog.CatalogComponent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private fun catalogResource(key: String): StringResource = when (key) {
    "starter_item_laptop" -> Res.string.starter_item_laptop
    "starter_item_charger" -> Res.string.starter_item_charger
    "starter_item_headphones" -> Res.string.starter_item_headphones
    "starter_item_badge" -> Res.string.starter_item_badge
    "starter_item_keys" -> Res.string.starter_item_keys
    "starter_item_water_bottle" -> Res.string.starter_item_water_bottle
    "starter_item_lunch_snack" -> Res.string.starter_item_lunch_snack
    "starter_item_swimsuit" -> Res.string.starter_item_swimsuit
    "starter_item_swim_cap" -> Res.string.starter_item_swim_cap
    "starter_item_goggles" -> Res.string.starter_item_goggles
    "starter_item_towel" -> Res.string.starter_item_towel
    "starter_item_shower_gel" -> Res.string.starter_item_shower_gel
    "starter_item_flip_flops" -> Res.string.starter_item_flip_flops
    "starter_item_wet_bag" -> Res.string.starter_item_wet_bag
    "starter_template_office" -> Res.string.starter_template_office
    "starter_template_pool" -> Res.string.starter_template_pool
    "starter_bag_backpack" -> Res.string.starter_bag_backpack
    "starter_bag_sports" -> Res.string.starter_bag_sports
    else -> error("Unknown bundled resource key: $key")
}

suspend fun resolveCatalogResource(key: com.sedsoftware.bagcue.domain.catalog.ResourceKey): String =
    getString(catalogResource(key.value))

@Composable
internal fun CatalogComponent.UserText.resolve(): String = when (this) {
    is CatalogComponent.UserText.Authored -> value
    is CatalogComponent.UserText.Resource -> stringResource(catalogResource(key))
}

@Composable
internal fun CatalogComponent.EditableName.resolve(): String = when (this) {
    is CatalogComponent.EditableName.Input -> value
    is CatalogComponent.EditableName.Resource -> stringResource(catalogResource(key))
}
