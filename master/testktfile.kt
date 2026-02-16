package com.barclays.mobilebanking.kyc.identitycheck

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.text.style.TextAlign.Companion.Start
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.barclays.blueprint.components.BlueprintAlert
import com.barclays.blueprint.components.BlueprintAlertAccessibility
import com.barclays.blueprint.components.BlueprintButton
import com.barclays.blueprint.components.BlueprintDrawerSheet
import com.barclays.blueprint.components.BlueprintDrawerSheet.State.Expanded
import com.barclays.blueprint.components.BlueprintImage
import com.barclays.blueprint.components.BlueprintImageAccessibility
import com.barclays.blueprint.components.BlueprintLink
import com.barclays.blueprint.components.BlueprintLink.Type.TextOnly
import com.barclays.blueprint.components.BlueprintLoadingSpinner
import com.barclays.blueprint.components.BlueprintScaffold
import com.barclays.blueprint.components.BlueprintStandardNavBar
import com.barclays.blueprint.components.BlueprintText
import com.barclays.blueprint.theme.BlueprintTheme
import com.barclays.blueprint.theme.LocalTypography
import com.barclays.mobilebanking.kyc.KycAemScreen
import com.barclays.mobilebanking.kyc.components.GenericAlertModel
import com.barclays.mobilebanking.kyc.components.KYCAlertLayout
import com.barclays.mobilebanking.kyc.components.KycAemParagraph
import com.barclays.mobilebanking.kyc.identitycheck.ScreenLoadingState.FAIL
import com.barclays.mobilebanking.kyc.identitycheck.ScreenLoadingState.IN_PROGRESS
import com.barclays.mobilebanking.kyc.identitycheck.ScreenLoadingState.SUCCESS
import com.barclays.mobilebanking.kyc.model.KycItemDescription
import com.barclays.mobilebanking.kyc.model.KycSectionItem

@Immutable
data class IdentityCheckUiState(
    val pageTitle: String?,
    val navigationTitle: String,
    val description: KycItemDescription,
    val notificationAlert: String,
    val buttonText: String,
    val linkText: String
)

@Immutable
data class DrawerUiState(
    val title: String,
    val items: List<KycSectionItem>
)

@Immutable
data class IdentityCheckActions(
    val onButtonClicked: () -> Unit,
    val onLinkClicked: () -> Unit,
    val onBackPressed: () -> Unit
)

@Composable
internal fun IdentityCheck(
    navController: NavController,
    modifier: Modifier = Modifier,
    journeyType: String? = null,
    viewModel: IdentityCheckViewModel = viewModel(
        factory = KycOnfidoJourneyViewModelFactory(
            navController = navController,
            journeyType = journeyType
        )
    )
) {
    IdentityCheckScreen(
        screenState = viewModel.screenLoading,
        uiState = viewModel.toUiState(),
        drawerState = viewModel.toDrawerState(),
        errorMessage = viewModel.errorString,
        actions = viewModel.toActions(),
        onLoadData = { viewModel.loadData(journeyType = journeyType) },
        onScreenLoad = { viewModel.logOnScreenLoad(label = Const.IDENTITY_CHECK_PAGE_NAME) },
        modifier = modifier
    )
}

@Composable
internal fun IdentityCheckScreen(
    screenState: ScreenLoadingState,
    uiState: IdentityCheckUiState,
    drawerState: DrawerUiState,
    errorMessage: String?,
    actions: IdentityCheckActions,
    onLoadData: () -> Unit,
    onScreenLoad: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (screenState) {
        SUCCESS -> {
            onScreenLoad()
            SuccessContent(
                uiState = uiState,
                drawerState = drawerState,
                actions = actions,
                modifier = modifier
            )
        }
        IN_PROGRESS -> {
            onLoadData()
            LoadingContent()
        }
        FAIL -> errorMessage?.let { ErrorContent(it) }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        BlueprintLoadingSpinner(modifier = Modifier.align(CenterHorizontally))
    }
}

@Composable
private fun ErrorContent(errorMessage: String) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    KYCAlertLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = Dimens.VerticalPadding,
                start = KycAemScreen.HORIZONTAL_PADDING,
                end = KycAemScreen.HORIZONTAL_PADDING
            ),
        model = GenericAlertModel(
            title = errorMessage,
            buttonText = stringResource(id = R.string.kyc_alert_ok)
        ),
        onPrimaryButtonClick = { backDispatcher?.onBackPressed() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    uiState: IdentityCheckUiState,
    drawerState: DrawerUiState,
    actions: IdentityCheckActions,
    modifier: Modifier = Modifier
) {
    var isDrawerOpen by rememberSaveable { mutableStateOf(false) }

    BlueprintScaffold(
        modifier = modifier,
        useWindowSizePadding = true,
        top = {
            IdentityCheckNavBar(
                title = uiState.pageTitle,
                onBackPressed = actions.onBackPressed
            )
        },
        content = {
            SuccessScrollableContent(
                uiState = uiState,
                onButtonClicked = actions.onButtonClicked,
                onLinkClicked = {
                    isDrawerOpen = true
                    actions.onLinkClicked()
                }
            )

            if (isDrawerOpen) {
                InfoDrawer(
                    state = drawerState,
                    onDismissed = { isDrawerOpen = false }
                )
            }
        }
    )
}

@Composable
private fun IdentityCheckNavBar(
    title: String?,
    onBackPressed: () -> Unit
) {
    BlueprintStandardNavBar(
        title = title ?: stringResource(id = R.string.kyc_title),
        showBackArrow = true,
        onBackPressed = onBackPressed
    )
}

