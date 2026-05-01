package com.skillconnect;

import android.content.Context;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Notification;

/**
 * WorkManager worker that fires 24 hours after provider marks job as done.
 * If the customer still hasn't paid the remaining 90%, they are sent a
 * local push notification and flagged as overdue in Firestore.
 * If status is still "ready_for_review" after 24hrs, the user can be blacklisted.
 */
public class PaymentDeadlineWorker extends Worker {

    public static final String KEY_BOOKING_ID  = "booking_id";
    public static final String KEY_CUSTOMER_ID = "customer_id";
    public static final String KEY_JOB_TITLE   = "job_title";
    private static final String CHANNEL_ID     = "payment_deadline";

    public PaymentDeadlineWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String bookingId  = getInputData().getString(KEY_BOOKING_ID);
        String customerId = getInputData().getString(KEY_CUSTOMER_ID);
        String jobTitle   = getInputData().getString(KEY_JOB_TITLE);
        if (bookingId == null || customerId == null) return Result.failure();

        FirebaseRepository repo = FirebaseRepository.getInstance();

        // Check if booking is still awaiting payment (still "ready_for_review")
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("bookings").document(bookingId).get()
            .addOnSuccessListener(bookingData -> {
                if (!bookingData.exists()) return;

            String status = (String) bookingData.get("status");
            if (!"ready_for_review".equals(status) && !"accepted".equals(status)) return;

            // Still unpaid after 24 hours — send overdue notification
            String title = "⏰ Payment Overdue!";
            String body  = "You have an outstanding payment for '" + jobTitle + "'. "
                    + "Please complete the payment immediately to avoid account suspension.";

            // Push local notification
            sendLocalNotification(title, body);

            // Create Firestore notification record
            repo.createNotification(new Notification(
                    customerId, "customer", "payment_overdue",
                    title, body, bookingId), null);

            // Mark customer as overdue in Firestore (can be shown as badge in UI)
            repo.blacklistUser(customerId,
                    "Did not complete final payment for booking " + bookingId + " within 24 hours.",
                    done -> {});
        });

        return Result.success();
    }

    private void sendLocalNotification(String title, String body) {
        Context ctx = getApplicationContext();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Payment Reminders", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Notifications for pending payments");
            nm.createNotificationChannel(ch);
        }

        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
                ctx, 0,
                new android.content.Intent(ctx, MainActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
                android.app.PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_skillconnect_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
