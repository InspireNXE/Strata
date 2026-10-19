package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.TextSpan;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The standard controls added after the tier 2 controls. Text in the test platform is 6 units per code point and 9
 * per line.
 */
class StandardControlsTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    private void type(final String text) {
        text.codePoints().forEach(code -> this.test.host().charTyped(code, Modifiers.NONE));
    }

    private void move(final float x, final float y) {
        this.test.host().pointerMoved(x, y, Modifiers.NONE);
    }

    private <C extends Component<C>> C topLeft(final C component) {
        return component.setAnchor(Anchor.TOP_LEFT);
    }

    private List<String> texts() {
        final NullCanvas canvas = new NullCanvas();
        this.test.host().render(canvas);
        return canvas.texts().stream().map(TextSpan::text).toList();
    }

    @Nested
    class Radios {
        private RadioGroup<String> group;
        private RadioButton easy;
        private RadioButton normal;
        private RadioButton hard;
        private final List<Optional<String>> heard = new ArrayList<>();

        private void show() {
            final var ui = StandardControlsTest.this.ui;
            this.group = ui.radioGroup();
            this.easy = this.group.add("Easy", "easy");
            this.normal = this.group.add("Normal", "normal");
            this.hard = this.group.add("Hard", "hard");
            this.group.onEvent(SingleSelection.SELECTED, event -> this.heard.add(this.group.selected()));
            StandardControlsTest.this.test.show(ui.stackPanel().add(this.easy).add(this.normal).add(this.hard));
        }

        @Test
        void selectingOneDeselectsTheOthers() {
            this.show();
            this.group.selectItem("normal");
            assertTrue(this.normal.isSelected());
            this.hard.setSelected(true);
            assertFalse(this.normal.isSelected());
            assertEquals(Optional.of("hard"), this.group.selected());
            assertSame(this.hard, this.group.selectedButton().orElseThrow());
            assertEquals(List.of(Optional.of("normal"), Optional.of("hard")), this.heard);
            this.hard.setSelected(false);
            assertEquals(Optional.empty(), this.group.selected());
        }

        @Test
        void aClickSelectsAndArrowsMoveSkippingDisabledButtons() {
            this.show();
            final Rect bounds = this.easy.bounds();
            StandardControlsTest.this.test.click(bounds.x() + 2, bounds.y() + 2);
            assertTrue(this.easy.isSelected());
            this.normal.setEnabled(false);
            StandardControlsTest.this.key(Key.DOWN);
            assertTrue(this.hard.isSelected());
            assertSame(this.hard, StandardControlsTest.this.test.focused());
            StandardControlsTest.this.key(Key.DOWN);
            assertTrue(this.easy.isSelected());
        }

        @Test
        void selectingAnUnknownValueClears() {
            this.show();
            this.group.selectItem("normal");
            this.group.selectItem("nightmare");
            assertEquals(-1, this.group.selectedIndex());
        }
    }

    @Nested
    class Toggles {
        @Test
        void measuresTrackGapAndCaption() {
            final ToggleSwitch toggle = StandardControlsTest.this.topLeft(StandardControlsTest.this.ui.toggleSwitch("On"));
            StandardControlsTest.this.test.show(toggle);
            assertEquals(13f * 2 + 4 + 12, toggle.desiredWidth());
            assertEquals(13f, toggle.desiredHeight());
        }

        @Test
        void aClickFlipsItUnlessDisabled() {
            final List<Boolean> heard = new ArrayList<>();
            final ToggleSwitch toggle = StandardControlsTest.this.topLeft(StandardControlsTest.this.ui.toggleSwitch("PvP"))
                    .onEvent(Toggle.TOGGLED, event -> heard.add(event.value()));
            StandardControlsTest.this.test.show(toggle);
            StandardControlsTest.this.test.click(5, 5);
            assertTrue(toggle.isSelected());
            toggle.setEnabled(false);
            toggle.toggle();
            assertTrue(toggle.isSelected());
            assertEquals(List.of(true), heard);
        }
    }

    @Nested
    class Spinners {
        private Spinner spinner;
        private final List<Double> heard = new ArrayList<>();

        private void show() {
            this.spinner = StandardControlsTest.this.topLeft(StandardControlsTest.this.ui.spinner())
                    .onEvent(Ranged.CHANGED, event -> this.heard.add(event.value()));
            StandardControlsTest.this.test.show(this.spinner);
            StandardControlsTest.this.test.click(10, 9);
        }

        private TextBox field() {
            return (TextBox) StandardControlsTest.this.test.focused();
        }

        @Test
        void typedTextBecomesTheValueOnEnterAndIsClamped() {
            this.show();
            this.spinner.setRange(0, 10);
            StandardControlsTest.this.type("9a9");
            assertEquals("099", this.field().text());
            StandardControlsTest.this.key(Key.ENTER);
            assertEquals(10.0, this.spinner.value());
            assertEquals("10", this.field().text());
        }

        @Test
        void keysAndButtonsStep() {
            this.show();
            StandardControlsTest.this.key(Key.UP);
            StandardControlsTest.this.key(Key.PAGE_UP);
            assertEquals(11.0, this.spinner.value());
            // Up button: the top half of the 12-unit column at the right of the 60-unit spinner
            StandardControlsTest.this.test.click(54, 4);
            assertEquals(12.0, this.spinner.value());
            StandardControlsTest.this.test.click(54, 15);
            assertEquals(11.0, this.spinner.value());
            assertEquals(List.of(1.0, 11.0, 12.0, 11.0), this.heard);
        }

        @Test
        void decimalsRoundAndFormat() {
            this.show();
            this.spinner.setDecimals(2).setValue(1.505);
            assertEquals(1.51, this.spinner.value());
            this.spinner.setDecimals(0);
            assertEquals(2.0, this.spinner.value());
        }

        @Test
        void textThatIsNotANumberGoesBack() {
            this.show();
            this.spinner.setRange(-5, 5);
            StandardControlsTest.this.key(Key.BACKSPACE);
            StandardControlsTest.this.type("-");
            StandardControlsTest.this.key(Key.ENTER);
            assertEquals(0.0, this.spinner.value());
            assertEquals("0", this.field().text());
        }
    }

    @Nested
    class TextAreas {
        private TextArea area;

        private void show(final String text) {
            this.area = StandardControlsTest.this.topLeft(StandardControlsTest.this.ui.textArea()).setText(text);
            this.area.setWidth(100);
            StandardControlsTest.this.test.show(this.area);
            this.area.requestFocus();
        }

        @Test
        void longLinesWrapAfterSpaces() {
            this.show("hello world foo bar");
            // 92 units inside the padding fit 15 characters
            assertEquals(List.of("hello world foo ", "bar"), StandardControlsTest.this.texts());
        }

        @Test
        void withoutWrappingRowsAreLines() {
            this.show("hello world foo bar\nnext");
            this.area.setWrap(false);
            assertEquals(List.of("hello world foo bar", "next"), StandardControlsTest.this.texts());
        }

        @Test
        void enterInsertsANewLine() {
            this.show("ab");
            assertEquals(0, this.area.caret());
            this.area.select(2, 2);
            StandardControlsTest.this.key(Key.ENTER);
            StandardControlsTest.this.type("c");
            assertEquals("ab\nc", this.area.text());
        }

        @Test
        void upAndDownKeepTheColumn() {
            this.show("abc\ndefgh\nij");
            this.area.select(12, 12);
            StandardControlsTest.this.key(Key.UP);
            assertEquals(6, this.area.caret());
            StandardControlsTest.this.key(Key.UP);
            assertEquals(2, this.area.caret());
            StandardControlsTest.this.key(Key.DOWN);
            StandardControlsTest.this.key(Key.DOWN);
            assertEquals(12, this.area.caret());
        }

        @Test
        void homeAndEndStayOnTheRow() {
            this.show("hello world foo bar");
            this.area.select(3, 3);
            StandardControlsTest.this.key(Key.END);
            assertEquals(15, this.area.caret());
            StandardControlsTest.this.key(Key.HOME);
            assertEquals(0, this.area.caret());
        }

        @Test
        void pastingKeepsLineBreaks() {
            this.show("");
            StandardControlsTest.this.test.platform().clipboard().set("x\r\ny\rz");
            StandardControlsTest.this.test.host().key(Key.V, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
            assertEquals("x\ny\nz", this.area.text());
        }

        @Test
        void theLengthLimitCountsLineBreaks() {
            this.show("");
            this.area.setMaxLength(3);
            StandardControlsTest.this.type("a");
            StandardControlsTest.this.key(Key.ENTER);
            StandardControlsTest.this.type("bc");
            assertEquals("a\nb", this.area.text());
        }

        @Test
        void tabIsLeftToFocus() {
            this.show("");
            StandardControlsTest.this.key(Key.TAB);
            assertEquals("", this.area.text());
        }
    }

    record Offer(String name, double price) {
    }

    @Nested
    class Tables {
        private final Offer apple = new Offer("apple", 3);
        private final Offer cherry = new Offer("Cherry", 1);
        private final Offer banana = new Offer("banana", 2);
        private TableView<Offer> table;

        private void show() {
            this.table = StandardControlsTest.this.topLeft(StandardControlsTest.this.ui.<Offer>tableView())
                    .addTextColumn("Name", GridLength.weighted(1), Offer::name)
                    .addTextColumn("Price", GridLength.fixed(60), offer -> Double.toString(offer.price()))
                    .setColumnComparator(1, java.util.Comparator.comparingDouble(Offer::price))
                    .setItems(List.of(this.apple, this.cherry, this.banana));
            this.table.setSize(200, 120);
            StandardControlsTest.this.test.show(this.table);
        }

        @Test
        void clickingATitleSortsAscendingThenDescending() {
            this.show();
            StandardControlsTest.this.test.click(10, 10);
            assertEquals(List.of(this.apple, this.banana, this.cherry), this.table.shownItems());
            StandardControlsTest.this.test.click(10, 10);
            assertEquals(List.of(this.cherry, this.banana, this.apple), this.table.shownItems());
            assertEquals(List.of(this.apple, this.cherry, this.banana), this.table.items());
        }

        @Test
        void theFixedColumnSitsAtTheRight() {
            this.show();
            // Name is weighted and takes 140 of 200, so Price starts at 140
            StandardControlsTest.this.test.click(150, 10);
            assertEquals(1, this.table.sortColumn());
            assertEquals(List.of(this.cherry, this.banana, this.apple), this.table.shownItems());
        }

        @Test
        void theSelectionFollowsItsItemThroughASort() {
            this.show();
            this.table.select(0);
            this.table.sortBy(0, false);
            assertEquals(Optional.of(this.apple), this.table.selected());
            assertEquals(2, this.table.selectedIndex());
        }

        @Test
        void sortingByAColumnWithoutAComparatorThrows() {
            this.show();
            this.table.setColumnComparator(0, null);
            assertThrows(IllegalArgumentException.class, () -> this.table.sortBy(0, true));
        }

        @Test
        void cellsShowTheirText() {
            this.show();
            final List<String> texts = StandardControlsTest.this.texts();
            assertTrue(texts.containsAll(List.of("Name", "Price", "apple", "3.0", "Cherry")), texts.toString());
        }
    }

    record Node(String name, List<Node> kids) {
        Node(final String name, final Node... kids) {
            this(name, Arrays.asList(kids));
        }
    }

    @Nested
    class Trees {
        private final Node leafA1 = new Node("A1");
        private final Node leafA2a = new Node("A2a");
        private final Node a2 = new Node("A2", this.leafA2a);
        private final Node a = new Node("A", this.leafA1, this.a2);
        private final Node b = new Node("B");
        private TreeView<Node> tree;

        private void show() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            this.tree = StandardControlsTest.this.topLeft(ui.<Node>treeView(node -> ui.label(node.name())))
                    .setChildren(Node::kids)
                    .setItems(List.of(this.a, this.b));
            this.tree.setSize(200, 200);
            StandardControlsTest.this.test.show(this.tree);
        }

        @Test
        void rightExpandsThenEntersAndLeftReturns() {
            this.show();
            StandardControlsTest.this.test.click(50, 10);
            assertEquals(Optional.of(this.a), this.tree.selected());
            StandardControlsTest.this.key(Key.RIGHT);
            assertEquals(List.of(this.a, this.leafA1, this.a2, this.b), this.tree.shownItems());
            StandardControlsTest.this.key(Key.RIGHT);
            assertEquals(Optional.of(this.leafA1), this.tree.selected());
            StandardControlsTest.this.key(Key.LEFT);
            assertEquals(Optional.of(this.a), this.tree.selected());
            StandardControlsTest.this.key(Key.LEFT);
            assertEquals(List.of(this.a, this.b), this.tree.shownItems());
        }

        @Test
        void selectingADeepItemExpandsItsAncestors() {
            this.show();
            this.tree.selectItem(this.leafA2a);
            assertEquals(List.of(this.a, this.leafA1, this.a2, this.leafA2a, this.b), this.tree.shownItems());
            assertEquals(2, this.tree.depth(3));
            assertEquals(Optional.of(this.leafA2a), this.tree.selected());
        }

        @Test
        void collapsingMovesTheSelectionUp() {
            this.show();
            final List<Optional<Node>> heard = new ArrayList<>();
            this.tree.selectItem(this.leafA2a);
            this.tree.onEvent(SingleSelection.SELECTED, e -> heard.add(this.tree.selected()));
            this.tree.setExpanded(this.a, false);
            assertEquals(Optional.of(this.a), this.tree.selected());
            assertEquals(List.of(Optional.of(this.a)), heard);
        }

        @Test
        void clickingTheArrowFlipsTheItem() {
            this.show();
            StandardControlsTest.this.test.click(6, 10);
            assertTrue(this.tree.isExpanded(this.a));
            StandardControlsTest.this.test.click(6, 10);
            assertFalse(this.tree.isExpanded(this.a));
        }

        @Test
        void expandAllShowsEverything() {
            this.show();
            this.tree.expandAll();
            assertEquals(5, this.tree.shownItems().size());
            this.tree.selectItem(this.leafA2a).collapseAll();
            assertEquals(List.of(this.a, this.b), this.tree.shownItems());
            assertEquals(Optional.of(this.a), this.tree.selected());
        }
    }

    @Nested
    class Menus {
        private final List<String> heard = new ArrayList<>();
        private Button target;
        private Menu menu;
        private Menu more;

        private void show() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            this.target = StandardControlsTest.this.topLeft(ui.button("Target"));
            StandardControlsTest.this.test.show(this.target);
            this.target.requestFocus();
            this.more = ui.menu().addItem("Deep", () -> this.heard.add("deep"));
            this.menu = ui.menu()
                    .addItem("One", "Ctrl+1", () -> this.heard.add("one"))
                    .addSeparator()
                    .addItem("Two", () -> this.heard.add("two"))
                    .addCheckItem("Check", false, checked -> this.heard.add("check " + checked))
                    .addSubmenu("More", this.more);
        }

        @Test
        void keysSkipSeparatorsAndChoosingClosesAndGivesFocusBack() {
            this.show();
            this.menu.openAt(10, 10);
            assertSame(this.menu, StandardControlsTest.this.test.focused());
            StandardControlsTest.this.key(Key.DOWN);
            StandardControlsTest.this.key(Key.DOWN);
            StandardControlsTest.this.key(Key.ENTER);
            assertEquals(List.of("two"), this.heard);
            assertFalse(this.menu.isOpen());
            assertSame(this.target, StandardControlsTest.this.test.focused());
        }

        @Test
        void checkItemsFlip() {
            this.show();
            this.menu.openAt(10, 10);
            StandardControlsTest.this.key(Key.UP);
            StandardControlsTest.this.key(Key.UP);
            StandardControlsTest.this.key(Key.SPACE);
            assertEquals(List.of("check true"), this.heard);
            assertTrue(this.menu.isItemChecked(3));
        }

        @Test
        void rightOpensASubmenuAndEscapeClosesOnlyIt() {
            this.show();
            this.menu.openAt(10, 10);
            StandardControlsTest.this.key(Key.UP);
            StandardControlsTest.this.key(Key.RIGHT);
            assertTrue(this.more.isOpen());
            assertSame(this.more, StandardControlsTest.this.test.focused());
            StandardControlsTest.this.key(Key.ESCAPE);
            assertFalse(this.more.isOpen());
            assertTrue(this.menu.isOpen());
            assertSame(this.menu, StandardControlsTest.this.test.focused());
        }

        @Test
        void aClickChoosesAndDisabledItemsDoNothing() {
            this.show();
            this.menu.setItemEnabled(0, false).openAt(10, 10);
            StandardControlsTest.this.test.host().layout();
            // Items start 2 units in; each is 15 tall
            StandardControlsTest.this.test.click(30, 10 + 2 + 7);
            assertTrue(this.menu.isOpen());
            StandardControlsTest.this.test.click(30, 10 + 2 + 15 + 5 + 7);
            assertEquals(List.of("two"), this.heard);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void aPressOutsideCloses() {
            this.show();
            this.menu.openAt(100, 100);
            StandardControlsTest.this.test.click(290, 290);
            assertFalse(this.menu.isOpen());
        }

        @Test
        void aMenuCannotBeItsOwnSubmenu() {
            this.show();
            assertThrows(IllegalArgumentException.class, () -> this.more.addSubmenu("Loop", this.menu));
            assertThrows(IllegalArgumentException.class, () -> this.menu.addSubmenu("Again", this.more));
        }
    }

    @Nested
    class MenuBars {
        @Test
        void clickOpensHoverSwitchesAndArrowsStep() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            final Menu file = ui.menu().addItem("Quit", () -> { });
            final Menu edit = ui.menu().addItem("Undo", () -> { });
            final MenuBar bar = StandardControlsTest.this.topLeft(ui.menuBar()).addMenu("File", file).addMenu("Edit", edit);
            StandardControlsTest.this.test.show(bar);
            assertEquals(80f, bar.desiredWidth());
            StandardControlsTest.this.move(20, 7);
            StandardControlsTest.this.test.click(20, 7);
            assertTrue(file.isOpen());
            StandardControlsTest.this.move(60, 7);
            assertTrue(edit.isOpen());
            assertFalse(file.isOpen());
            assertEquals(1, bar.selectedIndex());
            StandardControlsTest.this.key(Key.RIGHT);
            assertTrue(file.isOpen());
            assertEquals(0, bar.selectedIndex());
            assertThrows(IllegalArgumentException.class, () -> ui.menuBar().addMenu("Again", file));
        }
    }

    @Nested
    class Expanders {
        @Test
        void contentJoinsTheTreeOnlyWhileExpanded() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            final var inside = ui.label("Inside");
            final Expander expander = StandardControlsTest.this.topLeft(ui.expander("More")).setContent(inside);
            StandardControlsTest.this.test.show(expander);
            assertFalse(inside.isAttached());
            assertEquals(15f, expander.desiredHeight());
            StandardControlsTest.this.test.click(5, 5);
            StandardControlsTest.this.test.host().layout();
            assertTrue(expander.isExpanded());
            assertTrue(inside.isAttached());
            assertEquals(24f, expander.desiredHeight());
        }

        @Test
        void keysFromTheContentDoNotFlipIt() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            final TextBox box = ui.textBox();
            final Expander expander = StandardControlsTest.this.topLeft(ui.expander("More")).setContent(box).setExpanded(true);
            StandardControlsTest.this.test.show(expander);
            box.requestFocus();
            StandardControlsTest.this.key(Key.ENTER);
            assertTrue(expander.isExpanded());
            expander.requestFocus();
            StandardControlsTest.this.key(Key.ENTER);
            assertFalse(expander.isExpanded());
        }
    }

    @Nested
    class SplitPanes {
        @Test
        void theDividerSplitsTheSpaceAndDrags() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            final var left = ui.anchorPanel().setAnchor(Anchor.FILL);
            final var right = ui.anchorPanel().setAnchor(Anchor.FILL);
            final List<Float> heard = new ArrayList<>();
            final SplitPane split = StandardControlsTest.this.topLeft(ui.splitPane(Orientation.HORIZONTAL))
                    .setFirst(left).setSecond(right).onEvent(SplitPane.MOVED, event -> heard.add(event.value()));
            split.setSize(204, 50);
            StandardControlsTest.this.test.show(split);
            assertEquals(100f, left.bounds().width());
            assertEquals(104f, right.bounds().x());

            final var host = StandardControlsTest.this.test.host();
            host.pointerMoved(102, 25, Modifiers.NONE);
            host.pointerButton(102, 25, MouseButton.LEFT, true, Modifiers.NONE);
            host.pointerMoved(152, 25, Modifiers.NONE);
            host.pointerButton(152, 25, MouseButton.LEFT, false, Modifiers.NONE);
            host.layout();
            assertEquals(0.75f, split.position());
            assertEquals(150f, left.bounds().width());
            assertEquals(List.of(0.75f), heard);

            host.pointerMoved(152, 25, Modifiers.NONE);
            host.pointerButton(152, 25, MouseButton.LEFT, true, Modifiers.NONE);
            host.pointerMoved(250, 25, Modifiers.NONE);
            host.pointerButton(250, 25, MouseButton.LEFT, false, Modifiers.NONE);
            assertEquals(0.9f, split.position(), 0.0001f);
        }
    }

    @Nested
    class Toasts {
        @Test
        void aToastSitsTopRightIgnoresPressesAndExpires() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            StandardControlsTest.this.test.show(ui.anchorPanel());
            final Overlay toast = ui.overlays().toast("Saved");
            StandardControlsTest.this.test.host().layout();
            final Rect bounds = toast.content().bounds();
            assertEquals(new Rect(300 - 4 - 42, 4, 42, 21), bounds);
            StandardControlsTest.this.test.click(10, 10);
            assertTrue(toast.isOpen());
            StandardControlsTest.this.test.platform().advanceMillis(2999);
            StandardControlsTest.this.texts();
            assertTrue(toast.isOpen());
            StandardControlsTest.this.test.platform().advanceMillis(1);
            StandardControlsTest.this.texts();
            assertFalse(toast.isOpen());
        }

        @Test
        void toastsStackAndEscapeSkipsThem() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            StandardControlsTest.this.test.show(ui.anchorPanel());
            final Overlay dialog = ui.overlays().dialog(ui.button("OK"));
            final Overlay first = ui.overlays().toast("one");
            final Overlay second = ui.overlays().toast("two");
            StandardControlsTest.this.test.host().layout();
            assertEquals(4f + 21f + 4f, second.content().bounds().y());
            StandardControlsTest.this.key(Key.ESCAPE);
            assertFalse(dialog.isOpen());
            assertTrue(first.isOpen() && second.isOpen());
        }

        @Test
        void aClickClosesIt() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            StandardControlsTest.this.test.show(ui.anchorPanel());
            final Overlay toast = ui.overlays().toast("Saved");
            StandardControlsTest.this.test.host().layout();
            final Rect bounds = toast.content().bounds();
            StandardControlsTest.this.test.click(bounds.x() + 5, bounds.y() + 5);
            assertFalse(toast.isOpen());
        }
    }

    @Nested
    class GridViews {
        @Test
        void slotsLayOutInRowsAndReportClicks() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            final List<GridView.Click<?>> clicks = new ArrayList<>();
            final GridView<String> grid = StandardControlsTest.this.topLeft(ui.<String>gridView(ui::label))
                    .setColumns(3).setSpacing(2)
                    .setTooltips(Tooltip::text)
                    .setItems(Arrays.asList("a", null, "c", "d"))
                    .setSelectable(true)
                    .onEvent(GridView.CLICKED, e -> clicks.add(e.value()));
            StandardControlsTest.this.test.show(grid);
            assertEquals(3 * 18f + 2 * 2f, grid.desiredWidth());
            assertEquals(2 * 18f + 2f, grid.desiredHeight());
            // The fourth slot starts the second row
            StandardControlsTest.this.test.click(5, 20 + 5);
            StandardControlsTest.this.test.click(20 + 5, 5);
            assertEquals(2, clicks.size());
            assertEquals(3, clicks.getFirst().index());
            assertEquals("d", clicks.getFirst().item());
            assertEquals(1, clicks.get(1).index());
            assertEquals(null, clicks.get(1).item());
            assertEquals(1, grid.selectedIndex());
        }

        @Test
        void shrinkingTheItemsDropsALostSelection() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            final GridView<String> grid = ui.<String>gridView(ui::label).setItems(List.of("a", "b", "c")).select(2);
            grid.setItems(List.of("a"));
            assertEquals(-1, grid.selectedIndex());
            grid.setItems(List.of("a", "b")).select(1).setItem(1, null);
            assertEquals(1, grid.selectedIndex());
            assertEquals(null, grid.item(1));
        }
    }
    @Nested
    class ContentEvents {
        @Test
        void aButtonInATableCellIsHeardAroundTheTable() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            final List<Button> buttons = new ArrayList<>();
            final TableView<String> table = ui.<String>tableView()
                    .addColumn("Name", GridLength.weighted(1), name -> {
                        final Button button = ui.button(name);
                        buttons.add(button);
                        return button;
                    })
                    .setItems(List.of("Ana", "Ben"));
            table.setWidth(200).setHeight(100);
            final AbsolutePanel form = ui.absolutePanel().add(table);
            StandardControlsTest.this.test.show(form);
            StandardControlsTest.this.test.host().render(new NullCanvas());
            final List<String> pressed = new ArrayList<>();
            form.onEvent(Button.PRESSED, e -> pressed.add(e.control().text()));

            buttons.get(0).press();

            assertEquals(List.of("Ana"), pressed);
        }

        @Test
        void aButtonInAnExpanderIsHeardAroundTheExpander() {
            final ComponentFactory ui = StandardControlsTest.this.ui;
            final Button button = ui.button("Inside");
            final Expander expander = ui.expander("More").setContent(button).setExpanded(true);
            final AbsolutePanel form = ui.absolutePanel().add(expander);
            StandardControlsTest.this.test.show(form);
            final List<String> pressed = new ArrayList<>();
            form.onEvent(Button.PRESSED, e -> pressed.add(e.control().text()));

            button.press();

            assertEquals(List.of("Inside"), pressed);
        }
    }
}
