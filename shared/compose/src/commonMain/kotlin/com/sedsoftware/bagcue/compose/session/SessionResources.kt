package com.sedsoftware.bagcue.compose.session

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
import com.sedsoftware.bagcue.session.SessionComponent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val sessionResources: Map<String, StringResource> = mapOf(
    "starter_template_office" to Res.string.starter_template_office,
    "starter_template_pool" to Res.string.starter_template_pool,
    "starter_bag_backpack" to Res.string.starter_bag_backpack,
    "starter_bag_sports" to Res.string.starter_bag_sports,
    "starter_item_laptop" to Res.string.starter_item_laptop,
    "starter_item_charger" to Res.string.starter_item_charger,
    "starter_item_headphones" to Res.string.starter_item_headphones,
    "starter_item_badge" to Res.string.starter_item_badge,
    "starter_item_keys" to Res.string.starter_item_keys,
    "starter_item_water_bottle" to Res.string.starter_item_water_bottle,
    "starter_item_lunch_snack" to Res.string.starter_item_lunch_snack,
    "starter_item_swimsuit" to Res.string.starter_item_swimsuit,
    "starter_item_swim_cap" to Res.string.starter_item_swim_cap,
    "starter_item_goggles" to Res.string.starter_item_goggles,
    "starter_item_towel" to Res.string.starter_item_towel,
    "starter_item_shower_gel" to Res.string.starter_item_shower_gel,
    "starter_item_flip_flops" to Res.string.starter_item_flip_flops,
    "starter_item_wet_bag" to Res.string.starter_item_wet_bag,
)

@Composable
internal fun SessionComponent.UserText.resolve(): String = when (this) {
    is SessionComponent.UserText.Authored -> value
    is SessionComponent.UserText.Resource -> stringResource(sessionResources.getValue(key))
}

@Composable
internal fun SessionComponent.EditableText.resolve(): String = when (this) {
    is SessionComponent.EditableText.Input -> value
    is SessionComponent.EditableText.Resource -> stringResource(sessionResources.getValue(key))
}
