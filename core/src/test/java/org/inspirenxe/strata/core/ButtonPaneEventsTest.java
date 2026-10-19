package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Pre and Post events of the buttons, the panes and the scroll panel: requests fire only for the user's actions
 * and may be canceled or rewritten, reports fire for every change. Text in the test platform is 6 units per code
 * point and 9 per line.
 */
class ButtonPaneEventsTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    /**
     * Shows a component at the top left of a panel and returns the panel, which stands for a form around it.
     */
    private AbsolutePanel form(final Component<?> component) {
        final AbsolutePanel panel = this.ui.absolutePanel().add(component, 0, 0);
        this.test.show(panel);
        return panel;
    }

    @Nested
    class Buttons {
        private final List<String> seen = new ArrayList<>();
        private Button button;
        private AbsolutePanel form;

        private void show() {
            this.button = ButtonPaneEventsTest.this.ui.button("OK")
                    .onEvent(Button.PRESS, event -> this.seen.add("press"))
                    .onEvent(Button.PRESSED, event -> this.seen.add("pressed"));
            this.form = ButtonPaneEventsTest.this.form(this.button);
        }

        @Test
        void clicksAndKeysRequestThenReport() {
            this.show();
            ButtonPaneEventsTest.this.test.click(5, 5);
            this.button.requestFocus();
            ButtonPaneEventsTest.this.key(Key.ENTER);
            ButtonPaneEventsTest.this.key(Key.SPACE);
            assertEquals(List.of("press", "pressed", "press", "pressed", "press", "pressed"), this.seen);
        }

        @Test
        void cancelingThePressSkipsTheReport() {
            this.show();
            this.button.onEvent(Button.PRESS, event -> event.cancel());
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertEquals(List.of("press"), this.seen);
        }

        @Test
        void pressFromCodeOnlyReports() {
            this.show();
            this.button.press();
            this.button.setEnabled(false);
            this.button.press();
            assertEquals(List.of("pressed"), this.seen);
        }

        @Test
        void aFormCanRefuseAndHearPresses() {
            this.show();
            final List<Component<?>> targets = new ArrayList<>();
            this.form.onEvent(Button.PRESSED, event -> targets.add(event.control()));
            ButtonPaneEventsTest.this.test.click(5, 5);
            this.form.onEvent(Button.PRESS, event -> event.cancel());
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertEquals(List.of(this.button), targets);
            assertEquals(List.of("press", "pressed", "press"), this.seen);
        }
    }

    @Nested
    class CheckBoxes {
        private final List<Boolean> requests = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private CheckBox box;
        private AbsolutePanel form;

        private void show() {
            this.box = ButtonPaneEventsTest.this.ui.checkBox("Music")
                    .onEvent(CheckBox.TOGGLE, event -> {
                        // The box still has its old state while the request is delivered
                        assertEquals(!event.value(), event.control().isChecked());
                        this.requests.add(event.value());
                    })
                    .onEvent(CheckBox.TOGGLED, event -> this.heard.add(event.previous() + "->" + event.value()));
            this.form = ButtonPaneEventsTest.this.form(this.box);
        }

        @Test
        void clicksAndKeysToggle() {
            this.show();
            ButtonPaneEventsTest.this.test.click(5, 5);
            this.box.requestFocus();
            ButtonPaneEventsTest.this.key(Key.SPACE);
            assertEquals(List.of(true, false), this.requests);
            assertEquals(List.of("false->true", "true->false"), this.heard);
        }

        @Test
        void cancelingLeavesTheBox() {
            this.show();
            this.box.onEvent(CheckBox.TOGGLE, event -> event.cancel());
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.box.isChecked());
            assertEquals(List.of(true), this.requests);
            assertEquals(List.of(), this.heard);
        }

        @Test
        void aRewrittenValueIsApplied() {
            this.show();
            this.box.onEvent(CheckBox.TOGGLE, 1, event -> event.setValue(false));
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.box.isChecked());
            assertEquals(List.of(), this.heard);
        }

        @Test
        void codeOnlyReports() {
            this.show();
            this.box.setChecked(true);
            this.box.setChecked(true);
            this.box.toggle();
            assertEquals(List.of(), this.requests);
            assertEquals(List.of("false->true", "true->false"), this.heard);
        }

        @Test
        void aFormCanRefuse() {
            this.show();
            this.form.onEvent(CheckBox.TOGGLE, event -> event.cancel());
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.box.isChecked());
        }
    }

    @Nested
    class Toggles {
        private final List<Boolean> requests = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private ToggleSwitch toggle;
        private AbsolutePanel form;

        private void show() {
            this.toggle = ButtonPaneEventsTest.this.ui.toggleSwitch("PvP")
                    .onEvent(ToggleSwitch.TOGGLE, event -> {
                        assertEquals(!event.value(), event.control().isOn());
                        this.requests.add(event.value());
                    })
                    .onEvent(ToggleSwitch.TOGGLED, event -> this.heard.add(event.previous() + "->" + event.value()));
            this.form = ButtonPaneEventsTest.this.form(this.toggle);
        }

        @Test
        void clicksAndKeysFlip() {
            this.show();
            ButtonPaneEventsTest.this.test.click(5, 5);
            this.toggle.requestFocus();
            ButtonPaneEventsTest.this.key(Key.ENTER);
            assertEquals(List.of(true, false), this.requests);
            assertEquals(List.of("false->true", "true->false"), this.heard);
        }

        @Test
        void cancelingLeavesTheSwitch() {
            this.show();
            this.toggle.onEvent(ToggleSwitch.TOGGLE, event -> event.cancel());
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.toggle.isOn());
            assertEquals(List.of(), this.heard);
        }

        @Test
        void aRewrittenValueIsAppliedAndNullIgnored() {
            this.show();
            final Subscription off = this.toggle.subscribe(ToggleSwitch.TOGGLE, 1, event -> event.setValue(false));
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.toggle.isOn());
            off.close();
            this.toggle.onEvent(ToggleSwitch.TOGGLE, 1, event -> event.setValue(null));
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.toggle.isOn());
            assertEquals(List.of(), this.heard);
        }

        @Test
        void codeOnlyReports() {
            this.show();
            this.toggle.setOn(true);
            this.toggle.toggle();
            assertEquals(List.of(), this.requests);
            assertEquals(List.of("false->true", "true->false"), this.heard);
        }

        @Test
        void aFormHearsTheChange() {
            this.show();
            final List<Boolean> formHeard = new ArrayList<>();
            this.form.onEvent(ToggleSwitch.TOGGLED, event -> formHeard.add(event.value()));
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertEquals(List.of(true), formHeard);
        }
    }

    @Nested
    class Radios {
        private final List<String> requests = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private RadioGroup<String> group;
        private RadioButton easy;
        private RadioButton normal;
        private RadioButton hard;
        private StackPanel column;

        private void show() {
            final ComponentFactory ui = ButtonPaneEventsTest.this.ui;
            this.group = ui.radioGroup();
            this.easy = this.group.add("Easy", "easy");
            this.normal = this.group.add("Normal", "normal");
            this.hard = this.group.add("Hard", "hard");
            for (final RadioButton button : this.group.buttons()) {
                button.onEvent(RadioButton.SELECT, event -> {
                    assertFalse(event.control().isSelected());
                    this.requests.add(event.control().text());
                });
            }
            this.group.onEvent(RadioGroup.SELECTED, event -> this.heard.add(event.previous() + "->" + event.value()
                    + " " + ((RadioButton) event.target()).text()));
            this.column = ui.stackPanel().add(this.easy).add(this.normal).add(this.hard);
            ButtonPaneEventsTest.this.test.show(this.column);
        }

        private void click(final RadioButton button) {
            final Rect bounds = button.bounds();
            ButtonPaneEventsTest.this.test.click(bounds.x() + 2, bounds.y() + 2);
        }

        @Test
        void clicksAndArrowsSelect() {
            this.show();
            this.click(this.easy);
            this.easy.requestFocus();
            ButtonPaneEventsTest.this.key(Key.DOWN);
            assertTrue(this.normal.isSelected());
            assertSame(this.normal, ButtonPaneEventsTest.this.test.focused());
            assertEquals(List.of("Easy", "Normal"), this.requests);
            assertEquals(List.of("-1->0 Easy", "0->1 Normal"), this.heard);
        }

        @Test
        void clickingTheSelectedButtonSkipsTheRequest() {
            this.show();
            this.click(this.easy);
            this.click(this.easy);
            assertEquals(List.of("Easy"), this.requests);
            assertEquals(List.of("-1->0 Easy"), this.heard);
        }

        @Test
        void cancelingLeavesSelectionAndFocus() {
            this.show();
            this.click(this.easy);
            this.easy.requestFocus();
            this.normal.onEvent(RadioButton.SELECT, event -> event.cancel());
            this.hard.onEvent(RadioButton.SELECT, event -> event.cancel());
            ButtonPaneEventsTest.this.key(Key.DOWN);
            assertTrue(this.easy.isSelected());
            assertSame(this.easy, ButtonPaneEventsTest.this.test.focused());
            this.click(this.hard);
            assertTrue(this.easy.isSelected());
            assertEquals(List.of("Easy", "Normal", "Hard"), this.requests);
            assertEquals(List.of("-1->0 Easy"), this.heard);
        }

        @Test
        void codeOnlyReports() {
            this.show();
            this.group.select("hard");
            this.easy.setSelected(true);
            this.easy.setSelected(true);
            this.group.clearSelection();
            assertEquals(List.of(), this.requests);
            assertEquals(List.of("-1->2 Hard", "2->0 Easy", "0->-1 Easy"), this.heard);
        }

        @Test
        void aFormCanRefuseButGroupEventsDoNotBubble() {
            this.show();
            final List<Integer> outside = new ArrayList<>();
            this.column.onEvent(RadioGroup.SELECTED, event -> outside.add(event.value()));
            this.click(this.easy);
            this.column.onEvent(RadioButton.SELECT, event -> event.cancel());
            this.click(this.hard);
            assertTrue(this.easy.isSelected());
            assertEquals(List.of(), outside);
            assertEquals(List.of("-1->0 Easy"), this.heard);
        }

        @Test
        void groupHandlersChainAndUnsubscribe() {
            this.show();
            final List<Integer> heard = new ArrayList<>();
            assertSame(this.group, this.group.onEvent(RadioGroup.SELECTED, event -> { }));
            final Subscription subscription = this.group.subscribe(RadioGroup.SELECTED, 0,
                    event -> heard.add(event.value()));
            this.group.select("normal");
            subscription.close();
            this.group.select("easy");
            assertEquals(List.of(1), heard);
            assertSame(this.group, this.normal.group());
        }
    }

    @Nested
    class Expanders {
        private final List<Boolean> requests = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private Expander expander;
        private AbsolutePanel form;

        private void show() {
            final ComponentFactory ui = ButtonPaneEventsTest.this.ui;
            this.expander = ui.expander("More").setContent(ui.label("Inside"))
                    .onEvent(Expander.TOGGLE, event -> this.requests.add(event.value()))
                    .onEvent(Expander.TOGGLED, event -> this.heard.add(event.previous() + "->" + event.value()));
            this.form = ButtonPaneEventsTest.this.form(this.expander);
        }

        @Test
        void clicksAndKeysExpandAndCollapse() {
            this.show();
            ButtonPaneEventsTest.this.test.click(5, 5);
            this.expander.requestFocus();
            ButtonPaneEventsTest.this.key(Key.SPACE);
            assertEquals(List.of(true, false), this.requests);
            assertEquals(List.of("false->true", "true->false"), this.heard);
        }

        @Test
        void cancelingLeavesItCollapsed() {
            this.show();
            this.expander.onEvent(Expander.TOGGLE, event -> event.cancel());
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.expander.isExpanded());
            assertFalse(this.expander.content().isAttached());
            assertEquals(List.of(), this.heard);
        }

        @Test
        void aRewrittenValueIsApplied() {
            this.show();
            this.expander.onEvent(Expander.TOGGLE, event -> event.setValue(false));
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.expander.isExpanded());
            assertEquals(List.of(), this.heard);
        }

        @Test
        void codeOnlyReports() {
            this.show();
            this.expander.setExpanded(true);
            this.expander.toggle();
            assertEquals(List.of(), this.requests);
            assertEquals(List.of("false->true", "true->false"), this.heard);
        }

        @Test
        void aFormCanRefuse() {
            this.show();
            this.form.onEvent(Expander.TOGGLE, event -> event.cancel());
            ButtonPaneEventsTest.this.test.click(5, 5);
            assertFalse(this.expander.isExpanded());
        }
    }

    @Nested
    class Tabs {
        private final List<Integer> requests = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private TabBar tabs;
        private AbsolutePanel form;

        private void show() {
            this.tabs = ButtonPaneEventsTest.this.ui.tabBar().addTab("Buy").addTab("Sell").addTab("Trade")
                    .onEvent(TabBar.SELECT, event -> this.requests.add(event.value()))
                    .onEvent(TabBar.SELECTED, event -> this.heard.add(event.previous() + "->" + event.value()));
            this.tabs.setWidth(300);
            this.form = ButtonPaneEventsTest.this.form(this.tabs);
        }

        @Test
        void clicksAndKeysSelect() {
            this.show();
            ButtonPaneEventsTest.this.test.click(150, 5);
            this.tabs.requestFocus();
            ButtonPaneEventsTest.this.key(Key.RIGHT);
            ButtonPaneEventsTest.this.test.click(250, 5);
            assertEquals(List.of(1, 2), this.requests);
            assertEquals(List.of("0->1", "1->2"), this.heard);
        }

        @Test
        void cancelingKeepsTheTab() {
            this.show();
            this.tabs.onEvent(TabBar.SELECT, event -> event.cancel());
            ButtonPaneEventsTest.this.test.click(150, 5);
            this.tabs.requestFocus();
            ButtonPaneEventsTest.this.key(Key.RIGHT);
            assertEquals(0, this.tabs.selectedIndex());
            assertEquals(List.of(1, 1), this.requests);
            assertEquals(List.of(), this.heard);
        }

        @Test
        void aRewrittenIndexIsSelectedUnlessItCannotBe() {
            this.show();
            final Integer[] rewrite = {2};
            this.tabs.onEvent(TabBar.SELECT, -1, event -> event.setValue(rewrite[0]));
            ButtonPaneEventsTest.this.test.click(150, 5);
            assertEquals(2, this.tabs.selectedIndex());

            this.tabs.setTabEnabled(1, false);
            rewrite[0] = 1;
            ButtonPaneEventsTest.this.test.click(50, 5);
            rewrite[0] = 7;
            ButtonPaneEventsTest.this.test.click(50, 5);
            rewrite[0] = null;
            ButtonPaneEventsTest.this.test.click(50, 5);
            assertEquals(2, this.tabs.selectedIndex());
            assertEquals(List.of("0->2"), this.heard);
        }

        @Test
        void codeOnlyReports() {
            this.show();
            this.tabs.select(2);
            this.tabs.select(2);
            assertEquals(List.of(), this.requests);
            assertEquals(List.of("0->2"), this.heard);
        }

        @Test
        void theBarItselfFiresAndAFormHearsIt() {
            this.show();
            final List<Component<?>> targets = new ArrayList<>();
            this.form.onEvent(TabBar.SELECTED, event -> targets.add(event.target()));
            ButtonPaneEventsTest.this.test.click(150, 5);
            assertEquals(List.of(this.tabs), targets);
        }
    }

    @Nested
    class SplitPanes {
        private final List<Float> requests = new ArrayList<>();
        private final List<String> heard = new ArrayList<>();
        private SplitPane split;
        private AbsolutePanel form;

        private void show() {
            final ComponentFactory ui = ButtonPaneEventsTest.this.ui;
            this.split = ui.splitPane(Orientation.HORIZONTAL)
                    .setFirst(ui.anchorPanel().setAnchor(Anchor.FILL))
                    .setSecond(ui.anchorPanel().setAnchor(Anchor.FILL))
                    .onEvent(SplitPane.MOVE, event -> this.requests.add(event.value()))
                    .onEvent(SplitPane.MOVED, event -> this.heard.add(event.previous() + "->" + event.value()));
            // 200 units shared between the sides, the divider starting at x = 100
            this.split.setSize(204, 50);
            this.form = ButtonPaneEventsTest.this.form(this.split);
        }

        private void drag(final float from, final float to) {
            final var host = ButtonPaneEventsTest.this.test.host();
            host.pointerMoved(from, 25, Modifiers.NONE);
            host.pointerButton(from, 25, MouseButton.LEFT, true, Modifiers.NONE);
            host.pointerMoved(to, 25, Modifiers.NONE);
            host.pointerButton(to, 25, MouseButton.LEFT, false, Modifiers.NONE);
            host.layout();
        }

        @Test
        void draggingMovesTheDivider() {
            this.show();
            this.drag(102, 152);
            assertEquals(0.75f, this.split.position());
            assertEquals(List.of(0.75f), this.requests);
            assertEquals(List.of("0.5->0.75"), this.heard);
        }

        @Test
        void cancelingKeepsTheDivider() {
            this.show();
            this.split.onEvent(SplitPane.MOVE, event -> event.cancel());
            this.drag(102, 152);
            assertEquals(0.5f, this.split.position());
            assertEquals(List.of(0.75f), this.requests);
            assertEquals(List.of(), this.heard);
        }

        @Test
        void aRewrittenPositionIsKeptWithinTheMinimumSizes() {
            this.show();
            final Float[] rewrite = {0.25f};
            this.split.onEvent(SplitPane.MOVE, -1, event -> event.setValue(rewrite[0]));
            this.drag(102, 152);
            assertEquals(0.25f, this.split.position());

            // 20 units is the least either side gets: 0.1 of the space
            rewrite[0] = 0f;
            this.drag(52, 152);
            assertEquals(0.1f, this.split.position());
            rewrite[0] = Float.NaN;
            this.drag(22, 152);
            rewrite[0] = null;
            this.drag(22, 152);
            assertEquals(0.1f, this.split.position());
            assertEquals(List.of("0.5->0.25", "0.25->0.1"), this.heard);
        }

        @Test
        void aDragThatWouldNotMoveSkipsTheRequest() {
            this.show();
            // Both drags end past the second side's minimum, so the second one keeps the divider at 0.9
            this.drag(102, 300);
            this.drag(182, 300);
            assertEquals(List.of(0.9f), this.requests);
            assertEquals(List.of("0.5->0.9"), this.heard);
        }

        @Test
        void codeOnlyReports() {
            this.show();
            this.split.setPosition(0.3f);
            this.split.setPosition(0.3f);
            assertEquals(List.of(), this.requests);
            assertEquals(List.of("0.5->0.3"), this.heard);
        }

        @Test
        void thePaneItselfFiresAndAFormCanRefuse() {
            this.show();
            final List<Component<?>> targets = new ArrayList<>();
            this.form.onEvent(SplitPane.MOVE, event -> {
                targets.add(event.target());
                event.cancel();
            });
            this.drag(102, 152);
            assertEquals(List.of(this.split), targets);
            assertEquals(0.5f, this.split.position());
        }
    }

    @Nested
    class ScrollPanels {
        private final List<String> heard = new ArrayList<>();
        private ScrollPanel panel;
        private AbsolutePanel form;

        private void show() {
            final ComponentFactory ui = ButtonPaneEventsTest.this.ui;
            final AbsolutePanel content = ui.absolutePanel();
            content.setSize(50, 300);
            this.panel = ui.scrollPanel().add(content)
                    .onEvent(ScrollPanel.SCROLLED, event -> this.heard.add(event.previous().scrollY() + "->"
                            + event.value().scrollY() + " of " + event.value().maxScrollY()));
            this.panel.setSize(100, 100);
            this.form = ButtonPaneEventsTest.this.form(this.panel);
        }

        private void wheel(final float notches) {
            ButtonPaneEventsTest.this.test.host().scroll(30, 30, 0f, notches, ScrollKind.WHEEL, Modifiers.NONE);
        }

        @Test
        void inputAndCodeReportWithTheStateBefore() {
            this.show();
            this.wheel(-2);
            this.panel.setScroll(0, 50);
            this.panel.setScroll(0, 50);
            this.panel.scrollBy(0, 1000);
            assertEquals(List.of("0.0->20.0 of 200.0", "20.0->50.0 of 200.0", "50.0->200.0 of 200.0"), this.heard);
        }

        @Test
        void aScrollPastTheEndReportsNothing() {
            this.show();
            this.wheel(1);
            assertEquals(List.of(), this.heard);
        }

        @Test
        void aFormHearsScrolling() {
            this.show();
            final List<Component<?>> targets = new ArrayList<>();
            this.form.onEvent(ScrollPanel.SCROLLED, event -> targets.add(event.target()));
            this.wheel(-1);
            assertEquals(List.of(this.panel), targets);
        }
    }
}
