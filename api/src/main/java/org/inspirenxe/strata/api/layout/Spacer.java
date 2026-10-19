package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Component;

/**
 * An empty component that only takes up room. It draws nothing of its own, takes no input and asks for no size. It
 * has {@link Anchor#FILL} by default, so it fills its slot unless it has a {@linkplain #setSize fixed size}, and its
 * minimum and maximum sizes limit it as usual.
 *
 * <p>A spacer with a fixed size keeps a set gap, such as the room before a group of buttons. A spacer with a
 * {@linkplain StackPanel#setWeight weight} in a {@link StackPanel} takes whatever room is left, which pushes the
 * children after it to the far end.</p>
 *
 * <pre>{@code
 * StackPanel buttons = factory.stackPanel(Orientation.HORIZONTAL)
 *         .add(resetButton)
 *         .add(factory.spacer(), 1)
 *         .add(cancelButton)
 *         .add(factory.spacer(4, 0))
 *         .add(doneButton);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#spacer()}. Do not implement this
 * interface.</p>
 */
public interface Spacer extends Component<Spacer> {
}
