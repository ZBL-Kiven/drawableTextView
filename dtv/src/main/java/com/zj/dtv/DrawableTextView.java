package com.zj.dtv;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by ZJJ on 2018/7/13.
 * <p>
 * Enhanced textDrawableView, text supports one line for the time being, you can customize the picture, customize the color / picture conversion in two selection modes
 * <p>
 * {@link #initData} (float curAnimationFraction);
 * Supports custom size of pictures, supports transformation, supports relative positions such as up, down, left, right, etc.
 * According to the setting of DrawableTextView.initData (0.0f / 1.0f), instant switching between two states can be realized;
 * Can also be used for gradient switching with attribute animation;
 * <p>
 * v1.0.1 Badge supported , now you can set a badge in DrawableTextView, and set a String text with {@link #setBadgeText}
 * also you should declared the badge attrs in your xml file , the property 'badgeEnable' is required . you can set the badges text color/size ,
 * background ,padding ,margin ,minWidth ,minHeight . as always , it supported to set a Gravity for badges.
 * <p>
 * v1.0.2 badgeBackground ,background , badgeTextColor] are supported change with animation by selected/unselected.
 */

@SuppressWarnings("unused")
public class DrawableTextView extends View {
    private static final String ellipse = "\u2026";
    private float drawableWidth = 0;
    private float drawableHeight = 0;
    private Drawable replaceDrawable, selectedDrawable, badgeBackground, badgeBackgroundSelected, backgroundDrawable, backgroundDrawableSelected;
    private int orientation, drawableOrientation = DrawableOrientation.none;
    private int gravity, badgeGravity;
    private float paddingLeft = 0.0f, paddingTop = 0.0f, paddingRight = 0.0f, paddingBottom = 0.0f, minWidthOffset = 0f, minHeightOffset = 0f;
    private float drawablePadding = 0.0f;
    private String text, textSelected, badgeText;
    private List<TextInfo> drawTextInfoList;
    private float textSize = dp2px(12);
    private int textColor = Color.GRAY, textColorSelect = -1;
    private float maxLength = -1, textLineSpacing = 0.1f;
    private int textGravity = TextGravity.left;
    private final float defaultTextSpacing = dp2px(10);
    private int maxTextLength = -1, maxLines = -1;
    /**
     * default: the basic width and height affected by system attributes. layout: the actual measured width and height
     */
    private float defaultWidth;
    private float defaultHeight;
    private PointF textStart, badgeTextStart;
    private Rect drawableRect, badgeRect;
    private float minWidth;
    private float minHeight;
    private float layoutWidth;
    private float layoutHeight;
    private float badgeMinWidth;
    private float badgeMinHeight;
    //The actual drawing area of the content (excluding badges)
    private final RectF contentRect = new RectF();
    private boolean badgeEnable = false;
    private int badgeTextColor, badgeTextColorSelected = Color.BLACK;
    private float badgeTextSize = 0, badgePadding = 0.0f, badgeMarginStart = 0.0f, badgeMarginEnd = 0.0f, badgeMarginTop = 0.0f, badgeMarginBottom = 0.0f;
    private int animDuration = 0;
    //Must be a .ttf file address with a valid path
    private String fontPath = "", badgeFontPath = "";
    private int fontStyle = -1, badgeFontStyle = -1;
    private Paint textPaint;
    private Paint badgeTextPaint;
    private final ArgbEvaluator evaluator = new ArgbEvaluator();
    private DrawableValueAnimator animator;
    private boolean isSelected = false;
    private boolean selectionAble = true;
    private BadgeClickListener badgeClickListener;
    private DrawableClickListener drawableClickListener;
    private PointF onTouchDownPoint;

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public void setSelected(boolean selected) {
        if (!selectionAble || isSelected() == selected) return;
        isSelected = selected;
        if (animator == null) {
            curAnimFraction = isSelected ? 1f : 0f;
        } else {
            animator.start(isSelected());
        }
        refreshAndValidate();
    }

    @Target(ElementType.PARAMETER)
    public @interface Orientation {
        int left = 0;
        int top = 1;
        int right = 2;
        int bottom = 3;
    }

    @Target(ElementType.PARAMETER)
    public @interface DrawableOrientation {
        int left = 0;
        int top = 1;
        int right = 2;
        int bottom = 3;
        int none = -1;
    }

    @Target(ElementType.PARAMETER)
    public @interface TextGravity {
        int center = 1;
        int left = 2;
        int right = 4;
    }

    @Target(ElementType.PARAMETER)
    public @interface Gravity {
        int left = 0x0002;
        int top = 0x0004;
        int right = 0x0008;
        int bottom = 0x0010;
        int center = 0x0001;
    }

    public DrawableTextView(Context context) {
        this(context, null, 0);
    }

    public DrawableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initData();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DrawableTextView);
            try {
                selectionAble = ta.getBoolean(R.styleable.DrawableTextView_dtv_selectionAble, true);
                minWidth = ta.getDimension(R.styleable.DrawableTextView_dtv_viewWidth, 0f);
                minHeight = ta.getDimension(R.styleable.DrawableTextView_dtv_viewHeight, 0f);
                drawableWidth = ta.getDimension(R.styleable.DrawableTextView_dtv_drawableWidth, 0f);
                drawableHeight = ta.getDimension(R.styleable.DrawableTextView_dtv_drawableHeight, 0f);
                float padding = ta.getDimension(R.styleable.DrawableTextView_dtv_padding, 0f);
                paddingLeft = ta.getDimension(R.styleable.DrawableTextView_dtv_paddingLeft, padding);
                paddingRight = ta.getDimension(R.styleable.DrawableTextView_dtv_paddingRight, padding);
                paddingBottom = ta.getDimension(R.styleable.DrawableTextView_dtv_paddingBottom, padding);
                paddingTop = ta.getDimension(R.styleable.DrawableTextView_dtv_paddingTop, padding);
                drawablePadding = ta.getDimension(R.styleable.DrawableTextView_dtv_drawablePadding, drawablePadding);
                replaceDrawable = ta.getDrawable(R.styleable.DrawableTextView_dtv_replaceDrawable);
                selectedDrawable = ta.getDrawable(R.styleable.DrawableTextView_dtv_selectedDrawable);
                backgroundDrawable = ta.getDrawable(R.styleable.DrawableTextView_dtv_background);
                drawableOrientation = ta.getInt(R.styleable.DrawableTextView_dtv_drawableOrientation, DrawableOrientation.none);
                backgroundDrawableSelected = ta.getDrawable(R.styleable.DrawableTextView_dtv_backgroundSelected);
                text = ta.getString(R.styleable.DrawableTextView_dtv_text);
                textSelected = ta.getString(R.styleable.DrawableTextView_dtv_textSelected);
                textSize = ta.getDimension(R.styleable.DrawableTextView_dtv_textSize, textSize);
                textColor = ta.getColor(R.styleable.DrawableTextView_dtv_textColor, textColor);
                textColorSelect = ta.getColor(R.styleable.DrawableTextView_dtv_textColorSelect, textColorSelect);
                textLineSpacing = ta.getDimension(R.styleable.DrawableTextView_dtv_textLineSpacing, .1f);
                textGravity = ta.getInt(R.styleable.DrawableTextView_dtv_textGravity, TextGravity.center);
                maxLines = ta.getInt(R.styleable.DrawableTextView_dtv_maxLine, Integer.MAX_VALUE);
                maxLength = ta.getDimension(R.styleable.DrawableTextView_dtv_maxLength, -1f);
                maxTextLength = ta.getInt(R.styleable.DrawableTextView_dtv_maxTextLength, -1);
                orientation = ta.getInt(R.styleable.DrawableTextView_dtv_orientation, Orientation.left);
                animDuration = ta.getInt(R.styleable.DrawableTextView_dtv_animDuration, 0);
                gravity = ta.getInt(R.styleable.DrawableTextView_dtv_gravity, Gravity.center);
                badgeEnable = ta.getBoolean(R.styleable.DrawableTextView_dtv_badgeEnable, badgeEnable);
                boolean clearTextIfEmpty = ta.getBoolean(R.styleable.DrawableTextView_dtv_clearTextIfEmpty, false);
                boolean selected = ta.getBoolean(R.styleable.DrawableTextView_dtv_select, isSelected);
                fontPath = ta.getString(R.styleable.DrawableTextView_dtv_textFontPath);
                fontStyle = ta.getInt(R.styleable.DrawableTextView_dtv_textStyle, -1);
                badgeFontPath = ta.getString(R.styleable.DrawableTextView_dtv_badgeTextFontPath);
                badgeFontStyle = ta.getInt(R.styleable.DrawableTextView_dtv_badgeTextStyle, -1);
                if (badgeEnable) {
                    badgeText = ta.getString(R.styleable.DrawableTextView_dtv_badgeText);
                    badgeBackground = ta.getDrawable(R.styleable.DrawableTextView_dtv_badgeBackground);
                    badgeBackgroundSelected = ta.getDrawable(R.styleable.DrawableTextView_dtv_badgeBackgroundSelected);
                    badgeTextColor = ta.getColor(R.styleable.DrawableTextView_dtv_badgeTextColor, badgeTextColor);
                    badgeTextColorSelected = ta.getColor(R.styleable.DrawableTextView_dtv_badgeTextColorSelected, badgeTextColorSelected);
                    badgeTextSize = ta.getDimension(R.styleable.DrawableTextView_dtv_badgeTextSize, badgeTextSize);
                    badgePadding = ta.getDimension(R.styleable.DrawableTextView_dtv_badgePadding, badgePadding);
                    badgeGravity = ta.getInt(R.styleable.DrawableTextView_dtv_badgeInGravity, Gravity.center);
                    float badgeMargin = ta.getDimension(R.styleable.DrawableTextView_dtv_badgeMargin, 0f);
                    badgeMinWidth = ta.getDimension(R.styleable.DrawableTextView_dtv_badgeMinWidth, 0f);
                    badgeMinHeight = ta.getDimension(R.styleable.DrawableTextView_dtv_badgeMinHeight, 0f);
                    badgeMarginStart = ta.getDimension(R.styleable.DrawableTextView_dtv_badgeMarginStart, badgeMargin);
                    badgeMarginEnd = ta.getDimension(R.styleable.DrawableTextView_dtv_badgeMarginEnd, badgeMargin);
                    badgeMarginTop = ta.getDimension(R.styleable.DrawableTextView_dtv_badgeMarginTop, badgeMargin);
                    badgeMarginBottom = ta.getDimension(R.styleable.DrawableTextView_dtv_badgeMarginBottom, badgeMargin);
                }
                if (!clearTextIfEmpty && TextUtils.isEmpty(textSelected)) textSelected = TextUtils.isEmpty(text) ? text = "" : text;
                if (textColorSelect == -1) textColorSelect = textColor;
                if (badgeTextColorSelected == -1) badgeTextColorSelected = badgeTextColor;
                setSelected(selected);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ta.recycle();
            }
        }
    }

    @SuppressLint("WrongConstant")
    private void initData() {
        textPaint = new Paint();
        drawTextInfoList = new ArrayList<>();
        Typeface typeface = Typeface.DEFAULT;
        if (!TextUtils.isEmpty(fontPath)) {
            typeface = Typeface.createFromAsset(getContext().getAssets(), fontPath);
        }
        if (fontStyle >= 0) {
            int fs;
            switch (fontStyle) {
                case Typeface.BOLD:
                case Typeface.ITALIC:
                case Typeface.NORMAL:
                case Typeface.BOLD_ITALIC:
                    fs = fontStyle;
                    break;
                default:
                    throw new IllegalArgumentException("The font must follow the specified size and be in one of Typeface.BOLD, Typeface.ITALIC, Typeface.NORMAL, Typeface.BOLD_ITALIC");
            }
            typeface = Typeface.create(typeface, fs);
        }
        textPaint.setTypeface(typeface);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (badgeEnable) {
            Typeface badgeTypeface = Typeface.DEFAULT;
            if (!TextUtils.isEmpty(badgeFontPath)) {
                badgeTypeface = Typeface.createFromAsset(getContext().getAssets(), badgeFontPath);
            }
            if (badgeFontStyle >= 0) {
                int fs;
                switch (badgeFontStyle) {
                    case Typeface.BOLD:
                    case Typeface.ITALIC:
                    case Typeface.NORMAL:
                    case Typeface.BOLD_ITALIC:
                        fs = fontStyle;
                        break;
                    default:
                        throw new IllegalArgumentException("The font must follow the specified size and be in one of Typeface.BOLD, Typeface.ITALIC, Typeface.NORMAL, Typeface.BOLD_ITALIC");
                }
                badgeTypeface = Typeface.create(typeface, fs);
            }
            badgeTextPaint = new Paint();
            badgeTextPaint.setAntiAlias(true);
            badgeTextPaint.setTypeface(badgeTypeface);
            badgeTextPaint.setTextSize(badgeTextSize);
            badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        }
        if (animDuration > 0) {
            animator = new DrawableValueAnimator();
            animator.setDuration(animDuration);
            animator.setOnAnimListener(fraction -> {
                DrawableTextView.this.curAnimFraction = fraction;
                postInvalidate();
            });
        }
        postInvalidate();
    }

    private void calculationAll() {
        calculateViewDimension();
        calculateGravityBounds();
        calculateBadgeBounds();
    }

    private void calculateViewDimension() {
        float textWidth;
        float textHeight;
        drawTextInfoList.clear();
        if ((!isSelected && TextUtils.isEmpty(text)) || (isSelected && TextUtils.isEmpty(textSelected))) {
            textWidth = 0;
            textHeight = 0;
        } else {
            PointF textInfo = measureTextSize(isSelected ? textSelected : text);
            textWidth = textInfo.x;
            textHeight = textInfo.y;
        }
        float viewHeight;
        float viewWidth;
        float drawableW = 0f, drawableH = 0f, drawableP = 0f;
        if ((isSelected && selectedDrawable != null) || (!isSelected && replaceDrawable != null)) {
            drawableW = drawableWidth;
            drawableH = drawableHeight;
            drawableP = drawablePadding;
        }
        if (orientation == Orientation.top || orientation == Orientation.bottom) {
            viewWidth = Math.max(textWidth, drawableW);
            viewHeight = textHeight + drawableH + drawableP;
        } else {
            viewHeight = Math.max(textHeight, drawableH);
            viewWidth = textWidth + drawableW + drawableP;
        }
        for (TextInfo tInfo : drawTextInfoList) tInfo.update(textWidth, textHeight, viewWidth, textGravity, paddingLeft, orientation, textPaint);
        final float badgeTextHalfHeight = !badgeEnable ? 0 : Math.max(badgeMinHeight, badgeTextPaint.getFontMetrics().descent - badgeTextPaint.getFontMetrics().ascent) / 2f;
        final float badgeTextHalfWidth = !badgeEnable ? 0 : Math.max(badgeMinWidth, TextUtils.isEmpty(badgeText) ? 0 : badgeTextPaint.measureText(badgeText)) / 2f;
        final boolean isAlignBottom = badgeEnable && (badgeGravity & Gravity.bottom) != 0;
        final boolean isAlignRight = badgeEnable && (badgeGravity & Gravity.right) != 0;
        final boolean isAlignCenter = badgeEnable && (badgeGravity & Gravity.center) != 0;
        final float bml = !badgeEnable ? 0 : isAlignRight ? (badgeTextHalfWidth - badgeMarginStart > viewWidth ? badgeTextHalfWidth - badgeMarginStart - viewWidth : 0f) : (isAlignCenter || badgeMarginStart > 0 ? 0 : Math.abs(badgeMarginStart));
        final float bmr = !badgeEnable ? 0 : isAlignRight ? (badgeMarginEnd < 0 ? 0 : badgeMarginEnd) : ((badgeMarginEnd + badgeTextHalfWidth > viewWidth && !isAlignCenter) ? badgeMarginEnd + badgeTextHalfWidth - viewWidth : 0f);
        final float bmt = !badgeEnable ? 0 : isAlignBottom ? (badgeTextHalfHeight - badgeMarginTop > viewHeight ? badgeTextHalfHeight - badgeMarginTop - viewHeight : 0f) : (isAlignCenter || badgeMarginTop > 0 ? 0 : Math.abs(badgeMarginTop));
        final float bmb = !badgeEnable ? 0 : isAlignBottom ? (badgeMarginBottom < 0 ? 0 : badgeMarginBottom) : ((badgeMarginBottom + badgeTextHalfHeight > viewHeight && !isAlignCenter) ? badgeMarginBottom + badgeTextHalfHeight - viewHeight : 0f);
        if (bml * 2f + viewWidth < minWidth) {
            minWidthOffset = (minWidth - (bml * 2f + viewWidth)) / 2.0f;
        }
        if (bmt * 2f + viewHeight < minHeight) {
            minHeightOffset = (minHeight - (bmt * 2f + viewHeight)) / 2.0f;
        }

        contentRect.set(bml, bmt, viewWidth + bml + minWidthOffset * 2.0f + paddingLeft + paddingRight, viewHeight + bmt + minHeightOffset * 2.0f + paddingTop + paddingBottom);
        if (badgeEnable) {
            if (badgeMarginBottom > contentRect.height()) contentRect.offset(0, badgeMarginBottom - contentRect.height());

        }

        if (badgeEnable && !isAlignRight && badgeMarginStart < 0) contentRect.left += badgeMarginStart;
        layoutWidth = viewWidth + bml + bmr + minWidthOffset * 2f + paddingLeft + paddingRight;
        layoutHeight = viewHeight + bmt + bmb + minHeightOffset * 2f + paddingTop + paddingBottom;
        if (defaultWidth == 0) defaultWidth = layoutWidth;
        if (defaultHeight == 0) defaultHeight = layoutHeight;
        int drawableLeft = 0, drawableRight = 0, drawableTop = 0, drawableBottom = 0;
        float textX = 0, textY = 0;
        switch (orientation) {
            case Orientation.left:
                drawableLeft = (int) (paddingLeft + bml + minWidthOffset);
                drawableTop = (int) (viewHeight / 2.0f - drawableH / 2.0f + 0.5f + minHeightOffset + paddingTop);
                drawableTop = (int) calculateHWithDrawableOrientation(drawableTop, textHeight, drawableH);
                drawableRight = (int) (drawableLeft + drawableW);
                drawableBottom = (int) (drawableTop + drawableH);
                textX = drawableRight + drawableP;
                textY = viewHeight / 2.0f + minHeightOffset + paddingTop;
                break;
            case Orientation.right:
                drawableLeft = (int) (paddingLeft + textWidth + drawableP + 0.5f + minWidthOffset + bml);
                drawableTop = (int) (viewHeight / 2.0f - drawableH / 2.0f + 0.5f + minHeightOffset + paddingTop);
                drawableTop = (int) calculateHWithDrawableOrientation(drawableTop, textHeight, drawableH);
                drawableRight = (int) (drawableLeft + drawableW);
                drawableBottom = (int) (drawableTop + drawableH);
                textX = paddingLeft + minWidthOffset + bml;
                textY = viewHeight / 2.0f + minHeightOffset + paddingTop;
                break;
            case Orientation.top:
                float dl = viewWidth / 2.0f - drawableW / 2.0f + 0.5f + minWidthOffset + bml + paddingLeft;
                drawableLeft = (int) (calculateWWithDrawableOrientation(dl, textWidth, drawableW));
                drawableTop = (int) (paddingTop + minHeightOffset);
                drawableRight = (int) (drawableLeft + drawableW);
                drawableBottom = (int) (drawableTop + drawableH);
                textX = minWidthOffset + bml + paddingLeft;
                textY = drawableBottom + drawableP;
                break;
            case Orientation.bottom:
                float dl1 = viewWidth / 2.0f - drawableW / 2.0f + 0.5f + minWidthOffset + bml + paddingLeft;
                drawableLeft = (int) (calculateWWithDrawableOrientation(dl1, textWidth, drawableW));
                drawableTop = (int) (paddingTop + textHeight + drawableP + minHeightOffset);
                drawableRight = (int) (drawableLeft + drawableW);
                drawableBottom = (int) (drawableTop + drawableH);
                textX = minWidthOffset + bml + paddingLeft;
                textY = paddingTop + minHeightOffset;
                break;
        }
        drawableRect = new Rect(drawableLeft, drawableTop, drawableRight, drawableBottom);
        textStart = new PointF(textX, textY);
    }

    private float calculateHWithDrawableOrientation(float drawableTop, float textHeight, float drawableH) {
        if (drawableOrientation == DrawableOrientation.top || drawableOrientation == DrawableOrientation.bottom) {
            if (drawableOrientation == DrawableOrientation.top) {
                drawableTop -= textHeight / 2f - drawableH / 2f;
            } else {
                drawableTop += textHeight / 2f - drawableH / 2f;
            }
        }
        return drawableTop;
    }

    private float calculateWWithDrawableOrientation(float drawableLeft, float textWidth, float drawableW) {
        if (drawableOrientation == DrawableOrientation.left || drawableOrientation == DrawableOrientation.right) {
            if (drawableOrientation == DrawableOrientation.left) {
                drawableLeft -= textWidth / 2f - drawableW / 2f;
            } else {
                drawableLeft += textWidth / 2f - drawableW / 2f;
            }
        }
        return drawableLeft;
    }

    private void calculateGravityBounds() {
        float widthOffset = defaultWidth - layoutWidth;
        float heightOffset = defaultHeight - layoutHeight;
        if (widthOffset <= 0 && heightOffset <= 0) return;

        if ((gravity & Gravity.left) != 0) {
            widthOffset = 0;
            if ((gravity & Gravity.center) != 0) {
                contentRect.offset(0, heightOffset / 2f);
                return;
            }
        }
        if ((gravity & Gravity.top) != 0) {
            heightOffset = 0;
        }
        boolean isCenterXSet = false;
        boolean isCenterYSet = false;
        if ((gravity & Gravity.center) != 0) {
            if (widthOffset > 0) {
                isCenterXSet = true;
                contentRect.offset(widthOffset / 2f, 0);
            }
            if (heightOffset > 0) {
                isCenterYSet = true;
                contentRect.offset(0, heightOffset / 2f);
            }
        }
        if ((gravity & Gravity.right) != 0) {
            float xo = isCenterXSet ? widthOffset / 2f : widthOffset;
            contentRect.offset(xo, 0);
        }
        if ((gravity & Gravity.bottom) != 0) {
            float yo = isCenterYSet ? heightOffset / 2f : heightOffset;
            contentRect.offset(0, yo);
        }
    }

    private void calculateBadgeBounds() {
        if (!badgeEnable) return;
        Paint.FontMetrics metrics = badgeTextPaint.getFontMetrics();
        final float textHeight = metrics.descent - metrics.ascent;
        final float textWidth = TextUtils.isEmpty(badgeText) ? 0 : badgeTextPaint.measureText(badgeText);
        float badgeWidth = Math.max(badgeMinWidth, textWidth) + badgePadding * 2f;
        float badgeHeight = Math.max(badgeMinHeight, textHeight) + badgePadding * 2f;
        float left = 0, top = 0;
        float widthOffset = layoutWidth;
        float heightOffset = layoutHeight;
        if (widthOffset <= 0 && heightOffset <= 0) return;
        try {
            if ((badgeGravity & Gravity.left) != 0) {
                widthOffset = 0;
                if ((badgeGravity & Gravity.center) != 0) {
                    top += heightOffset / 2f - badgeHeight / 2f;
                    return;
                }
            }
            if ((badgeGravity & Gravity.top) != 0) {
                heightOffset = 0;
                if ((badgeGravity & Gravity.center) != 0) {
                    left += widthOffset / 2f - badgeWidth / 2f;
                    return;
                }
            }
            boolean isCenterXSet = false;
            boolean isCenterYSet = false;
            if ((badgeGravity & Gravity.center) != 0) {
                isCenterXSet = true;
                isCenterYSet = true;
                left += Math.max(0, widthOffset / 2f - badgeWidth / 2f);
                top += Math.max(0, heightOffset / 2f - badgeHeight / 2f);
            }
            if ((badgeGravity & Gravity.right) != 0) {
                float xo = isCenterXSet ? widthOffset / 2f - badgeWidth / 2f : widthOffset - badgeWidth;
                left += xo;
            }
            if ((badgeGravity & Gravity.bottom) != 0) {
                float yo = isCenterYSet ? heightOffset / 2f - badgeHeight / 2f : heightOffset - badgeHeight;
                top += yo;
            }
        } finally {
            final boolean isAlignBottom = badgeEnable && (badgeGravity & Gravity.bottom) != 0;

            int offsetY = (int) ((badgeEnable && isAlignBottom) ? (contentRect.top) : 0);

            left += badgeMarginStart - badgeMarginEnd;
            top += badgeMarginTop - badgeMarginBottom;

            int l = (int) (left + 0.5f + contentRect.left);
            int r = (int) (left + badgeWidth + 0.5f + contentRect.left);
            int t = (int) (top + 0.5f + offsetY);
            int b = (int) (top + badgeHeight + 0.5f + offsetY);
            badgeRect = new Rect(l, t, r, b);
            float textX = badgeRect.centerX();
            Paint.FontMetrics m = badgeTextPaint.getFontMetrics();
            float textY = badgeRect.centerY() - (m.bottom - m.top) / 2f - m.top;
            badgeTextStart = new PointF(textX, textY);
        }
    }

    private PointF measureTextSize(String s) {
        if (TextUtils.isEmpty(s)) {
            return new PointF(0f, 0f);
        } else {
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float sth = metrics.descent - metrics.ascent;
            float textLen = textPaint.measureText(s);
            float textWidth;
            int lines;
            if ((maxLength <= 0 && maxTextLength <= 0) || (maxTextLength > 0 && maxLength <= 0 && s.length() <= maxTextLength) || (maxTextLength <= 0 && textLen <= maxLength)) {
                float ty = sth / 2f;
                TextInfo ti = new TextInfo(s, ty, textLen, textPaint);
                drawTextInfoList.add(ti);
                return new PointF(textLen, sth);
            } else if (maxLength > 0 && maxTextLength > 0) {
                throw new IllegalArgumentException("unsupported to set both of 'maxLength' and 'maxTextLength' , Because the priority cannot be determined in different environments.");
            } else {
                int countOfLines;
                if (maxTextLength > 0) {
                    textWidth = textPaint.measureText(s.substring(0, maxTextLength));
                    lines = (int) (Math.ceil(s.length() * 1.0f / maxTextLength));
                    countOfLines = maxTextLength;
                } else {
                    textWidth = maxLength;
                    lines = (int) (Math.ceil(textLen / maxLength));
                    countOfLines = textPaint.breakText(s, false, maxLength, null);
                }
                boolean isOverLine = lines > maxLines;
                lines = Math.min(lines, maxLines);
                int lc = lines * countOfLines - ((isOverLine) ? ellipse.length() : 0);
                String breakText = (lc >= s.length()) ? s : s.substring(0, lc) + ellipse;
                float textHeight = sth;
                for (int i = 0; i < lines; i++) {
                    if (TextUtils.isEmpty(breakText)) break;
                    assert breakText != null;
                    String singleLineText = (countOfLines >= breakText.length()) ? breakText : breakText.substring(0, countOfLines);
                    textHeight = sth * i + textLineSpacing * defaultTextSpacing * Math.max(0, i - 1) + sth / 2f;
                    TextInfo ti = new TextInfo(singleLineText, textHeight, textWidth, textPaint);
                    textWidth = Math.max(textWidth, ti.textWidth);
                    drawTextInfoList.add(ti);
                    breakText = (countOfLines >= breakText.length()) ? null : breakText.substring(countOfLines);
                }
                return new PointF(textWidth, textHeight + sth / 2f);
            }
        }
    }

    private float curAnimFraction;

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        calculationAll();
        drawBackground(canvas);
        drawText(canvas);
        drawDrawable(canvas);
        drawBadge(canvas);
        canvas.restore();
    }

    private void drawText(Canvas canvas) {
        if (drawTextInfoList == null || drawTextInfoList.isEmpty()) {
            return;
        }
        int start = textColor;
        int end = textColorSelect;
        int evaTextColor = (int) evaluator.evaluate(curAnimFraction, textColor, textColorSelect);
        textPaint.setColor(evaTextColor);
        for (TextInfo info : drawTextInfoList) {
            if (TextUtils.isEmpty(info.text)) continue;
            canvas.drawText(info.text, info.textX + textStart.x + contentRect.left, textStart.y + contentRect.top + info.textY, textPaint);
        }
    }

    private void drawDrawable(Canvas canvas) {
        drawableRect.offset((int) (contentRect.left + 0.5f), (int) (contentRect.top + 0.5f));
        drawDrawables(canvas, selectedDrawable, replaceDrawable, drawableRect, false);
    }

    private void drawBadge(Canvas canvas) {
        if (!badgeEnable || badgeRect == null || TextUtils.isEmpty(badgeText)) return;
        drawDrawables(canvas, badgeBackgroundSelected, badgeBackground, badgeRect, true);
        int evaTextColor = (int) evaluator.evaluate(curAnimFraction, badgeTextColor, badgeTextColorSelected);
        badgeTextPaint.setColor(evaTextColor);
        canvas.drawText(badgeText, badgeTextStart.x, badgeTextStart.y, badgeTextPaint);
    }

    private void drawBackground(Canvas canvas) {
        Rect r = new Rect();
        contentRect.roundOut(r);
        final boolean isAlignRight = badgeEnable && (badgeGravity & Gravity.right) != 0;
        if (badgeEnable && !isAlignRight && badgeMarginStart < 0) r.left += (int) (Math.abs(badgeMarginStart + 0.5f));
        drawDrawables(canvas, backgroundDrawableSelected, backgroundDrawable, r, false);
    }

    private void drawDrawables(Canvas canvas, Drawable select, Drawable replace, Rect rect, boolean drawAlways) {
        if (select == null || replace == null) {
            if (select != null) {
                select.setBounds(rect);
                select.setAlpha(255);
                if (drawAlways || isSelected) select.draw(canvas);
            }
            if (replace != null) {
                replace.setAlpha(255);
                replace.setBounds(rect);
                if (drawAlways || !isSelected) replace.draw(canvas);
            }
            return;
        }
        replace.setBounds(rect);
        select.setBounds(rect);
        int curAlpha = (int) (curAnimFraction * 255f + 0.5f);
        replace.setAlpha(255 - curAlpha);
        select.setAlpha(curAlpha);
        if (curAlpha > 0) {
            select.draw(canvas);
        }
        if (curAlpha < 255) {
            replace.draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        calculationAll();
        float w, h;
        int widthSpec = MeasureSpec.makeMeasureSpec(0, widthMeasureSpec);
        int heightSpec = MeasureSpec.makeMeasureSpec(0, heightMeasureSpec);
        if (widthSpec == MeasureSpec.EXACTLY) {
            w = getMeasuredWidth();
        } else {
            w = Math.max(layoutWidth, defaultWidth);
        }
        if (heightSpec == MeasureSpec.EXACTLY) {
            h = getMeasuredHeight();
        } else {
            h = Math.max(layoutHeight, defaultHeight);
        }
        if (w > 0 && h > 0) setMeasuredDimension((int) w, (int) h);
        if (w != defaultWidth || h != defaultHeight) {
            defaultWidth = w;
            defaultHeight = h;
            refreshAndValidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (badgeClickListener == null && drawableClickListener == null) return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchDownPoint = new PointF(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                if (onTouchDownPoint == null) return super.onTouchEvent(event);
                if (Math.abs(event.getX() - onTouchDownPoint.x) <= 30 && Math.abs(event.getY() - onTouchDownPoint.y) <= 30) {
                    if (badgeClickListener != null && badgeRect != null && badgeRect.contains((int) onTouchDownPoint.x, (int) onTouchDownPoint.y)) {
                        badgeClickListener.onClick(this);
                        return true;
                    }
                    if (drawableClickListener != null && drawableRect.contains((int) onTouchDownPoint.x, (int) onTouchDownPoint.y)) {
                        drawableClickListener.onClick(this);
                        return true;
                    }
                }
                performClick();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - onTouchDownPoint.x) > 30 || Math.abs(event.getY() - onTouchDownPoint.y) > 30) {
                    ViewParent vp = getParent();
                    if (vp != null) vp.requestDisallowInterceptTouchEvent(false);
                    return false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                onTouchDownPoint = null;
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private interface OnAnimListener {
        void onAnimFraction(float fraction);
    }

    public float getContentWidth() {
        if (contentRect == null) return 0;
        return contentRect.width();
    }

    public float getContentHeight() {
        if (contentRect == null) return 0;
        return contentRect.height();
    }

    public float getBadgeWidth() {
        if (badgeRect == null) return 0;
        return badgeRect.width();
    }

    public float getBadgeHeight() {
        if (badgeRect == null) return 0;
        return badgeRect.height();
    }

    public float getDrawableWidth() {
        return drawableWidth;
    }

    public float getDrawableHeight() {
        return drawableHeight;
    }


    public Drawable getReplaceDrawable() {
        return replaceDrawable;
    }


    public Drawable getSelectedDrawable() {
        return selectedDrawable;
    }


    public Drawable getBadgeBackground() {
        return badgeBackground;
    }


    public Drawable getBadgeBackgroundSelected() {
        return badgeBackgroundSelected;
    }


    public Drawable getBackgroundDrawable() {
        return backgroundDrawable;
    }


    public Drawable getBackgroundDrawableSelected() {
        return backgroundDrawableSelected;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getGravity() {
        return gravity;
    }

    public int getBadgeGravity() {
        return badgeGravity;
    }


    public String getText() {
        return text;
    }


    public String getTextSelected() {
        return textSelected;
    }


    public String getBadgeText() {
        return badgeText;
    }

    public float getTextSize() {
        return textSize;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getTextColorSelect() {
        return textColorSelect;
    }

    public float getMinWidth() {
        return minWidth;
    }

    public float getMinHeight() {
        return minHeight;
    }

    public float getLayoutWidth() {
        return layoutWidth;
    }

    public float getLayoutHeight() {
        return layoutHeight;
    }

    public float getBadgeMinWidth() {
        return badgeMinWidth;
    }

    public float getBadgeMinHeight() {
        return badgeMinHeight;
    }

    public boolean isBadgeEnable() {
        return badgeEnable;
    }

    public int getBadgeTextColor() {
        return badgeTextColor;
    }

    public int getBadgeTextColorSelected() {
        return badgeTextColorSelected;
    }

    public float getBadgeTextSize() {
        return badgeTextSize;
    }

    public float getBadgePadding() {
        return badgePadding;
    }

    public float getBadgeMarginStart() {
        return badgeMarginStart;
    }

    public float getBadgeMarginEnd() {
        return badgeMarginEnd;
    }

    public float getBadgeMarginTop() {
        return badgeMarginTop;
    }

    public float getBadgeMarginBottom() {
        return badgeMarginBottom;
    }

    public Paint getTextPaint() {
        return textPaint;
    }

    public Paint getBadgeTextPaint() {
        return badgeTextPaint;
    }

    public void setMinWidth(float minWidth) {
        this.minWidth = minWidth;
        refreshAndValidate();
    }

    public void setMinHeight(float minHeight) {
        this.minHeight = minHeight;
        refreshAndValidate();
    }

    //Check that is sure you`re set the attrs property [badgeEnable = true] ,else it`ll never working;
    public void setBadgeText(String s) {
        if (!badgeEnable) throw new IllegalStateException("please check the attrs property [badgeEnable = true]");
        badgeText = TextUtils.isEmpty(s) ? "" : s;
        refreshAndValidate();
    }

    public void clearBadgeText() {
        badgeText = "";
        refreshAndValidate();
    }

    public void setText(String s) {
        this.text = TextUtils.isEmpty(s) ? "" : s;
        refreshAndValidate();
    }

    public void setTextColor(int color) {
        this.textColor = color;
        refreshAndValidate();
    }

    public void setDrawableBackground(Drawable drawable) {
        this.backgroundDrawable = drawable;
        refreshAndValidate();
    }

    public void setSelectedText(String s) {
        this.textSelected = TextUtils.isEmpty(s) ? "" : s;
        refreshAndValidate();
    }

    public void setOrientation(@Orientation int orientation) {
        this.orientation = orientation;
        refreshAndValidate();
    }

    public void setDrawableWidth(float drawableWidth) {
        this.drawableWidth = drawableWidth;
        refreshAndValidate();
    }

    public void setDrawableHeight(float drawableHeight) {
        this.drawableHeight = drawableHeight;
        refreshAndValidate();
    }

    public void setReplaceDrawable(Drawable replaceDrawable) {
        this.replaceDrawable = replaceDrawable;
        refreshAndValidate();
    }

    public void setSelectedDrawable(Drawable selectedDrawable) {
        this.selectedDrawable = selectedDrawable;
        refreshAndValidate();
    }

    public void setBadgeBackground(Drawable badgeBackground) {
        this.badgeBackground = badgeBackground;
        refreshAndValidate();
    }

    public void setBadgeBackgroundSelected(Drawable badgeBackgroundSelected) {
        this.badgeBackgroundSelected = badgeBackgroundSelected;
        refreshAndValidate();
    }

    public void setBackgroundDrawableSelected(Drawable backgroundDrawableSelected) {
        this.backgroundDrawableSelected = backgroundDrawableSelected;
        refreshAndValidate();
    }

    public void setGravity(@Gravity int gravity) {
        this.gravity = gravity;
        refreshAndValidate();
    }

    public void setBadgeGravity(int badgeGravity) {
        this.badgeGravity = badgeGravity;
        refreshAndValidate();
    }

    public void setPaddingLeft(float paddingLeft) {
        this.paddingLeft = paddingLeft;
        refreshAndValidate();
    }

    public void setPaddingTop(float paddingTop) {
        this.paddingTop = paddingTop;
        refreshAndValidate();
    }

    public void setPaddingRight(float paddingRight) {
        this.paddingRight = paddingRight;
        refreshAndValidate();
    }

    public void setPaddingBottom(float paddingBottom) {
        this.paddingBottom = paddingBottom;
        refreshAndValidate();
    }

    public void setDrawablePadding(float drawablePadding) {
        this.drawablePadding = drawablePadding;
        refreshAndValidate();
    }

    public void setTextSelected(String textSelected) {
        this.textSelected = textSelected;
        refreshAndValidate();
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
        refreshAndValidate();
    }

    public void setTextColorSelect(int textColorSelect) {
        this.textColorSelect = textColorSelect;
        refreshAndValidate();
    }

    public void setBadgeMinWidth(float badgeMinWidth) {
        this.badgeMinWidth = badgeMinWidth;
        refreshAndValidate();
    }

    public void setBadgeMinHeight(float badgeMinHeight) {
        this.badgeMinHeight = badgeMinHeight;
        refreshAndValidate();
    }

    public void setBadgeEnable(boolean badgeEnable) {
        this.badgeEnable = badgeEnable;
        refreshAndValidate();
    }

    public void setBadgeTextColor(int badgeTextColor) {
        this.badgeTextColor = badgeTextColor;
        refreshAndValidate();
    }

    public void setBadgeTextColorSelected(int badgeTextColorSelected) {
        this.badgeTextColorSelected = badgeTextColorSelected;
        refreshAndValidate();
    }

    public void setBadgeTextSize(float badgeTextSize) {
        this.badgeTextSize = badgeTextSize;
        refreshAndValidate();
    }

    public void setBadgePadding(float badgePadding) {
        this.badgePadding = badgePadding;
        refreshAndValidate();
    }

    public void setBadgeMarginStart(float badgeMarginStart) {
        this.badgeMarginStart = badgeMarginStart;
        refreshAndValidate();
    }

    public void setBadgeMarginEnd(float badgeMarginEnd) {
        this.badgeMarginEnd = badgeMarginEnd;
        refreshAndValidate();
    }

    public void setBadgeMarginTop(float badgeMarginTop) {
        this.badgeMarginTop = badgeMarginTop;
        refreshAndValidate();
    }

    public void setBadgeMarginBottom(float badgeMarginBottom) {
        this.badgeMarginBottom = badgeMarginBottom;
        refreshAndValidate();
    }

    public void setAnimDuration(int animDuration) {
        this.animDuration = animDuration;
        refreshAndValidate();
    }

    public void setFontPath(String fontPath) {
        this.fontPath = fontPath;
        refreshAndValidate();
    }

    public void setFontStyle(int fontStyle) {
        this.fontStyle = fontStyle;
        refreshAndValidate();
    }

    public void setTextPaint(Paint textPaint) {
        this.textPaint = textPaint;
        refreshAndValidate();
    }

    public void setBadgeTextPaint(Paint badgeTextPaint) {
        this.badgeTextPaint = badgeTextPaint;
        refreshAndValidate();
    }

    public void setAnimator(DrawableValueAnimator animator) {
        this.animator = animator;
        refreshAndValidate();
    }

    public float getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(float maxLength) {
        this.maxLength = maxLength;
        refreshAndValidate();
    }

    public int getMaxTextLength() {
        return maxTextLength;
    }

    public void setMaxTextLength(int maxTextLength) {
        this.maxTextLength = maxTextLength;
        refreshAndValidate();
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
        refreshAndValidate();
    }

    public void setOnBadgeClickListener(BadgeClickListener badgeClickListener) {
        this.badgeClickListener = badgeClickListener;
    }

    public void setOnDrawableClickListener(DrawableClickListener drawableClickListener) {
        this.drawableClickListener = drawableClickListener;
    }

    private void refreshAndValidate() {
        requestLayout();
    }

    public interface BadgeClickListener {

        void onClick(DrawableTextView v);
    }

    public interface DrawableClickListener {

        void onClick(DrawableTextView v);
    }

    private static class DrawableValueAnimator {

        private OnAnimListener onAnimListener;
        private ValueAnimator valueAnimator;

        private long curDuration;
        private float curFraction;

        private long maxDuration;
        private float maxFraction;
        private long animDuration;
        private boolean isSelected;

        void setDuration(long duration) {
            this.animDuration = duration;
        }

        void setOnAnimListener(OnAnimListener listener) {
            this.onAnimListener = listener;
        }

        void start(boolean isSelected) {
            this.isSelected = isSelected;
            boolean isRunning = valueAnimator != null && valueAnimator.isRunning();
            if (valueAnimator != null) valueAnimator.cancel();
            if (valueAnimator == null || !valueAnimator.isRunning()) {
                valueAnimator = getAnim(isRunning);
                valueAnimator.start();
                return;
            }

            valueAnimator = getAnim(true);
            valueAnimator.start();
        }

        private ValueAnimator getAnim(boolean isRunning) {
            maxFraction = isRunning ? curFraction : 1.0f;
            maxDuration = isRunning ? curDuration : animDuration;
            ValueAnimator animator = new ValueAnimator();
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            setValues(animator);
            setListener(animator);
            return animator;
        }

        private void setValues(ValueAnimator animator) {
            curFraction = 0;
            curDuration = 0;
            animator.setDuration(maxDuration);
            animator.setFloatValues(0.0f, maxFraction);
        }

        private void setListener(ValueAnimator animator) {
            animator.addUpdateListener(animation -> {
                curDuration = Math.min(animDuration, animation.getCurrentPlayTime());
                curFraction = isSelected ? animation.getAnimatedFraction() : Math.max(0, maxFraction - animation.getAnimatedFraction());
                if (onAnimListener != null) onAnimListener.onAnimFraction(curFraction);
            });
        }
    }

    private static class TextInfo {
        private float textX;
        private float textY;
        private final String text;
        private final float textWidth;

        TextInfo(String text, float textY, float maxWidth, Paint paint) {
            this.text = text;
            this.textY = textY + paint.getFontMetrics().descent / 2f;
            textWidth = TextUtils.isEmpty(text) ? 0 : paint.measureText(text);
        }

        void update(float maxWidth, float maxHeight, float viewWidth, int gravity, float padding, int orientation, Paint paint) {
            if (((gravity & TextGravity.center) != 0 && (gravity & TextGravity.left) != 0) || ((gravity & TextGravity.left) != 0 && (gravity & TextGravity.right) != 0) || ((gravity & TextGravity.center) != 0 && (gravity & TextGravity.right) != 0)) {
                throw new IllegalArgumentException("Only one of the horizontal arrangements of left, right and center is allowed to take effect");
            }
            Paint.FontMetrics metrics = paint.getFontMetrics();
            if (orientation == Orientation.left || orientation == Orientation.right) {
                textY -= (maxHeight / 2f) - metrics.descent;
            } else {
                textY += metrics.descent;
            }
            if ((gravity & TextGravity.center) != 0 || (((gravity & TextGravity.center) == 0 && (gravity & TextGravity.left) == 0 && (gravity & TextGravity.right) == 0))) {
                textX = (orientation == Orientation.left || orientation == Orientation.right) ? maxWidth / 2f : viewWidth / 2f;
            } else if ((gravity & TextGravity.left) != 0) {
                textX = (orientation == Orientation.left || orientation == Orientation.right) ? textWidth / 2f : textWidth / 2f + padding;
            } else if ((gravity & TextGravity.right) != 0) {
                textX = (maxWidth - textWidth) / 2f + maxWidth / 2f + ((orientation == Orientation.left || orientation == Orientation.right) ? 0 : padding);
            }
        }
    }

    private float dp2px(int in) {
        return getContext().getResources().getDisplayMetrics().density * in;
    }
}
