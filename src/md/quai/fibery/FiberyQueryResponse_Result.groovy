import com.google.gson.annotations.SerializedName;

class FiberyQueryResponse_Result {
    @SerializedName("fibery/public-id")
    String publicId;
    @SerializedName("fibery/id")
    String id;
    @SerializedName("workflow/state")
    FiberyQueryResponse_WorkflowState state;
}