package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.TextOverflow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * A mock-up of Almura's shared {@code IngameFeatureManagementScreen}, the admin editor behind both "Exchange -
 * Management" and "Store - Management": a list of features on the left, the selected one's fields on the right.
 */
public final class ManagementScreen implements Screen {
    private final String title;
    private final List<Feature> features;
    private ScreenHandle handle;
    private ComponentFactory ui;

    private ListView<Feature> list;
    private TextBox id;
    private TextBox name;
    private TextBox permission;
    private TextBox creator;
    private TextBox creatorId;
    private TextBox created;
    private Label creatorLabel;
    private Label creatorIdLabel;
    private Label createdLabel;
    private CheckBox hidden;
    private Button save;
    private Button open;

    /**
     * One exchange or store, as the management screen edits it.
     *
     * @param id the unique id
     * @param name the title
     * @param permission the permission node needed to open it
     * @param hidden whether only admins see it
     * @param creator who made it
     * @param creatorId the creator's UUID
     * @param created when it was made
     */
    record Feature(String id, String name, String permission, boolean hidden, String creator, UUID creatorId,
                   LocalDateTime created) {
    }

    /**
     * Creates the screen with a few features.
     *
     * @param kind "Exchange" or "Store", for the title and sample names
     */
    public ManagementScreen(final String kind) {
        this.title = kind + " - Management";
        this.features = new ArrayList<>();
        final UUID owner = UUID.nameUUIDFromBytes("Zidane".getBytes());
        final String lower = kind.toLowerCase(Locale.ROOT);
        this.features.add(new Feature(lower + ".global", "Global Market", "almura." + lower + ".global", false, "Zidane",
                owner, LocalDateTime.of(2018, 4, 2, 14, 30)));
        this.features.add(new Feature(lower + ".spawn", "Spawn " + kind, "almura." + lower + ".spawn", false, "Dockter",
                owner, LocalDateTime.of(2018, 6, 11, 9, 5)));
        this.features.add(new Feature(lower + ".staff", "Staff Only", "almura." + lower + ".staff", true, "Grinch",
                owner, LocalDateTime.of(2019, 1, 20, 21, 45)));
    }

    @Override
    public boolean hidesBackground() {
        return true;
    }

    @Override
    public void opened(final ScreenHandle handle) {
        this.handle = handle;
    }

    @Override
    public Component<?> build(final ComponentFactory ui) {
        this.ui = ui;
        this.list = ui.<Feature>listView(feature -> Look.label(ui, feature.name(), Look.WHITE)
                        .setOverflow(TextOverflow.ELLIPSIS).setMargin(Look.pad(4, 0))
                        .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.MIDDLE))
                .setRowHeight(20)
                .setKeyFunction(Feature::id)
                .onSelectionChanged(this::show)
                .onActivate(feature -> this.open());
        Look.panel(this.list).setAnchor(Anchor.FILL);

        this.id = this.field(0).setFilter(text -> text.replaceAll("[^A-Za-z0-9_.]", "").toLowerCase(Locale.ROOT));
        this.name = this.field(1);
        this.permission = this.field(2).setFilter(text -> text.replaceAll("[^A-Za-z0-9_.]", "").toLowerCase(Locale.ROOT));
        this.creator = this.field(3).setReadOnly(true);
        this.creatorId = this.field(4).setReadOnly(true);
        this.created = this.field(5).setReadOnly(true);
        this.creatorLabel = Look.label(ui, "Creator:", Look.WHITE);
        this.creatorIdLabel = Look.label(ui, "Creator UUID:", Look.WHITE);
        this.createdLabel = Look.label(ui, "Created:", Look.WHITE);
        this.hidden = ui.checkBox("Hidden").onCheckedChanged(checked -> this.validate());
        this.save = Look.button(ui, "Save", 40).onAction(this::save);
        this.open = Look.button(ui, "Open", 40).onAction(this::open);

