package com.example.couple_app.ui.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawingView extends View {

    private Paint paint;
    private final List<float[]> points = new ArrayList<>();
    private DatabaseReference drawRef;
    private boolean enabled = true;
    private boolean listenerAttached = false;

    // để giảm ghi Firebase liên tục
    private final List<Map<String, Object>> buffer = new ArrayList<>();
    private final Handler handler = new Handler();
    private final Runnable flushRunnable = new Runnable() {
        @Override
        public void run() {
            flushBuffer();
            handler.postDelayed(this, 100); // gửi mỗi 100ms
        }
    };

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        handler.postDelayed(flushRunnable, 100);
    }

    /** Gán roomId để đồng bộ dữ liệu vẽ */
    public void setRoomId(String roomId) {
        drawRef = FirebaseDatabase.getInstance()
                .getReference("rooms")
                .child(roomId)
                .child("draws");

        if (!listenerAttached) {
            listenerAttached = true;
            drawRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                    Float x = snapshot.child("x").getValue(Float.class);
                    Float y = snapshot.child("y").getValue(Float.class);
                    if (x != null && y != null && !enabled) { // chỉ người xem mới nhận
                        points.add(new float[]{x, y});
                        invalidate();
                    }
                }

                @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
                @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (float[] p : points) {
            canvas.drawCircle(p[0], p[1], 4, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!enabled) return false;

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();
            points.add(new float[]{x, y});
            invalidate();

            Map<String, Object> point = new HashMap<>();
            point.put("x", x);
            point.put("y", y);
            synchronized (buffer) {
                buffer.add(point);
            }
        }
        return true;
    }

    /** Gửi batch điểm vẽ lên Firebase */
    private void flushBuffer() {
        if (drawRef == null || !enabled) return;
        List<Map<String, Object>> batch;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            batch = new ArrayList<>(buffer);
            buffer.clear();
        }
        for (Map<String, Object> point : batch) {
            drawRef.push().setValue(point);
        }
    }

    /** Xuất ảnh vẽ */
    public Bitmap exportBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    /** Hiển thị ảnh từ người khác */
    public void showImage(Bitmap bitmap) {
        setBackground(new BitmapDrawable(getResources(), bitmap));
        points.clear();
        invalidate();
    }

    /** Xóa toàn bộ canvas (chỉ người vẽ mới dùng) */
    public void clearCanvas() {
        points.clear();
        invalidate();
        if (drawRef != null && enabled) {
            drawRef.removeValue();
        }
    }
}
