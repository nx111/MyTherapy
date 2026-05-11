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
import android.net.Uri;
import android.widget.ImageView;

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
            new IconShape("thick", R.string.icon_shape_capsule_thick)
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
        new AlertDialog.Builder(context)
                .setTitle(R.string.select_icon)
                .setItems(labels, (dialog, which) -> showShapePicker(context, families[which], listener))
                .show();
    }

    public static void apply(ImageView imageView, String iconType, String iconUri) {
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
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
                shapeLabel(context, family, shape),
                colorLabel(context, color));
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
        for (int i = 0; i < shapes.length; i++) {
            labels[i] = context.getString(shapes[i].labelRes);
        }
        new AlertDialog.Builder(context)
                .setTitle(familyLabel(context, family))
                .setItems(labels, (dialog, which) -> showColorPicker(context, family, shapes[which].key, listener))
                .show();
    }

    private static void showColorPicker(Context context, String family, String shape, IconSelectedListener listener) {
        String[] labels = new String[COLORS.length];
        for (int i = 0; i < COLORS.length; i++) {
            labels[i] = context.getString(COLORS[i].labelRes);
        }
        new AlertDialog.Builder(context)
                .setTitle(shapeLabel(context, family, shape))
                .setItems(labels, (dialog, which) ->
                        listener.onIconSelected(family + "_" + shape + "_" + COLORS[which].key))
                .show();
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
                Path path = new Path();
                path.moveTo(72, 55);
                path.lineTo(148, 55);
                path.lineTo(218, 125);
                path.lineTo(148, 195);
                path.lineTo(72, 195);
                path.cubicTo(30, 190, 30, 60, 72, 55);
                path.close();
                drawPathWithHighlight(canvas, path);
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
            RectF rect = "thick".equals(shape)
                    ? new RectF(32, 68, 218, 182)
                    : new RectF(18, 88, 232, 162);
            drawRound(canvas, rect, rect.height() / 2f);
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

        private int darker(int color) {
            return Color.rgb(
                    Math.max(0, (int) (Color.red(color) * 0.72f)),
                    Math.max(0, (int) (Color.green(color) * 0.72f)),
                    Math.max(0, (int) (Color.blue(color) * 0.72f)));
        }
    }
}
