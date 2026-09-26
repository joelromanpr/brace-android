# Tree and tree nodes

`BraceTree` renders a multi-root hierarchy from stable `BraceTreeNode.key` values. The screen owns the selected and expanded key sets. This is an Android adaptation of Blueprint 6.18.0 [Tree and TreeNode](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/tree/tree.mdx), with Compose semantics and a bounded lazy viewport.

```kotlin
val tree = rememberBraceTreeState(initialExpandedKeys = setOf("projects"))
val nodes = listOf(
    BraceTreeNode("projects", "Projects", children = listOf(
        BraceTreeNode("alpha", "Alpha", secondaryLabel = "Active"),
        BraceTreeNode("beta", "Beta", enabled = false),
    )),
    BraceTreeNode("reports", "Reports", hasChildren = true),
)
BraceTree(
    nodes = nodes,
    expandedKeys = tree.expandedKeys,
    onExpandedKeysChange = { tree.expandedKeys = it },
    selectedKeys = tree.selectedKeys,
    onSelectedKeysChange = { tree.selectedKeys = it },
    label = "Workspace hierarchy",
)
```

`rememberBraceTreeState` saves the two sets across configuration changes and process recreation. The explicit callback API also accepts a ViewModel or other owner. A callback proposes a new set; the view changes only when the owner passes that set back. Keys must be nonblank and unique across every descendant, including collapsed descendants. `hasChildren = true` supports a branch whose children will load after expansion. The caller supplies that loading state and the new children.

## Interaction and accessibility

- The entire row selects. The first 48 dp after its logical indentation toggles a branch, without changing selection. These pointer regions mirror in RTL. Mouse clicks use the same behavior. Disabled nodes have no click or expansion action.
- With keyboard focus on the tree, Up/Down move among enabled visible rows, Home/End jump to the first/last enabled row, Enter/Space select, and logical forward/back arrows expand, enter a child, collapse, or move to a parent. The arrow mapping reverses in RTL. Tab leaves the tree. `multiSelect = true` toggles each selected key; otherwise a selection replaces the set.
- TalkBack gets one node per visible row, with the full primary and secondary label, level, position among siblings, selected state, and expanded/collapsed state. The node offers a named selection action and, for a branch, a separate expand/collapse accessibility action. Collapsed descendants leave the semantics tree.
- `maxHeight` must fit a 48 dp row. Rows retain at least a 48 dp target in compact density. Labels can wrap to two lines at large font scales; deeper indentation is capped to preserve text room on narrow screens. Focus has a visible token-colored outline. Light, dark, high-contrast, brand, and density changes come from `BraceTheme` tokens. Motion is instantaneous, including reduced-motion mode.

The node model's `leadingContent` slot is decorative and is excluded from the spoken row name; use the text label for meaning. The API uses stable keys instead of Blueprint's positional `nodePath`, and has no DOM `getNodeContentElement` method or CSS `className`. Blueprint's `nodeData` belongs in the caller's data model. Generic React label/secondary-label elements are represented by text and a decorative Compose slot. The secondary label stacks below the primary label on Android so it remains readable at large text sizes instead of Blueprint's right-aligned web layout. Per-node context menus, double-click, and mouse enter/leave callbacks are not yet separate Tree APIs; a screen can still place contextual actions beside the tree. These gaps remain in progress in the inventory.

The component is **in progress**, with no release version or stable-coverage credit. See the [M31 report](milestones/m31-tree.md) for current verification and limitations.
