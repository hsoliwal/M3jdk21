# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Generate META-INF/rewrite/m3-nebula-grid-viewport.yml: the Nebula Grid viewport hooks (one table, JSON-quoted).

Apply with com.synexia.openrewrite.viewport.M3PerFileApply (crate m3-viewport-catalogue-recipes), with this
directory first on the classpath; a second pass must report UNCHANGED.
"""
import json, os, sys

GRID = 'org.eclipse.nebula.widgets.grid.Grid'
ITEM = 'org.eclipse.nebula.widgets.grid.GridItem'


def q(s):
    return json.dumps(s)


GRID_MEMBERS = [
    # (memberName, beforeMethod, source)
    ('viewport', 'checkStyle',
     '\t/**\n\t * Row geometry mediation (see {@link GridViewport}); {@code null} when the Grid runs in the\n'
     '\t * original mode.\n\t */\n\tGridViewport viewport;'),
    ('rowsIndexed', 'usesSparseVirtualItems',
     '\t/**\n\t * @return {@code true} when the viewport answers the row geometry: the item list holds every row\n'
     '\t *         (it is not the sparse list of a flat virtual Grid) and rows can be hidden or differ in\n'
     '\t *         height\n\t */\n'
     '\tboolean rowsIndexed() {\n\t\treturn viewport != null && !usesSparseVirtualItems() && (isTree || hasDifferingHeights);\n\t}'),
    ('viewportRowRange', 'getGridHeight',
     '\t/**\n\t * @return the row range the viewport answered last\n\t */\n'
     '\tprivate RowRange viewportRowRange() {\n\t\tfinal RowRange range = new RowRange();\n'
     '\t\trange.startIndex = viewport.range[0];\n\t\trange.endIndex = viewport.range[1];\n'
     '\t\trange.rows = viewport.range[2];\n\t\trange.height = viewport.range[3];\n\t\treturn range;\n\t}'),
]

GRID_HOOKS = [
    # (method, parameterCount, position, anchor, statements[, parameterTypes])
    ('Grid', 3, 'METHOD_END', '',
     '\t\tif (GridViewport.ENABLED) {\n\t\t\tviewport = new GridViewport(this, items);\n\t\t\tviewport.applyLook();\n\t\t}'),
    ('getTopIndex', 0, 'AFTER', 'if (topIndex != -1)',
     '\t\tif (rowsIndexed()) {\n\t\t\ttopIndex = viewport.getTopIndex(isTree);\n\t\t\treturn topIndex;\n\t\t}'),
    ('setTopIndex', 1, 'AFTER', 'if (!vScroll.getVisible())',
     '\t\tif (viewport != null && !usesSparseVirtualItems()) {\n'
     '\t\t\tvScroll.setSelection(viewport.scrollSelectionOf(index, isTree));\n'
     '\t\t\ttopIndex = -1;\n\t\t\tbottomIndex = -1;\n\t\t\tfireViewportChanged();\n\t\t\tredraw();\n\t\t\treturn;\n\t\t}'),
    ('getRowRange', 2, 'METHOD_START', '',
     '\t\tif (rowsIndexed()) {\n\t\t\treturn viewport.rowRange(startIndex, endIndex) ? viewportRowRange() : null;\n\t\t}',
     'int,int'),
    ('getRowRange', 4, 'METHOD_START', '',
     '\t\tif (rowsIndexed()) {\n\t\t\treturn viewport.rowRange(startIndex, availableHeight, forceEndCompletelyInside, inverse)\n'
     '\t\t\t\t\t? viewportRowRange()\n\t\t\t\t\t: null;\n\t\t}'),
    ('getItem', 1, 'AFTER', 'int row = getTopIndex();',
     '\t\tif (rowsIndexed()) {\n\t\t\titemToReturn = viewport.itemAt(row, p.y - y2, getClientArea().height - y2);\n'
     '\t\t\trow = items.size(); // the walk below has no row left to visit\n\t\t}', 'Point'),
    ('getOrigin', 2, 'BEFORE', 'while (currIndex != itemIndex)',
     '\t\t\tif (viewport != null && !usesSparseVirtualItems() && currIndex <= items.size()\n'
     '\t\t\t\t\t&& itemIndex <= items.size()) {\n'
     '\t\t\t\ty += viewport.offsetBetween(currIndex, itemIndex);\n\t\t\t\tcurrIndex = itemIndex;\n\t\t\t}'),
    ('paintRows', 13, 'BEFORE', 'while (!item.isVisible() && row < items.size() - 1)',
     '\t\t\t\tif (viewport != null && !item.isVisible() && !usesSparseVirtualItems()) {\n'
     '\t\t\t\t\trow = viewport.nextVisibleRow(row);\n\t\t\t\t\titem = items.get(row);\n\t\t\t\t}'),
    ('getNextVisibleItem', 1, 'AFTER', 'int index = item.getRowIndex();',
     '\t\tif (viewport != null && !usesSparseVirtualItems() && index >= -1 && index < items.size()) {\n'
     '\t\t\treturn viewport.nextVisibleItem(index + 1);\n\t\t}'),
    ('getPreviousVisibleItem', 1, 'BEFORE', 'GridItem prevItem = items.get(index - 1);',
     '\t\tif (viewport != null && !usesSparseVirtualItems() && index >= 1 && index <= items.size()) {\n'
     '\t\t\treturn viewport.previousVisibleItem(index - 1);\n\t\t}'),
    ('newItem', 3, 'BEFORE', 'currentVisibleItems++;',
     '\t\tif (viewport != null) {\n\t\t\tviewport.inserted(item, row);\n\t\t}'),
    ('removeItem', 1, 'AFTER', 'items.remove(item);',
     '\t\tif (viewport != null) {\n\t\t\tviewport.rowsChanged();\n\t\t}'),
    ('setItemHeight', 1, 'AFTER', 'itemHeight = height;',
     '\t\tif (viewport != null) {\n\t\t\tviewport.rowsChanged();\n\t\t}'),
]

COLUMN = 'org.eclipse.nebula.widgets.grid.GridColumn'

COLUMN_HOOKS = [
    ('init', 3, 'BEFORE', 'initHeaderRenderer();',
     '\t\tif (table.viewport != null) {\n\t\t\ttable.viewport.applyLook(this);\n\t\t}'),
]

ITEM_MEMBERS = [
    ('knownRow', 'increaseRow',
     '\t/**\n\t * @return the row index this item keeps, or -1 when it keeps none (no search)\n\t */\n'
     '\tint knownRow() {\n\t\tsynchronized (ROW_LOCK) {\n\t\t\treturn row;\n\t\t}\n\t}'),
]

ITEM_HOOKS = [
    ('setVisible', 1, 'AFTER', 'this.visible = visible;',
     '\t\tif (parent.viewport != null) {\n\t\t\tparent.viewport.rowChanged(this);\n\t\t}'),
    ('setHeight', 1, 'AFTER', 'height = newHeight;',
     '\t\tif (parent.viewport != null) {\n\t\t\tparent.viewport.rowChanged(this);\n\t\t}'),
]


def members(cls, rows):
    out = []
    for name, before, source in rows:
        out.append('  - com.synexia.openrewrite.viewport.M3AddMemberIfMissing:')
        out.append('      declaringClasses: [%s]' % cls)
        out.append('      memberName: %s' % name)
        out.append('      beforeMethod: %s' % before)
        out.append('      memberSource: %s' % q(source))
    return out


def entries(cls, hooks):
    out = []
    for hook in hooks:
        method, params, position, anchor, statements = hook[:5]
        types = hook[5] if len(hook) > 5 else None
        out.append('  - com.synexia.openrewrite.viewport.M3InsertStatements:')
        out.append('      declaringClasses: [%s]' % cls)
        out.append('      methodName: %s' % method)
        out.append('      parameterCount: %d' % params)
        if types:
            out.append('      parameterTypes: %s' % q(types))
        if anchor:
            out.append('      anchor: %s' % q(anchor))
        out.append('      position: %s' % position)
        out.append('      statements: %s' % q(statements))
    return out


lines = [
    '---',
    'type: specs.openrewrite.org/v1beta/recipe',
    'name: com.synexia.openrewrite.viewport.NebulaGridViewport',
    'displayName: Nebula Grid answers its row geometry through the viewport',
    'description: >-',
    '  Catalogue VIEWPORT_WINDOWING. Grid answers its top row, row ranges, row at a point, row origin and next or',
    '  previous visible row from GridRowExtents (Fenwick visible-count and extent lanes) through GridViewport',
    '  instead of walking items. Each hook is one delegation; the logic lives in GridViewport. The viewport is the',
    '  default Grid mode; -Dswt.widget.mode.Grid=original (or the global swt.widget.mode) selects the original paths.',
    'recipeList:',
    '  - com.synexia.openrewrite.viewport.NebulaGridViewportGrid',
    '  - com.synexia.openrewrite.viewport.NebulaGridViewportGridItem',
    '  - com.synexia.openrewrite.viewport.NebulaGridViewportGridColumn',
    '---',
    'type: specs.openrewrite.org/v1beta/recipe',
    'name: com.synexia.openrewrite.viewport.NebulaGridViewportGrid',
    'displayName: Nebula Grid viewport hooks',
    'description: The viewport field, two helpers and the Grid delegations.',
    'recipeList:',
] + members(GRID, GRID_MEMBERS) + entries(GRID, GRID_HOOKS) + [
    '---',
    'type: specs.openrewrite.org/v1beta/recipe',
    'name: com.synexia.openrewrite.viewport.NebulaGridViewportGridItem',
    'displayName: Nebula GridItem viewport hooks',
    'description: A visibility or height change updates one position of the row extent index.',
    'recipeList:',
] + members(ITEM, ITEM_MEMBERS) + entries(ITEM, ITEM_HOOKS) + [
    '---',
    'type: specs.openrewrite.org/v1beta/recipe',
    'name: com.synexia.openrewrite.viewport.NebulaGridViewportGridColumn',
    'displayName: Nebula GridColumn viewport hooks',
    'description: A new column of a viewport Grid gets the native look renderers (GridNativeLook).',
    'recipeList:',
] + entries(COLUMN, COLUMN_HOOKS)

out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), 'META-INF', 'rewrite', 'm3-nebula-grid-viewport.yml')
with open(out, 'w', encoding='utf-8', newline='\n') as f:
    f.write('\n'.join(lines) + '\n')
print('hooks: Grid %d (+%d members), GridItem %d (+%d members), GridColumn %d' % (len(GRID_HOOKS), len(GRID_MEMBERS), len(ITEM_HOOKS), len(ITEM_MEMBERS), len(COLUMN_HOOKS)))
