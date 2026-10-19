package org.inspirenxe.strata.examples.overlays;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;

import java.util.List;

/**
 * The overlays guide's examples, and the message box in the controls guide.
 */
public final class OverlayExamples {

    private OverlayExamples() {
    }

    static void confirmDelete(final ComponentFactory factory, final Runnable delete) {
        // --8<-- [start:message-box]
        factory.overlays().dialog(factory.messageBox("Delete", "Delete this exchange?", MessageBox.Buttons.YES_NO)
                .onResult(result -> {
                    if (result == MessageBox.Result.YES) {
                        delete.run();
                    }
                }));
        // --8<-- [end:message-box]
    }

    static Overlay popup(final ComponentFactory factory, final Button more) {
        // --8<-- [start:popup]
        final AnchorPanel menu = factory.anchorPanel().add(factory.label("Popup content"));
        menu.setPadding(4).setBackground(Paint.solid(0xF0101010));
        final Overlay popup = factory.overlays().popup(menu, more.bounds());
        popup.onClosed(() -> more.setText("More"));
        // --8<-- [end:popup]
        return popup;
    }

    static void quantity(final ComponentFactory factory, final long max) {
        // --8<-- [start:dialog]
        final TextBox amount = factory.textBox().setText("1").setSelectAllOnFocus(true);
        final Button buy = factory.button("Buy");
        final Window window = factory.window("Enter a Quantity").add(amount);
        window.setWidth(130);
        amount.onSubmit(text -> buy.press());
        buy.onAction(() -> {
            // ... buy Long.parseLong(amount.text()) ...
            window.close();
        });
        factory.overlays().dialog(window);
        // --8<-- [end:dialog]
    }

    static Label tooltips(final ComponentFactory factory) {
        // --8<-- [start:tooltips]
        final Label price = factory.label("125.50/ea").setTooltip("Total: 1,506");

        final TextStyle white = TextStyle.DEFAULT.withColor(0xFFFFFFFF);
        final TextStyle blue = TextStyle.DEFAULT.withColor(0xFF5555FF).withItalic(true);
        price.setTooltip(Tooltip.lines(List.of(
                List.of(new TextSpan("Diamond", white)),
                List.of(new TextSpan("Example Mod", blue)))));
        // --8<-- [end:tooltips]
        return price;
    }

    static void modifiers(final AnchorPanel screen, final Button buyOne) {
        // --8<-- [start:modifiers]
        screen.on(EventTypes.MODIFIERS_CHANGED, event ->
                buyOne.setText(event.modifiers().control() ? "Delist" : "Buy 1"));
        // --8<-- [end:modifiers]
    }
}
