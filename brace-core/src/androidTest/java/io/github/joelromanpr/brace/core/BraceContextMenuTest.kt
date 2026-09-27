package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BraceContextMenuTest {
    @get:Rule val rule = createComposeRule()

    @Test fun secondaryClickOpensAndActionDismissesWithFocusReturn() {
        var expanded by mutableStateOf(false)
        var copies = 0
        rule.setContent {
            BraceTheme {
                BraceContextMenu(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    title = "Record actions",
                    target = { targetModifier -> Text("Record A", modifier = targetModifier) },
                    modifier = Modifier.testTag("record-target"),
                ) { dismiss ->
                    BraceMenuItem("Copy record", onClick = { copies++; dismiss() })
                    BraceMenuItem("Unavailable", onClick = {}, enabled = false)
                }
            }
        }
        rule.onNodeWithTag("record-target").performMouseInput { rightClick() }
        rule.onNodeWithText("Copy record").assertExists().performClick()
        rule.onNodeWithText("Copy record").assertDoesNotExist()
        rule.onNodeWithTag("record-target").assertIsFocused()
        assertEquals(1, copies)
    }

    @Test fun focusableButtonTargetHasOneFocusAndTalkBackStop() {
        var expanded by mutableStateOf(false)
        var taps = 0
        var actions = 0
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceContextMenu(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    title = "Button actions",
                    targetIsFocusable = true,
                    target = { targetModifier ->
                        BraceButton(
                            label = "Record button",
                            onClick = { taps++ },
                            modifier = targetModifier.testTag("record-button"),
                        )
                    },
                ) { dismiss ->
                    BraceMenuItem("Process record", onClick = { actions++; dismiss() })
                }
            }
        }
        rule.onNodeWithTag("record-button").performClick()
        assertEquals(1, taps)
        rule.onNodeWithText("Process record").assertDoesNotExist()
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        rule.onNodeWithTag("record-button").requestFocus().assertIsFocused()
        assertEquals(1, rule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().size)
        val clickNode = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().single()
        val longClickNode = rule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick),
        ).fetchSemanticsNodes().single()
        assertEquals(clickNode.id, longClickNode.id)
        rule.onNodeWithTag("record-button").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.F10)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText("Process record").performClick()
        assertEquals(1, actions)
        assertEquals(1, taps)
        rule.onNodeWithTag("record-button").assertIsFocused()
    }

    @Test fun escapeDismissesEvenWhenAndroidBackIsDisabled() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceContextMenuPopup(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    targetOffset = IntOffset(50, 50),
                    title = "Escape actions",
                    dismissOnBackPress = false,
                ) { dismiss ->
                    BraceMenuItem("Escape target", onClick = { dismiss() })
                }
            }
        }
        rule.onNodeWithText("Escape target").requestFocus()
            .performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText("Escape target").assertDoesNotExist()
    }

    @Test fun normalTapReachesTargetAndLongPressOpensWithoutClickingIt() {
        var expanded by mutableStateOf(false)
        var taps = 0
        rule.setContent {
            BraceTheme {
                BraceContextMenu(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    title = "Document actions",
                    target = { targetModifier ->
                        BraceButton("Document", onClick = { taps++ }, modifier = targetModifier)
                    },
                    targetIsFocusable = true,
                    modifier = Modifier.testTag("document-target"),
                ) { dismiss ->
                    BraceMenuItem("Archive", onClick = { dismiss() })
                }
            }
        }
        rule.onNodeWithTag("document-target").performTouchInput { click() }
        assertEquals(1, taps)
        rule.onNodeWithText("Archive").assertDoesNotExist()
        rule.onNodeWithTag("document-target").performTouchInput { longClick() }
        rule.onNodeWithText("Archive").assertExists()
        assertEquals(1, taps)
    }

    @Test fun shiftF10MenuKeyAndTalkBackActionOpenTheSameMenu() {
        var expanded by mutableStateOf(false)
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BraceTheme {
                BraceContextMenu(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    title = "Row actions",
                    openActionLabel = "Open row actions",
                    target = { targetModifier -> Text("Row target", modifier = targetModifier) },
                    modifier = Modifier.testTag("row-target"),
                ) { dismiss ->
                    BraceMenuItem("Inspect", onClick = { dismiss() })
                }
            }
        }
        rule.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        val trigger = rule.onNodeWithTag("row-target")
        trigger.requestFocus().performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.F10)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText("Inspect").assertExists()
        rule.runOnIdle { expanded = false }
        trigger.assertIsFocused().performKeyInput { pressKey(Key.Menu) }
        rule.onNodeWithText("Inspect").assertExists()
        rule.runOnIdle { expanded = false }
        trigger.performSemanticsAction(SemanticsActions.OnLongClick)
        rule.onNodeWithText("Inspect").assertExists()
    }

    @Test fun deepestNestedTargetOwnsSecondaryClickAndLongPress() {
        var outerOpen by mutableStateOf(false)
        var innerOpen by mutableStateOf(false)
        rule.setContent {
            BraceTheme {
                BraceContextMenu(
                    expanded = outerOpen,
                    onExpandedChange = { outerOpen = it },
                    title = "Outer actions",
                    modifier = Modifier.testTag("outer-target"),
                    target = { outerModifier ->
                        Box(outerModifier) {
                            BraceContextMenu(
                                expanded = innerOpen,
                                onExpandedChange = { innerOpen = it },
                                title = "Inner actions",
                                modifier = Modifier.testTag("inner-target"),
                                target = { innerModifier ->
                                    Text("Nested record", modifier = innerModifier)
                                },
                            ) { dismiss ->
                                BraceMenuItem("Inner action", onClick = { dismiss() })
                            }
                        }
                    },
                ) { dismiss ->
                    BraceMenuItem("Outer action", onClick = { dismiss() })
                }
            }
        }
        rule.onNodeWithTag("inner-target").performMouseInput { rightClick() }
        rule.onNodeWithText("Inner action").assertExists()
        rule.onNodeWithText("Outer action").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(false, outerOpen)
            innerOpen = false
        }
        rule.onNodeWithTag("inner-target").performTouchInput { longClick() }
        rule.onNodeWithText("Inner action").assertExists()
        rule.onNodeWithText("Outer action").assertDoesNotExist()
        rule.runOnIdle { assertEquals(false, outerOpen) }
    }

    @Test fun disabledTriggerClosesAndDoesNotReopen() {
        var expanded by mutableStateOf(true)
        var enabled by mutableStateOf(true)
        rule.setContent {
            BraceTheme {
                BraceContextMenu(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    title = "Actions",
                    target = { targetModifier -> Text("Target", modifier = targetModifier) },
                    enabled = enabled,
                    modifier = Modifier.testTag("disabled-target"),
                ) { dismiss -> BraceMenuItem("Run", onClick = { dismiss() }) }
            }
        }
        rule.onNodeWithText("Run").assertExists()
        rule.runOnIdle { enabled = false }
        rule.onNodeWithText("Run").assertDoesNotExist()
        rule.onNodeWithTag("disabled-target").performMouseInput { rightClick() }
        rule.onNodeWithText("Run").assertDoesNotExist()
        rule.runOnIdle { assertEquals(false, expanded) }
    }

    @Test fun lowerLevelPopupUsesPaneSemanticsAndRtlLargeTextTouchTargets() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            val pixelDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(pixelDensity, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                BraceTheme(mode = BraceColorMode.Dark, contrast = BraceContrast.High) {
                    Box(Modifier.size(80.dp)) {
                        BraceContextMenuPopup(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            targetOffset = IntOffset(250, 250),
                            title = "Context actions",
                            modifier = Modifier.testTag("context-popup"),
                        ) { dismiss ->
                            BraceMenuItem(
                                label = "A long action label that can wrap at large text sizes",
                                onClick = { dismiss() },
                                multiline = true,
                            )
                        }
                    }
                }
            }
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Context actions"))
            .assertExists()
        rule.enableAccessibilityChecks()
        rule.onNodeWithText("A long action label that can wrap at large text sizes")
            .assertHeightIsAtLeast(48.dp)
            .tryPerformAccessibilityChecks()
            .performClick()
        rule.onNodeWithTag("context-popup").assertDoesNotExist()
    }

    @Test fun restoredOpenMenuUsesCurrentTargetAndFollowsLayoutMovement() {
        var leftInset by mutableStateOf(72.dp)
        var topInset by mutableStateOf(90.dp)
        var targetScreen = Offset.Unspecified
        var itemScreen = Offset.Unspecified
        val restore = StateRestorationTester(rule)
        restore.setContent {
            var expanded by rememberSaveable { mutableStateOf(false) }
            BraceTheme {
                Box(Modifier.fillMaxSize().padding(start = leftInset, top = topInset)) {
                    BraceContextMenu(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        title = "Restored actions",
                        target = { targetModifier ->
                            Text("Anchor", modifier = targetModifier
                                .testTag("restored-target")
                                .onGloballyPositioned { targetScreen = it.positionOnScreen() })
                        },
                    ) { dismiss ->
                        BraceMenuItem(
                            label = "Restore action",
                            onClick = { dismiss() },
                            modifier = Modifier.onGloballyPositioned {
                                itemScreen = it.positionOnScreen()
                            },
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("restored-target").performMouseInput { rightClick() }
        rule.onNodeWithText("Restore action").assertExists()
        targetScreen = Offset.Unspecified
        itemScreen = Offset.Unspecified
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Restore action").assertExists()
        rule.runOnIdle {
            assertTrue(!targetScreen.x.isNaN() && !itemScreen.x.isNaN())
            // The restored state has no saved pointer point; it anchors at the target start.
            assertTrue("restored target=$targetScreen item=$itemScreen",
                abs(itemScreen.x - targetScreen.x) < with(rule.density) { 24.dp.toPx() })
            assertTrue(itemScreen.y >= targetScreen.y)
        }
        val beforeTarget = targetScreen
        val beforeItem = itemScreen
        rule.runOnIdle {
            leftInset = 96.dp
            topInset = 106.dp
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertTrue(abs((itemScreen.x - beforeItem.x) - (targetScreen.x - beforeTarget.x)) < 3f)
            assertTrue(abs((itemScreen.y - beforeItem.y) - (targetScreen.y - beforeTarget.y)) < 3f)
        }
    }

    @Test fun lowerLevelPopupCallerCanRestoreTriggerFocusAfterActionDismissal() {
        var expanded by mutableStateOf(false)
        val trigger = FocusRequester()
        rule.setContent {
            var hadOpened by remember { mutableStateOf(false) }
            LaunchedEffect(expanded) {
                if (expanded) hadOpened = true
                else if (hadOpened) {
                    trigger.requestFocus()
                    hadOpened = false
                }
            }
            BraceTheme {
                BraceButton(
                    "Open point menu",
                    onClick = { expanded = true },
                    modifier = Modifier.focusRequester(trigger).testTag("point-trigger"),
                )
                BraceContextMenuPopup(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    targetOffset = IntOffset(80, 220),
                    title = "Point actions",
                ) { dismiss ->
                    BraceMenuItem("Finish point action", onClick = { dismiss() })
                }
            }
        }
        rule.onNodeWithTag("point-trigger").requestFocus().performClick()
        rule.onNodeWithText("Finish point action").performClick()
        rule.onNodeWithTag("point-trigger").assertIsFocused()
    }

    @Test fun singleItemPopupOpensAtLeftPointAndFlipsInsideRightEdge() {
        var point by mutableStateOf(IntOffset(16, 100))
        var itemScreen = Offset.Unspecified
        var itemWidth = 0
        var windowWidth = 0
        rule.setContent {
            windowWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
            BraceTheme {
                BraceContextMenuPopup(
                    expanded = true,
                    onDismissRequest = {},
                    targetOffset = point,
                    title = "Edge actions",
                ) { _ ->
                    BraceMenuItem(
                        label = "Single action",
                        onClick = {},
                        modifier = Modifier.onGloballyPositioned {
                            itemScreen = it.positionOnScreen()
                            itemWidth = it.size.width
                        },
                    )
                }
            }
        }
        rule.onNodeWithText("Single action").assertExists()
        rule.runOnIdle {
            assertTrue("left point=$point item=$itemScreen", itemScreen.x in 16f..32f)
        }
        rule.runOnIdle { point = IntOffset(windowWidth - 16, 100) }
        rule.waitForIdle()
        rule.runOnIdle {
            assertTrue("right point=$point item=$itemScreen", itemScreen.x < point.x - 16f)
            assertTrue(itemScreen.x >= 0f)
            assertTrue(itemScreen.x + itemWidth <= windowWidth.toFloat())
        }
    }

    @Test fun windowPointMirrorsLogicalSideAndFlipsOrClampsAtEdges() {
        val window = IntSize(300, 300)
        val menu = IntSize(90, 70)
        val anchor = IntRect(0, 0, 0, 0)
        val middle = IntOffset(120, 100)
        assertEquals(IntOffset(120, 100), BraceContextMenuPositionProvider(
            middle, 8, LayoutDirection.Ltr,
        ).calculatePosition(anchor, window, LayoutDirection.Ltr, menu))
        assertEquals(IntOffset(30, 100), BraceContextMenuPositionProvider(
            middle, 8, LayoutDirection.Rtl,
        ).calculatePosition(anchor, window, LayoutDirection.Rtl, menu))
        assertEquals(IntOffset(195, 220), BraceContextMenuPositionProvider(
            IntOffset(285, 290), 8, LayoutDirection.Ltr,
        ).calculatePosition(anchor, window, LayoutDirection.Ltr, menu))
        assertEquals(IntOffset(15, 15), BraceContextMenuPositionProvider(
            IntOffset(15, 15), 8, LayoutDirection.Rtl,
        ).calculatePosition(anchor, window, LayoutDirection.Rtl, menu))
        // A popup constrained to the 8 px side margins of a narrow window stays visible.
        assertEquals(IntOffset(8, 215), BraceContextMenuPositionProvider(
            IntOffset(155, 285), 8, LayoutDirection.Ltr,
        ).calculatePosition(anchor, IntSize(160, 300), LayoutDirection.Ltr, IntSize(144, 70)))
    }
}
