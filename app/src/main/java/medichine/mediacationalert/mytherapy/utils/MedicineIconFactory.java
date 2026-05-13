package medichine.mediacationalert.mytherapy.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import medichine.mediacationalert.mytherapy.R;

public class MedicineIconFactory {
    private static final int ICON_SIZE = 250;

    public interface IconSelectedListener {
        void onIconSelected(String iconType);
    }

    public interface ImageSourceListener {
        void onGallerySelected();

        void onCameraSelected();

        void onUseIconSelected();
    }

    public interface CroppedIconListener {
        void onCropped(String iconUri);

        void onCropFailed();
    }

    private static class IconShape {
        final String key;
        final int labelRes;

        IconShape(String key, int labelRes) {
            this.key = key;
            this.labelRes = labelRes;
        }
    }

    private static class IconColor {
        final String key;
        final int labelRes;
        final int value;

        IconColor(String key, int labelRes, int value) {
            this.key = key;
            this.labelRes = labelRes;
            this.value = value;
        }
    }

    private static final IconShape[] PILL_SHAPES = new IconShape[]{
            new IconShape("circle", R.string.icon_shape_circle),
            new IconShape("oval", R.string.icon_shape_oval),
            new IconShape("rectangle", R.string.icon_shape_rectangle),
            new IconShape("oval_triangle", R.string.icon_shape_oval_triangle),
            new IconShape("triangle", R.string.icon_shape_triangle),
            new IconShape("square", R.string.icon_shape_square),
            new IconShape("hexagon", R.string.icon_shape_hexagon),
            new IconShape("pentagon", R.string.icon_shape_pentagon),
            new IconShape("semicircle", R.string.icon_shape_semicircle)
    };

    private static final IconShape[] CAPSULE_SHAPES = new IconShape[]{
            new IconShape("slim", R.string.icon_shape_capsule_slim),
            new IconShape("thick", R.string.icon_shape_capsule_thick),
            new IconShape("slim_half", R.string.icon_shape_capsule_slim_half),
            new IconShape("thick_half", R.string.icon_shape_capsule_thick_half)
    };

    private static final IconShape[] LIQUID_SHAPES = new IconShape[]{
            new IconShape("injection", R.string.icon_shape_injection),
            new IconShape("oral_bottle", R.string.icon_shape_oral_bottle),
            new IconShape("measuring_cup", R.string.icon_shape_measuring_cup)
    };

    private static final IconColor[] COLORS = new IconColor[]{
            new IconColor("white", R.string.icon_color_white, Color.WHITE),
            new IconColor("yellow", R.string.icon_color_yellow, Color.rgb(250, 206, 82)),
            new IconColor("blue", R.string.icon_color_blue, Color.rgb(79, 142, 255)),
            new IconColor("red", R.string.icon_color_red, Color.rgb(231, 88, 104))
    };

    public static void showPicker(Context context, IconSelectedListener listener) {
        String[] labels = new String[]{
                context.getString(R.string.pill),
                context.getString(R.string.capsule),
                context.getString(R.string.liquid)
        };
        String[] families = new String[]{"pill", "capsule", "liquid"};
        String[] previews = new String[]{
                "pill_circle_white",
                "capsule_slim_white",
                "liquid_oral_bottle_white"
        };
        showPreviewDialog(context, context.getString(R.string.select_icon), labels, previews, true, which ->
                showShapePicker(context, families[which], listener));
    }

    public static void showImageSourcePicker(Context context, String iconType, String iconUri,
                                             ImageSourceListener listener) {
        boolean hasPhoto = iconUri != null && iconUri.length() > 0;
        int size = hasPhoto ? 4 : 3;
        String[] labels = new String[size];
        String[] previews = new String[size];
        String[] previewUris = new String[size];
        int index = 0;
        if (hasPhoto) {
            labels[index] = context.getString(R.string.photo_selected);
            previews[index] = normalize(iconType);
            previewUris[index] = iconUri;
            index++;
        }
        labels[index] = context.getString(R.string.gallery);
        previews[index] = "pill_square_blue";
        previewUris[index] = "";
        index++;
        labels[index] = context.getString(R.string.camera);
        previews[index] = "pill_circle_yellow";
        previewUris[index] = "";
        index++;
        labels[index] = context.getString(R.string.use_icon);
        previews[index] = normalize(iconType);
        previewUris[index] = "";

        showPreviewDialog(context, context.getString(R.string.medicine_photo),
                labels, previews, previewUris, true, which -> {
                    if (hasPhoto && which == 0) {
                        return;
                    }
                    int action = hasPhoto ? which - 1 : which;
                    if (action == 0) {
                        listener.onGallerySelected();
                    } else if (action == 1) {
                        listener.onCameraSelected();
                    } else {
                        listener.onUseIconSelected();
                    }
                });
    }

