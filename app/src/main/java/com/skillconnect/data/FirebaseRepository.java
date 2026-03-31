package com.skillconnect.data;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.skillconnect.models.Bid;
import com.skillconnect.models.Booking;
import com.skillconnect.models.JobPost;
import com.skillconnect.models.Message;
import com.skillconnect.models.Provider;
import com.skillconnect.models.Review;
import com.skillconnect.models.Skill;
import com.skillconnect.models.User;
import com.skillconnect.models.ChatThread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Firebase Firestore data layer — replaces SQLite DatabaseHelper entirely.
 * All methods are async (no background threads needed — Firebase handles it internally).
 *
 * Collections:
 *   users/{uid}      — user profile documents
 *   skills/{id}      — skill listing documents
 *   bookings/{id}    — booking documents
 *   reviews/{id}     — review documents
 */
public class FirebaseRepository {

    @FunctionalInterface
    public interface Callback<T> {
        void onSuccess(T result);
        /** Override for error handling. Default: silently ignored. */
        default void onError(String error) {}
    }

    private static final String COL_USERS    = "users";
    private static final String COL_SKILLS   = "skills";
    private static final String COL_BOOKINGS = "bookings";
    private static final String COL_REVIEWS  = "reviews";
    private static final String COL_CHATS    = "chats";
    private static final String COL_JOBS     = "jobs";
    private static final String COL_BIDS     = "bids";
    private static final String COL_PAYMENTS = "payments";

    /** Listener interface for real-time chat messages. */
    public interface MessageListener {
        void onMessage(DocumentChange.Type type, Message message);
    }

    private static FirebaseRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    /** Active Firestore snapshot listeners keyed by chatId so we can remove them. */
    private final Map<String, ListenerRegistration> chatListeners = new ConcurrentHashMap<>();

    private FirebaseRepository() {
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) instance = new FirebaseRepository();
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────
    // SECURITY — Input Sanitization
    // ─────────────────────────────────────────────────────────────────────

    /** Strip HTML tags, script content, and dangerous characters from user input */
    private static String sanitize(String input) {
        if (input == null) return "";
        return input
            .replaceAll("<script[^>]*>.*?</script>", "")   // Remove script blocks
            .replaceAll("<[^>]+>", "")                       // Remove all HTML tags
            .replaceAll("[<>\"';]", "")                      // Remove dangerous chars
            .trim();
    }

    /** Sanitize and enforce max length */
    private static String sanitize(String input, int maxLen) {
        String clean = sanitize(input);
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }

