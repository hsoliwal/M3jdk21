/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * A native Swing tree dropdown inspired by Nebula CTreeCombo. A caller can
 * supply a JTree or SwingX JXTree with its original TreeModel and UI delegate.
 * The selection is a TreePath, not a copied hierarchy or per-row widget.
 */
public final class M3TreeCombo extends M3PopupSelector<JTree> {
    private static final long serialVersionUID = 1L;
    private final JTree tree;
    private TreeModel observedModel;
    private final TreeModelListener modelListener = new TreeModelListener() {
        @Override public void treeNodesChanged(TreeModelEvent e) { refreshText(); }
        @Override public void treeNodesInserted(TreeModelEvent e) { refreshText(); }
        @Override public void treeNodesRemoved(TreeModelEvent e) { refreshText(); }
        @Override public void treeStructureChanged(TreeModelEvent e) { refreshText(); }
    };

    public M3TreeCombo(TreeModel model) { this(new JTree(Objects.requireNonNull(model, "model"))); }
    public M3TreeCombo(JTree tree) {
        super(tree);
        this.tree = tree;
        tree.addTreeSelectionListener(e -> refreshText());
        trackModel();
        tree.addPropertyChangeListener("model", e -> {
            trackModel();
            refreshText();
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1 && tree.getPathForLocation(event.getX(), event.getY()) != null) {
                    commitSelection();
                }
            }
        });
        tree.getInputMap(JTree.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "m3.commit");
        tree.getActionMap().put("m3.commit", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent event) { commitSelection(); }
        });
        refreshText();
    }

    private void trackModel() {
        TreeModel current = tree.getModel();
        if (observedModel == current) return;
        if (observedModel != null) observedModel.removeTreeModelListener(modelListener);
        observedModel = current;
        current.addTreeModelListener(modelListener);
    }

    public JTree getTree() { return tree; }
    public TreePath getSelectedPath() { return tree.getSelectionPath(); }
    public void setSelectedPath(TreePath path) {
        requireEdt();
        tree.setSelectionPath(path);
        refreshText();
    }
    public void commitSelection() {
        requireEdt();
        if (tree.getSelectionPath() != null) {
            refreshText();
            fireSelection();
        }
    }
    public void refreshText() {
        requireEdt();
        TreePath selected = tree.getSelectionPath();
        display(selected == null ? "" : String.valueOf(selected.getLastPathComponent()));
    }
    @Override protected int rowCount() { return tree.getRowCount(); }
    @Override protected int rowHeight() { return tree.getRowHeight(); }
}
