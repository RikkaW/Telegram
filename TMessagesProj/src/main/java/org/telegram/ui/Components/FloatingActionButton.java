package org.telegram.ui.Components;

import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ConfigurationHelper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;

/**
 * Created by Rikka on 2016/11/3.
 */

public class FloatingActionButton extends ImageView {

    /**
     * The mini sized button. Will always been smaller than {@link #SIZE_NORMAL}.
     *
     * @see #setSize(int)
     */
    public static final int SIZE_MINI = 1;

    /**
     * The normal sized button. Will always been larger than {@link #SIZE_MINI}.
     *
     * @see #setSize(int)
     */
    public static final int SIZE_NORMAL = 0;

    /**
     * Size which will change based on the window size. For small sized windows
     * (largest screen dimension < 470dp) this will select a small sized button, and for
     * larger sized windows it will select a larger size.
     *
     * @see #setSize(int)
     */
    public static final int SIZE_AUTO = -1;

    /**
     * The switch point for the largest screen edge where SIZE_AUTO switches from mini to normal.
     */
    private static final int AUTO_MINI_LARGEST_SCREEN_WIDTH = 470;

    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorAccent, outValue, true);
        int color = ContextCompat.getColor(context, outValue.resourceId);
        context.getTheme().resolveAttribute(android.R.attr.colorControlHighlight, outValue, true);
        int rippleColor = ContextCompat.getColor(context, outValue.resourceId);

        Drawable shapeDrawable = createShapeDrawable();
        shapeDrawable.setTint(color);

        Drawable borderDrawable = createBorderDrawable(context, (int) (0.5 * AndroidUtilities.density), ColorStateList.valueOf(color));
        Drawable rippleContent = new LayerDrawable(new Drawable[]{borderDrawable, shapeDrawable});

        RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(rippleColor),
                rippleContent, null);
        setBackground(rippleDrawable);
        setScaleType(ImageView.ScaleType.CENTER);

        setElevation(AndroidUtilities.dp(6));
        StateListAnimator stateListAnimator = new StateListAnimator();
        stateListAnimator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(this, "translationZ", AndroidUtilities.dp(6)).setDuration(200));
        stateListAnimator.addState(new int[0], ObjectAnimator.ofFloat(this, "translationZ", AndroidUtilities.dp(0)).setDuration(200));
        setStateListAnimator(stateListAnimator);
    }

    /*int getSizeDimension() {
        return getSizeDimension(mSize);
    }

    private int getSizeDimension(final int size) {
        final Resources res = getResources();
        switch (size) {
            case SIZE_AUTO:
                // If we're set to auto, grab the size from resources and refresh
                final int width = ConfigurationHelper.getScreenWidthDp(res);
                final int height = ConfigurationHelper.getScreenHeightDp(res);
                return Math.max(width, height) < AUTO_MINI_LARGEST_SCREEN_WIDTH
                        ? getSizeDimension(SIZE_MINI)
                        : getSizeDimension(SIZE_NORMAL);
            case SIZE_MINI:
                return res.getDimensionPixelSize(R.dimen.design_fab_size_mini);
            case SIZE_NORMAL:
            default:
                return res.getDimensionPixelSize(R.dimen.design_fab_size_normal);
        }
    }*/

    private GradientDrawable createShapeDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(Color.WHITE);
        return d;
    }

    private CircularBorderDrawable createBorderDrawable(Context context, int borderWidth, ColorStateList backgroundTint) {
        CircularBorderDrawable borderDrawable = new CircularBorderDrawable();
        borderDrawable.setGradientColors(
                0x2EFFFFFF/*ContextCompat.getColor(context, R.color.design_fab_stroke_top_outer_color)*/,
                0x1AFFFFFF/*ContextCompat.getColor(context, R.color.design_fab_stroke_top_inner_color)*/,
                0x0A000000/*ContextCompat.getColor(context, R.color.design_fab_stroke_end_inner_color)*/,
                0x0F000000/*ContextCompat.getColor(context, R.color.design_fab_stroke_end_outer_color)*/);
        borderDrawable.setBorderWidth(borderWidth);
        borderDrawable.setBorderTint(backgroundTint);
        return borderDrawable;
    }
}
