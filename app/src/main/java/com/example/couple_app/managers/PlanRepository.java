package com.example.couple_app.managers;

import com.example.couple_app.models.Plan;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository quản lý tất cả các thao tác liên quan đến Plan trong Firestore
 */
public class PlanRepository {

    private static final String COLLECTION_COUPLE_PLANS = "couple_plans";
    private final FirebaseFirestore db;

    public PlanRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Thêm kế hoạch mới
     */
    public Task<DocumentReference> addPlan(Plan plan) {
        return db.collection(COLLECTION_COUPLE_PLANS).add(plan);
    }

    /**
     * Cập nhật nội dung kế hoạch
     */
    public Task<Void> updatePlanContent(String planId, String newContent) {
        return db.collection(COLLECTION_COUPLE_PLANS)
                .document(planId)
                .update("content", newContent);
    }

    /**
     * Xóa kế hoạch
     */
    public Task<Void> deletePlan(String planId) {
        return db.collection(COLLECTION_COUPLE_PLANS)
                .document(planId)
                .delete();
    }

    /**
     * Lấy danh sách kế hoạch theo coupleId và ngày
     */
    public Task<QuerySnapshot> getPlansByDate(String coupleId, String date) {
        return db.collection(COLLECTION_COUPLE_PLANS)
                .whereEqualTo("coupleId", coupleId)
                .whereEqualTo("date", date)
                .get();
    }

    /**
     * Lấy danh sách kế hoạch theo coupleId (tất cả các ngày)
     */
    public Task<QuerySnapshot> getAllPlans(String coupleId) {
        return db.collection(COLLECTION_COUPLE_PLANS)
                .whereEqualTo("coupleId", coupleId)
                .get();
    }

    /**
     * Chuyển đổi QuerySnapshot thành List<Plan>
     */
    public static List<Plan> convertToPlanList(QuerySnapshot querySnapshot) {
        List<Plan> plans = new ArrayList<>();
        if (querySnapshot != null) {
            querySnapshot.forEach(doc -> {
                Plan plan = doc.toObject(Plan.class);
                plan.setId(doc.getId());
                plans.add(plan);
            });
        }
        return plans;
    }
}