@Composable
private fun SuccessScrollableContent(
    uiState: IdentityCheckUiState,
    onButtonClicked: () -> Unit,
    onLinkClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = SpaceBetween
    ) {
        IdentityInfoSection(
            navigationTitle = uiState.navigationTitle,
            description = uiState.description,
            notificationAlert = uiState.notificationAlert
        )

        ActionButtonSection(
            buttonText = uiState.buttonText,
            linkText = uiState.linkText,
            onButtonClicked = onButtonClicked,
            onLinkClicked = onLinkClicked
        )
    }
}

@Composable
private fun IdentityInfoSection(
    navigationTitle: String,
    description: KycItemDescription,
    notificationAlert: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(Dimens.VerticalButtonPadding))

        BlueprintImage(
            painter = painterResource(R.drawable.id_check),
            accessibility = BlueprintImageAccessibility(contentDescription = Const.EMPTY)
        )

        BlueprintText(
            textAlign = Center,
            color = BlueprintTheme.colors.title,
            style = LocalTypography.current.extraLarge,
            fontWeight = Medium,
            text = navigationTitle
        )

        description.json.forEach { model ->
            KycAemParagraph(model = model)
        }

        NotificationAlert(description = notificationAlert)
    }
}

@Composable
private fun NotificationAlert(description: String) {
    BlueprintAlert(
        modifier = Modifier.padding(top = Dimens.TextPadding),
        description = description,
        accessibility = BlueprintAlertAccessibility(
            imageContentDescription = stringResource(id = R.string.icon_info_information)
        ),
        link = null
    )
}

@Composable
private fun ActionButtonSection(
    buttonText: String,
    linkText: String,
    onButtonClicked: () -> Unit,
    onLinkClicked: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = CenterHorizontally
    ) {
        BlueprintButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.VerticalButtonPadding),
            text = buttonText,
            onClick = onButtonClicked
        )

        BlueprintLink(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.VerticalButtonPadding),
            type = TextOnly,
            text = linkText,
            textDecoration = Underline,
            onClick = onLinkClicked
        )
    }
}

@Composable
private fun InfoDrawer(
    state: DrawerUiState,
    onDismissed: () -> Unit
) {
    BlueprintDrawerSheet(
        title = state.title,
        state = Expanded,
        onDrawerSheetDismissed = onDismissed,
        content = { DrawerItemList(items = state.items) }
    )
}

@Composable
private fun DrawerItemList(items: List<KycSectionItem>) {
    Column(modifier = Modifier.padding(horizontal = Dimens.DrawerTextPadding)) {
        items.forEachIndexed { index, item ->
            DrawerSectionItem(item = item)
            if (index == Const.FIRST_ITEM_INDEX) {
                DrawerDivider()
            }
        }
    }
}

@Composable
private fun DrawerSectionItem(item: KycSectionItem) {
    DrawerSectionTitle(title = item.title.orEmpty())
    DrawerSectionDescription(description = item.description)
}

@Composable
private fun DrawerSectionTitle(title: String) {
    BlueprintText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.HorizontalPadding)
            .semantics { heading() },
        textAlign = Start,
        color = BlueprintTheme.colors.title,
        style = LocalTypography.current.small,
        fontWeight = Medium,
        text = title
    )
}

@Composable
private fun DrawerSectionDescription(description: KycItemDescription) {
    description.json.forEach { model ->
        model.content?.forEach { content ->
            BlueprintText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.HorizontalPadding),
                textAlign = Start,
                color = BlueprintTheme.colors.neutral900,
                style = LocalTypography.current.small,
                fontWeight = Normal,
                text = content.value.orEmpty()
            )
        }
    }
}

@Composable
private fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.DrawerTextPadding),
        color = BlueprintTheme.colors.neutral300,
        thickness = Dimens.DividerThickness
    )
}

private fun IdentityCheckViewModel.toUiState(): IdentityCheckUiState {
    return IdentityCheckUiState(
        pageTitle = uiState.pageTitle,
        navigationTitle = navigationTitle,
        description = description,
        notificationAlert = notificationAlert,
        buttonText = uiState.actions.firstOrNull()?.label.orEmpty(),
        linkText = uiState.actions.lastOrNull()?.label.orEmpty()
    )
}

private fun IdentityCheckViewModel.toDrawerState(): DrawerUiState {
    return DrawerUiState(
        title = drawerTitle,
        items = drawerItem
    )
}

private fun IdentityCheckViewModel.toActions(): IdentityCheckActions {
    return IdentityCheckActions(
        onButtonClicked = { onContinueButtonClicked(uiState.actions.firstOrNull()) },
        onLinkClicked = { logOnLinkClicked(label = uiState.actions.lastOrNull()?.label.orEmpty()) },
        onBackPressed = { onBackPressed() }
    )
}

@Preview
@Composable
private fun IdentityCheckPreview() {
    IdentityCheck(navController = rememberNavController())
}

private object Dimens {
    val VerticalButtonPadding = 16.dp
    val TextPadding = 32.dp
    val HorizontalPadding = 8.dp
    val DrawerTextPadding = 16.dp
    val DividerThickness = 1.dp
    val VerticalPadding = 16.dp
}

private object Const {
    const val FIRST_ITEM_INDEX = 0
    const val EMPTY = ""
    const val IDENTITY_CHECK_PAGE_NAME = "IdentityCheck"
}
