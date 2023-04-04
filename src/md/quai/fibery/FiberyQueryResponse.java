package md.quai.fibery;

import com.google.gson.annotations.SerializedName;

public class FiberyQueryResponse {
    static class FiberyQueryResponse_WorkflowState {
        @SerializedName("fibery/id")
        String id;
        @SerializedName("enum/name")
        String name;
    }
    static class FiberyQueryResponse_Result {
        @SerializedName("fibery/public-id")
        String publicId;
        @SerializedName("fibery/id")
        String id;
        @SerializedName("workflow/state")
        FiberyQueryResponse_WorkflowState state;
    }
    boolean success;
    FiberyQueryResponse_Result[] result;
}