    public static void apply(ImageView imageView, String iconType, String iconUri) {
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.clearColorFilter();
        if ("lab".equals(iconType)) {
            imageView.setImageResource(R.drawable.ic_lab_24);
            imageView.setColorFilter(imageView.getResources().getColor(R.color.nav_selected));
            return;
        }
        if (iconUri != null && iconUri.length() > 0) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(Uri.parse(iconUri));
            return;
        }
        imageView.setImageDrawable(new MedicineIconDrawable(normalize(iconType)));
    }

    public static String label(Context context, String iconType) {
        String normalized = normalize(iconType);
        String[] parts = normalized.split("_");
        if (parts.length < 3) {
            return context.getString(R.string.pill);
        }
        String family = parts[0];
        String color = parts[parts.length - 1];
        String shape = normalized.substring(family.length() + 1, normalized.length() - color.length() - 1);
        return context.getString(R.string.icon_label_format,
                familyLabel(context, family),
                shapeLabel(context, family, shape));
    }

    public static String saveScaledIcon(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Cannot open image");
        }
        Bitmap source = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        if (source == null) {
            throw new IOException("Cannot decode image");
        }
        return saveScaledIcon(context, source);
    }

    public static void showCropDialog(Context context, Uri uri, CroppedIconListener listener) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                listener.onCropFailed();
                return;
            }
            Bitmap source = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            if (source == null) {
                listener.onCropFailed();
                return;
            }
            showCropDialog(context, source, listener);
        } catch (IOException e) {
            listener.onCropFailed();
        }
    }

    public static void showCropDialog(Context context, Bitmap source, CroppedIconListener listener) {
        if (source == null) {
            listener.onCropFailed();
            return;
        }
        CropView cropView = new CropView(context, source);
        cropView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 320)));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.medicine_photo)
                .setView(cropView)
                .setPositiveButton(R.string.saved, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Bitmap cropped = cropView.createCroppedBitmap(ICON_SIZE);
            try {
                String savedUri = saveScaledIcon(context, cropped);
                cropped.recycle();
                listener.onCropped(savedUri);
                dialog.dismiss();
            } catch (IOException e) {
                cropped.recycle();
                listener.onCropFailed();
            }
        }));
        dialog.setOnDismissListener(d -> source.recycle());
        dialog.show();
    }

    public static String saveScaledIcon(Context context, Bitmap source) throws IOException {
        Bitmap scaled = scaleCenterCrop(source, ICON_SIZE, ICON_SIZE);
        File file = new File(context.getFilesDir(), "medicine_icon_" + System.currentTimeMillis() + ".png");
        FileOutputStream out = new FileOutputStream(file);
        scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
        if (scaled != source) {
            scaled.recycle();
        }
        return Uri.fromFile(file).toString();
    }

    private static void showShapePicker(Context context, String family, IconSelectedListener listener) {
        IconShape[] shapes = shapesForFamily(family);
        String[] labels = new String[shapes.length];
        String[] previews = new String[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            labels[i] = context.getString(shapes[i].labelRes);
            previews[i] = family + "_" + shapes[i].key + "_white";
        }
        showPreviewDialog(context, familyLabel(context, family), labels, previews, true, which ->
                showColorPicker(context, family, shapes[which].key, listener));
    }

    private static void showColorPicker(Context context, String family, String shape, IconSelectedListener listener) {
        String[] labels = new String[COLORS.length];
        String[] previews = new String[COLORS.length];
        for (int i = 0; i < COLORS.length; i++) {
            labels[i] = context.getString(COLORS[i].labelRes);
            previews[i] = family + "_" + shape + "_" + COLORS[i].key;
        }
        showPreviewDialog(context, shapeLabel(context, family, shape), labels, previews, false, which ->
                listener.onIconSelected(family + "_" + shape + "_" + COLORS[which].key));
    }

    private interface PreviewSelectedListener {
        void onSelected(int index);
    }

    private static void showPreviewDialog(Context context, String title, String[] labels, String[] iconTypes,
                                          boolean showLabels, PreviewSelectedListener listener) {
        String[] iconUris = new String[iconTypes.length];
        for (int i = 0; i < iconUris.length; i++) {
            iconUris[i] = "";
        }
        showPreviewDialog(context, title, labels, iconTypes, iconUris, showLabels, listener);
    }

    private static void showPreviewDialog(Context context, String title, String[] labels, String[] iconTypes,
                                          String[] iconUris, boolean showLabels,
                                          PreviewSelectedListener listener) {
        int padding = dp(context, 14);
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(showLabels ? Math.min(iconTypes.length, 3) : Math.min(iconTypes.length, 4));
        grid.setPadding(padding, padding, padding, padding);
        grid.setUseDefaultMargins(false);

        AlertDialog[] dialogHolder = new AlertDialog[1];
        for (int i = 0; i < iconTypes.length; i++) {
            View tile = previewTile(context, labels[i], iconTypes[i], iconUris[i], showLabels);
            final int index = i;
            tile.setOnClickListener(v -> {
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
                listener.onSelected(index);
            });
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(context, showLabels ? 86 : 60);
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
            grid.addView(tile, params);
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(grid);
        dialogHolder[0] = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(scrollView)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialogHolder[0].show();
    }

    private static View previewTile(Context context, String label, String iconType, boolean showLabel) {
        return previewTile(context, label, iconType, "", showLabel);
    }

    private static View previewTile(Context context, String label, String iconType, String iconUri,
                                    boolean showLabel) {
        LinearLayout tile = new LinearLayout(context);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription(label);
        tile.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.TRANSPARENT);
        background.setCornerRadius(dp(context, 8));
        background.setStroke(dp(context, 1), Color.argb(80, 130, 138, 146));
        tile.setBackground(background);

        ImageView preview = new ImageView(context);
        apply(preview, iconType, iconUri);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(context, 48), dp(context, 48));
        tile.addView(preview, imageParams);

        if (showLabel) {
            TextView text = new TextView(context);
            text.setText(label);
            text.setGravity(Gravity.CENTER);
            text.setSingleLine(false);
            text.setTextSize(12);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            textParams.topMargin = dp(context, 6);
            tile.addView(text, textParams);
        }
        return tile;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String normalize(String iconType) {
        if (iconType == null || iconType.length() == 0 || "pill".equals(iconType)) {
            return "pill_circle_white";
        }
        if ("capsule".equals(iconType)) {
            return "capsule_slim_white";
        }
        if ("liquid".equals(iconType)) {
            return "liquid_oral_bottle_white";
        }
        return iconType;
    }

    private static IconShape[] shapesForFamily(String family) {
        if ("capsule".equals(family)) {
            return CAPSULE_SHAPES;
        }
        if ("liquid".equals(family)) {
            return LIQUID_SHAPES;
        }
        return PILL_SHAPES;
    }

    private static String familyLabel(Context context, String family) {
        if ("capsule".equals(family)) {
            return context.getString(R.string.capsule);
        }
        if ("liquid".equals(family)) {
            return context.getString(R.string.liquid);
        }
        return context.getString(R.string.pill);
    }

    private static String shapeLabel(Context context, String family, String shape) {
        for (IconShape iconShape : shapesForFamily(family)) {
            if (iconShape.key.equals(shape)) {
                return context.getString(iconShape.labelRes);
            }
        }
        return context.getString(R.string.icon_shape_circle);
    }

    private static String colorLabel(Context context, String color) {
        for (IconColor iconColor : COLORS) {
            if (iconColor.key.equals(color)) {
                return context.getString(iconColor.labelRes);
            }
        }
        return context.getString(R.string.icon_color_white);
    }

    private static int colorValue(String color) {
        for (IconColor iconColor : COLORS) {
            if (iconColor.key.equals(color)) {
                return iconColor.value;
            }
        }
        return Color.WHITE;
    }

    private static Bitmap scaleCenterCrop(Bitmap source, int width, int height) {
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        float scale = Math.max((float) width / source.getWidth(), (float) height / source.getHeight());
        float scaledWidth = source.getWidth() * scale;
        float scaledHeight = source.getHeight() * scale;
        RectF target = new RectF(
                (width - scaledWidth) / 2f,
                (height - scaledHeight) / 2f,
                (width + scaledWidth) / 2f,
                (height + scaledHeight) / 2f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, null, target, paint);
        return output;
    }

    private static class MedicineIconDrawable extends Drawable {
        private final String family;
        private final String shape;
        private final String colorKey;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

        MedicineIconDrawable(String iconType) {
            String[] parts = iconType.split("_");
            family = parts.length > 0 ? parts[0] : "pill";
            colorKey = parts.length > 1 ? parts[parts.length - 1] : "white";
            shape = iconType.substring(family.length() + 1, iconType.length() - colorKey.length() - 1);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(colorValue(colorKey));
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(9f);
            stroke.setColor("white".equals(colorKey) ? Color.rgb(130, 138, 146) : darker(fill.getColor()));
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(8f);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setColor(Color.argb(150, 255, 255, 255));
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.save();
            RectF bounds = new RectF(getBounds());
            float scale = Math.min(bounds.width(), bounds.height()) / 250f;
            canvas.translate(bounds.left + (bounds.width() - 250f * scale) / 2f,
                    bounds.top + (bounds.height() - 250f * scale) / 2f);
            canvas.scale(scale, scale);
            if ("capsule".equals(family)) {
                drawCapsule(canvas);
            } else if ("liquid".equals(family)) {
                drawLiquid(canvas);
            } else {
                drawPill(canvas);
            }
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            fill.setAlpha(alpha);
            stroke.setAlpha(alpha);
            line.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            fill.setColorFilter(colorFilter);
            stroke.setColorFilter(colorFilter);
            line.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        private void drawPill(Canvas canvas) {
            if ("circle".equals(shape)) {
                canvas.drawCircle(125, 125, 76, fill);
                canvas.drawCircle(125, 125, 76, stroke);
                canvas.drawLine(88, 88, 162, 162, line);
            } else if ("oval".equals(shape)) {
                drawRound(canvas, new RectF(28, 76, 222, 174), 50);
            } else if ("rectangle".equals(shape)) {
                drawRound(canvas, new RectF(48, 62, 202, 188), 16);
            } else if ("oval_triangle".equals(shape)) {
                drawPathWithHighlight(canvas, ovalTrianglePath());
            } else if ("triangle".equals(shape)) {
                Path path = regularPolygon(3, 125, 130, 84, -90);
                drawPathWithHighlight(canvas, path);
            } else if ("square".equals(shape)) {
                drawRound(canvas, new RectF(55, 55, 195, 195), 14);
            } else if ("hexagon".equals(shape)) {
                drawPathWithHighlight(canvas, regularPolygon(6, 125, 125, 82, 30));
            } else if ("pentagon".equals(shape)) {
                drawPathWithHighlight(canvas, regularPolygon(5, 125, 128, 84, -90));
            } else {
                Path path = new Path();
                RectF oval = new RectF(45, 45, 205, 205);
                path.addArc(oval, 180, 180);
                path.lineTo(205, 125);
                path.lineTo(45, 125);
                path.close();
                drawPathWithHighlight(canvas, path);
            }
        }

        private void drawCapsule(Canvas canvas) {
            canvas.save();
            canvas.rotate(-18, 125, 125);
            boolean thick = shape.startsWith("thick");
            boolean halfWhite = shape.endsWith("_half");
            float capsuleScale = thick ? 1.2f : 0.85f;
            canvas.scale(capsuleScale, capsuleScale, 125, 125);
            RectF rect = thick
                    ? new RectF(32, 82.25f, 218, 167.75f)
                    : new RectF(18, 88, 232, 162);
            if (halfWhite) {
                float split = rect.centerX();
                Paint whiteFill = new Paint(fill);
                whiteFill.setColor(Color.WHITE);
                canvas.save();
                canvas.clipRect(rect.left, rect.top, split, rect.bottom);
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, fill);
                canvas.restore();
                canvas.save();
                canvas.clipRect(split, rect.top, rect.right, rect.bottom);
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, whiteFill);
                canvas.restore();
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, stroke);
                canvas.drawLine(split, rect.top + 6, split, rect.bottom - 6, stroke);
                canvas.restore();
                return;
            }
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, fill);
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, stroke);
            canvas.drawLine(125, rect.top + 12, 125, rect.bottom - 12, line);
            canvas.restore();
        }

        private void drawLiquid(Canvas canvas) {
            if ("injection".equals(shape)) {
                canvas.save();
                canvas.rotate(-35, 125, 125);
                drawRound(canvas, new RectF(66, 94, 172, 142), 14);
                canvas.drawLine(172, 118, 226, 118, stroke);
                canvas.drawLine(38, 118, 66, 118, stroke);
                canvas.drawLine(42, 94, 42, 142, stroke);
                canvas.drawLine(86, 98, 86, 138, line);
                canvas.drawLine(112, 98, 112, 138, line);
                canvas.restore();
            } else if ("measuring_cup".equals(shape)) {
                Path cup = new Path();
                cup.moveTo(70, 66);
                cup.lineTo(180, 66);
                cup.lineTo(160, 198);
                cup.lineTo(90, 198);
                cup.close();
                drawPathWithHighlight(canvas, cup);
                canvas.drawLine(92, 104, 124, 104, line);
                canvas.drawLine(88, 142, 116, 142, line);
            } else {
                drawRound(canvas, new RectF(82, 72, 168, 198), 18);
                drawRound(canvas, new RectF(96, 42, 154, 78), 10);
                canvas.drawLine(96, 112, 154, 112, line);
            }
        }

        private void drawRound(Canvas canvas, RectF rect, float radius) {
            canvas.drawRoundRect(rect, radius, radius, fill);
            canvas.drawRoundRect(rect, radius, radius, stroke);
            canvas.drawLine(rect.left + rect.width() * 0.25f, rect.top + rect.height() * 0.25f,
                    rect.right - rect.width() * 0.25f, rect.bottom - rect.height() * 0.25f, line);
        }

        private void drawPathWithHighlight(Canvas canvas, Path path) {
            canvas.drawPath(path, fill);
            canvas.drawPath(path, stroke);
            canvas.drawLine(88, 88, 162, 162, line);
        }

        private Path regularPolygon(int sides, float cx, float cy, float radius, float startDegrees) {
            Path path = new Path();
            for (int i = 0; i < sides; i++) {
                double angle = Math.toRadians(startDegrees + i * 360.0 / sides);
                float x = cx + (float) Math.cos(angle) * radius;
                float y = cy + (float) Math.sin(angle) * radius;
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.close();
            return path;
        }

        private Path ovalTrianglePath() {
            Path path = new Path();
            path.moveTo(125, 42);
            path.cubicTo(180, 58, 218, 122, 215, 188);
            path.cubicTo(174, 218, 76, 218, 35, 188);
            path.cubicTo(32, 122, 70, 58, 125, 42);
            path.close();
            return path;
        }

        private int darker(int color) {
            return Color.rgb(
                    Math.max(0, (int) (Color.red(color) * 0.72f)),
                    Math.max(0, (int) (Color.green(color) * 0.72f)),
                    Math.max(0, (int) (Color.blue(color) * 0.72f)));
        }
    }

    private static class CropView extends View {
        private final Bitmap bitmap;
        private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF cropRect = new RectF();
        private float scale;
        private float minScale;
        private float offsetX;
        private float offsetY;
        private float lastX;
        private float lastY;
        private float lastDistance;
        private boolean initialized;

        CropView(Context context, Bitmap bitmap) {
            super(context);
            this.bitmap = bitmap;
            overlayPaint.setColor(Color.argb(150, 0, 0, 0));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dp(context, 2));
            borderPaint.setColor(Color.WHITE);
            setBackgroundColor(Color.BLACK);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            int padding = dp(getContext(), 24);
            float size = Math.max(1, Math.min(w, h) - padding * 2f);
            cropRect.set((w - size) / 2f, (h - size) / 2f, (w + size) / 2f, (h + size) / 2f);
            if (!initialized) {
                minScale = Math.max(cropRect.width() / bitmap.getWidth(),
                        cropRect.height() / bitmap.getHeight());
                scale = minScale;
                offsetX = cropRect.centerX() - bitmap.getWidth() * scale / 2f;
                offsetY = cropRect.centerY() - bitmap.getHeight() * scale / 2f;
                initialized = true;
            } else {
                minScale = Math.max(cropRect.width() / bitmap.getWidth(),
                        cropRect.height() / bitmap.getHeight());
                scale = Math.max(scale, minScale);
            }
            clampImage();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawBitmap(bitmap, null, imageRect(), bitmapPaint);
            canvas.drawRect(0, 0, getWidth(), cropRect.top, overlayPaint);
            canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), overlayPaint);
            canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint);
            canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, overlayPaint);
            canvas.drawRect(cropRect, borderPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getPointerCount() >= 2) {
                handlePinch(event);
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    offsetX += event.getX() - lastX;
                    offsetY += event.getY() - lastY;
                    lastX = event.getX();
                    lastY = event.getY();
                    clampImage();
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    lastDistance = 0;
                    return true;
                default:
                    return true;
            }
        }

        Bitmap createCroppedBitmap(int size) {
            if (scale <= 0 || cropRect.width() <= 0 || cropRect.height() <= 0) {
                return scaleCenterCrop(bitmap, size, size);
            }
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            RectF source = new RectF(
                    (cropRect.left - offsetX) / scale,
                    (cropRect.top - offsetY) / scale,
                    (cropRect.right - offsetX) / scale,
                    (cropRect.bottom - offsetY) / scale);
            if (!source.intersect(0, 0, bitmap.getWidth(), bitmap.getHeight())) {
                output.recycle();
                return scaleCenterCrop(bitmap, size, size);
            }
            android.graphics.Rect sourceRect = new android.graphics.Rect(
                    Math.max(0, (int) Math.floor(source.left)),
                    Math.max(0, (int) Math.floor(source.top)),
                    Math.min(bitmap.getWidth(), (int) Math.ceil(source.right)),
                    Math.min(bitmap.getHeight(), (int) Math.ceil(source.bottom)));
            if (sourceRect.width() <= 0 || sourceRect.height() <= 0) {
                output.recycle();
                return scaleCenterCrop(bitmap, size, size);
            }
            canvas.drawBitmap(bitmap, sourceRect, new RectF(0, 0, size, size), bitmapPaint);
            return output;
        }

        private void handlePinch(MotionEvent event) {
            float distance = pointerDistance(event);
            float focusX = (event.getX(0) + event.getX(1)) / 2f;
            float focusY = (event.getY(0) + event.getY(1)) / 2f;
            if (lastDistance > 0 && distance > 0) {
                float oldScale = scale;
                scale = Math.max(minScale, Math.min(scale * distance / lastDistance, minScale * 4f));
                offsetX = focusX - (focusX - offsetX) * scale / oldScale;
                offsetY = focusY - (focusY - offsetY) * scale / oldScale;
                clampImage();
                invalidate();
            }
            lastDistance = distance;
        }

        private float pointerDistance(MotionEvent event) {
            float dx = event.getX(0) - event.getX(1);
            float dy = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private RectF imageRect() {
            return new RectF(offsetX, offsetY,
                    offsetX + bitmap.getWidth() * scale,
                    offsetY + bitmap.getHeight() * scale);
        }

        private void clampImage() {
            float width = bitmap.getWidth() * scale;
            float height = bitmap.getHeight() * scale;
            offsetX = clampOffset(offsetX, width, cropRect.left, cropRect.right);
            offsetY = clampOffset(offsetY, height, cropRect.top, cropRect.bottom);
        }

        private float clampOffset(float offset, float imageSize, float cropStart, float cropEnd) {
            float cropSize = cropEnd - cropStart;
            if (imageSize <= cropSize) {
                return cropStart + (cropSize - imageSize) / 2f;
            }
            float min = cropEnd - imageSize;
            float max = cropStart;
            return Math.max(min, Math.min(offset, max));
        }
    }
}
