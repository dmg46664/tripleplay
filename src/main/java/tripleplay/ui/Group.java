//
// Triple Play - utilities for use in PlayN-based games
// Copyright (c) 2011, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/tripleplay/blob/master/LICENSE

package tripleplay.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import playn.core.Transform;

import pythagoras.f.AffineTransform;
import pythagoras.f.Dimension;
import pythagoras.f.Point;

/**
 * A grouping element that contains other elements and lays them out according to a layout policy.
 */
public class Group extends Element
{
    /**
     * Creates a group with the specified layout and no custom styles.
     */
    public Group (Layout layout) {
        _layout = layout;
    }

    /**
     * Creates a group with the specified layout and custom styles.
     */
    public Group (Layout layout, Styles styles) {
        _layout = layout;
        setStyles(styles);
    }

    /**
     * Returns the stylesheet configured for this group, or null.
     */
    public Stylesheet stylesheet () {
        return _sheet;
    }

    /**
     * Configures the stylesheet to be used by this group.
     */
    public Group setStylesheet (Stylesheet sheet) {
        _sheet = sheet;
        return this;
    }

    public int childCount () {
        return _children.size();
    }

    public Element childAt (int index) {
        return _children.get(index);
    }

    public Group add (Element... children) {
        for (Element child : children) {
            add(_children.size(), child);
        }
        return this;
    }

    public Group add (int index, Element child) {
        return add(index, child, null);
    }

    public Group add (Element child, Layout.Constraint constraint) {
        return add(_children.size(), child, constraint);
    }

    public Group add (int index, Element child, Layout.Constraint constraint) {
        // TODO: check if child is already added here? has parent?
        _children.add(index, child);
        if (constraint != null) {
            if (_constraints == null) {
                _constraints = new HashMap<Element, Layout.Constraint>();
            }
            _constraints.put(child, constraint);
        }
        didAdd(child);
        invalidate();
        return this;
    }

    public void remove (Element child) {
        if (_children.remove(child)) {
            didRemove(child);
            invalidate();
        }
    }

    public void removeAt (int index) {
        didRemove(_children.remove(index));
        invalidate();
    }

    public void removeAll () {
        _constraints = null;
        while (!_children.isEmpty()) {
            removeAt(_children.size()-1);
        }
        invalidate();
    }

    protected void didAdd (Element child) {
        layer.add(child.layer);
        if (isAdded()) child.wasAdded(this);
    }

    protected void didRemove (Element child) {
        layer.remove(child.layer);
        if (_constraints != null) _constraints.remove(child);
        if (isAdded()) child.wasRemoved();
    }

    @Override protected void wasAdded (Group parent) {
        super.wasAdded(parent);
        for (Element child : _children) {
            child.wasAdded(this);
        }
    }

    @Override protected void wasRemoved () {
        super.wasRemoved();
        for (Element child : _children) {
            child.wasRemoved();
        }

        // clear out our background instance
        if (_bginst != null) {
            _bginst.destroy();
            _bginst = null;
        }
        // if we're added again, we'll be re-laid-out
    }

    @Override protected Element hitTest (AffineTransform xform, Point point) {
        // transform the point into our coordinate system
        Transform lt = layer.transform();
        xform.setTransform(lt.m00(), lt.m10(), lt.m01(), lt.m11(), lt.tx(), lt.ty());
        point = xform.inverseTransform(point, point);
        // check whether it falls within our bounds
        float x = point.x + layer.originX(), y = point.y + layer.originY();
        if (!contains(x, y)) return null;
        // determine whether it falls within the bounds of any of our children
        for (Element child : _children) {
            Element hit = child.hitTest(xform, point.set(x, y));
            if (hit != null) return hit;
        }
        return null;
    }

    @Override protected Dimension computeSize (float hintX, float hintY) {
        LayoutData ldata = computeLayout(hintX, hintY);
        Dimension size = _layout.computeSize(
            _children, _constraints, hintX - ldata.bg.width(), hintY - ldata.bg.height());
        return ldata.bg.addInsets(size);
    }

    @Override protected void layout () {
        LayoutData ldata = computeLayout(_size.width, _size.height);

        // prepare our background
        if (_bginst != null) _bginst.destroy();
        _bginst = ldata.bg.instantiate(_size);
        _bginst.addTo(layer);

        // layout our children
        _layout.layout(_children, _constraints, ldata.bg.left, ldata.bg.top,
                       _size.width - ldata.bg.width(), _size.height - ldata.bg.height());

        // layout is only called as part of revalidation, so now we validate our children
        for (Element child : _children) {
            child.validate();
        }
    }

    protected LayoutData computeLayout (float hintX, float hintY) {
        if (_ldata == null) {
            _ldata = new LayoutData();
            // determine our background
            _ldata.bg = resolveStyle(state(), Style.BACKGROUND);
        }
        return _ldata;
    }

    protected static class LayoutData {
        public Background bg;
    }

    protected final Layout _layout;
    protected final List<Element> _children = new ArrayList<Element>();
    protected Stylesheet _sheet;
    protected Map<Element, Layout.Constraint> _constraints; // lazily created

    protected LayoutData _ldata;
    protected Background.Instance _bginst;
}
