package com.sedsoftware.bagcue.compose.assets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import bagcue.shared.compose.generated.resources.Res
import bagcue.shared.compose.generated.resources.bagcue_mark
import bagcue.shared.compose.generated.resources.ic_add
import bagcue.shared.compose.generated.resources.ic_arrow_back
import bagcue.shared.compose.generated.resources.ic_bag
import bagcue.shared.compose.generated.resources.ic_calendar
import bagcue.shared.compose.generated.resources.ic_check
import bagcue.shared.compose.generated.resources.ic_delete
import bagcue.shared.compose.generated.resources.ic_duplicate
import bagcue.shared.compose.generated.resources.ic_edit
import bagcue.shared.compose.generated.resources.ic_more
import bagcue.shared.compose.generated.resources.ic_notifications
import bagcue.shared.compose.generated.resources.ic_privacy
import bagcue.shared.compose.generated.resources.ic_search
import bagcue.shared.compose.generated.resources.ic_sessions
import bagcue.shared.compose.generated.resources.ic_settings
import bagcue.shared.compose.generated.resources.ic_templates
import bagcue.shared.compose.generated.resources.ic_today
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Production resource registry for the frozen ASSET-001..017 inventory. */
object BagCueAssets {
    val Mark: DrawableResource get() = Res.drawable.bagcue_mark
    val Today: DrawableResource get() = Res.drawable.ic_today
    val Sessions: DrawableResource get() = Res.drawable.ic_sessions
    val Templates: DrawableResource get() = Res.drawable.ic_templates
    val Settings: DrawableResource get() = Res.drawable.ic_settings
    val Add: DrawableResource get() = Res.drawable.ic_add
    val Check: DrawableResource get() = Res.drawable.ic_check
    val Calendar: DrawableResource get() = Res.drawable.ic_calendar
    val Edit: DrawableResource get() = Res.drawable.ic_edit
    val Delete: DrawableResource get() = Res.drawable.ic_delete
    val More: DrawableResource get() = Res.drawable.ic_more
    val Search: DrawableResource get() = Res.drawable.ic_search
    val Duplicate: DrawableResource get() = Res.drawable.ic_duplicate
    val Notifications: DrawableResource get() = Res.drawable.ic_notifications
    val Privacy: DrawableResource get() = Res.drawable.ic_privacy
    val Back: DrawableResource get() = Res.drawable.ic_arrow_back
    val Bag: DrawableResource get() = Res.drawable.ic_bag
}

@Composable
fun BagCueIcon(
    resource: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(resource),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

@Composable
fun BagCueBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(BagCueAssets.Mark),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = Color.Unspecified,
    )
}
