/*
 * This is the source code of Telegram for Android v. 3.x.x
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
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.ForegroundFrameLayout;
import org.telegram.ui.Components.LayoutHelper;

public class TextBlockCell extends ForegroundFrameLayout {

    private TextView textView;
    private static Paint paint;
    private boolean needDivider;

    public TextBlockCell(Context context) {
        super(context);

        if (paint == null) {
            paint = new Paint();
            paint.setColor(ContextCompat.getColor(context, R.color.divider));
            paint.setStrokeWidth(1);
        }

        textView = new TextView(context);
        textView.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 17, 10, 17, 10));
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setText(String text, boolean divider) {
        textView.setText(text);
        needDivider = divider;
        setWillNotDraw(!divider);
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
