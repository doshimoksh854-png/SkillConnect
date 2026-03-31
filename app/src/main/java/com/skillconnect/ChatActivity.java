package com.skillconnect;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentChange;
import com.skillconnect.adapters.ChatAdapter;
import com.skillconnect.data.FirebaseRepository;
import com.skillconnect.data.SessionManager;
import com.skillconnect.models.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Real-time chat screen between customer and provider.
 * Intent extras required:
 *   "chat_partner_id"   — UID of the other user
 *   "chat_partner_name" — Display name of other user
 */
public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 101;

    private RecyclerView      rvMessages;
    private TextInputEditText etMessage;
    private FloatingActionButton fabSend, fabAttach;
    private ChatAdapter       adapter;
    private List<Message>     messages = new ArrayList<>();

    private FirebaseRepository repo;
    private SessionManager     session;
    private String chatId, currentUserId, currentUserName, partnerName;

    // Image picker — picks images from gallery
    private ActivityResultLauncher<String> imagePickerLauncher;
    // File picker — picks any document (pdf, docx, etc.)
    private ActivityResultLauncher<String[]> filePickerLauncher;
    // Permission request
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        repo    = FirebaseRepository.getInstance();
        session = new SessionManager(this);

        currentUserId   = session.getUserId();
        currentUserName = session.getUserName();

        String partnerId = getIntent().getStringExtra("chat_partner_id");
        partnerName      = getIntent().getStringExtra("chat_partner_name");

        // Chat ID is always the two UIDs sorted alphabetically (deterministic)
        chatId = buildChatId(currentUserId, partnerId);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(partnerName != null ? partnerName : "Chat");
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage  = findViewById(R.id.etMessage);
        fabSend    = findViewById(R.id.fabSend);
        fabAttach  = findViewById(R.id.fabAttach);

        // Permission launcher — for Android 6+
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                perms -> {
                    boolean allGranted = !perms.containsValue(false);
                    if (allGranted) {
                        showAttachmentPicker();
                    } else {
                        Toast.makeText(this, "Storage permission is needed to attach files", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) uploadFileAndSend(uri, "image"); }
        );

        // File picker launcher (supports any MIME)
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) uploadFileAndSend(uri, "file"); }
        );

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        adapter = new ChatAdapter(messages, currentUserId);
        rvMessages.setAdapter(adapter);

        listenForMessages();

        fabSend.setOnClickListener(v -> sendMessage());
        if (fabAttach != null) {
            fabAttach.setOnClickListener(v -> checkPermissionsAndAttach());
        }
    }

    // ── Permission check ────────────────────────────────────────────────────────

    private void checkPermissionsAndAttach() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: READ_MEDIA_IMAGES replaces READ_EXTERNAL_STORAGE
            String perm = Manifest.permission.READ_MEDIA_IMAGES;
            if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
                showAttachmentPicker();
            } else {
                permissionLauncher.launch(new String[]{perm});
            }
        } else {
            // Android 6–12
            String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
                showAttachmentPicker();
            } else {
                permissionLauncher.launch(new String[]{perm});
            }
        }
    }

    // ── Attachment picker dialog ─────────────────────────────────────────────────

    private void showAttachmentPicker() {
        String[] options = {"📷 Image from Gallery", "📄 Document / File"};
        new AlertDialog.Builder(this)
                .setTitle("Attach")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        imagePickerLauncher.launch("image/*");
                    } else {
                        // Allow any document type
                        filePickerLauncher.launch(new String[]{"*/*"});
                    }
                })
                .show();
    }

    // ── Chat messages ──────────────────────────────────────────────────────────

    private void listenForMessages() {
        repo.listenMessages(chatId, (type, message) -> {
            if (type == DocumentChange.Type.ADDED) {
                messages.add(message);
                adapter.notifyItemInserted(messages.size() - 1);
                rvMessages.scrollToPosition(messages.size() - 1);
            }
        });
    }

    private void sendMessage() {
        if (etMessage == null) return;
        String text = etMessage.getText() != null
                ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        Message msg = new Message(chatId, currentUserId, currentUserName, text);
        etMessage.setText("");

        repo.sendMessage(msg, partnerName != null ? partnerName : "User", new FirebaseRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) { /* message appears via listener */ }
            @Override public void onError(String e) {
                etMessage.setError("Failed to send");
            }
        });
    }

    private void uploadFileAndSend(Uri fileUri, String type) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        // Grant persistent read permission (important for non-image files)
        try {
            getContentResolver().takePersistableUriPermission(fileUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Not all URIs support persistable permissions — that's OK
        }

        repo.uploadChatImage(fileUri, chatId, new FirebaseRepository.Callback<String>() {
            @Override
            public void onSuccess(String fileUrl) {
                Message msg = new Message(chatId, currentUserId, currentUserName, "");
                msg.setImageUrl(fileUrl);
                repo.sendMessage(msg, partnerName != null ? partnerName : "User", new FirebaseRepository.Callback<Void>() {
                    @Override public void onSuccess(Void v) { }
                    @Override public void onError(String e) {
                        Toast.makeText(ChatActivity.this, "Failed to send file", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ChatActivity.this, "Upload failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Deterministic chat ID using sorted UIDs */
    public static String buildChatId(String uid1, String uid2) {
        if (uid1 == null) uid1 = "";
        if (uid2 == null) uid2 = "";
        return uid1.compareTo(uid2) < 0
                ? uid1 + "_" + uid2
                : uid2 + "_" + uid1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repo.removeMessageListener(chatId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
