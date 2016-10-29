/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.LayoutHelper;

public class RadioCell extends FrameLayout {

    private TextView textView;
    private RadioButton radioButton;
    private static Paint paint;
    private boolean needDivider;

    public RadioCell(Context context) {
        super(context);

        if (paint == null) {
            paint = new Paint();
            paint.setColor(ContextCompat.getColor(context, R.color.divider));
            paint.setStrokeWidth(1);
        }

        setElevation(AndroidUtilities.dp(2));
        setBackgroundColor(ContextCompat.getColor(context, R.color.card_background));

        textView = new TextView(context);
        textView.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 17, 0, 17, 0));

        radioButton = new RadioButton(context);
        radioButton.setClickable(false);
        radioButton.setFocusable(false);
        radioButton.setBackground(null);
        /*radioButton.setSize(AndroidUtilities.dp(20));
        radioButton.setColor(0xffb3b3b3, 0xff37a9f0);*/
        addView(radioButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, (LocaleController.isRTL ? 18 : 0), 0, (LocaleController.isRTL ? 0 : 18), 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(48) + (needDivider ? 1 : 0));

        int availableWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - AndroidUtilities.dp(34);
        radioButton.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
        textView.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setText(String text, boolean checked, boolean divider) {
        textView.setText(text);
        radioButton.setChecked(checked/*, false*/);
        needDivider = divider;
        setWillNotDraw(!divider);
    }

    public void setChecked(boolean checked, boolean animated) {
        radioButton.setChecked(checked/*, animated*/);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(getPaddingLeft(), getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, paint);
        }
    }

    public static void resetDivider() {
        paint = null;
    }
}
