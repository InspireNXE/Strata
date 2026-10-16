package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Insets;

import java.util.List;

/**
 * Strata's half of a {@link CustomContainer}.
 */
public interface ContainerPeer extends ComponentPeer {

    List<Component<?>> children();

    void add(Component<?> child);

    void remove(Component<?> child);

    void clear();

    Insets padding();

    void setPadding(Insets padding);

    boolean clipsChildren();

    void setClipChildren(boolean clipChildren);
}