        final GridPanel detail = Look.grid(ui, GridLength.weighted(1), GridLength.fixed(140))
                .rows(GridLength.auto(), GridLength.auto(), GridLength.auto(), GridLength.auto(), GridLength.auto(),
                        GridLength.auto(), GridLength.weighted(1), GridLength.auto())
                .setSpacing(4, 2);
        this.addRow(detail, 0, Look.label(ui, "ID:", Look.WHITE), this.id);
        this.addRow(detail, 1, Look.label(ui, "Title:", Look.WHITE), this.name);
        this.addRow(detail, 2, Look.label(ui, "Permission:", Look.WHITE), this.permission);
        this.addRow(detail, 3, this.creatorLabel, this.creator);
        this.addRow(detail, 4, this.creatorIdLabel, this.creatorId);
        this.addRow(detail, 5, this.createdLabel, this.created);
        final GridPanel actions = Look.grid(ui, GridLength.weighted(1), GridLength.auto(), GridLength.auto())
                .rows(GridLength.auto()).setSpacing(2, 0);
        actions.add(this.hidden.setAnchor(Anchor.MIDDLE_LEFT), 0, 0).add(this.open, 0, 1).add(this.save, 0, 2);
        actions.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.BOTTOM);
        detail.add(actions, 7, 0, 1, 2);
        detail.setBorder(Look.PANEL_BORDER);
        detail.setPadding(4);

        final Button add = Look.button(ui, "+", 20).onAction(() -> {
            this.list.select(-1);
            this.id.requestFocus();
        });
        add.setTooltip("Add");
        final Button remove = Look.button(ui, "-", 20).onAction(this::confirmDelete);
        remove.setTooltip("Delete");
        final Button close = Look.button(ui, "Close", 40);

        final GridPanel bottom = Look.grid(ui, GridLength.auto(), GridLength.auto(), GridLength.weighted(1), GridLength.auto())
                .rows(GridLength.auto()).setSpacing(2, 0);
        bottom.add(add, 0, 0).add(remove, 0, 1).add(close, 0, 3);
        bottom.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final GridPanel layout = Look.grid(ui, GridLength.fixed(120), GridLength.weighted(1))
                .rows(GridLength.weighted(1), GridLength.auto()).setSpacing(4, 4);
        layout.add(this.list, 0, 0).add(detail, 0, 1).add(bottom, 1, 0, 1, 2);

        final Window window = ui.window(this.title).add(layout);
        window.setWidth(375);
        final Runnable leave = () -> {
            if (this.handle != null) {
                this.handle.close();
            }
        };
        window.onClose(leave);
        close.onAction(leave);

        this.list.setItems(this.features);
        this.list.select(0);
        final AnchorPanel screen = ui.anchorPanel().add(window);
        return screen.setAnchor(Anchor.FILL);
    }

    private TextBox field(final int tabIndex) {
        final TextBox box = this.ui.textBox().onTextChanged(text -> this.validate()).onSubmit(text -> this.save());
        box.setTabIndex(tabIndex);
        return box;
    }

    private void addRow(final GridPanel grid, final int row, final Label label, final TextBox box) {
        grid.add(label.setAnchor(Anchor.MIDDLE_LEFT), row, 0).add(box.setAnchor(Anchor.FILL), row, 1);
    }

    private void show(final Optional<Feature> selected) {
        final boolean existing = selected.isPresent();
        this.id.setText(selected.map(Feature::id).orElse("")).setReadOnly(existing);
        this.name.setText(selected.map(Feature::name).orElse(""));
        this.permission.setText(selected.map(Feature::permission).orElse(""));
        this.creator.setText(selected.map(Feature::creator).orElse(""));
        this.creatorId.setText(selected.map(feature -> feature.creatorId().toString()).orElse(""));
        this.created.setText(selected.map(feature -> feature.created().format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.US))).orElse(""));
        for (final Component<?> part : List.of(this.creator, this.creatorId, this.created, this.creatorLabel,
                this.creatorIdLabel, this.createdLabel)) {
            part.setVisible(existing);
        }
        this.hidden.setChecked(selected.map(Feature::hidden).orElse(false));
        this.open.setEnabled(existing);
        this.validate();
    }

    private void validate() {
        if (this.save == null) {
            return;
        }
        final boolean filled = !this.id.text().isEmpty() && !this.name.text().isEmpty() && !this.permission.text().isEmpty();
        final boolean unique = this.list.selected().isPresent()
                || this.features.stream().noneMatch(feature -> feature.id().equalsIgnoreCase(this.id.text()));
        this.save.setEnabled(filled && unique);
    }

    private void save() {
        if (!this.save.isEnabled()) {
            return;
        }
        final boolean adding = this.list.selected().isEmpty();
        final MessageBox confirm = this.ui.messageBox(this.title,
                "Are you sure you want to " + (adding ? "add" : "modify") + " " + this.name.text() + "?",
                MessageBox.Buttons.YES_NO).onResult(result -> {
                    if (result != MessageBox.Result.YES) {
                        return;
                    }
                    final Feature old = this.list.selected().orElse(null);
                    final Feature next = new Feature(this.id.text(), this.name.text(), this.permission.text(),
                            this.hidden.isChecked(), old == null ? "Zidane" : old.creator(),
                            old == null ? UUID.nameUUIDFromBytes("Zidane".getBytes()) : old.creatorId(),
                            old == null ? LocalDateTime.now() : old.created());
                    if (old == null) {
                        this.features.add(next);
                    } else {
                        this.features.set(this.features.indexOf(old), next);
                    }
                    this.list.setItems(this.features);
                    this.list.selectItem(next);
                });
        this.ui.overlays().dialog(confirm);
    }

    private void confirmDelete() {
        this.list.selected().ifPresent(feature -> this.ui.overlays().dialog(this.ui.messageBox(this.title,
                "Are you sure you want to delete " + feature.name() + "?", MessageBox.Buttons.YES_NO).onResult(result -> {
                    if (result == MessageBox.Result.YES) {
                        this.features.remove(feature);
                        this.list.setItems(this.features);
                        if (!this.features.isEmpty()) {
                            this.list.select(0);
                        }
                    }
                })));
    }

    private void open() {
        this.list.selected().ifPresent(feature -> this.ui.overlays().dialog(this.ui.messageBox(this.title,
                "This would run /almura open " + feature.id() + " on a server.", MessageBox.Buttons.OK)));
    }

    // For snapshots

    void openDeleteConfirmation() {
        this.confirmDelete();
    }
}