    // ─────────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────────

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void login(String email, String password, Callback<User> callback) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                FirebaseUser firebaseUser = result.getUser();
                if (firebaseUser == null) { callback.onError("Login failed"); return; }
                // Fetch profile from Firestore
                db.collection(COL_USERS).document(firebaseUser.getUid()).get()
                  .addOnSuccessListener(doc -> {
                      User user = docToUser(doc);
                      if (user != null) {
                          callback.onSuccess(user);
                      } else {
                          callback.onError("User profile not found");
                      }
                  })
                  .addOnFailureListener(e -> callback.onError(e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void register(String name, String email, String password, String role,
                         Callback<User> callback) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                FirebaseUser firebaseUser = result.getUser();
                if (firebaseUser == null) { callback.onError("Registration failed"); return; }
                String uid = firebaseUser.getUid();

                // Write user profile to Firestore
                Map<String, Object> userDoc = new HashMap<>();
                userDoc.put("id",        uid);
                userDoc.put("name",      name);
                userDoc.put("email",     email);
                userDoc.put("role",      role);
                userDoc.put("phone",     "");
                userDoc.put("createdAt", System.currentTimeMillis());

                db.collection(COL_USERS).document(uid).set(userDoc)
                  .addOnSuccessListener(v -> {
                      // Seed initial skills if first provider registration
                      seedSkillsIfNeeded();
                      User user = new User(name, email, role);
                      user.setId(uid);
                      callback.onSuccess(user);
                  })
                  .addOnFailureListener(e -> callback.onError(e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void logout() {
        auth.signOut();
    }

    // ─────────────────────────────────────────────────────────────────────
    // USER
    // ─────────────────────────────────────────────────────────────────────

    public void getUserById(String uid, Callback<User> callback) {
        db.collection(COL_USERS).document(uid).get()
          .addOnSuccessListener(doc -> {
              User user = docToUser(doc);
              if (user != null) callback.onSuccess(user);
              else callback.onError("User not found");
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateUser(String uid, String name, String email, String phone,
                           Callback<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name",  name);
        updates.put("email", email);
        updates.put("phone", phone);
        db.collection(COL_USERS).document(uid).update(updates)
          .addOnSuccessListener(v -> callback.onSuccess(true))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────
    // SKILLS
    // ─────────────────────────────────────────────────────────────────────

    public void getAllSkills(String sortBy, Callback<List<Skill>> callback) {
        buildSkillQuery(null, sortBy)
          .get()
          .addOnSuccessListener(snap -> callback.onSuccess(docsToSkills(snap)))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getSkillsByCategory(String category, String sortBy, Callback<List<Skill>> callback) {
        // Fetch all skills sorted appropriately, then filter category client-side
        // This avoids requiring a Firestore composite index which was causing "Error loading skills"
        buildSkillQuery(null, sortBy)
          .get()
          .addOnSuccessListener(snap -> {
              List<Skill> results = new ArrayList<>();
              for (Skill s : docsToSkills(snap)) {
                  if (category != null && category.equalsIgnoreCase(s.getCategoryName())) {
                      results.add(s);
                  }
              }
              callback.onSuccess(results);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void searchSkills(String query, Callback<List<Skill>> callback) {
        // Firestore doesn't support contains-search natively — fetch all and filter client-side
        db.collection(COL_SKILLS).orderBy("rating", Query.Direction.DESCENDING).get()
          .addOnSuccessListener(snap -> {
              String lower = query.toLowerCase();
              List<Skill> results = new ArrayList<>();
              for (Skill s : docsToSkills(snap)) {
                  if (s.getTitle().toLowerCase().contains(lower)
                   || s.getCategoryName().toLowerCase().contains(lower)
                   || s.getDescription().toLowerCase().contains(lower)) {
                      results.add(s);
                  }
              }
              callback.onSuccess(results);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getSkillsByProviderId(String providerId, Callback<List<Skill>> callback) {
        db.collection(COL_SKILLS)
          .whereEqualTo("providerId", providerId)
          .get()
          .addOnSuccessListener(snap -> callback.onSuccess(docsToSkills(snap)))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getFeaturedProviders(Callback<List<Provider>> callback) {
        db.collection(COL_SKILLS)
          .orderBy("rating", Query.Direction.DESCENDING)
          .limit(20)
          .get()
          .addOnSuccessListener(snap -> {
              // Aggregate by providerId to get unique top providers
              Map<String, float[]> providerData = new HashMap<>();
              Map<String, String[]> providerInfo = new HashMap<>();
              for (DocumentSnapshot doc : snap.getDocuments()) {
                  String pid  = getString(doc, "providerId");
                  String pname= getString(doc, "providerName");
                  String cat  = getString(doc, "category");
                  float rating= getFloat(doc, "rating");
                  if (pid == null || pid.isEmpty()) continue;
                  if (!providerData.containsKey(pid)) {
                      providerData.put(pid, new float[]{rating, 1});
                      providerInfo.put(pid, new String[]{pname, cat});
                  }
              }
              List<Provider> providers = new ArrayList<>();
              for (Map.Entry<String, float[]> e : providerData.entrySet()) {
                  String pid = e.getKey();
                  float avg  = e.getValue()[0];
                  String name = providerInfo.get(pid)[0];
                  String spec = providerInfo.get(pid)[1];
                  providers.add(new Provider(0, pid, name, spec, avg));
              }
              callback.onSuccess(providers);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getSkillById(String documentId, Callback<Skill> callback) {
        db.collection(COL_SKILLS).document(documentId).get()
          .addOnSuccessListener(doc -> {
              Skill skill = docToSkill(doc);
              if (skill != null) callback.onSuccess(skill);
              else callback.onError("Skill not found");
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addSkill(Skill skill, Callback<String> callback) {
        Map<String, Object> doc = skillToMap(skill);
        db.collection(COL_SKILLS).add(doc)
          .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateSkill(Skill skill, Callback<Boolean> callback) {
        if (skill.getDocumentId() == null) { callback.onError("No document ID"); return; }
        Map<String, Object> updates = new HashMap<>();
        updates.put("title",       skill.getTitle());
        updates.put("description", skill.getDescription());
        updates.put("price",      skill.getPrice());
        updates.put("category",   skill.getCategoryName());
        db.collection(COL_SKILLS).document(skill.getDocumentId()).update(updates)
          .addOnSuccessListener(v -> callback.onSuccess(true))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteSkill(String documentId, Callback<Boolean> callback) {
        db.collection(COL_SKILLS).document(documentId).delete()
          .addOnSuccessListener(v -> callback.onSuccess(true))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────
    // BOOKINGS
    // ─────────────────────────────────────────────────────────────────────

    public void createBooking(Booking booking, Callback<String> callback) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("userId",       booking.getUserId());
        doc.put("providerId",   booking.getProviderId());
        doc.put("skillId",      booking.getSkillId());
        doc.put("skillTitle",   booking.getSkillTitle());
        doc.put("providerName", booking.getProviderName());
        doc.put("price",        booking.getPrice());
        doc.put("status",       booking.getStatus());
        doc.put("bookingDate",  booking.getBookingDate());
        doc.put("notes",        booking.getNotes() != null ? booking.getNotes() : "");
        db.collection(COL_BOOKINGS).add(doc)
          .addOnSuccessListener(ref -> {
              com.skillconnect.models.Notification notif = new com.skillconnect.models.Notification(
                      booking.getProviderId(),
                      "provider",
                      "booking_received",
                      "New Booking Request",
                      "You have a new booking request for " + booking.getSkillTitle() + ".",
                      ref.getId()
              );
              notif.setActionRoute("bookings");
              createNotification(notif, null);
              callback.onSuccess(ref.getId());
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserBookings(String userId, Callback<List<Booking>> callback) {
        db.collection(COL_BOOKINGS)
          .whereEqualTo("userId", userId)
          .get()
          .addOnSuccessListener(snap -> {
              List<Booking> list = docsToBookings(snap);
              // Sort client-side — avoids composite index requirement
              list.sort((a, b) -> Long.compare(b.getBookingDate(), a.getBookingDate()));
              callback.onSuccess(list);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getProviderBookings(String providerId, Callback<List<Booking>> callback) {
        db.collection(COL_BOOKINGS)
          .whereEqualTo("providerId", providerId)
          .get()
          .addOnSuccessListener(snap -> {
              List<Booking> list = docsToBookings(snap);
              // Sort client-side — avoids composite index requirement
              list.sort((a, b) -> Long.compare(b.getBookingDate(), a.getBookingDate()));
              callback.onSuccess(list);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateBookingStatus(String documentId, String status, Callback<Boolean> callback) {
        db.collection(COL_BOOKINGS).document(documentId)
          .update("status", status)
          .addOnSuccessListener(v -> callback.onSuccess(true))
          .addOnFailureListener(e -> {
              if (callback != null) callback.onError(e.getMessage());
          });
    }

    public void getProviderStats(String providerId, Callback<int[]> callback) {
        db.collection(COL_BOOKINGS).whereEqualTo("providerId", providerId).get()
          .addOnSuccessListener(snap -> {
              int pending  = 0, completed = 0;
              for (DocumentSnapshot doc : snap.getDocuments()) {
                  String st = getString(doc, "status");
                  if ("pending".equals(st) || "accepted".equals(st)) pending++;
                  if ("completed".equals(st)) completed++;
              }
              callback.onSuccess(new int[]{pending, completed});
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getProviderEarnings(String providerId, Callback<Double> callback) {
        db.collection(COL_BOOKINGS)
          .whereEqualTo("providerId", providerId)
          .whereEqualTo("status", "completed")
          .get()
          .addOnSuccessListener(snap -> {
              double total = 0;
              for (DocumentSnapshot doc : snap.getDocuments()) {
                  Double price = (Double) doc.get("price");
                  if (price != null) total += price;
              }
              callback.onSuccess(total);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────
    // REVIEWS
    // ─────────────────────────────────────────────────────────────────────

    public void hasReviewForBooking(String bookingId, Callback<Boolean> callback) {
        db.collection(COL_REVIEWS)
          .whereEqualTo("bookingId", bookingId)
          .limit(1).get()
          .addOnSuccessListener(snap -> callback.onSuccess(!snap.isEmpty()))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addReview(Review review, Callback<String> callback) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("userId",    review.getUserId());
        doc.put("skillId",   review.getSkillId());
        doc.put("bookingId", review.getBookingId());
        doc.put("rating",    review.getRating());
        doc.put("comment",   review.getComment());
        doc.put("userName",  review.getUserName());
        doc.put("createdAt", System.currentTimeMillis());
        db.collection(COL_REVIEWS).add(doc)
          .addOnSuccessListener(ref -> {
              // Update skill average rating
              updateSkillRatingInFirestore(review.getSkillId());
              callback.onSuccess(ref.getId());
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getSkillReviews(String skillId, Callback<List<Review>> callback) {
        db.collection(COL_REVIEWS)
          .whereEqualTo("skillId", skillId)
          .get()
          .addOnSuccessListener(snap -> {
              List<Review> reviews = new ArrayList<>();
              int idx = 0;
              for (DocumentSnapshot doc : snap.getDocuments()) {
                  Review r = new Review();
                  r.setDocumentId(doc.getId());
                  r.setUserId(getString(doc, "userId"));
                  r.setSkillId(getString(doc, "skillId"));
                  r.setBookingId(getString(doc, "bookingId"));
                  r.setRating(getFloat(doc, "rating"));
                  r.setComment(getString(doc, "comment"));
                  r.setUserName(getString(doc, "userName"));
                  r.setCreatedAt(getLong(doc, "createdAt"));
                  reviews.add(r);
              }
              callback.onSuccess(reviews);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEED DATA
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called after first registration — seeds demo skills if the collection is empty.
     */
    public void seedSkillsIfNeeded() {
        db.collection(COL_SKILLS).limit(1).get().addOnSuccessListener(snap -> {
            if (!snap.isEmpty()) return; // already seeded
            WriteBatch batch = db.batch();
            String[][] seeds = {
                {"Android App Development",
                 "Complete Android app development from scratch using Java/Kotlin, Material Design UI.",
                 "500", "Software Development", "4.9", "127"},
                {"Web Development (React)",
                 "Modern web application using React.js, Next.js with responsive design and API integration.",
                 "450", "Software Development", "4.8", "98"},
                {"UI/UX Design",
                 "Professional UI/UX design for mobile and web apps including wireframes and Figma prototypes.",
                 "350", "Creative & Design", "4.8", "156"},
                {"Data Science & ML",
                 "Machine learning model development, data analysis, and predictive analytics using Python.",
                 "600", "Software Development", "4.7", "89"},
                {"Digital Marketing",
                 "SEO, social media marketing, Google Ads campaign management, and analytics.",
                 "300", "Digital Marketing", "4.6", "201"},
                {"Python Programming Tutor",
                 "One-on-one Python lessons from beginner to advanced, covering OOP and frameworks.",
                 "200", "Education", "4.8", "312"},
                {"Logo & Brand Design",
                 "Professional logo design, brand identity kits with colour palettes and guidelines.",
                 "250", "Creative & Design", "4.9", "178"},
                {"Business Consulting",
                 "Business strategy consulting, market analysis, financial planning, and startup mentoring.",
                 "700", "Business & Remote IT", "4.3", "34"},
            };
            for (String[] s : seeds) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("title",        s[0]);
                doc.put("description",  s[1]);
                doc.put("price",        Double.parseDouble(s[2]));
                doc.put("category",     s[3]);
                doc.put("rating",       Float.parseFloat(s[4]));
                doc.put("reviewCount",  Integer.parseInt(s[5]));
                doc.put("providerId",   "demo");
                doc.put("providerName", "SkillConnect Expert");
                batch.set(db.collection(COL_SKILLS).document(), doc);
            }
            batch.commit();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPER MAPPING
    // ─────────────────────────────────────────────────────────────────────

    private Query buildSkillQuery(String category, String sortBy) {
        Query q = db.collection(COL_SKILLS);
        if (category != null && !category.isEmpty()) {
            q = ((com.google.firebase.firestore.CollectionReference) q)
                    .whereEqualTo("category", category);
        }
        String field = "rating";
        Query.Direction dir = Query.Direction.DESCENDING;
        if ("price_asc".equals(sortBy))  { field = "price"; dir = Query.Direction.ASCENDING; }
        if ("price_desc".equals(sortBy)) { field = "price"; dir = Query.Direction.DESCENDING; }
        return q.orderBy(field, dir);
    }

    private User docToUser(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        User u = new User();
        u.setId(doc.getId());
        u.setName(getString(doc, "name"));
        u.setEmail(getString(doc, "email"));
        u.setRole(getString(doc, "role"));
        u.setPhone(getString(doc, "phone"));
        u.setCreatedAt(getLong(doc, "createdAt"));
        return u;
    }

    private Skill docToSkill(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Skill s = new Skill();
        s.setDocumentId(doc.getId());
        s.setTitle(getString(doc, "title"));
        s.setDescription(getString(doc, "description"));
        s.setPrice(getDouble(doc, "price"));
        s.setProviderId(getString(doc, "providerId"));
        s.setProviderName(getString(doc, "providerName"));
        s.setRating(getFloat(doc, "rating"));
        s.setReviewCount(getInt(doc, "reviewCount"));
        s.setCategoryName(getString(doc, "category"));
        return s;
    }

    private List<Skill> docsToSkills(QuerySnapshot snap) {
        List<Skill> list = new ArrayList<>();
        int i = 0;
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Skill s = docToSkill(doc);
            if (s != null) { s.setId(i++); list.add(s); }
        }
        return list;
    }

    private Booking docToBooking(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Booking b = new Booking();
        b.setDocumentId(doc.getId());
        b.setUserId(getString(doc, "userId"));
        b.setProviderId(getString(doc, "providerId"));
        b.setSkillId(getString(doc, "skillId"));
        b.setSkillTitle(getString(doc, "skillTitle"));
        b.setProviderName(getString(doc, "providerName"));
        b.setPrice(getDouble(doc, "price"));
        b.setStatus(getString(doc, "status"));
        b.setBookingDate(getLong(doc, "bookingDate"));
        b.setNotes(getString(doc, "notes"));
        return b;
    }

    private List<Booking> docsToBookings(QuerySnapshot snap) {
        List<Booking> list = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Booking b = docToBooking(doc);
            if (b != null) list.add(b);
        }
        return list;
    }

    private Map<String, Object> skillToMap(Skill skill) {
        Map<String, Object> m = new HashMap<>();
        m.put("title",        skill.getTitle());
        m.put("description",  skill.getDescription());
        m.put("price",        skill.getPrice());
        m.put("category",     skill.getCategoryName());
        m.put("providerId",   skill.getProviderId());
        m.put("providerName", skill.getProviderName());
        m.put("rating",       skill.getRating());
        m.put("reviewCount",  skill.getReviewCount());
        return m;
    }

    private void updateSkillRatingInFirestore(String skillId) {
        db.collection(COL_REVIEWS).whereEqualTo("skillId", skillId).get()
          .addOnSuccessListener(snap -> {
              if (snap.isEmpty()) return;
              float total = 0;
              for (DocumentSnapshot r : snap.getDocuments()) total += getFloat(r, "rating");
              float avg = total / snap.size();
              Map<String, Object> u = new HashMap<>();
              u.put("rating",      avg);
              u.put("reviewCount", snap.size());
              db.collection(COL_SKILLS).document(skillId).update(u);
          });
    }

    // ── Jobs / Bids (Phase 2) ─────────────────────────────────────────────────────

    public void createJobPost(JobPost job, Callback<String> callback) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("customerId",      job.getCustomerId());
        doc.put("customerName",    sanitize(job.getCustomerName(), 100));
        doc.put("title",           sanitize(job.getTitle(), 200));
        doc.put("description",     sanitize(job.getDescription(), 2000));
        doc.put("category",        sanitize(job.getCategory(), 100));
        doc.put("budget",          job.getBudget());
        doc.put("status",          job.getStatus());
        doc.put("timestamp",       job.getTimestamp());
        doc.put("deadline",        job.getDeadline());
        doc.put("imageUrl",        job.getImageUrl()        != null ? job.getImageUrl()        : "");
        doc.put("attachmentUrl",   job.getAttachmentUrl()   != null ? job.getAttachmentUrl()   : "");
        doc.put("attachmentName",  job.getAttachmentName()  != null ? job.getAttachmentName()  : "");

        db.collection(COL_JOBS).add(doc)
          .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Job file/image upload ───────────────────────────────────────────────────

    /** Upload a job cover image to Cloudinary, return secure URL */
    public void uploadJobImage(Uri imageUri, Callback<String> callback) {
        uploadToCloudinary(imageUri, callback);
    }

    /** Upload a job attachment file to Cloudinary, return secure URL */
    public void uploadJobFile(Uri fileUri, Callback<String> callback) {
        uploadToCloudinary(fileUri, callback);
    }

    /** Shared helper: upload any file to Cloudinary via REST API and return secure URL */
    private void uploadToCloudinary(Uri fileUri, Callback<String> callback) {
        new Thread(() -> {
            try {
                android.content.Context ctx = com.skillconnect.SkillConnectApp.getContext();
                android.content.ContentResolver cr = ctx.getContentResolver();
                String mimeType = cr.getType(fileUri);
                
                String resourceType = "auto";
                String extension = "";
                if (mimeType != null) {
                    if (mimeType.startsWith("image/")) resourceType = "image";
                    else if (mimeType.startsWith("video/")) resourceType = "video";
                    else resourceType = "raw";

                    if (mimeType.startsWith("image/jpeg") || mimeType.startsWith("image/jpg")) extension = ".jpg";
                    else if (mimeType.startsWith("image/png"))  extension = ".png";
                    else if (mimeType.startsWith("image/gif"))  extension = ".gif";
                    else if (mimeType.startsWith("image/webp")) extension = ".webp";
                    else if (mimeType.startsWith("application/pdf")) extension = ".pdf";
                    else if (mimeType.contains("wordprocessingml") || mimeType.equals("application/msword")) extension = ".docx";
                    else if (mimeType.contains("spreadsheetml") || mimeType.equals("application/vnd.ms-excel")) extension = ".xlsx";
                    else if (mimeType.startsWith("text/plain")) extension = ".txt";
                }

                // Read all bytes
                java.io.InputStream is = cr.openInputStream(fileUri);
                if (is == null) throw new java.io.FileNotFoundException("Could not open URI");
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                int nRead;
                byte[] buf = new byte[16384];
                while ((nRead = is.read(buf, 0, buf.length)) != -1) buffer.write(buf, 0, nRead);
                byte[] fileBytes = buffer.toByteArray();
                is.close();

                if (fileBytes.length == 0) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError("File is empty or unreadable."));
                    return;
                }

                // Cloudinary HTTP Upload
                String urlStr = "https://api.cloudinary.com/v1_1/dwu4svwho/" + resourceType + "/upload";
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                String boundary = "----CloudinaryFormBoundary" + System.currentTimeMillis();
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                
                java.io.DataOutputStream dos = new java.io.DataOutputStream(conn.getOutputStream());
                
                // Upload preset field
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes("skillconnect_upload\r\n");
                
                // File data field
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"upload_file" + extension + "\"\r\n");
                dos.writeBytes("Content-Type: " + (mimeType != null ? mimeType : "application/octet-stream") + "\r\n\r\n");
                dos.write(fileBytes);
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();
                
                // Read response
                int responseCode = conn.getResponseCode();
                java.io.InputStream in = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
                java.io.ByteArrayOutputStream respBuffer = new java.io.ByteArrayOutputStream();
                while ((nRead = in.read(buf, 0, buf.length)) != -1) respBuffer.write(buf, 0, nRead);
                String response = new String(respBuffer.toByteArray());
                
                if (responseCode >= 200 && responseCode < 300) {
                    org.json.JSONObject json = new org.json.JSONObject(response);
                    String secureUrl = json.getString("secure_url");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(secureUrl));
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError("Cloudinary error: " + response));
                }
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError("Upload failed: " + e.getMessage()));
            }
        }).start();
    }

    public void getAllJobPosts(Callback<List<JobPost>> callback) {
        db.collection(COL_JOBS)
          .get()
          .addOnSuccessListener(snap -> {
              List<JobPost> jobs = docsToJobs(snap);
              // Sort by timestamp descending client-side (avoids composite index requirement)
              jobs.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
              callback.onSuccess(jobs);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getCustomerJobPosts(String customerId, Callback<List<JobPost>> callback) {
        db.collection(COL_JOBS)
          .whereEqualTo("customerId", customerId)
          .get()
          .addOnSuccessListener(snap -> {
              List<JobPost> jobs = docsToJobs(snap);
              // Sort client-side to avoid composite index requirement
              jobs.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
              callback.onSuccess(jobs);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createBid(Bid bid, Callback<String> callback) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("jobId",        bid.getJobId());
        doc.put("providerId",   bid.getProviderId());
        doc.put("providerName", sanitize(bid.getProviderName(), 100));
        doc.put("bidAmount",    bid.getBidAmount());
        doc.put("proposal",     sanitize(bid.getProposal(), 2000));
        doc.put("status",       bid.getStatus());
        doc.put("timestamp",    bid.getTimestamp());

        db.collection(COL_BIDS).add(doc)
          .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getBidsForJob(String jobId, Callback<List<Bid>> callback) {
        db.collection(COL_BIDS)
          .whereEqualTo("jobId", jobId)
          .get()
          .addOnSuccessListener(snap -> {
              List<Bid> bids = docsToBids(snap);
              bids.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
              callback.onSuccess(bids);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public com.google.firebase.firestore.ListenerRegistration listenBidsForJob(String jobId, Callback<List<Bid>> callback) {
        return db.collection(COL_BIDS)
          .whereEqualTo("jobId", jobId)
          .addSnapshotListener((snap, err) -> {
              if (err != null) {
                  callback.onError(err.getMessage());
                  return;
              }
              if (snap != null) {
                  List<Bid> bids = docsToBids(snap);
                  bids.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                  callback.onSuccess(bids);
              }
          });
    }
    /** Transaction: Accept Bid -> Update Job to 'awarded' -> Create Booking */
    public void acceptBidAndCreateBooking(String jobId, Bid winningBid, Callback<String> callback) {
        db.runTransaction(transaction -> {
            // ── READS FIRST (Firestore rule: all reads must come before writes) ──
            DocumentSnapshot jobDoc = transaction.get(db.collection(COL_JOBS).document(jobId));
            String customerId   = getString(jobDoc, "customerId");
            String customerName = getString(jobDoc, "customerName");

            // ── WRITES SECOND ───────────────────────────────────────────────────
            // 1. Mark job as awarded
            transaction.update(db.collection(COL_JOBS).document(jobId), "status", "awarded");

            // 2. Mark winning bid as accepted
            if (winningBid.getDocumentId() != null && !winningBid.getDocumentId().isEmpty()) {
                transaction.update(db.collection(COL_BIDS).document(winningBid.getDocumentId()),
                        "status", "accepted");
            }

            // 3. Create booking document
            String newBookingId = db.collection(COL_BOOKINGS).document().getId();
            Map<String, Object> booking = new HashMap<>();
            booking.put("userId",        customerId);
            booking.put("userName",      customerName);
            booking.put("providerId",    winningBid.getProviderId());
            booking.put("providerName",  winningBid.getProviderName());
            booking.put("skillTitle",    getString(jobDoc, "title"));
            booking.put("price",         winningBid.getBidAmount());
            booking.put("status",        "pending");
            booking.put("paymentStatus", "unpaid");
            booking.put("bookingDate",   System.currentTimeMillis());
            booking.put("notes",         "Job Awarded. Proposal: " + winningBid.getProposal());
            transaction.set(db.collection(COL_BOOKINGS).document(newBookingId), booking);

            // Return the data we need post-transaction
            Map<String, Object> result = new HashMap<>();
            result.put("bookingId",    newBookingId);
            result.put("customerId",   customerId);
            result.put("customerName", customerName);
            return result;
        })
        .addOnSuccessListener(resultObj -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = (Map<String, Object>) resultObj;
            String bookingId   = (String) res.get("bookingId");
            String customerId  = (String) res.get("customerId");
            String customerName = (String) res.get("customerName");

            // Initialize chat thread so it appears in inbox immediately
            initializeChatThread(customerId, customerName,
                    winningBid.getProviderId(), winningBid.getProviderName());

            // Create notification for Provider
            com.skillconnect.models.Notification notif = new com.skillconnect.models.Notification(
                    winningBid.getProviderId(),
                    "provider",
                    "bid_accepted",
                    "Bid Accepted!",
                    "Your bid for '" + winningBid.getProposal().substring(0, Math.min(20, winningBid.getProposal().length())) + "...' was accepted by " + customerName + ".",
                    bookingId
            );
            notif.setActionRoute("bookings");
            createNotification(notif, null);

            callback.onSuccess(bookingId);
        })
        .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }


    private void initializeChatThread(String customerId, String customerName, String providerId, String providerName) {
        String chatId = com.skillconnect.ChatActivity.buildChatId(customerId, providerId);
        long now = System.currentTimeMillis();

        // Customer sees Provider
        Map<String, Object> t1 = new HashMap<>();
        t1.put("partnerId", providerId);
        t1.put("partnerName", providerName);
        t1.put("lastMessage", "Service allotted. Chat initialized.");
        t1.put("lastTimestamp", now);
        t1.put("chatId", chatId);
        db.collection("user_chats").document(customerId).collection("threads").document(providerId).set(t1);

        // Provider sees Customer
        Map<String, Object> t2 = new HashMap<>();
        t2.put("partnerId", customerId);
        t2.put("partnerName", customerName);
        t2.put("lastMessage", "Service allotted. Chat initialized.");
        t2.put("lastTimestamp", now);
        t2.put("chatId", chatId);
        db.collection("user_chats").document(providerId).collection("threads").document(customerId).set(t2);
    }

    // ── Payments (Phase 3B) ────────────────────────────────────────────────────────

    public void createPaymentRecord(Map<String, Object> doc, Callback<String> callback) {
        db.collection(COL_PAYMENTS).add(doc)
          .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateBookingPaymentStatus(String bookingId, String razorpayPaymentId, Callback<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentStatus",    "paid");
        updates.put("razorpayPaymentId", razorpayPaymentId);

        db.collection(COL_BOOKINGS).document(bookingId).update(updates)
          .addOnSuccessListener(v -> callback.onSuccess(true))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getPaymentHistory(String userId, Callback<List<Map<String, Object>>> callback) {
        // Get payments where user is customer or provider
        db.collection(COL_PAYMENTS)
          .whereEqualTo("customerId", userId)
          .orderBy("timestamp", Query.Direction.DESCENDING)
          .get()
          .addOnSuccessListener(snap -> {
              List<Map<String, Object>> list = new ArrayList<>();
              for (DocumentSnapshot doc : snap.getDocuments()) {
                  Map<String, Object> p = new HashMap<>(doc.getData());
                  p.put("documentId", doc.getId());
                  list.add(p);
              }
              callback.onSuccess(list);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Disputes & Verification (Phase 5) ──────────────────────────────────────

    public void createDispute(Map<String, Object> dispute, Callback<String> callback) {
        db.collection("disputes").add(dispute)
          .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void requestVerification(String providerId, Map<String, Object> data, Callback<Boolean> callback) {
        data.put("providerId", providerId);
        data.put("status", "pending");
        data.put("timestamp", System.currentTimeMillis());
        db.collection("verification_requests").add(data)
          .addOnSuccessListener(ref -> {
              // Also mark user as "verification_pending"
              db.collection(COL_USERS).document(providerId)
                .update("verificationStatus", "pending")
                .addOnSuccessListener(v -> callback.onSuccess(true))
                .addOnFailureListener(e -> callback.onSuccess(true)); // still succeed
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getVerificationStatus(String providerId, Callback<String> callback) {
        db.collection(COL_USERS).document(providerId).get()
          .addOnSuccessListener(doc -> {
              String status = doc.getString("verificationStatus");
              callback.onSuccess(status != null ? status : "unverified");
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private List<JobPost> docsToJobs(QuerySnapshot snap) {
        List<JobPost> list = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            JobPost j = new JobPost();
            j.setDocumentId(doc.getId());
            j.setCustomerId(getString(doc, "customerId"));
            j.setCustomerName(getString(doc, "customerName"));
            j.setTitle(getString(doc, "title"));
            j.setDescription(getString(doc, "description"));
            j.setCategory(getString(doc, "category"));
            j.setBudget(getDouble(doc, "budget"));
            j.setStatus(getString(doc, "status"));
            j.setTimestamp(getLong(doc, "timestamp"));
            j.setDeadline(getLong(doc, "deadline"));
            j.setImageUrl(getString(doc, "imageUrl"));
            j.setAttachmentUrl(getString(doc, "attachmentUrl"));
            j.setAttachmentName(getString(doc, "attachmentName"));
            list.add(j);
        }
        return list;
    }

    private List<Bid> docsToBids(QuerySnapshot snap) {
        List<Bid> list = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Bid b = new Bid();
            b.setDocumentId(doc.getId());
            b.setJobId(getString(doc, "jobId"));
            b.setProviderId(getString(doc, "providerId"));
            b.setProviderName(getString(doc, "providerName"));
            b.setBidAmount(getDouble(doc, "bidAmount"));
            b.setProposal(getString(doc, "proposal"));
            b.setStatus(getString(doc, "status"));
            b.setTimestamp(getLong(doc, "timestamp"));
            list.add(b);
        }
        return list;
    }

    // ── Chat / Messaging ──────────────────────────────────────────────────────────

    /** Upload any file (image or document) to Cloudinary and return download URL */
    public void uploadChatImage(android.net.Uri fileUri, String chatId, Callback<String> callback) {
        uploadToCloudinary(fileUri, callback);
    }

    /** Write a message to chats/{chatId}/messages and update thread metadata for both users */
    public void sendMessage(Message message, String partnerName, Callback<Void> callback) {
        Map<String, Object> m = new HashMap<>();
        m.put("chatId",     message.getChatId());
        m.put("senderId",   message.getSenderId());
        m.put("senderName", message.getSenderName());
        m.put("text",       message.getText());
        m.put("imageUrl",   message.getImageUrl() != null ? message.getImageUrl() : "");
        m.put("timestamp",  message.getTimestamp());

        // 1. Save message
        db.collection(COL_CHATS)
          .document(message.getChatId())
          .collection("messages")
          .add(m)
          .addOnSuccessListener(r -> {
              // 2. Update thread metadata for both participants
              updateThreadMetadata(message, partnerName);
              callback.onSuccess(null);
          })
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void updateThreadMetadata(Message msg, String partnerName) {
        String myId = msg.getSenderId();
        String myName = msg.getSenderName();
        String chatId = msg.getChatId();
        String[] participants = chatId.split("_");
        String partnerId = participants[0].equals(myId) ? participants[1] : participants[0];

        String lastText = (msg.getText() != null && !msg.getText().isEmpty()) ? msg.getText() : "Image attachment";

        // Update for ME
        Map<String, Object> myThread = new HashMap<>();
        myThread.put("partnerId", partnerId);
        myThread.put("partnerName", partnerName);
        myThread.put("lastMessage", lastText);
        myThread.put("lastTimestamp", msg.getTimestamp());
        myThread.put("chatId", chatId);

        db.collection("user_chats").document(myId)
          .collection("threads").document(partnerId).set(myThread);

        // Update for PARTNER
        Map<String, Object> partnerThread = new HashMap<>();
        partnerThread.put("partnerId", myId);
        partnerThread.put("partnerName", myName);
        partnerThread.put("lastMessage", lastText);
        partnerThread.put("lastTimestamp", msg.getTimestamp());
        partnerThread.put("chatId", chatId);

        db.collection("user_chats").document(partnerId)
          .collection("threads").document(myId).set(partnerThread);
    }

    public com.google.firebase.firestore.ListenerRegistration listenChatThreads(String userId, Callback<List<ChatThread>> callback) {
        return db.collection("user_chats").document(userId)
          .collection("threads")
          // No orderBy — avoids composite index requirement on fresh installs
          .addSnapshotListener((snap, err) -> {
              if (err != null) {
                  // PERMISSION_DENIED or missing collection: just return empty list
                  // so the UI shows "No conversations yet" instead of an error
                  callback.onSuccess(new ArrayList<>());
                  return;
              }
              List<ChatThread> threads = new ArrayList<>();
              if (snap != null) {
                  for (DocumentSnapshot doc : snap.getDocuments()) {
                      ChatThread t = new ChatThread();
                      t.setId(getString(doc, "chatId"));
                      t.setPartnerId(getString(doc, "partnerId"));
                      t.setPartnerName(getString(doc, "partnerName"));
                      t.setLastMessage(getString(doc, "lastMessage"));
                      t.setLastTimestamp(getLong(doc, "lastTimestamp"));
                      threads.add(t);
                  }
                  // Sort by lastTimestamp descending (newest first) client-side
                  threads.sort((a, b) -> Long.compare(b.getLastTimestamp(), a.getLastTimestamp()));
              }
              callback.onSuccess(threads);
          });
    }

    /** Real-time listener for new messages in a chat thread. */
    public void listenMessages(String chatId, MessageListener listener) {
        ListenerRegistration reg = db.collection(COL_CHATS)
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        DocumentSnapshot d = dc.getDocument();
                        Message msg = new Message(
                                getString(d, "chatId"),
                                getString(d, "senderId"),
                                getString(d, "senderName"),
                                getString(d, "text"));
                        msg.setId(d.getId());
                        msg.setImageUrl(getString(d, "imageUrl"));
                        msg.setTimestamp(getLong(d, "timestamp"));
                        listener.onMessage(dc.getType(), msg);
                    }
                });
        chatListeners.put(chatId, reg);
    }

    /** Remove the Firestore snapshot listener when chat screen is closed. */
    public void removeMessageListener(String chatId) {
        ListenerRegistration reg = chatListeners.remove(chatId);
        if (reg != null) reg.remove();
    }

    // ── Field helpers ─────────────────────────────────────────────────────────────
    private String  getString(DocumentSnapshot d, String k) { Object v = d.get(k); return v != null ? v.toString() : ""; }
    private double  getDouble(DocumentSnapshot d, String k) { Double  v = d.getDouble(k); return v != null ? v : 0.0; }
    private float   getFloat (DocumentSnapshot d, String k) { Double  v = d.getDouble(k); return v != null ? v.floatValue() : 0f; }
    private int     getInt   (DocumentSnapshot d, String k) { Long    v = d.getLong(k);   return v != null ? v.intValue() : 0; }
    private long    getLong  (DocumentSnapshot d, String k) { Long    v = d.getLong(k);   return v != null ? v : 0L; }

    // ─────────────────────────────────────────────────────────────────────
    // NOTIFICATIONS
    // ─────────────────────────────────────────────────────────────────────

    private static final String COL_NOTIFICATIONS = "notifications";

    public void createNotification(com.skillconnect.models.Notification n, Callback<String> cb) {
        java.util.Map<String, Object> doc = new java.util.HashMap<>();
        doc.put("userId",      n.getUserId());
        doc.put("role",        n.getRole());
        doc.put("type",        n.getType());
        doc.put("title",       n.getTitle());
        doc.put("message",     n.getMessage());
        doc.put("relatedId",   n.getRelatedId() != null ? n.getRelatedId() : "");
        doc.put("isRead",      false);
        doc.put("createdAt",   n.getCreatedAt());
        doc.put("actionRoute", n.getActionRoute() != null ? n.getActionRoute() : "");
        db.collection(COL_NOTIFICATIONS).add(doc)
          .addOnSuccessListener(ref -> { if (cb != null) cb.onSuccess(ref.getId()); })
          .addOnFailureListener(e  -> { if (cb != null) cb.onError(e.getMessage()); });
    }

    public void getNotificationsForUser(String uid, Callback<java.util.List<com.skillconnect.models.Notification>> cb) {
        db.collection(COL_NOTIFICATIONS)
          .whereEqualTo("userId", uid)
          .get()
          .addOnSuccessListener(snap -> {
              java.util.List<com.skillconnect.models.Notification> list = new java.util.ArrayList<>();
              for (DocumentSnapshot d : snap.getDocuments()) {
                  com.skillconnect.models.Notification n = new com.skillconnect.models.Notification();
                  n.setId(d.getId());
                  n.setUserId(getString(d, "userId"));
                  n.setRole(getString(d, "role"));
                  n.setType(getString(d, "type"));
                  n.setTitle(getString(d, "title"));
                  n.setMessage(getString(d, "message"));
                  n.setRelatedId(getString(d, "relatedId"));
                  Boolean read = d.getBoolean("isRead");
                  n.setRead(read != null && read);
                  n.setCreatedAt(getLong(d, "createdAt"));
                  n.setActionRoute(getString(d, "actionRoute"));
                  list.add(n);
              }
              list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
              cb.onSuccess(list);
          })
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void getUnreadNotificationCount(String uid, Callback<Integer> cb) {
        db.collection(COL_NOTIFICATIONS)
          .whereEqualTo("userId", uid)
          .whereEqualTo("isRead", false)
          .get()
          .addOnSuccessListener(snap -> cb.onSuccess(snap.size()))
          .addOnFailureListener(e   -> cb.onError(e.getMessage()));
    }

    public void markNotificationRead(String notifId, Callback<Boolean> cb) {
        db.collection(COL_NOTIFICATIONS).document(notifId)
          .update("isRead", true)
          .addOnSuccessListener(v -> { if (cb != null) cb.onSuccess(true); })
          .addOnFailureListener(e -> { if (cb != null) cb.onError(e.getMessage()); });
    }

    public void markAllNotificationsRead(String uid, Callback<Boolean> cb) {
        db.collection(COL_NOTIFICATIONS)
          .whereEqualTo("userId", uid)
          .whereEqualTo("isRead", false)
          .get()
          .addOnSuccessListener(snap -> {
              if (snap.isEmpty()) { if (cb != null) cb.onSuccess(true); return; }
              WriteBatch batch = db.batch();
              for (DocumentSnapshot d : snap.getDocuments()) {
                  batch.update(d.getReference(), "isRead", true);
              }
              batch.commit()
                   .addOnSuccessListener(v -> { if (cb != null) cb.onSuccess(true); })
                   .addOnFailureListener(e -> { if (cb != null) cb.onError(e.getMessage()); });
          })
          .addOnFailureListener(e -> { if (cb != null) cb.onError(e.getMessage()); });
    }

    public void deleteNotification(String notifId, Callback<Boolean> cb) {
        db.collection(COL_NOTIFICATIONS).document(notifId).delete()
          .addOnSuccessListener(v -> { if (cb != null) cb.onSuccess(true); })
          .addOnFailureListener(e -> { if (cb != null) cb.onError(e.getMessage()); });
    }

    // ─────────────────────────────────────────────────────────────────────
    // WALLET
    // ─────────────────────────────────────────────────────────────────────

    private static final String COL_WALLETS = "wallets";

    public void getOrCreateWallet(String uid, Callback<com.skillconnect.models.Wallet> cb) {
        db.collection(COL_WALLETS).document(uid).get()
          .addOnSuccessListener(doc -> {
              com.skillconnect.models.Wallet w = new com.skillconnect.models.Wallet();
              w.setUserId(uid);
              if (doc.exists()) {
                  w.setBalance(getDouble(doc, "balance"));
                  w.setPendingBalance(getDouble(doc, "pendingBalance"));
                  w.setCurrency(getString(doc, "currency"));
                  w.setUpdatedAt(getLong(doc, "updatedAt"));
                  cb.onSuccess(w);
              } else {
                  // Create empty wallet
                  java.util.Map<String, Object> init = new java.util.HashMap<>();
                  init.put("balance", 0.0);
                  init.put("pendingBalance", 0.0);
                  init.put("currency", "INR");
                  init.put("updatedAt", System.currentTimeMillis());
                  db.collection(COL_WALLETS).document(uid).set(init)
                    .addOnSuccessListener(v -> cb.onSuccess(w))
                    .addOnFailureListener(e -> cb.onError(e.getMessage()));
              }
          })
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void updateWalletBalance(String uid, double newBalance, Callback<Boolean> cb) {
        java.util.Map<String, Object> upd = new java.util.HashMap<>();
        upd.put("balance", newBalance);
        upd.put("updatedAt", System.currentTimeMillis());
        db.collection(COL_WALLETS).document(uid).set(upd, com.google.firebase.firestore.SetOptions.merge())
          .addOnSuccessListener(v -> { if (cb != null) cb.onSuccess(true); })
          .addOnFailureListener(e -> { if (cb != null) cb.onError(e.getMessage()); });
    }

    public void topUpWallet(String uid, double amount, Callback<Boolean> cb) {
        getOrCreateWallet(uid, new Callback<com.skillconnect.models.Wallet>() {
            @Override public void onSuccess(com.skillconnect.models.Wallet w) {
                double newBalance = w.getBalance() + amount;
                updateWalletBalance(uid, newBalance, cb);
            }
            @Override public void onError(String e) { if (cb != null) cb.onError(e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // TRANSACTIONS
    // ─────────────────────────────────────────────────────────────────────

    private static final String COL_TRANSACTIONS = "transactions";

    public void createTransaction(com.skillconnect.models.Transaction t, Callback<String> cb) {
        java.util.Map<String, Object> doc = new java.util.HashMap<>();
        doc.put("userId",      t.getUserId());
        doc.put("bookingId",   t.getBookingId() != null ? t.getBookingId() : "");
        doc.put("type",        t.getType());
        doc.put("amount",      t.getAmount());
        doc.put("status",      t.getStatus());
        doc.put("method",      t.getMethod());
        doc.put("description", t.getDescription() != null ? t.getDescription() : "");
        doc.put("createdAt",   t.getCreatedAt());
        db.collection(COL_TRANSACTIONS).add(doc)
          .addOnSuccessListener(ref -> { if (cb != null) cb.onSuccess(ref.getId()); })
          .addOnFailureListener(e   -> { if (cb != null) cb.onError(e.getMessage()); });
    }

    public void getTransactionsForUser(String uid, Callback<java.util.List<com.skillconnect.models.Transaction>> cb) {
        db.collection(COL_TRANSACTIONS)
          .whereEqualTo("userId", uid)
          .get()
          .addOnSuccessListener(snap -> {
              java.util.List<com.skillconnect.models.Transaction> list = new java.util.ArrayList<>();
              for (DocumentSnapshot d : snap.getDocuments()) {
                  com.skillconnect.models.Transaction t = new com.skillconnect.models.Transaction();
                  t.setTransactionId(d.getId());
                  t.setUserId(getString(d, "userId"));
                  t.setBookingId(getString(d, "bookingId"));
                  t.setType(getString(d, "type"));
                  t.setAmount(getDouble(d, "amount"));
                  t.setStatus(getString(d, "status"));
                  t.setMethod(getString(d, "method"));
                  t.setDescription(getString(d, "description"));
                  t.setCreatedAt(getLong(d, "createdAt"));
                  list.add(t);
              }
              list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
              cb.onSuccess(list);
          })
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────
    // PROFILE IMAGE
    // ─────────────────────────────────────────────────────────────────────

    public void updateUserProfileImage(String uid, String imageUrl, Callback<Boolean> cb) {
        java.util.Map<String, Object> upd = new java.util.HashMap<>();
        upd.put("profileImageUrl", imageUrl);
        upd.put("updatedAt", System.currentTimeMillis());
        db.collection(COL_USERS).document(uid)
          .set(upd, com.google.firebase.firestore.SetOptions.merge())
          .addOnSuccessListener(v -> { if (cb != null) cb.onSuccess(true); })
          .addOnFailureListener(e -> { if (cb != null) cb.onError(e.getMessage()); });
    }

    // ─────────────────────────────────────────────────────────────────────
    // ADMIN ANALYTICS
    // ─────────────────────────────────────────────────────────────────────

    public void getAdminUserCount(Callback<int[]> cb) {
        db.collection(COL_USERS).get()
          .addOnSuccessListener(snap -> {
              int total = snap.size();
              int providers = 0;
              for (DocumentSnapshot d : snap.getDocuments()) {
                  if ("provider".equals(getString(d, "role"))) providers++;
              }
              cb.onSuccess(new int[]{total, providers});
          })
          .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void getAdminRevenue(Callback<Double> cb) {
        db.collection(COL_TRANSACTIONS)
          .whereEqualTo("status", "completed")
          .get()
          .addOnSuccessListener(snap -> {
              double total = 0;
              for (DocumentSnapshot d : snap.getDocuments()) {
                  total += getDouble(d, "amount");
              }
              final double result = total;
              if (result == 0) {
                  db.collection(COL_PAYMENTS).get()
                    .addOnSuccessListener(snap2 -> {
                        double t2 = 0;
                        for (DocumentSnapshot d : snap2.getDocuments()) {
                            String s = getString(d, "status");
                            if ("completed".equals(s) || "paid".equals(s)) t2 += getDouble(d, "amount");
                        }
                        cb.onSuccess(t2);
                    })
                    .addOnFailureListener(e2 -> cb.onSuccess(0.0));
              } else {
                  cb.onSuccess(result);
              }
          })
          .addOnFailureListener(e -> cb.onSuccess(0.0));
    }

    public void getAdminJobCompletionRate(Callback<double[]> cb) {
        db.collection(COL_BOOKINGS).get()
          .addOnSuccessListener(snap -> {
              double total = snap.size();
              double completed = 0;
              for (DocumentSnapshot d : snap.getDocuments()) {
                  String s = getString(d, "status");
                  if ("completed".equals(s) || "done".equals(s)) completed++;
              }
              double pct = total > 0 ? (completed / total) * 100.0 : 0;
              cb.onSuccess(new double[]{pct, completed, total});
          })
          .addOnFailureListener(e -> cb.onSuccess(new double[]{0, 0, 0}));
    }

    public void getAdminMonthlyGrowth(Callback<Map<String, Integer>> cb) {
        db.collection(COL_USERS).get()
          .addOnSuccessListener(snap -> {
              java.util.LinkedHashMap<String, Integer> monthly = new java.util.LinkedHashMap<>();
              String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
              for (int i = 5; i >= 0; i--) {
                  java.util.Calendar c = java.util.Calendar.getInstance();
                  c.set(java.util.Calendar.DAY_OF_MONTH, 1);
                  c.add(java.util.Calendar.MONTH, -i);
                  int m = c.get(java.util.Calendar.MONTH);
                  int y = c.get(java.util.Calendar.YEAR);
                  monthly.put(months[m] + " " + (y % 100), 0);
              }
              for (DocumentSnapshot d : snap.getDocuments()) {
                  long createdAt = getLong(d, "createdAt");
                  if (createdAt == 0) continue;
                  java.util.Calendar uc = java.util.Calendar.getInstance();
                  uc.setTimeInMillis(createdAt);
                  int um = uc.get(java.util.Calendar.MONTH);
                  int uy = uc.get(java.util.Calendar.YEAR);
                  String key = months[um] + " " + (uy % 100);
                  if (monthly.containsKey(key)) {
                      monthly.put(key, monthly.get(key) + 1);
                  }
              }
              cb.onSuccess(monthly);
          })
          .addOnFailureListener(e -> cb.onSuccess(new java.util.LinkedHashMap<>()));
    }

    public void getAdminBookingStats(Callback<Map<String, Integer>> cb) {
        db.collection(COL_BOOKINGS).get()
          .addOnSuccessListener(snap -> {
              java.util.LinkedHashMap<String, Integer> stats = new java.util.LinkedHashMap<>();
              for (DocumentSnapshot d : snap.getDocuments()) {
                  String status = getString(d, "status");
                  if (status == null || status.isEmpty()) status = "unknown";
                  stats.put(status, stats.getOrDefault(status, 0) + 1);
              }
              cb.onSuccess(stats);
          })
          .addOnFailureListener(e -> cb.onSuccess(new java.util.LinkedHashMap<>()));
    }
}
