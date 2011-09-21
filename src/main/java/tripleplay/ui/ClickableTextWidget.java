//
// Triple Play - utilities for use in PlayN-based games
// Copyright (c) 2011, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/tripleplay/blob/master/LICENSE

package tripleplay.ui;

import pythagoras.f.Dimension;

import react.Signal;
import react.SignalView;

/**
 * A text widget that provides button-like behavior.
 */
public class ClickableTextWidget<T extends ClickableTextWidget<T>> extends TextWidget<T>
    implements Clickable<T>
{
    @Override public SignalView<T> clicked () {
        return _clicked;
    }

    @Override protected void wasRemoved () {
        super.wasRemoved();
        // clear out our background instance
        if (_bginst != null) {
            _bginst.destroy();
            _bginst = null;
        }
        // if we're added again, we'll be re-laid-out
    }

    @Override protected Dimension computeSize (float hintX, float hintY) {
        LayoutData ldata = computeLayout(hintX, hintY);
        Dimension size = computeTextSize(ldata, new Dimension());
        return ldata.bg.addInsets(size);
    }

    @Override protected void layout () {
        float width = _size.width, height = _size.height;
        LayoutData ldata = computeLayout(width, height);

        // prepare our background
        Background bg = ldata.bg;
        if (_bginst != null) _bginst.destroy();
        if (_size.width > 0 && _size.height > 0) {
            _bginst = bg.instantiate(_size);
            _bginst.addTo(layer);
        }
        width -= bg.width();
        height -= bg.height();

        // prepare our label and icon
        renderLayout(ldata, bg.left, bg.top, width, height);

        clearLayoutData(); // we no longer need our layout data
    }

    @Override protected void onPointerStart (float x, float y) {
        super.onPointerStart(x, y);
        set(Flag.SELECTED, true);
        invalidate();
    }

    @Override protected void onPointerDrag (float x, float y) {
        super.onPointerDrag(x, y);
        boolean selected = contains(x, y);
        if (selected != isSelected()) {
            set(Flag.SELECTED, selected);
            invalidate();
        }
    }

    @Override protected void onPointerEnd (float x, float y) {
        super.onPointerEnd(x, y);
        // we don't check whether the supplied coordinates are in our bounds or not because only
        // the drag changes result in changes to the button's visualization, and we want to behave
        // based on what the user sees
        if (isSelected()) {
            set(Flag.SELECTED, false);
            invalidate();
            _clicked.emit(asT()); // emit a click event
        }
    }

    @Override protected void clearLayoutData () {
        super.clearLayoutData();
        _ldata = null;
    }

    protected LayoutData computeLayout (float hintX, float hintY) {
        if (_ldata != null) return _ldata;
        _ldata = new LayoutData();

        // determine our background
        Background bg = resolveStyle(Style.BACKGROUND);
        hintX -= bg.width();
        hintY -= bg.height();
        _ldata.bg = bg;

        // layout our text
        layoutText(_ldata, _text, hintX, hintY);

        return _ldata;
    }

    protected static class LayoutData extends TextWidget.LayoutData {
        public Background bg;
    }

    protected Background.Instance _bginst;
    protected LayoutData _ldata;

    protected final Signal<T> _clicked = Signal.create();
}